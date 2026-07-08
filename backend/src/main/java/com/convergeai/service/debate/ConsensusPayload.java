package com.convergeai.service.debate;

import java.util.List;

/**
 * Parsed output of the Consensus Engine.
 */
public record ConsensusPayload(
        String finalAnswer,
        List<String> areasOfAgreement,
        List<String> areasOfDisagreement,
        int confidenceScore
) {
}
