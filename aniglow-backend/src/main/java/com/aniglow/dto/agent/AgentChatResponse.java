package com.aniglow.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {
    private String role;
    private String roleName;
    private String reply;
    private String emotion;      // "happy" / "normal" / "complex"
    private Boolean cached;
    private Boolean rateLimited;
    private Integer quotaLimit;
    private Integer quotaRemaining;
    private String quotaResetAt;
}
