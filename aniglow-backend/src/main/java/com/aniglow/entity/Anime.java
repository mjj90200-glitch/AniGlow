package com.aniglow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "anime", indexes = {
    @Index(name = "idx_mal_id", columnList = "mal_id", unique = true),
    @Index(name = "idx_rating", columnList = "bayesian_rating DESC"),
    @Index(name = "idx_popularity", columnList = "popularity"),
    @Index(name = "idx_has_agent", columnList = "has_agent"),
    @Index(name = "idx_title_cn", columnList = "title_cn"),
    @Index(name = "idx_anime_year_season", columnList = "year, season"),
    @Index(name = "idx_anime_status", columnList = "status"),
    @Index(name = "idx_community_rating", columnList = "community_bayesian_rating DESC"),
    @Index(name = "idx_firefly_votes", columnList = "firefly_vote_count DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Jikan API 的 MAL ID
    @Column(name = "mal_id", unique = true)
    private Long malId;

    // AniList API 的 ID（跨源去重键，与 mal_id 并存）
    @Column(name = "anilist_id", unique = true)
    private Long anilistId;

    // 国家/地区（AniList countryOfOrigin 权威映射：日本/中国/韩国/美国等）
    @Column(name = "country", length = 50)
    private String country;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "title_japanese", length = 200)
    private String titleJapanese;

    @Column(name = "title_english", length = 200)
    private String titleEnglish;

    @Column(name = "title_cn", length = 200)
    private String titleCn;

    @Column(name = "search_aliases", length = 1200)
    private String searchAliases;

    @Column(length = 2000)
    private String synopsis;

    @Column(name = "synopsis_cn", length = 2000)
    private String synopsisCn;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    @Column(length = 50)
    private String type; // TV, Movie, OVA, ONA, Special, Music

    @Column(length = 100)
    private String status; // Airing, Finished, Not yet aired

    @Column(name = "aired_from")
    private LocalDate airedFrom;

    @Column(name = "aired_to")
    private LocalDate airedTo;

    @Column(name = "episodes")
    private Integer episodes;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(length = 50)
    private String rating; // G, PG, PG-13, R, R+, Rx

    @Column(name = "popularity")
    private Long popularity;

    @Column(name = "members_count")
    private Long membersCount;

    @Column(name = "favorites_count")
    private Long favoritesCount;

    @Column(name = "has_agent", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean hasAgent = false;

    // 贝叶斯平均分
    @Column(name = "bayesian_rating", precision = 4, scale = 2)
    private BigDecimal bayesianRating;

    // 原始平均分
    @Column(name = "mean_rating", precision = 4, scale = 2)
    private BigDecimal meanRating;

    @Column(name = "rating_count")
    private Long ratingCount;

    @Column(name = "community_bayesian_rating", precision = 4, scale = 2)
    private BigDecimal communityBayesianRating;

    @Column(name = "community_mean_rating", precision = 4, scale = 2)
    private BigDecimal communityMeanRating;

    @Column(name = "community_rating_count", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long communityRatingCount = 0L;

    @Column(name = "firefly_vote_count", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long fireflyVoteCount = 0L;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anime_genres", joinColumns = @JoinColumn(name = "anime_id"))
    @Column(name = "genre")
    @BatchSize(size = 50)
    @Builder.Default
    private Set<String> genres = new HashSet<>();

    @Column(name = "studio", length = 100)
    private String studio;

    @Column(name = "source", length = 50)
    private String source; // Manga, Light novel, Original, etc.

    @Column(name = "season", length = 20)
    private String season; // Winter 2024, Spring 2024, etc.

    @Column(name = "year")
    private Integer year;

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Rating> ratings = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}
