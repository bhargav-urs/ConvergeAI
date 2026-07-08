package com.convergeai.service.debate;

import com.convergeai.ai.ChatMessage;
import com.convergeai.ai.LlmClient;
import com.convergeai.ai.LlmEndpoint;
import com.convergeai.ai.LlmRouter;
import com.convergeai.domain.AgentName;
import com.convergeai.domain.DebateMode;
import com.convergeai.domain.AgentResponseEntity;
import com.convergeai.domain.AgentResponseStatus;
import com.convergeai.domain.ConsensusResultEntity;
import com.convergeai.domain.QuestionEntity;
import com.convergeai.domain.QuestionStatus;
import com.convergeai.dto.AgentMessageDto;
import com.convergeai.dto.ConsensusDto;
import com.convergeai.dto.ContextSnippetDto;
import com.convergeai.dto.DebateEvent;
import com.convergeai.repository.AgentResponseRepository;
import com.convergeai.repository.ConsensusResultRepository;
import com.convergeai.repository.QuestionRepository;
import com.convergeai.service.RetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Orchestrates the full debate lifecycle for one question:
 *
 * <pre>
 * Phase 2: retrieval        → context.retrieved
 * Phase 2: round 1 answers  → agent.response   (3 agents in parallel)
 * Phase 3: round 2 critique → agent.critique   (parallel)
 * Phase 4: round 3 revision → agent.revision   (parallel)
 * Phase 5: consensus        → consensus.generated
 * </pre>
 *
 * Failure policy: individual agent failures degrade the debate rather than
 * abort it. The pipeline only fails outright when no agent produces a round-1
 * answer, when retrieval finds nothing, or on unexpected errors.
 */
