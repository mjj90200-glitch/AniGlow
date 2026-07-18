package com.aniglow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_posts", indexes = {
        @Index(name = "idx_post_community_reply", columnList = "community_id, last_replied_at"),
        @Index(name = "idx_post_community_created", columnList = "community_id, created_at"),
        @Index(name = "idx_post_author", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Long likeCount = 0L;

    @Column(name = "reply_count", nullable = false)
    @Builder.Default
    private Long replyCount = 0L;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "last_replied_at")
    private LocalDateTime lastRepliedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
