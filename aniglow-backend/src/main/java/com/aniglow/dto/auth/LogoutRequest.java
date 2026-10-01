package com.aniglow.dto.auth;

import lombok.Data;

@Data
public class LogoutRequest {

    private String accessToken;
    private String refreshToken;
}
