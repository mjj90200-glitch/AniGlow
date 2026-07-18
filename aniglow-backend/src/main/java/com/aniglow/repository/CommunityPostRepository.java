package com.aniglow.repository;

import com.aniglow.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByCommunitySlugAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(String slug, Pageable pageable);

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByCommunitySlugAndIsDeletedFalseOrderByLikeCountDescReplyCountDescCreatedAtDesc(String slug, Pageable pageable);

    long countByCommunityIdAndIsDeletedFalse(Long communityId);

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"community", "user"})
    Page<CommunityPost> findByCommunityRelatedAnimeIdAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(
            Long animeId,
            Pageable pageable);
}
