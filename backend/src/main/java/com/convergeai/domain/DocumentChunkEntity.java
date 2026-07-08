package com.convergeai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "document_chunks")
public class DocumentChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "char_count", nullable = false)
    private int charCount;

    /**
     * pgvector column, written through hibernate-vector. Dimension must match the
     * all-MiniLM-L6-v2 embedding size declared in the Flyway migration.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    @Column(nullable = false)
    private float[] embedding;

    protected DocumentChunkEntity() {
    }

    public DocumentChunkEntity(DocumentEntity document, int chunkIndex, String content, float[] embedding) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.charCount = content.length();
        this.embedding = embedding;
    }

    public UUID getId() {
        return id;
    }

    public DocumentEntity getDocument() {
        return document;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public int getCharCount() {
        return charCount;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}
