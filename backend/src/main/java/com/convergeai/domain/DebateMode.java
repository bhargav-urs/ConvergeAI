package com.convergeai.domain;

/**
 * How thorough the debate should be.
 *
 * <ul>
 *   <li>{@code NORMAL} — full pipeline: independent answers → cross-critique →
 *       revision → consensus. Maximum trustworthiness.</li>
 *   <li>{@code FAST} — single round: independent (concise) answers → consensus.
 *       Skips critique and revision and caps output length for speed.</li>
 * </ul>
 */
public enum DebateMode {
    NORMAL,
    FAST
}
