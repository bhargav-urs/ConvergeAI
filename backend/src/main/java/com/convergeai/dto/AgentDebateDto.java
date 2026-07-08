package com.convergeai.dto;

import com.convergeai.domain.AgentName;

/**
 * The full trajectory of one agent through a debate: initial answer (round 1),
 * the critique it authored (round 2), and its revised answer (round 3).
 */
public record AgentDebateDto(
        AgentName agent,
        String displayName,
        String roleDescription,
        String model,
        AgentMessageDto initialResponse,
        AgentMessageDto critique,
        AgentMessageDto revision
) {
}
