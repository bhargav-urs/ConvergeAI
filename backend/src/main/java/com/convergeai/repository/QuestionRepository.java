package com.convergeai.repository;

import com.convergeai.domain.QuestionEntity;
import com.convergeai.domain.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, UUID> {

    List<QuestionEntity> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    List<QuestionEntity> findTop50ByOrderByCreatedAtDesc();

    /** Rolling analytics window: the most recent completed debates. */
    List<QuestionEntity> findTop100ByStatusOrderByCreatedAtDesc(QuestionStatus status);

    long countByStatus(QuestionStatus status);

    @Query("SELECT AVG(q.processingTimeMs) FROM QuestionEntity q WHERE q.status = com.convergeai.domain.QuestionStatus.COMPLETED")
    Double averageProcessingTimeMs();
}
