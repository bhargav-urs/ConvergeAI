package com.convergeai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One agent utterance in one debate round.
 * Round 1 = initial answer, round 2 = critique of peers, round 3 = revised answer.
 * {@code critiqueReceived} on a round-3 row stores the combined peer feedback the
 * agent revised against.
 */
@Entity
@Table(name = "agent_responses")
public class AgentResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_name", nullable = false, length = 50)
    private AgentName agentName;

    @Column(length = 120)
    private String model;

    @Column(name = "round", nullable = false)
    private int round;

    @Column(columnDefinition = "text")
    private String response;

    @Column(name = "critique_received", columnDefinition = "text")
    private String critiqueReceived;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgentResponseStatus status = AgentResponseStatus.OK;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentResponseEntity() {
    }

    public AgentResponseEntity(QuestionEntity question, AgentName agentName, String model, int round) {
        this.question = question;
        this.agentName = agentName;
        this.model = model;
        this.round = round;
    }

    public UUID getId() {
        return id;
    }

    public QuestionEntity getQuestion() {
        return question;
    }

    public AgentName getAgentName() {
        return agentName;
    }

    public String getModel() {
        return model;
    }

    /** Overwritten with the model that actually served the call (fallback routing). */
    public void setModel(String model) {
        this.model = model;
    }

    public int getRound() {
        return round;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getCritiqueReceived() {
        return critiqueReceived;
    }

    public void setCritiqueReceived(String critiqueReceived) {
        this.critiqueReceived = critiqueReceived;
    }

    public AgentResponseStatus getStatus() {
        return status;
    }

    public void setStatus(AgentResponseStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
