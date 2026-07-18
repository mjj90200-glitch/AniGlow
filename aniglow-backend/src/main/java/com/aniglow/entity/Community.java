package com.aniglow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "communities", indexes = {
        @Index(name = "idx_community_slug", columnList = "slug", unique = true),
        @Index(name = "idx_community_category", columnList = "category"),
        @Index(name = "idx_community_heat", columnList = "heat_score DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(length = 50)
    private String category;

    @Column(length = 500)
    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_anime_id")
    private Anime relatedAnime;

    @Column(name = "post_count", nullable = false)
    @Builder.Default
    private Long postCount = 0L;

    @Column(name = "member_count", nullable = false)
    @Builder.Default
    private Long memberCount = 0L;

    @Column(name = "heat_score", nullable = false)
    @Builder.Default
    private Long heatScore = 0L;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "creator_id")
    private Long creatorId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
