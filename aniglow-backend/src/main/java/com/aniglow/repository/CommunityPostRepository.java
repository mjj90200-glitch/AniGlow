package com.aniglow.repository;

import com.aniglow.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    Page<CommunityPost> findByCommunitySlugAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(String slug, Pageable pageable);

    Page<CommunityPost> findByCommunitySlugAndIsDeletedFalseOrderByLikeCountDescReplyCountDescCreatedAtDesc(String slug, Pageable pageable);

    long countByCommunityIdAndIsDeletedFalse(Long communityId);

    Page<CommunityPost> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<CommunityPost> findByCommunityRelatedAnimeIdAndIsDeletedFalseOrderByPinnedDescLastRepliedAtDescCreatedAtDesc(
            Long animeId,
            Pageable pageable);
}
