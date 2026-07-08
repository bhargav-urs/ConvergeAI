package com.convergeai.dto;

import com.convergeai.domain.DebateMode;
import com.convergeai.domain.QuestionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionDetailDto(
        UUID id,
        UUID documentId,
        String documentFilename,
        String questionText,
        QuestionStatus status,
        DebateMode mode,
        String errorMessage,
        Long processingTimeMs,
        Instant createdAt,
        Instant completedAt,
        List<ContextSnippetDto> context,
        List<AgentDebateDto> agents,
        ConsensusDto consensus
) {
}
