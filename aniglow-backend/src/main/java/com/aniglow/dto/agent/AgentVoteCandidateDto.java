package com.aniglow.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVoteCandidateDto {
    private Long id;
    private String code;
    private String displayName;
    private String sourceTitle;
    private String avatarUrl;
    private Long voteCount;
    private String weekKey;
}
