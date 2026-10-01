package com.aniglow.repository;

import com.aniglow.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByCommunitySlugAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(String slug, Pageable pageable);

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByCommunitySlugAndIsDeletedFalseOrderByLikeCountDescReplyCountDescCreatedAtDesc(String slug, Pageable pageable);

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByCommunitySlugInAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(
            Collection<String> slugs,
            Pageable pageable);

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByCommunitySlugInAndIsDeletedFalseOrderByLikeCountDescReplyCountDescCreatedAtDesc(
            Collection<String> slugs,
            Pageable pageable);

    long countByCommunityIdAndIsDeletedFalse(Long communityId);

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByCommunityRelatedAnimeIdAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(
            Long animeId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"community", "user"})
    Optional<CommunityPost> findByIdAndIsDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"community", "user"})
    @Query("SELECT post FROM CommunityPost post WHERE post.id = :id AND post.isDeleted = false")
    Optional<CommunityPost> findActiveForUpdate(@Param("id") Long id);
}