@Service
public class DebateOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DebateOrchestrator.class);

    /**
     * Fast mode caps agent answers via a smaller token budget. Consensus keeps
     * the full default budget: reasoning models burn tokens thinking before the
     * JSON, and truncating them produces garbage that costs more time (reformat
     * retry) than it saves — brevity is requested in the prompt instead.
     */
    private static final int FAST_AGENT_MAX_TOKENS = 1024;

    private final QuestionRepository questionRepository;
    private final AgentResponseRepository agentResponseRepository;
    private final ConsensusResultRepository consensusResultRepository;
    private final RetrievalService retrievalService;
    private final LlmClient llmClient;
    private final LlmRouter llmRouter;
    private final DebateEventPublisher events;
    private final ConsensusParser consensusParser;
    private final ExecutorService agentExecutor;

    public DebateOrchestrator(QuestionRepository questionRepository,
                              AgentResponseRepository agentResponseRepository,
                              ConsensusResultRepository consensusResultRepository,
                              RetrievalService retrievalService,
                              LlmClient llmClient,
                              LlmRouter llmRouter,
                              DebateEventPublisher events,
                              ConsensusParser consensusParser,
                              ExecutorService agentExecutor) {
        this.questionRepository = questionRepository;
        this.agentResponseRepository = agentResponseRepository;
        this.consensusResultRepository = consensusResultRepository;
        this.retrievalService = retrievalService;
        this.llmClient = llmClient;
        this.llmRouter = llmRouter;
        this.events = events;
        this.consensusParser = consensusParser;
        this.agentExecutor = agentExecutor;
    }

    @Async
    public void runDebate(UUID questionId) {
        long startedAt = System.currentTimeMillis();
        QuestionEntity question = questionRepository.findById(questionId).orElse(null);
        if (question == null) {
            log.error("Debate requested for unknown question {}", questionId);
            return;
        }
        try {
            // ---- Phase 2a: retrieval -------------------------------------------------
            transition(question, QuestionStatus.RETRIEVING);
            List<ContextSnippetDto> snippets =
                    retrievalService.retrieveAndPersist(question, question.getDocument().getId());
            if (snippets.isEmpty()) {
                failDebate(question, startedAt,
                        "No indexed chunks found for this document — re-upload it and try again.");
                return;
            }
            events.publish(questionId, DebateEvent.of(
                    DebateEvent.Types.CONTEXT_RETRIEVED, questionId, snippets));
            String contextBlock = AgentPrompts.contextBlock(snippets);
            String questionText = question.getQuestionText();
            DebateMode mode = question.getMode();
            boolean fast = mode == DebateMode.FAST;
            Integer agentTokenBudget = fast ? FAST_AGENT_MAX_TOKENS : null;

            // ---- Phase 2b: round 1 — independent initial answers ---------------------
            transition(question, QuestionStatus.DEBATING_ROUND_1);
            Map<AgentName, String> round1 = runRound(question, 1, DebateEvent.Types.AGENT_RESPONSE,
                    List.of(AgentName.values()),
                    agent -> AgentPrompts.round1(questionText, contextBlock, fast),
                    agent -> null,
                    agentTokenBudget);
            if (round1.isEmpty()) {
                failDebate(question, startedAt, describeRound1Failure(questionId));
                return;
            }

            Map<AgentName, String> finalAnswers;
            if (fast) {
                // Fast mode: straight from independent answers to consensus.
                finalAnswers = round1;
            } else {
                // ---- Phase 3: round 2 — cross-critique --------------------------------
                transition(question, QuestionStatus.DEBATING_ROUND_2);
                List<AgentName> critics = round1.keySet().stream()
                        .filter(agent -> round1.keySet().stream().anyMatch(peer -> peer != agent))
                        .toList();
                Map<AgentName, String> critiques = critics.isEmpty() ? Map.of()
                        : runRound(question, 2, DebateEvent.Types.AGENT_CRITIQUE,
                        critics,
                        agent -> AgentPrompts.round2(agent, questionText, contextBlock, round1),
                        agent -> null,
                        null);

                // ---- Phase 4: round 3 — revision ---------------------------------------
                transition(question, QuestionStatus.DEBATING_ROUND_3);
                Map<AgentName, String> critiquesReceived = new EnumMap<>(AgentName.class);
                for (AgentName agent : round1.keySet()) {
                    critiquesReceived.put(agent, combineCritiquesFor(agent, critiques));
                }
                Map<AgentName, String> revised = runRound(question, 3, DebateEvent.Types.AGENT_REVISION,
                        List.copyOf(round1.keySet()),
                        agent -> AgentPrompts.round3(questionText, contextBlock,
                                round1.get(agent), critiquesReceived.get(agent)),
                        critiquesReceived::get,
                        null);

                // Agents whose revision call failed still participate in consensus with
                // their round-1 answer — a degraded debate beats a dead one.
                finalAnswers = new EnumMap<>(AgentName.class);
                for (AgentName agent : round1.keySet()) {
                    finalAnswers.put(agent, revised.getOrDefault(agent, round1.get(agent)));
                }
            }

            // ---- Phase 5: consensus -----------------------------------------------------
            transition(question, QuestionStatus.CONSENSUS);
            ConsensusOutcome outcome;
            try {
                outcome = generateConsensus(questionText, contextBlock, finalAnswers, fast);
            } catch (Exception e) {
                // Even with retries and provider failover the consensus call can be
                // rate-limited to death on free tiers. A surviving agent answer,
                // honestly labeled, beats failing the whole debate at the last step.
                log.warn("Consensus engine unavailable for question {}: {}", questionId, e.getMessage());
                outcome = consensusUnavailableFallback(finalAnswers);
            }
            ConsensusPayload payload = outcome.payload();
            ConsensusResultEntity consensus = consensusResultRepository.save(new ConsensusResultEntity(
                    question,
                    payload.finalAnswer(),
                    payload.areasOfAgreement(),
                    payload.areasOfDisagreement(),
                    payload.confidenceScore(),
                    outcome.servedByModel()));
            events.publish(questionId, DebateEvent.of(
                    DebateEvent.Types.CONSENSUS_GENERATED, questionId, ConsensusDto.from(consensus)));

            long elapsed = System.currentTimeMillis() - startedAt;
            question.setStatus(QuestionStatus.COMPLETED);
            question.setProcessingTimeMs(elapsed);
            question.setCompletedAt(Instant.now());
            questionRepository.save(question);
            events.publish(questionId, DebateEvent.of(
                    DebateEvent.Types.DEBATE_COMPLETED, questionId,
                    Map.of("processingTimeMs", elapsed, "status", QuestionStatus.COMPLETED)));
            log.info("Debate {} completed in {} ms with {} agents", questionId, elapsed, finalAnswers.size());
        } catch (Exception e) {
            log.error("Debate {} failed unexpectedly", questionId, e);
            failDebate(question, startedAt,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /**
     * Runs one debate round for the given agents in parallel. Each agent's row is
     * persisted and its STOMP event published the moment that agent finishes, so
     * the UI streams results as they land rather than waiting for the round.
     *
     * @return map of agent → response text, containing only successful agents
     */
    private Map<AgentName, String> runRound(QuestionEntity question,
                                            int round,
                                            String eventType,
                                            List<AgentName> agents,
                                            java.util.function.Function<AgentName, String> userPrompt,
                                            java.util.function.Function<AgentName, String> critiqueReceived,
                                            Integer maxTokensOverride) {
        Map<AgentName, CompletableFuture<String>> futures = new EnumMap<>(AgentName.class);
        for (AgentName agent : agents) {
            futures.put(agent, CompletableFuture.supplyAsync(
                    () -> executeAgentCall(question, agent, round, eventType,
                            userPrompt.apply(agent), critiqueReceived.apply(agent), maxTokensOverride),
                    agentExecutor));
        }
        Map<AgentName, String> results = new EnumMap<>(AgentName.class);
        futures.forEach((agent, future) -> {
            String content = future.join();
            if (content != null) {
                results.put(agent, content);
            }
        });
        return results;
    }

    /**
     * @return the agent's response text, or {@code null} if the call failed
     *         (failure row is persisted and a failed event is published)
     */
    private String executeAgentCall(QuestionEntity question,
                                    AgentName agent,
                                    int round,
                                    String eventType,
                                    String userPrompt,
                                    String critiqueReceived,
                                    Integer maxTokensOverride) {
        List<LlmEndpoint> chain = llmRouter.routesFor(agent);
        String intendedModel = chain.isEmpty() ? "unconfigured" : chain.getFirst().label();
        AgentResponseEntity entity = new AgentResponseEntity(question, agent, intendedModel, round);
        entity.setCritiqueReceived(critiqueReceived);
        long callStart = System.currentTimeMillis();
        try {
            LlmClient.CompletionResult result = llmClient.complete(chain, List.of(
                    ChatMessage.system(AgentPrompts.systemPrompt(agent)),
                    ChatMessage.user(userPrompt)), maxTokensOverride);
            entity.setResponse(result.content());
            entity.setModel(result.servedBy());
            entity.setLatencyMs(System.currentTimeMillis() - callStart);
            agentResponseRepository.save(entity);
            events.publish(question.getId(),
                    DebateEvent.of(eventType, question.getId(), AgentMessageDto.from(entity)));
            return result.content();
        } catch (Exception e) {
            log.warn("Agent {} failed in round {} for question {}: {}",
                    agent, round, question.getId(), e.getMessage());
            // Fresh entity for the failure record: if the try-block's save itself
            // failed (e.g. un-storable model output), the original instance already
            // carries a generated ID from the aborted persist, and re-saving it
            // would throw ObjectOptimisticLockingFailureException.
            AgentResponseEntity failure = new AgentResponseEntity(question, agent, intendedModel, round);
            failure.setCritiqueReceived(critiqueReceived);
            failure.setStatus(AgentResponseStatus.FAILED);
            failure.setErrorMessage(truncate(summarizeCause(e.getMessage()), 500));
            failure.setLatencyMs(System.currentTimeMillis() - callStart);
            try {
                agentResponseRepository.save(failure);
            } catch (Exception persistError) {
                // Losing one failure record must never kill the debate.
                log.error("Could not persist failure record for agent {} round {}: {}",
                        agent, round, persistError.getMessage());
            }
            events.publish(question.getId(),
                    DebateEvent.of(eventType, question.getId(), AgentMessageDto.from(failure)));
            return null;
        }
    }

    /** Concatenates the round-2 outputs authored by an agent's peers. */
    private static String combineCritiquesFor(AgentName agent, Map<AgentName, String> critiques) {
        StringBuilder combined = new StringBuilder();
        critiques.forEach((critic, critique) -> {
            if (critic != agent) {
                combined.append("--- Critique from ").append(critic.displayName()).append(" ---\n")
                        .append(critique).append("\n\n");
            }
        });
        if (combined.isEmpty()) {
            return "(No peer critiques were produced this round. Re-examine your answer against the "
                    + "document context on your own and correct anything unsupported.)";
        }
        return combined.toString().strip();
    }

    private record ConsensusOutcome(ConsensusPayload payload, String servedByModel) {
    }

    /**
     * Last-resort consensus when every LLM endpoint is exhausted: present one
     * agent's answer, preferring The Reviewer's (it survived adversarial
     * fact-checking), clearly labeled and with reduced confidence.
     */
    private static ConsensusOutcome consensusUnavailableFallback(Map<AgentName, String> finalAnswers) {
        AgentName spokesperson = finalAnswers.containsKey(AgentName.REVIEWER)
                ? AgentName.REVIEWER
                : finalAnswers.keySet().iterator().next();
        String note = "The consensus engine was unavailable (upstream rate limits), so this is "
                + spokesperson.displayName() + "'s answer without cross-agent synthesis. "
                + "Compare the individual agent answers in the debate panel.";
        return new ConsensusOutcome(
                new ConsensusPayload(finalAnswers.get(spokesperson), List.of(), List.of(note), 40),
                "fallback:" + spokesperson.name().toLowerCase(java.util.Locale.ROOT));
    }

    private ConsensusOutcome generateConsensus(String questionText,
                                               String contextBlock,
                                               Map<AgentName, String> finalAnswers,
                                               boolean fast) {
        List<LlmEndpoint> chain = llmRouter.routesForConsensus();
        Integer tokenBudget = null;
        LlmClient.CompletionResult result = llmClient.complete(chain, List.of(
                ChatMessage.user(AgentPrompts.consensus(questionText, contextBlock, finalAnswers, !fast))),
                tokenBudget);

        Optional<ConsensusPayload> parsed = consensusParser.parse(result.content());
        if (parsed.isPresent()) {
            return new ConsensusOutcome(parsed.get(), result.servedBy());
        }

        // One repair attempt: ask the model to reshape its own output into valid JSON.
        log.warn("Consensus output was not valid JSON, attempting reformat");
        try {
            LlmClient.CompletionResult reformatted = llmClient.complete(chain, List.of(
                    ChatMessage.user(AgentPrompts.consensusReformat(result.content()))), tokenBudget);
            Optional<ConsensusPayload> reparsed = consensusParser.parse(reformatted.content());
            if (reparsed.isPresent()) {
                return new ConsensusOutcome(reparsed.get(), reformatted.servedBy());
            }
        } catch (Exception e) {
            log.warn("Consensus reformat attempt failed: {}", e.getMessage());
        }

        // Last resort: surface the raw synthesis rather than failing the debate.
        return new ConsensusOutcome(
                new ConsensusPayload(result.content().strip(), List.of(), List.of(), 50),
                result.servedBy());
    }

    /**
     * Builds an honest failure summary from the actual round-1 agent errors
     * (e.g. missing API key, 404 on a rotated free model, rate limits) instead
     * of a generic guess.
     */
    private String describeRound1Failure(UUID questionId) {
        List<String> causes = agentResponseRepository
                .findByQuestionIdOrderByRoundAscAgentNameAsc(questionId).stream()
                .filter(r -> r.getRound() == 1 && r.getStatus() == AgentResponseStatus.FAILED)
                .map(AgentResponseEntity::getErrorMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .map(DebateOrchestrator::summarizeCause)
                .distinct()
                .toList();
        String summary = "All three agents failed to produce an initial answer.";
        if (causes.isEmpty()) {
            return summary + " Try again shortly.";
        }
        String detail = String.join(" · ", causes);
        if (detail.contains("HTTP 404")) {
            detail += " — the model may have been rotated off OpenRouter's free tier; "
                    + "override MODEL_ANALYST / MODEL_ENGINEER / MODEL_REVIEWER env vars.";
        }
        return summary + " Cause: " + detail;
    }

    private static final java.util.regex.Pattern RAW_REASON =
            java.util.regex.Pattern.compile("\"raw\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Turns a provider error (often a JSON blob) into one human-readable line.
     * OpenRouter puts the upstream explanation in a nested {@code "raw"} field;
     * everything else is noise to the person reading the failure banner.
     */
    static String summarizeCause(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error";
        }
        java.util.regex.Matcher matcher = RAW_REASON.matcher(message);
        if (matcher.find()) {
            String prefix = message.substring(0, Math.max(0, message.indexOf(':')));
            String raw = matcher.group(1);
            // Drop the boilerplate upsell suffix OpenRouter appends.
            int cut = raw.indexOf(". Please retry shortly");
            if (cut > 0) {
                raw = raw.substring(0, cut);
            }
            return (prefix.isBlank() ? raw : prefix + ": " + raw);
        }
        int jsonStart = message.indexOf('{');
        String cleaned = jsonStart > 0 ? message.substring(0, jsonStart).strip() : message;
        return cleaned.length() <= 200 ? cleaned : cleaned.substring(0, 200) + "…";
    }

    private void transition(QuestionEntity question, QuestionStatus status) {
        question.setStatus(status);
        questionRepository.save(question);
        events.publish(question.getId(), DebateEvent.of(
                DebateEvent.Types.STAGE_CHANGED, question.getId(), Map.of("stage", status)));
    }

    private void failDebate(QuestionEntity question, long startedAt, String reason) {
        long elapsed = System.currentTimeMillis() - startedAt;
        question.setStatus(QuestionStatus.FAILED);
        question.setErrorMessage(truncate(reason, 900));
        question.setProcessingTimeMs(elapsed);
        question.setCompletedAt(Instant.now());
        questionRepository.save(question);
        events.publish(question.getId(), DebateEvent.of(
                DebateEvent.Types.DEBATE_ERROR, question.getId(),
                Map.of("error", reason, "processingTimeMs", elapsed)));
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }
}
