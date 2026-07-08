package com.convergeai.dto;

import com.convergeai.domain.AgentName;

import java.util.List;

public record AnalyticsSummaryDto(
        long totalDocuments,
        long totalQuestions,
        long completedQuestions,
        long failedQuestions,
        Double avgProcessingTimeMs,
        Double avgConfidenceScore,
        List<AgentStatsDto> agentStats
) {

    /**
     * Per-agent debate metrics. {@code avgStabilityScore} is the mean word-level
     * Jaccard similarity between an agent's round-1 and round-3 answers across
     * completed debates: 1.0 means the agent never changed its answer (fewest
     * revisions needed), lower values mean heavier revision after critique.
     */
    public record AgentStatsDto(
            AgentName agent,
            String displayName,
            String model,
            int debatesParticipated,
            Double avgStabilityScore,
            Double avgInitialLatencyMs
    ) {
    }
}
