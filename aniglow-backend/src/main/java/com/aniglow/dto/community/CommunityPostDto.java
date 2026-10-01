package com.aniglow.dto.community;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostDto {

    private Long id;
    private Long communityId;
    private String communitySlug;
    private String communityName;
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String title;
    private String content;
    private String coverImage;
    private List<String> images;
    private Long likeCount;
    private Boolean likedByMe;
    private Long replyCount;
    private Long viewCount;
    private Boolean pinned;
    private Boolean featured;
    private LocalDateTime lastRepliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
