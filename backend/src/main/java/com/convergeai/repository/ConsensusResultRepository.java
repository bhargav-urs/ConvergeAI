package com.convergeai.repository;

import com.convergeai.domain.ConsensusResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsensusResultRepository extends JpaRepository<ConsensusResultEntity, UUID> {

    Optional<ConsensusResultEntity> findByQuestionId(UUID questionId);

    List<ConsensusResultEntity> findByQuestionIdIn(List<UUID> questionIds);

    @Query("SELECT AVG(c.confidenceScore) FROM ConsensusResultEntity c")
    Double averageConfidenceScore();
}
