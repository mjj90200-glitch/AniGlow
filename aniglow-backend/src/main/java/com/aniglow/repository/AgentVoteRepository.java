package com.aniglow.repository;

import com.aniglow.entity.AgentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AgentVoteRepository extends JpaRepository<AgentVote, Long> {

    Optional<AgentVote> findByUserIdAndWeekKey(Long userId, String weekKey);

    long countByCandidateId(Long candidateId);

    boolean existsByUserIdAndWeekKey(Long userId, String weekKey);
}
