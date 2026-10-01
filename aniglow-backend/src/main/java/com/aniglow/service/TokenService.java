package com.aniglow.service;

import com.aniglow.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String REVOKED_PREFIX = "auth:token:revoked:";
    private static final String REFRESH_USED_PREFIX = "auth:token:refresh-used:";

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    @Value("${aniglow.security.token-revocation-enabled:true}")
    private boolean revocationEnabled;

    public TokenPair issue(UserDetails userDetails) {
        return new TokenPair(jwtUtils.generateToken(userDetails), jwtUtils.generateRefreshToken(userDetails));
    }

    public long getAccessTokenExpiration() {
        return jwtUtils.getExpirationTime();
    }

    public RefreshedSession refresh(String refreshToken) {
        if (!jwtUtils.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("刷新令牌无效或已过期");
        }
        consumeRefreshToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(jwtUtils.extractUsername(refreshToken));
        return new RefreshedSession(userDetails, issue(userDetails));
    }

    public boolean isAccessTokenRevoked(String accessToken) {
        if (!revocationEnabled) {
            return false;
        }
        String tokenId = jwtUtils.extractTokenId(accessToken);
        if (tokenId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_PREFIX + tokenId));
        } catch (RuntimeException exception) {
            log.warn("Redis unavailable while checking access token revocation: {}", exception.getMessage());
            return false;
        }
    }

    public void revoke(String accessToken, String refreshToken) {
        if (!revocationEnabled) {
            return;
        }
        storeTokenMarker(accessToken, REVOKED_PREFIX, false);
        storeTokenMarker(refreshToken, REFRESH_USED_PREFIX, false);
    }

    private void consumeRefreshToken(String refreshToken) {
        if (!revocationEnabled) {
            return;
        }
        String tokenId = jwtUtils.extractTokenId(refreshToken);
        if (tokenId == null) {
            throw new BadCredentialsException("刷新令牌缺少唯一标识");
        }
        Duration ttl = Duration.ofMillis(jwtUtils.getRemainingTime(refreshToken));
        if (ttl.isZero()) {
            throw new BadCredentialsException("刷新令牌已过期");
        }
        try {
            Boolean firstUse = redisTemplate.opsForValue()
                    .setIfAbsent(REFRESH_USED_PREFIX + tokenId, "1", ttl);
            if (!Boolean.TRUE.equals(firstUse)) {
                throw new BadCredentialsException("刷新令牌已使用，请重新登录");
            }
        } catch (BadCredentialsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Redis unavailable while consuming refresh token", exception);
            throw new BadCredentialsException("暂时无法刷新登录状态，请重新登录");
        }
    }

    private void storeTokenMarker(String token, String prefix, boolean strict) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            String tokenId = jwtUtils.extractTokenId(token);
            long remaining = jwtUtils.getRemainingTime(token);
            if (tokenId != null && remaining > 0) {
                redisTemplate.opsForValue().set(prefix + tokenId, "1", Duration.ofMillis(remaining));
            }
        } catch (RuntimeException exception) {
            if (strict) {
                throw exception;
            }
            log.warn("Unable to persist token revocation marker: {}", exception.getMessage());
        }
    }

    public record TokenPair(String accessToken, String refreshToken) {}

    public record RefreshedSession(UserDetails userDetails, TokenPair tokens) {}
}
