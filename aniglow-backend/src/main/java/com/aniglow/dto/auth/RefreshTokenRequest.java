package com.aniglow.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "刷新凭证不能为空")
    private String refreshToken;
}
