package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.entity.Anime;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.service.BayesianRatingService;
import com.aniglow.service.JikanSyncService;
import com.aniglow.service.RedisRankingService;
import com.aniglow.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "管理后台", description = "管理员操作接口")
public class AdminController {

    private final JikanSyncService jikanSyncService;
    private final RedisRankingService redisRankingService;
    private final BayesianRatingService bayesianRatingService;
    private final TranslationService translationService;
    private final AnimeRepository animeRepository;

    @PostMapping("/sync/jikan")
    @Operation(summary = "手动同步 Jikan Top 数据", description = "立即执行 Jikan Top API 数据同步",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<String>> syncJikanData() {
        new Thread(() -> jikanSyncService.syncTopAnime()).start();
        return ResponseEntity.ok(ApiResponse.success("Jikan 数据同步任务已启动"));
    }

    @PostMapping("/sync/full")
    @Operation(summary = "全量同步番剧", description = "同步 Top 榜单 + 当季新番 + 即将上映",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<String>> fullSync() {
        new Thread(() -> jikanSyncService.fullSync()).start();
        return ResponseEntity.ok(ApiResponse.success("全量番剧同步任务已启动（Top625 + 当季 + 即将上映）"));
    }

    @PostMapping("/sync/jikan/{malId}")
    @Operation(summary = "同步单个动漫", description = "根据 MAL ID 同步单个动漫详情",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<String>> syncSingleAnime(@PathVariable Long malId) {
        var anime = jikanSyncService.syncAnimeByMalId(malId);
        if (anime != null) {
            return ResponseEntity.ok(ApiResponse.success("动漫同步成功: " + anime.getTitle()));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("同步失败，MAL ID 可能不存在"));
        }
    }

    @PostMapping("/rankings/rebuild")
    @Operation(summary = "重建排行榜", description = "重新计算并重建 Redis 排行榜",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<String>> rebuildRankings() {
        redisRankingService.rebuildAllRankings();
        return ResponseEntity.ok(ApiResponse.success("排行榜重建完成"));
    }

    @PostMapping("/repair/synopses")
    @Operation(summary = "修复中文简介", description = "批量翻译缺失中文简介的动漫（英文→中文 AI 翻译）",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<String>> repairSynopses() {
        new Thread(() -> jikanSyncService.repairMissingChineseSynopses()).start();
        return ResponseEntity.ok(ApiResponse.success("简介修复任务已启动"));
    }

    @PostMapping("/repair/titles")
    @Operation(summary = "修复中文标题", description = "批量翻译缺失中文标题的动漫（日文/英文→中文 AI 翻译）",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<String>> repairTitles() {
        new Thread(() -> {
            List<Anime> list = animeRepository.findByTitleCnIsNull();
            int total = list.size();
            int success = 0;
            int fail = 0;
            for (Anime a : list) {
                try {
                    String cn = translationService.translateTitleToChinese(a.getTitleJapanese(), a.getTitle());
                    if (cn != null && !cn.isBlank()) {
                        a.setTitleCn(cn);
                        animeRepository.save(a);
                        success++;
                        // API 限速：每 5 部停顿一下
                        if (success % 5 == 0) {
                            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        }
                    } else {
                        fail++;
                    }
                } catch (Exception e) {
                    fail++;
                }
            }
            log.info("中文标题修复完成: 总计{}部, 成功{}部, 失败{}部", total, success, fail);
        }).start();
        return ResponseEntity.ok(ApiResponse.success("中文标题翻译任务已启动，正在后台执行..."));
    }

    @PostMapping("/ratings/recalculate")
    @Operation(summary = "重新计算贝叶斯评分", description = "全量重新计算所有动漫的贝叶斯评分",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<String>> recalculateRatings() {
        // 异步执行
        new Thread(() -> bayesianRatingService.recalculateAllRatings()).start();
        return ResponseEntity.ok(ApiResponse.success("贝叶斯评分重算任务已启动"));
    }

    @GetMapping("/system/status")
    @Operation(summary = "系统状态", description = "获取系统运行状态",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatus() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;

        Map<String, Object> status = Map.of(
                "totalMemoryMB", totalMemory,
                "freeMemoryMB", freeMemory,
                "maxMemoryMB", maxMemory,
                "usedMemoryMB", totalMemory - freeMemory,
                "availableProcessors", runtime.availableProcessors()
        );

        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
