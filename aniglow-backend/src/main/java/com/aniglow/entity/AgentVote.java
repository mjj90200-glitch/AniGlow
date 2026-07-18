package com.aniglow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "week_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private AgentVoteCandidate candidate;

    @Column(name = "week_key", length = 10)
    private String weekKey;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
