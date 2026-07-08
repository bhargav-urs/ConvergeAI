package com.convergeai.web;

import com.convergeai.dto.ChunkDto;
import com.convergeai.dto.DocumentDto;
import com.convergeai.service.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** Accepts a PDF (or txt/md) and indexes it asynchronously. Returns 202 immediately. */
    @PostMapping
    public ResponseEntity<DocumentDto> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(documentService.upload(file));
    }

    @GetMapping
    public List<DocumentDto> list() {
        return documentService.list();
    }

    @GetMapping("/{id}")
    public DocumentDto get(@PathVariable UUID id) {
        return documentService.get(id);
    }

    @GetMapping("/{id}/chunks")
    public Page<ChunkDto> chunks(@PathVariable UUID id,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return documentService.chunks(id, page, size);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
