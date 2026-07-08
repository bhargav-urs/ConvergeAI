package com.convergeai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TextSimilarityTest {

    @Test
    void identicalTextsScoreOne() {
        assertThat(TextSimilarity.jaccard("The term is 24 months", "The term is 24 months"))
                .isEqualTo(1.0);
    }

    @Test
    void disjointTextsScoreZero() {
        assertThat(TextSimilarity.jaccard("alpha beta", "gamma delta")).isEqualTo(0.0);
    }

    @Test
    void isCaseAndPunctuationInsensitive() {
        assertThat(TextSimilarity.jaccard("Hello, World!", "hello world")).isEqualTo(1.0);
    }

    @Test
    void partialOverlapScoresBetweenZeroAndOne() {
        double score = TextSimilarity.jaccard("a b c d", "a b x y");
        // intersection {a,b}=2, union {a,b,c,d,x,y}=6
        assertThat(score).isCloseTo(2.0 / 6.0, within(1e-9));
    }

    @Test
    void handlesNullAndEmpty() {
        assertThat(TextSimilarity.jaccard(null, null)).isEqualTo(1.0);
        assertThat(TextSimilarity.jaccard("", "")).isEqualTo(1.0);
        assertThat(TextSimilarity.jaccard("words", "")).isEqualTo(0.0);
        assertThat(TextSimilarity.jaccard(null, "words")).isEqualTo(0.0);
    }
}
