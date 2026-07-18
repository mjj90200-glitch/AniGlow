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
}
