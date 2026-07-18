package com.aniglow.repository;

import com.aniglow.entity.RatingReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingReplyRepository extends JpaRepository<RatingReply, Long> {

    @EntityGraph(attributePaths = {"rating", "user"})
    Page<RatingReply> findByRatingIdOrderByCreatedAtAsc(Long ratingId, Pageable pageable);

    long countByRatingId(Long ratingId);
}
