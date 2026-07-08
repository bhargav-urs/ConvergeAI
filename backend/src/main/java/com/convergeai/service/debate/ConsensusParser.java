package com.convergeai.service.debate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Defensive parser for the Consensus Engine's JSON. Free-tier models routinely
 * wrap JSON in markdown fences, add commentary around it, or rename keys, so
 * parsing is tolerant: it extracts the outermost JSON object from the text and
 * accepts several aliases per field.
 */
@Component
public class ConsensusParser {

    private static final List<String> FINAL_ANSWER_KEYS =
            List.of("final_answer", "finalAnswer", "answer", "final");
    private static final List<String> AGREEMENT_KEYS =
            List.of("areas_of_agreement", "areasOfAgreement", "agreement_points", "agreements", "agreement");
    private static final List<String> DISAGREEMENT_KEYS =
            List.of("areas_of_disagreement", "areasOfDisagreement", "disagreement_points", "disagreements", "disagreement");
    private static final List<String> CONFIDENCE_KEYS =
            List.of("confidence_score", "confidenceScore", "confidence");

    private final ObjectMapper objectMapper;

    public ConsensusParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @return the parsed payload, or empty if no usable JSON object could be
     *         extracted (caller decides whether to retry or fall back)
     */
    public Optional<ConsensusPayload> parse(String rawModelOutput) {
        if (rawModelOutput == null || rawModelOutput.isBlank()) {
            return Optional.empty();
        }
        Optional<JsonNode> root = extractJsonObject(rawModelOutput);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        JsonNode node = root.get();

        String finalAnswer = firstText(node, FINAL_ANSWER_KEYS);
        if (finalAnswer == null || finalAnswer.isBlank()) {
            return Optional.empty();
        }
        List<String> agreement = firstStringList(node, AGREEMENT_KEYS);
        List<String> disagreement = firstStringList(node, DISAGREEMENT_KEYS);
        int confidence = firstConfidence(node);

        return Optional.of(new ConsensusPayload(finalAnswer.strip(), agreement, disagreement, confidence));
    }

    private Optional<JsonNode> extractJsonObject(String text) {
        String candidate = stripCodeFences(text);
        int start = candidate.indexOf('{');
        int end = candidate.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return Optional.empty();
        }
        // Try the widest span first, then narrow from the right — handles trailing
        // commentary after the JSON object.
        for (int close = end; close > start; close = candidate.lastIndexOf('}', close - 1)) {
            try {
                JsonNode node = objectMapper.readTree(candidate.substring(start, close + 1));
                if (node != null && node.isObject()) {
                    return Optional.of(node);
                }
            } catch (Exception ignored) {
                // keep narrowing
            }
        }
        return Optional.empty();
    }

    private static String stripCodeFences(String text) {
        String stripped = text.strip();
        if (stripped.startsWith("```")) {
            int firstNewline = stripped.indexOf('\n');
            if (firstNewline > 0) {
                stripped = stripped.substring(firstNewline + 1);
            }
            int fenceEnd = stripped.lastIndexOf("```");
            if (fenceEnd >= 0) {
                stripped = stripped.substring(0, fenceEnd);
            }
        }
        return stripped;
    }

    private static String firstText(JsonNode node, List<String> keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                return value.isTextual() ? value.asText() : value.toString();
            }
        }
        return null;
    }

    private static List<String> firstStringList(JsonNode node, List<String> keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value == null || value.isNull()) {
                continue;
            }
            List<String> items = new ArrayList<>();
            if (value.isArray()) {
                value.forEach(item -> items.add(item.isTextual() ? item.asText() : item.toString()));
            } else if (value.isTextual() && !value.asText().isBlank()) {
                items.add(value.asText());
            }
            return items;
        }
        return List.of();
    }

    private int firstConfidence(JsonNode node) {
        for (String key : CONFIDENCE_KEYS) {
            JsonNode value = node.get(key);
            if (value == null || value.isNull()) {
                continue;
            }
            double score;
            if (value.isNumber()) {
                score = value.asDouble();
            } else if (value.isTextual()) {
                try {
                    score = Double.parseDouble(value.asText().replace("%", "").strip());
                } catch (NumberFormatException e) {
                    continue;
                }
            } else {
                continue;
            }
            // Models sometimes report confidence on a 0-1 scale.
            if (score > 0 && score <= 1.0) {
                score *= 100;
            }
            return (int) Math.round(Math.max(0, Math.min(100, score)));
        }
        return 50;
    }
}
