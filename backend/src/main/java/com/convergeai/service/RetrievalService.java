package com.convergeai.service;

import com.convergeai.config.ConvergeProperties;
import com.convergeai.domain.DocumentChunkEntity;
import com.convergeai.domain.QuestionContextEntity;
import com.convergeai.domain.QuestionEntity;
import com.convergeai.dto.ContextSnippetDto;
import com.convergeai.repository.ChunkMatch;
import com.convergeai.repository.DocumentChunkRepository;
import com.convergeai.repository.QuestionContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Phase 2 (retrieval): embeds the question with the same local model used at
 * indexing time and runs a pgvector cosine similarity search scoped to the
 * question's document. The retrieved set is persisted so citations stay
 * auditable after the fact.
 */
@Service
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final QuestionContextRepository contextRepository;
    private final ConvergeProperties properties;

    public RetrievalService(EmbeddingService embeddingService,
                            DocumentChunkRepository chunkRepository,
                            QuestionContextRepository contextRepository,
                            ConvergeProperties properties) {
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
        this.contextRepository = contextRepository;
        this.properties = properties;
    }

    @Transactional
    public List<ContextSnippetDto> retrieveAndPersist(QuestionEntity question, UUID documentId) {
        float[] queryVector = embeddingService.embed(question.getQuestionText());
        String literal = EmbeddingService.toVectorLiteral(queryVector);

        List<ChunkMatch> matches = chunkRepository.findNearestChunks(
                documentId, literal, properties.retrieval().topK());

        List<ContextSnippetDto> snippets = new ArrayList<>(matches.size());
        int rank = 1;
        for (ChunkMatch match : matches) {
            DocumentChunkEntity chunk = chunkRepository.getReferenceById(match.getId());
            contextRepository.save(new QuestionContextEntity(question, chunk, rank, match.getDistance()));
            snippets.add(new ContextSnippetDto(
                    match.getId(), match.getChunkIndex(), rank, match.getContent(), match.getDistance()));
            rank++;
        }
        return snippets;
    }
}
