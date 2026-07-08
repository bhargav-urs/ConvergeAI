package com.convergeai.web;

import com.convergeai.dto.QuestionDetailDto;
import com.convergeai.dto.QuestionSummaryDto;
import com.convergeai.dto.SubmitQuestionRequest;
import com.convergeai.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * Submits a question and starts the debate pipeline. The client should
     * subscribe to /topic/debate/{id} with the returned id, then fetch
     * GET /api/questions/{id} once to hydrate any events that fired before the
     * subscription landed.
     */
    @PostMapping
    public ResponseEntity<QuestionSummaryDto> submit(@Valid @RequestBody SubmitQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(questionService.submit(request));
    }

    @GetMapping
    public List<QuestionSummaryDto> list(@RequestParam(required = false) UUID documentId) {
        return questionService.list(documentId);
    }

    @GetMapping("/{id}")
    public QuestionDetailDto detail(@PathVariable UUID id) {
        return questionService.detail(id);
    }
}
