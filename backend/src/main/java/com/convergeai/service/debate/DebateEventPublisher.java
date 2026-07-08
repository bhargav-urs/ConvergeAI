package com.convergeai.service.debate;

import com.convergeai.dto.DebateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Single choke point for all outbound STOMP traffic. Publishing failures are
 * logged but never allowed to break the debate pipeline — clients can always
 * recover state via REST.
 */
@Component
public class DebateEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DebateEventPublisher.class);

    public static final String DEBATE_TOPIC_PREFIX = "/topic/debate/";
    public static final String DOCUMENTS_TOPIC = "/topic/documents";

    private final SimpMessagingTemplate messagingTemplate;

    public DebateEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(UUID questionId, DebateEvent event) {
        try {
            messagingTemplate.convertAndSend(DEBATE_TOPIC_PREFIX + questionId, event);
        } catch (Exception e) {
            log.warn("Failed to publish {} event for question {}: {}", event.type(), questionId, e.getMessage());
        }
    }

    public void publishDocumentEvent(DebateEvent event) {
        try {
            messagingTemplate.convertAndSend(DOCUMENTS_TOPIC, event);
        } catch (Exception e) {
            log.warn("Failed to publish {} document event: {}", event.type(), e.getMessage());
        }
    }
}
