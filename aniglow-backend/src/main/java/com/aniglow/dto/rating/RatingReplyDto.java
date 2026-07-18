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
public class RatingReplyDto {

    private Long id;
    private Long ratingId;
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String content;
    private Long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
