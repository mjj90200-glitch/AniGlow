package com.aniglow.dto.rating;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDto {

    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Long animeId;
    private String animeTitle;
    private String animeCoverImage;
    private Double score;
    private String review;
    private Double storyScore;
    private Double animationScore;
    private Double soundScore;
    private Double characterScore;
    private Double enjoymentScore;
    private Boolean isRecommended;
    private Boolean containsSpoiler;
    private Long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
