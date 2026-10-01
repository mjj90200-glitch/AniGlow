package com.aniglow.config;

import com.aniglow.service.AniListSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** AniList 数据源定时同步：每 6 小时全量刷新一次热门榜单（启动后 3 分钟首跑）。 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aniglow.anilist.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class AniListSyncScheduler {

    private final AniListSyncService anilistSyncService;

    @Scheduled(
            fixedDelayString = "${aniglow.anilist.sync-interval:21600000}",
            initialDelayString = "${aniglow.anilist.initial-delay:180000}"
    )
    public void synchronize() {
        anilistSyncService.syncPopularAnime();
    }
}
