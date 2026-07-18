package com.aniglow.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthingLoginRequest {

    @NotNull(message = "Authing ID 不能为空")
    private String authingId;

    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
}
