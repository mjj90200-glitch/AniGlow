package com.aniglow.repository;

import com.aniglow.entity.CommunityReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CommunityReplyRepository extends JpaRepository<CommunityReply, Long> {

    @EntityGraph(attributePaths = {"post", "user"})
    Page<CommunityReply> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId, Pageable pageable);

    long countByPostIdAndIsDeletedFalse(Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"post", "post.community", "post.user", "user"})
    @Query("SELECT reply FROM CommunityReply reply WHERE reply.id = :id AND reply.isDeleted = false AND reply.post.isDeleted = false")
    Optional<CommunityReply> findActiveForUpdate(@Param("id") Long id);
}
