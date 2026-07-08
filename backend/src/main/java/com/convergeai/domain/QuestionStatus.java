package com.convergeai.domain;

/**
 * Lifecycle of a question as it moves through the RAG + debate pipeline.
 * The ordinal order mirrors the pipeline stages and is relied on by the UI timeline.
 */
public enum QuestionStatus {
    PENDING,
    RETRIEVING,
    DEBATING_ROUND_1,
    DEBATING_ROUND_2,
    DEBATING_ROUND_3,
    CONSENSUS,
    COMPLETED,
    FAILED
}
