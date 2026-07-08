package com.convergeai.service.debate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ConsensusParserTest {

    private final ConsensusParser parser = new ConsensusParser(new ObjectMapper());

    @Test
    void parsesCleanJson() {
        Optional<ConsensusPayload> result = parser.parse("""
                {
                  "final_answer": "The contract term is 24 months [Chunk 1].",
                  "areas_of_agreement": ["Term length", "Renewal clause"],
                  "areas_of_disagreement": ["Penalty interpretation"],
                  "confidence_score": 88
                }""");

        assertThat(result).isPresent();
        assertThat(result.get().finalAnswer()).contains("24 months");
        assertThat(result.get().areasOfAgreement()).containsExactly("Term length", "Renewal clause");
        assertThat(result.get().areasOfDisagreement()).containsExactly("Penalty interpretation");
        assertThat(result.get().confidenceScore()).isEqualTo(88);
    }

    @Test
    void parsesJsonWrappedInMarkdownFences() {
        Optional<ConsensusPayload> result = parser.parse("""
                Here is the consensus:
                ```json
                {"final_answer": "Answer.", "areas_of_agreement": [], "areas_of_disagreement": [], "confidence_score": 70}
                ```
                """);

        assertThat(result).isPresent();
        assertThat(result.get().confidenceScore()).isEqualTo(70);
    }

    @Test
    void acceptsCamelCaseAliases() {
        Optional<ConsensusPayload> result = parser.parse("""
                {"finalAnswer": "Aliased.", "areasOfAgreement": ["a"], "confidenceScore": 42}""");

        assertThat(result).isPresent();
        assertThat(result.get().finalAnswer()).isEqualTo("Aliased.");
        assertThat(result.get().areasOfAgreement()).containsExactly("a");
        assertThat(result.get().confidenceScore()).isEqualTo(42);
    }

    @Test
    void normalisesFractionalConfidenceToPercent() {
        Optional<ConsensusPayload> result = parser.parse("""
                {"final_answer": "x", "confidence_score": 0.85}""");

        assertThat(result).isPresent();
        assertThat(result.get().confidenceScore()).isEqualTo(85);
    }

    @Test
    void clampsConfidenceIntoRange() {
        Optional<ConsensusPayload> result = parser.parse("""
                {"final_answer": "x", "confidence_score": 250}""");

        assertThat(result).isPresent();
        assertThat(result.get().confidenceScore()).isEqualTo(100);
    }

    @Test
    void rejectsTextWithoutJson() {
        assertThat(parser.parse("I could not reach a consensus, sorry.")).isEmpty();
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void rejectsJsonWithoutFinalAnswer() {
        assertThat(parser.parse("{\"confidence_score\": 90}")).isEmpty();
    }

    @Test
    void survivesTrailingCommentaryAfterJson() {
        Optional<ConsensusPayload> result = parser.parse("""
                {"final_answer": "Grounded answer.", "confidence_score": 77}
                Let me know if you need anything else!""");

        assertThat(result).isPresent();
        assertThat(result.get().finalAnswer()).isEqualTo("Grounded answer.");
    }
}
