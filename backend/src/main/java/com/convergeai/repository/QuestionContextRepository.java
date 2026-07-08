package com.convergeai.repository;

import com.convergeai.domain.QuestionContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionContextRepository extends JpaRepository<QuestionContextEntity, UUID> {

    @Query("""
            SELECT qc FROM QuestionContextEntity qc
            JOIN FETCH qc.chunk
            WHERE qc.question.id = :questionId
            ORDER BY qc.rank
            """)
    List<QuestionContextEntity> findByQuestionIdWithChunks(@Param("questionId") UUID questionId);
}
