package com.convergeai.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around the in-process ONNX MiniLM model. Both documents and
 * queries must go through this service so they land in the same vector space.
 */
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] embed(String text) {
        return embeddingModel.embed(text).content().vector();
    }

    /**
     * Embeds segments in sub-batches to keep peak memory flat on large documents.
     */
    public List<float[]> embedAll(List<TextSegment> segments, int batchSize) {
        List<float[]> vectors = new ArrayList<>(segments.size());
        for (int from = 0; from < segments.size(); from += batchSize) {
            int to = Math.min(from + batchSize, segments.size());
            List<Embedding> batch = embeddingModel.embedAll(segments.subList(from, to)).content();
            for (Embedding embedding : batch) {
                vectors.add(embedding.vector());
            }
        }
        return vectors;
    }

    /**
     * Renders a vector as a pgvector text literal ("[0.1,0.2,...]") for use in
     * native similarity queries.
     */
    public static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 12 + 2);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
