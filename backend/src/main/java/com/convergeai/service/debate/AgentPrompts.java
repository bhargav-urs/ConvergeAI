package com.convergeai.service.debate;

import com.convergeai.domain.AgentName;
import com.convergeai.dto.ContextSnippetDto;

import java.util.List;
import java.util.Map;

/**
 * Central prompt factory for the structured debate. All prompts share two hard
 * rules: answers must be grounded exclusively in the supplied [Chunk n] context,
 * and claims should cite the chunk that supports them.
 */
public final class AgentPrompts {

    private static final String GROUNDING_RULES = """
            Ground rules (non-negotiable):
            - Use ONLY the information inside the DOCUMENT CONTEXT below. Do not use outside knowledge.
            - Cite supporting chunks inline like [Chunk 2] after each claim.
            - If the context does not contain the answer, say exactly that instead of guessing.
            - Be concise: no preamble, no restating the question.""";

    private AgentPrompts() {
    }

    public static String systemPrompt(AgentName agent) {
        return switch (agent) {
            case ANALYST -> """
                    You are The Analyst, one of three AI agents in a document-grounded debate. \
                    Your specialty is deep logical reasoning and step-by-step extraction. \
                    You break the retrieved document context down into the core facts relevant to the question, \
                    reason carefully about how they connect, and never assert anything the context does not support.""";
            case ENGINEER -> """
                    You are The Engineer, one of three AI agents in a document-grounded debate. \
                    Your specialty is practical synthesis: you turn extracted facts into a direct, structured, \
                    actionable answer. You value clarity and usefulness, but only ever build on what the \
                    document context actually says.""";
            case REVIEWER -> """
                    You are The Reviewer, one of three AI agents in a document-grounded debate. \
                    Your specialty is adversarial fact-checking. You compare claims against the retrieved \
                    document context, hunt for hallucinations, unsupported statements, missing nuances and \
                    logical flaws, and you are constructively blunt about what you find.""";
        };
    }

    private static final String CONCISE_RULE =
            " Keep the entire answer under 150 words — terse bullet points, no filler.";

    /** Round 1: independent initial answers. Concise variant powers fast mode. */
    public static String round1(String question, String contextBlock, boolean concise) {
        return """
                %s

                DOCUMENT CONTEXT:
                %s

                QUESTION:
                %s

                Task: Answer the question from your role's perspective, grounded strictly in the document context. \
                Structure your answer with short paragraphs or bullet points and cite chunks inline.%s""".formatted(
                GROUNDING_RULES, contextBlock, question, concise ? CONCISE_RULE : "");
    }

    /** Round 2: cross-critique of the other two agents' initial answers. */
    public static String round2(AgentName self,
                                String question,
                                String contextBlock,
                                Map<AgentName, String> peerAnswers) {
        StringBuilder peers = new StringBuilder();
        peerAnswers.forEach((agent, answer) -> {
            if (agent != self) {
                peers.append("--- Answer from ").append(agent.displayName()).append(" ---\n")
                        .append(answer).append("\n\n");
            }
        });
        return """
                %s

                DOCUMENT CONTEXT:
                %s

                QUESTION:
                %s

                PEER ANSWERS TO REVIEW:
                %s
                Task: Critique the other agents' answers. For each peer, identify (a) statements NOT supported \
                by the document context, (b) missing nuances or omitted relevant facts from the context, and \
                (c) logical flaws. Reference the exact claim and the chunk evidence, e.g. "claims X but \
                [Chunk 3] says Y". Return constructive criticism only — do not rewrite their answers and do \
                not add praise padding. If an answer is fully supported, state that explicitly.""".formatted(GROUNDING_RULES, contextBlock, question, peers);
    }

    /** Round 3: revision of the agent's own answer in light of peer critiques. */
    public static String round3(String question,
                                String contextBlock,
                                String ownAnswer,
                                String critiquesReceived) {
        return """
                %s

                DOCUMENT CONTEXT:
                %s

                QUESTION:
                %s

                YOUR ORIGINAL ANSWER:
                %s

                CRITIQUES FROM YOUR PEERS:
                %s

                Task: Revise your answer considering the feedback. Fix every justified criticism, keep what \
                was correct, and ensure absolute fidelity to the document context — remove or correct any \
                claim you cannot cite. Output only the revised answer with inline [Chunk n] citations.""".formatted(GROUNDING_RULES, contextBlock, question, ownAnswer, critiquesReceived);
    }

    /**
     * Phase 5: consensus synthesis. In normal mode the inputs are post-critique
     * revised answers; in fast mode they are the agents' single-round answers.
     * Expects strict JSON output.
     */
    public static String consensus(String question,
                                   String contextBlock,
                                   Map<AgentName, String> agentAnswers,
                                   boolean answersWereRevised) {
        String answerKind = answersWereRevised ? "Revised answer" : "Answer";
        StringBuilder answers = new StringBuilder();
        agentAnswers.forEach((agent, answer) ->
                answers.append("--- ").append(answerKind).append(" from ")
                        .append(agent.displayName()).append(" ---\n")
                        .append(answer).append("\n\n"));
        String debateShape = answersWereRevised
                ? "Three AI agents have debated a question over three rounds, grounded in document context, "
                + "and produced revised answers."
                : "Three AI agents have independently answered a question, grounded in document context.";
        return """
                You are the Consensus Engine of a multi-agent debate system. %s

                DOCUMENT CONTEXT:
                %s

                QUESTION:
                %s

                AGENT ANSWERS:
                %s
                Task: Synthesize a single, final, trustworthy answer. Weigh claims by how well they are \
                supported by the document context. Where agents agree, that is signal; where they disagree, \
                surface it honestly instead of papering over it.

                Respond with ONLY a JSON object — no markdown fences, no commentary — in exactly this shape:
                {
                  "final_answer": "the synthesized answer, with [Chunk n] citations preserved",
                  "areas_of_agreement": ["point all agents supported", "..."],
                  "areas_of_disagreement": ["point where agents diverged and why", "..."],
                  "confidence_score": 87
                }
                confidence_score is an integer 0-100 reflecting how well the final answer is grounded in the \
                context and how strongly the agents converged.%s""".formatted(
                debateShape, contextBlock, question, answers,
                answersWereRevised ? "" : "\nKeep final_answer under 200 words.");
    }

    public static String consensusReformat(String malformedOutput) {
        return """
                The following text was supposed to be a single valid JSON object with keys "final_answer" \
                (string), "areas_of_agreement" (array of strings), "areas_of_disagreement" (array of strings) \
                and "confidence_score" (integer 0-100), but it is malformed. Convert it into exactly that JSON \
                object, preserving the content faithfully. Respond with ONLY the JSON object.

                TEXT:
                %s""".formatted(malformedOutput);
    }

    /** Renders retrieved snippets as the numbered context block used in every prompt. */
    public static String contextBlock(List<ContextSnippetDto> snippets) {
        StringBuilder block = new StringBuilder();
        for (ContextSnippetDto snippet : snippets) {
            block.append("[Chunk ").append(snippet.rank()).append("]\n")
                    .append(snippet.content().strip())
                    .append("\n\n");
        }
        return block.toString().strip();
    }
}
