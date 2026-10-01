package com.aniglow.dto.anime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimeDto {

    private Long id;
    private Long malId;
    private Long anilistId;
    private String title;
    private String titleJapanese;
    private String titleEnglish;
    private String titleCn;
    private String searchAliases;
    private String synopsis;
    private String synopsisCn;
    private String coverImage;
    private String country;
    private String trailerUrl;
    private String type;
    private String status;
    private LocalDate airedFrom;
    private LocalDate airedTo;
    private Integer episodes;
    private Integer durationMinutes;
    private String rating;
    private Long popularity;
    private Long membersCount;
    private Long favoritesCount;
    private Boolean hasAgent;
    private Double bayesianRating;
    private Double meanRating;
    private Long ratingCount;
    private Double communityBayesianRating;
    private Double communityMeanRating;
    private Long communityRatingCount;
    private Long fireflyVoteCount;
    private Set<String> genres;
    private String studio;
    private String source;
    private String season;
    private Integer year;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
