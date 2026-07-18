package com.aniglow.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RatingRequest {

    @NotNull(message = "动漫ID不能为空")
    private Long animeId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于 1")
    @Max(value = 10, message = "评分不能高于 10")
    private Double score;

    private String review;

    @Size(max = 80)
    private String displayName;

    @Size(max = 2_000_000)
    private String avatarUrl;

    @Min(1)
    @Max(10)
    private Double storyScore;

    @Min(1)
    @Max(10)
    private Double animationScore;

    @Min(1)
    @Max(10)
    private Double soundScore;

    @Min(1)
    @Max(10)
    private Double characterScore;

    @Min(1)
    @Max(10)
    private Double enjoymentScore;

    private Boolean isRecommended;
    private Boolean containsSpoiler;
}
