package com.convergeai.dto;

import java.util.UUID;

/**
 * One retrieved chunk as shown to both the agents and the UI.
 * {@code rank} is 1-based and matches the [Chunk n] citation labels in prompts.
 */
public record ContextSnippetDto(
        UUID chunkId,
        int chunkIndex,
        int rank,
        String content,
        double distance
) {
}
