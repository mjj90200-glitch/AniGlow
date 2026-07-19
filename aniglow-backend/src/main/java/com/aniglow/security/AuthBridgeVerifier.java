package com.aniglow.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AuthBridgeVerifier {

    private final byte[] expectedSecret;

    public AuthBridgeVerifier(@Value("${aniglow.auth.bridge-secret}") String expectedSecret) {
        if (expectedSecret == null || expectedSecret.length() < 32) {
            throw new IllegalStateException("AUTH_BRIDGE_SECRET 必须至少为 32 个字符");
        }
        this.expectedSecret = expectedSecret.getBytes(StandardCharsets.UTF_8);
    }

    public void requireValid(String suppliedSecret) {
        byte[] supplied = suppliedSecret == null
                ? new byte[0]
                : suppliedSecret.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedSecret, supplied)) {
            throw new AuthenticationServiceException("认证桥接校验失败");
        }
    }
}
