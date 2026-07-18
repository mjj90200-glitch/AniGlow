package com.aniglow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_vote_candidates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "week_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentVoteCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "source_title", length = 120)
    private String sourceTitle;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 1000)
    private String personality;

    @Column(name = "speech_style", length = 1000)
    private String speechStyle;

    @Column(length = 500)
    private String catchphrases;

    @Column(name = "extra_prompt", columnDefinition = "TEXT")
    private String extraPrompt;

    @Column(name = "vote_count")
    @Builder.Default
    private Long voteCount = 0L;

    @Column(name = "is_winner")
    @Builder.Default
    private Boolean isWinner = false;

    @Column(name = "week_key", length = 10)
    private String weekKey;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
