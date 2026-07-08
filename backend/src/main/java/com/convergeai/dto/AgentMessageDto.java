package com.convergeai.dto;

import com.convergeai.domain.AgentName;
import com.convergeai.domain.AgentResponseEntity;
import com.convergeai.domain.AgentResponseStatus;

/**
 * A single agent utterance (answer, critique, or revision) as sent over the
 * WebSocket and embedded in question detail responses.
 */
public record AgentMessageDto(
        AgentName agent,
        String displayName,
        String model,
        int round,
        String content,
        String critiqueReceived,
        AgentResponseStatus status,
        String errorMessage,
        Long latencyMs
) {

    public static AgentMessageDto from(AgentResponseEntity entity) {
        return new AgentMessageDto(
                entity.getAgentName(),
                entity.getAgentName().displayName(),
                entity.getModel(),
                entity.getRound(),
                entity.getResponse(),
                entity.getCritiqueReceived(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getLatencyMs()
        );
    }
}
