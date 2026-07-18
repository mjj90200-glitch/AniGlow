package com.aniglow.dto.community;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityReplyDto {

    private Long id;
    private Long postId;
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String content;
    private Long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
