package com.aniglow.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestRateLimiter {

    private static final long CLEANUP_INTERVAL = 256;

    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();
    private final Clock clock;

    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String key, int limit, Duration window) {
        if (limit <= 0 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("限流规则必须使用正数");
        }

        long now = clock.millis();
        long windowMillis = window.toMillis();
        long windowNumber = now / windowMillis;
        String windowKey = key + ':' + windowNumber;
        long expiresAt = (windowNumber + 1) * windowMillis;

        WindowCounter counter = counters.computeIfAbsent(
                windowKey,
                ignored -> new WindowCounter(new AtomicInteger(), expiresAt)
        );
        boolean allowed = counter.count().incrementAndGet() <= limit;

        if (operations.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            counters.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        }
        return allowed;
    }

    private record WindowCounter(AtomicInteger count, long expiresAt) {
    }
}
