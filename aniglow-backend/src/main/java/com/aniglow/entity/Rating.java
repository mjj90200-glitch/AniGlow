package com.aniglow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ratings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "anime_id"}, name = "uk_user_anime_rating")
}, indexes = {
    @Index(name = "idx_anime_rating", columnList = "anime_id, score DESC"),
    @Index(name = "idx_user_rating", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal score; // 1.0 - 10.0

    @Column(length = 1000)
    private String review;

    // 评分维度（可选）
    @Column(name = "story_score", precision = 3, scale = 1)
    private BigDecimal storyScore;

    @Column(name = "animation_score", precision = 3, scale = 1)
    private BigDecimal animationScore;

    @Column(name = "sound_score", precision = 3, scale = 1)
    private BigDecimal soundScore;

    @Column(name = "character_score", precision = 3, scale = 1)
    private BigDecimal characterScore;

    @Column(name = "enjoyment_score", precision = 3, scale = 1)
    private BigDecimal enjoymentScore;

    @Column(name = "is_recommended")
    private Boolean isRecommended;

    @Column(name = "contains_spoiler")
    @Builder.Default
    private Boolean containsSpoiler = false;

    @Column(name = "like_count")
    @Builder.Default
    private Long likeCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
