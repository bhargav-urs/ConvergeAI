package com.convergeai.service;

import com.convergeai.config.ConvergeProperties;
import com.convergeai.domain.DocumentEntity;
import com.convergeai.dto.ChunkDto;
import com.convergeai.dto.DocumentDto;
import com.convergeai.exception.BadRequestException;
import com.convergeai.exception.NotFoundException;
import com.convergeai.repository.DocumentChunkRepository;
import com.convergeai.repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Synchronous document API surface: accepts uploads, exposes documents and
 * chunk listings. Heavy lifting (parse → chunk → embed → index) is delegated
 * to {@link DocumentIngestionWorker} on a background virtual thread.
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentIngestionWorker ingestionWorker;
    private final ConvergeProperties properties;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentChunkRepository chunkRepository,
                           DocumentIngestionWorker ingestionWorker,
                           ConvergeProperties properties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.ingestionWorker = ingestionWorker;
        this.properties = properties;
    }

    public DocumentDto upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }
        String filename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
        String extension = extensionOf(filename);
        List<String> allowed = properties.ingestion().allowedExtensions();
        if (!allowed.contains(extension)) {
            throw new BadRequestException(
                    "Unsupported file type '." + extension + "'. Allowed: " + String.join(", ", allowed));
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file: " + e.getMessage());
        }

        DocumentEntity document = documentRepository.save(
                new DocumentEntity(filename, file.getContentType(), file.getSize()));
        // Entity is committed before the async worker starts (no outer transaction),
        // so the worker always sees the row.
        ingestionWorker.ingestAsync(document.getId(), bytes);
        return DocumentDto.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> list() {
        return documentRepository.findAllByOrderByUploadedAtDesc().stream()
                .map(DocumentDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDto get(UUID id) {
        return DocumentDto.from(requireDocument(id));
    }

    @Transactional(readOnly = true)
    public Page<ChunkDto> chunks(UUID documentId, int page, int size) {
        requireDocument(documentId);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return chunkRepository
                .findByDocumentIdOrderByChunkIndex(documentId, PageRequest.of(Math.max(page, 0), safeSize))
                .map(ChunkDto::from);
    }

    @Transactional
    public void delete(UUID id) {
        DocumentEntity document = requireDocument(id);
        // Chunks, questions, responses and consensus rows cascade at the DB level.
        documentRepository.delete(document);
    }

    DocumentEntity requireDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
