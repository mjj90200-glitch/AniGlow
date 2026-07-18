package com.aniglow.repository;

import com.aniglow.entity.FireflyVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FireflyVoteRepository extends JpaRepository<FireflyVote, Long> {

    long countByAnimeId(Long animeId);

    long countByUserId(Long userId);
}
