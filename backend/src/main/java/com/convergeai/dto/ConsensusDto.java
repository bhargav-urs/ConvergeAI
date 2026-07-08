package com.convergeai.dto;

import com.convergeai.domain.ConsensusResultEntity;

import java.util.List;

public record ConsensusDto(
        String finalAnswer,
        List<String> agreementPoints,
        List<String> disagreementPoints,
        int confidenceScore,
        String model
) {

    public static ConsensusDto from(ConsensusResultEntity entity) {
        return new ConsensusDto(
                entity.getFinalAnswer(),
                entity.getAgreementPoints(),
                entity.getDisagreementPoints(),
                entity.getConfidenceScore(),
                entity.getModel()
        );
    }
}
