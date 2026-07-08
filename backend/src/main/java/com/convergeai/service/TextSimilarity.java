package com.convergeai.service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lightweight lexical similarity used for the "answer stability" analytics
 * metric (how much an agent's answer changed between round 1 and round 3).
 * Word-level Jaccard is order-insensitive and O(n), which is exactly enough:
 * we're measuring revision magnitude, not semantic equivalence.
 */
public final class TextSimilarity {

    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");

    private TextSimilarity() {
    }

    /** @return Jaccard similarity of the two texts' word sets, in [0, 1]. */
    public static double jaccard(String a, String b) {
        Set<String> wordsA = tokenize(a);
        Set<String> wordsB = tokenize(b);
        if (wordsA.isEmpty() && wordsB.isEmpty()) {
            return 1.0;
        }
        if (wordsA.isEmpty() || wordsB.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(wordsA);
        intersection.retainAll(wordsB);
        Set<String> union = new HashSet<>(wordsA);
        union.addAll(wordsB);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> tokenize(String text) {
        Set<String> words = new HashSet<>();
        if (text == null) {
            return words;
        }
        for (String token : NON_WORD.split(text.toLowerCase(Locale.ROOT))) {
            if (!token.isBlank()) {
                words.add(token);
            }
        }
        return words;
    }
}
