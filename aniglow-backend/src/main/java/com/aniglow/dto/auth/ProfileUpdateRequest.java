package com.aniglow.dto.auth;

import lombok.Data;

@Data
public class ProfileUpdateRequest {

    private String displayName;
    private String avatarUrl;
}
