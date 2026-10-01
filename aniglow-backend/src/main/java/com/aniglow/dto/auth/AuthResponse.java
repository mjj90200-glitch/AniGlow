package com.aniglow.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String refreshToken;
    private String type;
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String phone;
    private Boolean profileComplete;
    private Boolean credentialsInitialized;
    private String email;
    private List<String> roles;
    private Long expiresIn;
}
