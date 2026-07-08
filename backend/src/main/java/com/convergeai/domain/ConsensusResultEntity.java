package com.convergeai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "consensus_results")
public class ConsensusResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private QuestionEntity question;

    @Column(name = "final_answer", nullable = false, columnDefinition = "text")
    private String finalAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "agreement_points", columnDefinition = "jsonb")
    private List<String> agreementPoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "disagreement_points", columnDefinition = "jsonb")
    private List<String> disagreementPoints;

    @Column(name = "confidence_score", nullable = false)
    private int confidenceScore;

    @Column(length = 120)
    private String model;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConsensusResultEntity() {
    }

    public ConsensusResultEntity(QuestionEntity question,
                                 String finalAnswer,
                                 List<String> agreementPoints,
                                 List<String> disagreementPoints,
                                 int confidenceScore,
                                 String model) {
        this.question = question;
        this.finalAnswer = finalAnswer;
        this.agreementPoints = agreementPoints;
        this.disagreementPoints = disagreementPoints;
        this.confidenceScore = confidenceScore;
        this.model = model;
    }

    public UUID getId() {
        return id;
    }

    public QuestionEntity getQuestion() {
        return question;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public List<String> getAgreementPoints() {
        return agreementPoints;
    }

    public List<String> getDisagreementPoints() {
        return disagreementPoints;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public String getModel() {
        return model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
