package com.aniglow.mapper;

import com.aniglow.dto.rating.RatingDto;
import com.aniglow.dto.rating.RatingReplyDto;
import com.aniglow.entity.Rating;
import com.aniglow.entity.RatingReply;
import com.aniglow.entity.User;
import com.aniglow.service.UserDisplayNameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RatingMapper {

    private final UserDisplayNameResolver displayNameResolver;

    public RatingDto toDto(Rating rating) {
        User user = rating.getUser();
        return RatingDto.builder()
                .id(rating.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(displayNameResolver.resolvePublic(user))
                .avatarUrl(user.getAvatarUrl())
                .animeId(rating.getAnime().getId())
                .animeTitle(rating.getAnime().getTitle())
                .animeCoverImage(rating.getAnime().getCoverImage())
                .score(number(rating.getScore()))
                .review(rating.getReview())
                .storyScore(number(rating.getStoryScore()))
                .animationScore(number(rating.getAnimationScore()))
                .soundScore(number(rating.getSoundScore()))
                .characterScore(number(rating.getCharacterScore()))
                .enjoymentScore(number(rating.getEnjoymentScore()))
                .isRecommended(rating.getIsRecommended())
                .containsSpoiler(rating.getContainsSpoiler())
                .likeCount(rating.getLikeCount())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }

    public RatingReplyDto toReplyDto(RatingReply reply) {
        User user = reply.getUser();
        return RatingReplyDto.builder()
                .id(reply.getId())
                .ratingId(reply.getRating().getId())
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(displayNameResolver.resolvePublic(user))
                .avatarUrl(user.getAvatarUrl())
                .content(reply.getContent())
                .likeCount(reply.getLikeCount())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }

    private Double number(Number value) {
        return value == null ? null : value.doubleValue();
    }
}
