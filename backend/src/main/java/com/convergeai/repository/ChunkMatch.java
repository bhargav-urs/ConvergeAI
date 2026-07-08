package com.convergeai.repository;

import java.util.UUID;

/**
 * Native-query projection for a similarity search hit.
 * {@code distance} is pgvector cosine distance (0 = identical, 2 = opposite).
 */
public interface ChunkMatch {

    UUID getId();

    int getChunkIndex();

    String getContent();

    double getDistance();
}
