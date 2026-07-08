package com.convergeai.repository;

import com.convergeai.domain.AgentResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentResponseRepository extends JpaRepository<AgentResponseEntity, UUID> {

    List<AgentResponseEntity> findByQuestionIdOrderByRoundAscAgentNameAsc(UUID questionId);

    List<AgentResponseEntity> findByQuestionIdIn(List<UUID> questionIds);
}
