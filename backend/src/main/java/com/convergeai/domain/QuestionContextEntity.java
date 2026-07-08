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

import java.util.UUID;

/**
 * Join record capturing exactly which chunks (and at which similarity) grounded a
 * question's debate. Enables auditable [Chunk n] citations in agent answers.
 */
@Entity
@Table(name = "question_context")
public class QuestionContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunk_id", nullable = false)
    private DocumentChunkEntity chunk;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(nullable = false)
    private double distance;

    protected QuestionContextEntity() {
    }

    public QuestionContextEntity(QuestionEntity question, DocumentChunkEntity chunk, int rank, double distance) {
        this.question = question;
        this.chunk = chunk;
        this.rank = rank;
        this.distance = distance;
    }

    public UUID getId() {
        return id;
    }

    public QuestionEntity getQuestion() {
        return question;
    }

    public DocumentChunkEntity getChunk() {
        return chunk;
    }

    public int getRank() {
        return rank;
    }

    public double getDistance() {
        return distance;
    }
}
