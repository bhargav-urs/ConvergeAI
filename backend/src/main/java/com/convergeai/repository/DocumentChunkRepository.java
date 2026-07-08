package com.convergeai.repository;

import com.convergeai.domain.DocumentChunkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    Page<DocumentChunkEntity> findByDocumentIdOrderByChunkIndex(UUID documentId, Pageable pageable);

    long countByDocumentId(UUID documentId);

    /**
     * pgvector similarity search scoped to one document. MiniLM embeddings are
     * L2-normalised, so cosine distance ({@code <=>}) is the appropriate metric and
     * matches the HNSW index operator class created in the Flyway migration.
     * The query vector is passed as a pgvector text literal ("[0.1,0.2,...]").
     */
    @Query(value = """
            SELECT c.id          AS id,
                   c.chunk_index AS chunkIndex,
                   c.content     AS content,
                   (c.embedding <=> CAST(:queryVector AS vector)) AS distance
            FROM document_chunks c
            WHERE c.document_id = :documentId
            ORDER BY c.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<ChunkMatch> findNearestChunks(@Param("documentId") UUID documentId,
                                       @Param("queryVector") String queryVector,
                                       @Param("limit") int limit);
}
