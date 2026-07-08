package com.convergeai.dto;

import com.convergeai.domain.DocumentChunkEntity;

import java.util.UUID;

public record ChunkDto(
        UUID id,
        int chunkIndex,
        String content,
        int charCount
) {

    public static ChunkDto from(DocumentChunkEntity entity) {
        return new ChunkDto(entity.getId(), entity.getChunkIndex(), entity.getContent(), entity.getCharCount());
    }
}
