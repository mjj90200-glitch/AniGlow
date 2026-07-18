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
public class CommunityDto {

    private Long id;
    private String slug;
    private String name;
    private String description;
    private String coverImage;
    private String category;
    private List<String> tags;
    private Long relatedAnimeId;
    private String relatedAnimeTitle;
    private Long postCount;
    private Long memberCount;
    private Long heatScore;
    private Boolean featured;
    private Long creatorId;
    private LocalDateTime createdAt;
}
