package com.aniglow.repository;

import com.aniglow.entity.CommunityReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityReplyLikeRepository extends JpaRepository<CommunityReplyLike, Long> {

    Optional<CommunityReplyLike> findByReplyIdAndUserId(Long replyId, Long userId);

    @Query("SELECT reaction.reply.id FROM CommunityReplyLike reaction WHERE reaction.user.id = :userId AND reaction.reply.id IN :replyIds")
    List<Long> findLikedReplyIds(@Param("userId") Long userId, @Param("replyIds") Collection<Long> replyIds);
}
