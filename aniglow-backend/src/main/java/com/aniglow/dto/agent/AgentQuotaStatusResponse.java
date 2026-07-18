package com.aniglow.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentQuotaStatusResponse {
    private Integer limit;
    private Integer used;
    private Integer remaining;
    private String resetAt;
    private Boolean activeMember;
    private String planName;
    private Boolean voiceEnabled;
}
