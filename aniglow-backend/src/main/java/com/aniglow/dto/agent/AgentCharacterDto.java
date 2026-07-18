package com.aniglow.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCharacterDto {

    private String code;
    private String displayName;
    private String sourceTitle;
    private String avatarUrl;
    private String backgroundUrl;
    private String voiceId;
    private String openingAudioUrl;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Integer sortOrder;
}
