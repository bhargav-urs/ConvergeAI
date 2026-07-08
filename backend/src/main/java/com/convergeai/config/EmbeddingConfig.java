package com.convergeai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-process, quantized ONNX embedding model (all-MiniLM-L6-v2, 384 dimensions).
 * Runs entirely inside the JVM: zero API cost, zero network latency, and the
 * exact same vector space for indexing and querying.
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }
}
