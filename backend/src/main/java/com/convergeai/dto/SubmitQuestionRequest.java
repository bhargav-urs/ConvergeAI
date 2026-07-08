package com.convergeai.dto;

import com.convergeai.domain.DebateMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitQuestionRequest(
        @NotNull(message = "documentId is required")
        UUID documentId,

        @NotBlank(message = "questionText must not be blank")
        @Size(max = 4000, message = "questionText must be at most 4000 characters")
        String questionText,

        /** Optional; defaults to NORMAL (full 3-round debate). */
        DebateMode mode
) {
}
