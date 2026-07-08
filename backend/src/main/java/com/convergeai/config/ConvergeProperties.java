package com.convergeai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Application-level tuning knobs. Chunk sizes are expressed in characters;
 * 2000 chars / 200 overlap approximates the brief's 500-token / 50-token target
 * for English prose without pulling in a tokenizer dependency.
 */
@ConfigurationProperties(prefix = "converge")
public record ConvergeProperties(
        Cors cors,
        Ingestion ingestion,
        Retrieval retrieval
) {

    public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
    }

    public record Ingestion(
            @DefaultValue("2000") int chunkSizeChars,
            @DefaultValue("200") int chunkOverlapChars,
            @DefaultValue({"pdf", "txt", "md"}) List<String> allowedExtensions,
            @DefaultValue("16") int embeddingBatchSize
    ) {
    }

    public record Retrieval(@DefaultValue("5") int topK) {
    }
}
