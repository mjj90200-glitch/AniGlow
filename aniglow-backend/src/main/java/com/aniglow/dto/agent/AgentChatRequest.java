package com.aniglow.dto.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String message;

    @Builder.Default
    private String role = "Makima";

    /** 用户标识（用于对话历史隔离），可选 */
    private String userId;
}
