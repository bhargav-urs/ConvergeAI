package com.convergeai.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope for every STOMP event published to /topic/debate/{questionId} and
 * /topic/documents. {@code type} is one of the {@link Types} constants.
 */
public record DebateEvent(
        String type,
        UUID questionId,
        Instant timestamp,
        Object payload
) {

    public static DebateEvent of(String type, UUID questionId, Object payload) {
        return new DebateEvent(type, questionId, Instant.now(), payload);
    }

    public static final class Types {
        public static final String DOCUMENT_INDEXED = "document.indexed";
        public static final String DOCUMENT_FAILED = "document.failed";
        public static final String STAGE_CHANGED = "stage.changed";
        public static final String CONTEXT_RETRIEVED = "context.retrieved";
        public static final String AGENT_RESPONSE = "agent.response";
        public static final String AGENT_CRITIQUE = "agent.critique";
        public static final String AGENT_REVISION = "agent.revision";
        public static final String CONSENSUS_GENERATED = "consensus.generated";
        public static final String DEBATE_COMPLETED = "debate.completed";
        public static final String DEBATE_ERROR = "debate.error";
        public static final String QUESTION_ACCEPTED = "question.accepted";

        private Types() {
        }
    }
}
