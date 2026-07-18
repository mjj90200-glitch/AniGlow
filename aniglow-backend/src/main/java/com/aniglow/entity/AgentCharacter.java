package com.aniglow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_characters", indexes = {
    @Index(name = "idx_agent_character_code", columnList = "code", unique = true),
    @Index(name = "idx_agent_character_enabled", columnList = "enabled")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "source_title", length = 120)
    private String sourceTitle;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "background_url", length = 500)
    private String backgroundUrl;

    @Column(name = "personality", length = 1000)
    private String personality;

    @Column(name = "speech_style", length = 1000)
    private String speechStyle;

    @Column(name = "catchphrases", length = 500)
    private String catchphrases;

    @Column(name = "extra_prompt", columnDefinition = "TEXT")
    private String extraPrompt;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "voice_id", length = 120)
    private String voiceId;

    @Column(name = "opening_audio_url", length = 500)
    private String openingAudioUrl;

    @Column(name = "model", length = 120)
    private String model;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
