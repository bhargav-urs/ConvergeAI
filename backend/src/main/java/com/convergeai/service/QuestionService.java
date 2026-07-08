package com.convergeai.service;

import com.convergeai.domain.AgentName;
import com.convergeai.domain.AgentResponseEntity;
import com.convergeai.domain.ConsensusResultEntity;
import com.convergeai.domain.DocumentEntity;
import com.convergeai.domain.DocumentStatus;
import com.convergeai.domain.QuestionEntity;
import com.convergeai.dto.AgentDebateDto;
import com.convergeai.dto.AgentMessageDto;
import com.convergeai.dto.ConsensusDto;
import com.convergeai.dto.ContextSnippetDto;
import com.convergeai.dto.QuestionDetailDto;
import com.convergeai.dto.QuestionSummaryDto;
import com.convergeai.dto.SubmitQuestionRequest;
import com.convergeai.ai.LlmRouter;
import com.convergeai.exception.ConflictException;
import com.convergeai.exception.NotFoundException;
import com.convergeai.repository.AgentResponseRepository;
import com.convergeai.repository.ConsensusResultRepository;
import com.convergeai.repository.QuestionContextRepository;
import com.convergeai.repository.QuestionRepository;
import com.convergeai.service.debate.DebateOrchestrator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionContextRepository contextRepository;
    private final AgentResponseRepository agentResponseRepository;
    private final ConsensusResultRepository consensusResultRepository;
    private final DocumentService documentService;
    private final DebateOrchestrator debateOrchestrator;
    private final LlmRouter llmRouter;

    public QuestionService(QuestionRepository questionRepository,
                           QuestionContextRepository contextRepository,
                           AgentResponseRepository agentResponseRepository,
                           ConsensusResultRepository consensusResultRepository,
                           DocumentService documentService,
                           DebateOrchestrator debateOrchestrator,
                           LlmRouter llmRouter) {
        this.questionRepository = questionRepository;
        this.contextRepository = contextRepository;
        this.agentResponseRepository = agentResponseRepository;
        this.consensusResultRepository = consensusResultRepository;
        this.documentService = documentService;
        this.debateOrchestrator = debateOrchestrator;
        this.llmRouter = llmRouter;
    }

    /**
     * Persists the question and kicks off the async debate pipeline. Deliberately
     * not transactional: the question row must be committed before the async
     * orchestrator (running on another thread) loads it.
     */
    public QuestionSummaryDto submit(SubmitQuestionRequest request) {
        // Fail fast with the real reason instead of letting the debate run and
        // fail three agent calls later with a generic message.
        if (!llmRouter.isAnyProviderConfigured()) {
            throw new ConflictException(
                    "The server has no LLM provider configured, so debates cannot run. Set "
                            + "OPENROUTER_API_KEY (free key: https://openrouter.ai/keys) — and optionally "
                            + "GROQ_API_KEY / CEREBRAS_API_KEY / GEMINI_API_KEY for faster debates — then restart the backend.");
        }
        DocumentEntity document = documentService.requireDocument(request.documentId());
        if (document.getStatus() != DocumentStatus.READY) {
            throw new ConflictException(
                    "Document is not ready for questions (status: " + document.getStatus() + ")");
        }
        QuestionEntity question = questionRepository.save(
                new QuestionEntity(document, request.questionText().strip(), request.mode()));
        debateOrchestrator.runDebate(question.getId());
        return toSummary(question, document, null);
    }

    @Transactional(readOnly = true)
    public List<QuestionSummaryDto> list(UUID documentId) {
        List<QuestionEntity> questions = documentId == null
                ? questionRepository.findTop50ByOrderByCreatedAtDesc()
                : questionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
        if (questions.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = questions.stream().map(QuestionEntity::getId).toList();
        Map<UUID, ConsensusResultEntity> consensusByQuestion =
                consensusResultRepository.findByQuestionIdIn(ids).stream()
                        .collect(Collectors.toMap(c -> c.getQuestion().getId(), Function.identity()));
        return questions.stream()
                .map(q -> toSummary(q, q.getDocument(), consensusByQuestion.get(q.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionDetailDto detail(UUID questionId) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question " + questionId + " not found"));

        List<ContextSnippetDto> context = contextRepository.findByQuestionIdWithChunks(questionId).stream()
                .map(qc -> new ContextSnippetDto(
                        qc.getChunk().getId(),
                        qc.getChunk().getChunkIndex(),
                        qc.getRank(),
                        qc.getChunk().getContent(),
                        qc.getDistance()))
                .toList();

        List<AgentResponseEntity> responses =
                agentResponseRepository.findByQuestionIdOrderByRoundAscAgentNameAsc(questionId);
        Map<AgentName, Map<Integer, AgentResponseEntity>> byAgent = new EnumMap<>(AgentName.class);
        for (AgentResponseEntity response : responses) {
            byAgent.computeIfAbsent(response.getAgentName(), a -> new java.util.HashMap<>())
                    .put(response.getRound(), response);
        }
        List<AgentDebateDto> agents = new ArrayList<>();
        for (AgentName agent : AgentName.values()) {
            Map<Integer, AgentResponseEntity> rounds = byAgent.get(agent);
            if (rounds == null) {
                continue;
            }
            AgentResponseEntity any = rounds.values().iterator().next();
            agents.add(new AgentDebateDto(
                    agent,
                    agent.displayName(),
                    agent.roleDescription(),
                    any.getModel(),
                    toMessage(rounds.get(1)),
                    toMessage(rounds.get(2)),
                    toMessage(rounds.get(3))));
        }

        ConsensusDto consensus = consensusResultRepository.findByQuestionId(questionId)
                .map(ConsensusDto::from)
                .orElse(null);

        return new QuestionDetailDto(
                question.getId(),
                question.getDocument().getId(),
                question.getDocument().getFilename(),
                question.getQuestionText(),
                question.getStatus(),
                question.getMode(),
                question.getErrorMessage(),
                question.getProcessingTimeMs(),
                question.getCreatedAt(),
                question.getCompletedAt(),
                context,
                agents,
                consensus);
    }

    private static AgentMessageDto toMessage(AgentResponseEntity entity) {
        return entity == null ? null : AgentMessageDto.from(entity);
    }

    private static QuestionSummaryDto toSummary(QuestionEntity question,
                                                DocumentEntity document,
                                                ConsensusResultEntity consensus) {
        String preview = null;
        Integer confidence = null;
        if (consensus != null) {
            confidence = consensus.getConfidenceScore();
            String answer = consensus.getFinalAnswer();
            preview = answer.length() <= 240 ? answer : answer.substring(0, 240) + "…";
        }
        return new QuestionSummaryDto(
                question.getId(),
                document.getId(),
                document.getFilename(),
                question.getQuestionText(),
                question.getStatus(),
                question.getMode(),
                question.getProcessingTimeMs(),
                confidence,
                preview,
                question.getCreatedAt());
    }
}
