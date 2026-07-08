package com.convergeai.web;

import com.convergeai.dto.DebateEvent;
import com.convergeai.dto.QuestionSummaryDto;
import com.convergeai.dto.SubmitQuestionRequest;
import com.convergeai.service.QuestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * STOMP entry point for question submission (client → /app/debate).
 *
 * The client includes a self-generated {@code clientRequestId} and listens on
 * /topic/debate/requests/{clientRequestId}; the server answers there with a
 * question.accepted event carrying the questionId, which the client then uses
 * to subscribe to the debate topic. REST submission remains the primary path.
 */
@Controller
public class DebateSocketController {

    private static final Logger log = LoggerFactory.getLogger(DebateSocketController.class);

    private final QuestionService questionService;
    private final SimpMessagingTemplate messagingTemplate;

    public DebateSocketController(QuestionService questionService, SimpMessagingTemplate messagingTemplate) {
        this.questionService = questionService;
        this.messagingTemplate = messagingTemplate;
    }

    public record StompQuestionSubmit(
            @Valid SubmitQuestionRequest question,
            String clientRequestId
    ) {
    }

    @MessageMapping("/debate")
    public void submit(StompQuestionSubmit payload) {
        String replyTopic = "/topic/debate/requests/" + payload.clientRequestId();
        try {
            QuestionSummaryDto summary = questionService.submit(payload.question());
            messagingTemplate.convertAndSend(replyTopic, DebateEvent.of(
                    DebateEvent.Types.QUESTION_ACCEPTED, summary.id(),
                    Map.of("questionId", summary.id(), "status", summary.status())));
        } catch (Exception e) {
            log.warn("STOMP question submission failed: {}", e.getMessage());
            messagingTemplate.convertAndSend(replyTopic, DebateEvent.of(
                    DebateEvent.Types.DEBATE_ERROR, null,
                    Map.of("error", e.getMessage() == null ? "Submission failed" : e.getMessage())));
        }
    }
}
