package com.aniglow.repository;

import com.aniglow.entity.AgentVoteCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AgentVoteCandidateRepository extends JpaRepository<AgentVoteCandidate, Long> {

    List<AgentVoteCandidate> findByWeekKeyOrderByVoteCountDesc(String weekKey);

    Optional<AgentVoteCandidate> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByWeekKey(String weekKey);
}
