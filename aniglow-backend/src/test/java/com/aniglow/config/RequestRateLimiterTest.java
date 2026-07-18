package com.aniglow.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RequestRateLimiterTest {

    @Test
    void rejectsRequestsBeyondTheWindowLimit() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC);
        RequestRateLimiter limiter = new RequestRateLimiter(clock);

        assertThat(limiter.tryAcquire("login:127.0.0.1", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.tryAcquire("login:127.0.0.1", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.tryAcquire("login:127.0.0.1", 2, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void keepsDifferentIdentitiesIndependent() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC);
        RequestRateLimiter limiter = new RequestRateLimiter(clock);

        assertThat(limiter.tryAcquire("login:user-a", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.tryAcquire("login:user-a", 1, Duration.ofMinutes(1))).isFalse();
        assertThat(limiter.tryAcquire("login:user-b", 1, Duration.ofMinutes(1))).isTrue();
    }
}
