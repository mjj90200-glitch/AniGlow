package com.aniglow.repository;

import com.aniglow.entity.CommunityReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityReplyRepository extends JpaRepository<CommunityReply, Long> {

    @EntityGraph(attributePaths = {"post", "user"})
    Page<CommunityReply> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId, Pageable pageable);

    long countByPostIdAndIsDeletedFalse(Long postId);
}
