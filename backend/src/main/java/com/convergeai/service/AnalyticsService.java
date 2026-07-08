package com.convergeai.service;

import com.convergeai.config.OpenRouterProperties;
import com.convergeai.domain.AgentName;
import com.convergeai.domain.AgentResponseEntity;
import com.convergeai.domain.AgentResponseStatus;
import com.convergeai.domain.QuestionEntity;
import com.convergeai.domain.QuestionStatus;
import com.convergeai.dto.AnalyticsSummaryDto;
import com.convergeai.repository.AgentResponseRepository;
import com.convergeai.repository.ConsensusResultRepository;
import com.convergeai.repository.DocumentRepository;
import com.convergeai.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final DocumentRepository documentRepository;
    private final QuestionRepository questionRepository;
    private final AgentResponseRepository agentResponseRepository;
    private final ConsensusResultRepository consensusResultRepository;
    private final OpenRouterProperties openRouterProperties;

    public AnalyticsService(DocumentRepository documentRepository,
                            QuestionRepository questionRepository,
                            AgentResponseRepository agentResponseRepository,
                            ConsensusResultRepository consensusResultRepository,
                            OpenRouterProperties openRouterProperties) {
        this.documentRepository = documentRepository;
        this.questionRepository = questionRepository;
        this.agentResponseRepository = agentResponseRepository;
        this.consensusResultRepository = consensusResultRepository;
        this.openRouterProperties = openRouterProperties;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto summary() {
        long totalDocuments = documentRepository.count();
        long totalQuestions = questionRepository.count();
        long completed = questionRepository.countByStatus(QuestionStatus.COMPLETED);
        long failed = questionRepository.countByStatus(QuestionStatus.FAILED);
        Double avgProcessingTime = questionRepository.averageProcessingTimeMs();
        Double avgConfidence = consensusResultRepository.averageConfidenceScore();

        return new AnalyticsSummaryDto(
                totalDocuments,
                totalQuestions,
                completed,
                failed,
                avgProcessingTime,
                avgConfidence,
                computeAgentStats());
    }

    /**
     * Per-agent stability over a rolling window of the 100 most recent completed
     * debates. Stability = Jaccard similarity between the agent's round-1 and
     * round-3 answers; 1.0 means the initial answer survived critique untouched
     * (fewest revisions needed).
     */
    private List<AnalyticsSummaryDto.AgentStatsDto> computeAgentStats() {
        List<QuestionEntity> window =
                questionRepository.findTop100ByStatusOrderByCreatedAtDesc(QuestionStatus.COMPLETED);

        Map<AgentName, List<Double>> stabilityScores = new EnumMap<>(AgentName.class);
        Map<AgentName, List<Long>> initialLatencies = new EnumMap<>(AgentName.class);
        Map<AgentName, Integer> participation = new EnumMap<>(AgentName.class);

        if (!window.isEmpty()) {
            List<UUID> ids = window.stream().map(QuestionEntity::getId).toList();
            Map<UUID, Map<AgentName, Map<Integer, AgentResponseEntity>>> grouped = new java.util.HashMap<>();
            for (AgentResponseEntity response : agentResponseRepository.findByQuestionIdIn(ids)) {
                grouped.computeIfAbsent(response.getQuestion().getId(), q -> new EnumMap<>(AgentName.class))
                        .computeIfAbsent(response.getAgentName(), a -> new java.util.HashMap<>())
                        .put(response.getRound(), response);
            }
            for (Map<AgentName, Map<Integer, AgentResponseEntity>> byAgent : grouped.values()) {
                byAgent.forEach((agent, rounds) -> {
                    AgentResponseEntity round1 = rounds.get(1);
                    AgentResponseEntity round3 = rounds.get(3);
                    if (round1 != null && round1.getStatus() == AgentResponseStatus.OK) {
                        participation.merge(agent, 1, Integer::sum);
                        if (round1.getLatencyMs() != null) {
                            initialLatencies.computeIfAbsent(agent, a -> new ArrayList<>())
                                    .add(round1.getLatencyMs());
                        }
                        if (round3 != null && round3.getStatus() == AgentResponseStatus.OK) {
                            stabilityScores.computeIfAbsent(agent, a -> new ArrayList<>())
                                    .add(TextSimilarity.jaccard(round1.getResponse(), round3.getResponse()));
                        }
                    }
                });
            }
        }

        List<AnalyticsSummaryDto.AgentStatsDto> stats = new ArrayList<>();
        for (AgentName agent : AgentName.values()) {
            stats.add(new AnalyticsSummaryDto.AgentStatsDto(
                    agent,
                    agent.displayName(),
                    openRouterProperties.modelFor(agent),
                    participation.getOrDefault(agent, 0),
                    average(stabilityScores.get(agent)),
                    averageLongs(initialLatencies.get(agent))));
        }
        return stats;
    }

    private static Double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static Double averageLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }
}
