package com.convergeai.service;

import com.convergeai.config.ConvergeProperties;
import com.convergeai.domain.DocumentChunkEntity;
import com.convergeai.domain.DocumentEntity;
import com.convergeai.domain.DocumentStatus;
import com.convergeai.dto.DebateEvent;
import com.convergeai.exception.DocumentProcessingException;
import com.convergeai.repository.DocumentChunkRepository;
import com.convergeai.repository.DocumentRepository;
import com.convergeai.service.debate.DebateEventPublisher;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 1 of the RAG pipeline: parse (Apache Tika) → split into overlapping
 * chunks → embed locally (ONNX MiniLM) → index into pgvector. Runs on a
 * background virtual thread so uploads return immediately; progress is
 * announced over STOMP on /topic/documents.
 */
@Component
public class DocumentIngestionWorker {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionWorker.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final DebateEventPublisher eventPublisher;
    private final ConvergeProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();

    public DocumentIngestionWorker(DocumentRepository documentRepository,
                                   DocumentChunkRepository chunkRepository,
                                   EmbeddingService embeddingService,
                                   DebateEventPublisher eventPublisher,
                                   ConvergeProperties properties,
                                   TransactionTemplate transactionTemplate) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    @Async
    public void ingestAsync(UUID documentId, byte[] fileBytes) {
        long startedAt = System.currentTimeMillis();
        try {
            int chunkCount = ingest(documentId, fileBytes);
            long elapsed = System.currentTimeMillis() - startedAt;
            log.info("Indexed document {} into {} chunks in {} ms", documentId, chunkCount, elapsed);
            eventPublisher.publishDocumentEvent(DebateEvent.of(
                    DebateEvent.Types.DOCUMENT_INDEXED, null,
                    Map.of("documentId", documentId, "chunkCount", chunkCount, "elapsedMs", elapsed)));
        } catch (Exception e) {
            log.error("Failed to ingest document {}", documentId, e);
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            transactionTemplate.executeWithoutResult(tx ->
                    documentRepository.findById(documentId).ifPresent(doc -> {
                        doc.setStatus(DocumentStatus.FAILED);
                        doc.setErrorMessage(truncate(reason, 900));
                    }));
            eventPublisher.publishDocumentEvent(DebateEvent.of(
                    DebateEvent.Types.DOCUMENT_FAILED, null,
                    Map.of("documentId", documentId, "error", reason)));
        }
    }

    private int ingest(UUID documentId, byte[] fileBytes) {
        Document parsed;
        try {
            parsed = parser.parse(new ByteArrayInputStream(fileBytes));
        } catch (Exception e) {
            throw new DocumentProcessingException("Could not parse document: " + e.getMessage(), e);
        }

        // NUL bytes (possible in malformed PDFs) are rejected by PostgreSQL text columns.
        String text = parsed.text() == null ? "" : parsed.text().replace("\u0000", "").strip();
        if (text.isEmpty()) {
            throw new DocumentProcessingException(
                    "No extractable text found. Scanned/image-only PDFs are not supported.");
        }

        DocumentSplitter splitter = DocumentSplitters.recursive(
                properties.ingestion().chunkSizeChars(),
                properties.ingestion().chunkOverlapChars());
        List<TextSegment> segments = splitter.split(Document.from(text));
        if (segments.isEmpty()) {
            throw new DocumentProcessingException("Document produced no chunks after splitting");
        }

        List<float[]> vectors = embeddingService.embedAll(segments, properties.ingestion().embeddingBatchSize());

        Integer chunkCount = transactionTemplate.execute(tx -> {
            DocumentEntity document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new DocumentProcessingException(
                            "Document " + documentId + " disappeared during ingestion"));
            List<DocumentChunkEntity> chunks = new ArrayList<>(segments.size());
            for (int i = 0; i < segments.size(); i++) {
                chunks.add(new DocumentChunkEntity(document, i, segments.get(i).text(), vectors.get(i)));
            }
            chunkRepository.saveAll(chunks);
            document.setStatus(DocumentStatus.READY);
            document.setChunkCount(chunks.size());
            document.setCharCount(text.length());
            return chunks.size();
        });
        return chunkCount == null ? 0 : chunkCount;
    }

    private static String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }
}
