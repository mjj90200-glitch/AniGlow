package com.aniglow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    @Test
    void sixthLoginAttemptReturns429() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RateLimitFilter filter = new RateLimitFilter(new RequestRateLimiter(), objectMapper);
        FilterChain chain = mock(FilterChain.class);

        for (int attempt = 1; attempt <= 5; attempt++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(loginRequest("203.0.113.8"), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest("203.0.113.8"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("请求过于频繁");
        verify(chain, times(5)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest loginRequest(String clientIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContextPath("/api");
        request.setServletPath("/auth/login");
        request.setRemoteAddr(clientIp);
        return request;
    }
}
