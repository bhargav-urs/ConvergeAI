package com.convergeai.dto;

import com.convergeai.domain.DocumentEntity;
import com.convergeai.domain.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentDto(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        DocumentStatus status,
        int chunkCount,
        long charCount,
        String errorMessage,
        Instant uploadedAt
) {

    public static DocumentDto from(DocumentEntity entity) {
        return new DocumentDto(
                entity.getId(),
                entity.getFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getStatus(),
                entity.getChunkCount(),
                entity.getCharCount(),
                entity.getErrorMessage(),
                entity.getUploadedAt()
        );
    }
}
