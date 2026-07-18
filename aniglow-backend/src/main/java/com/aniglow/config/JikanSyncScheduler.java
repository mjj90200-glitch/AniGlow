package com.aniglow.config;

import com.aniglow.service.JikanSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Keeps external API work out of the application startup path. */
@Component
@Profile("!test")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aniglow.jikan.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class JikanSyncScheduler {

    private final JikanSyncService jikanSyncService;

    @Scheduled(
            fixedDelayString = "${aniglow.jikan.sync-interval:3600000}",
            initialDelayString = "${aniglow.jikan.initial-delay:300000}"
    )
    public void synchronize() {
        jikanSyncService.syncTopAnime();
    }
}
