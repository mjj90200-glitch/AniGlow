package com.aniglow.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMembershipStatusResponse {
    private Boolean active;
    private String planCode;
    private String planName;
    private String priceText;
    private Integer quotaLimitPerHour;
    /** 保留旧字段，避免已发布客户端升级期间解析失败。 */
    private Integer quotaLimitPerDay;
    private Boolean voiceEnabled;
    private String expiresAt;
    private String paymentQrUrl;
}
