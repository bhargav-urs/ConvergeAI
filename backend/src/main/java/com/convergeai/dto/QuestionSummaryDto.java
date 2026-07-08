package com.convergeai.dto;

import com.convergeai.domain.DebateMode;
import com.convergeai.domain.QuestionStatus;

import java.time.Instant;
import java.util.UUID;

public record QuestionSummaryDto(
        UUID id,
        UUID documentId,
        String documentFilename,
        String questionText,
        QuestionStatus status,
        DebateMode mode,
        Long processingTimeMs,
        Integer confidenceScore,
        String finalAnswerPreview,
        Instant createdAt
) {
}
