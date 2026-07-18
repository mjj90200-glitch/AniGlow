package com.aniglow.config;

import com.aniglow.dto.ApiResponse;
import com.aniglow.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private final RequestRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Rule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String identity = resolveIdentity(request);
        if (rateLimiter.tryAcquire(rule.name() + ':' + identity, rule.limit(), ONE_MINUTE)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error("请求过于频繁，请稍后再试"));
    }

    private Rule resolveRule(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String path = request.getServletPath();
        if ("/auth/login".equals(path) || "/auth/authing-login".equals(path)) {
            return new Rule("auth-login", 5);
        }
        if ("/auth/register".equals(path)) {
            return new Rule("auth-register", 3);
        }
        if ("/communities/upload/images".equals(path)) {
            return new Rule("community-upload", 5);
        }
        if (path.startsWith("/communities/")) {
            return new Rule("community-write", 10);
        }
        if ("/agent/chat".equals(path) || "/agent/chat/stream".equals(path)) {
            return new Rule("agent-chat", 30);
        }
        if (path.startsWith("/ratings") || path.startsWith("/rating-replies")) {
            return new Rule("rating-write", 20);
        }
        return null;
    }

    private String resolveIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl user) {
            return "user:" + user.getId();
        }
        return "ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Rule(String name, int limit) {
    }
}
