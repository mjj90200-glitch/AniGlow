package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.service.RedisRankingService;
import com.aniglow.service.RedisRankingService.RankingItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
@Tag(name = "排行榜", description = "动漫排行榜接口")
public class RankingController {

    private final RedisRankingService rankingService;

    @GetMapping
    @Operation(summary = "获取排行榜（默认总榜）")
    public ResponseEntity<ApiResponse<List<RankingItem>>> getDefaultRanking(
            @RequestParam(defaultValue = "all_time") String type,
            @RequestParam(defaultValue = "20") int limit) {
        List<RankingItem> ranking = rankingService.getRankingWithDetails(type, limit);
        return ResponseEntity.ok(ApiResponse.success(ranking));
    }

    @GetMapping("/{type}")
    @Operation(summary = "获取排行榜", description = "类型: weekly, monthly, yearly, all_time")
    public ResponseEntity<ApiResponse<List<RankingItem>>> getRanking(
            @Parameter(description = "排行榜类型") @PathVariable String type,
            @RequestParam(defaultValue = "20") int limit) {
        List<RankingItem> ranking = rankingService.getRankingWithDetails(type, limit);
        return ResponseEntity.ok(ApiResponse.success(ranking));
    }

    @GetMapping("/anime/{animeId}/rank")
    @Operation(summary = "获取动漫排名", description = "查询指定动漫在各排行榜中的排名")
    public ResponseEntity<ApiResponse<AnimeRankResponse>> getAnimeRank(
            @PathVariable Long animeId) {
        AnimeRankResponse response = new AnimeRankResponse(
                animeId,
                rankingService.getAnimeRank(animeId, "weekly"),
                rankingService.getAnimeRank(animeId, "monthly"),
                rankingService.getAnimeRank(animeId, "yearly"),
                rankingService.getAnimeRank(animeId, "all_time")
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/rebuild")
    @Operation(summary = "重建排行榜", description = "手动触发排行榜重建（管理员）")
    public ResponseEntity<ApiResponse<String>> rebuildRankings() {
        rankingService.rebuildAllRankings();
        return ResponseEntity.ok(ApiResponse.success("排行榜重建任务已启动"));
    }

    public record AnimeRankResponse(
            Long animeId,
            long weeklyRank,
            long monthlyRank,
            long yearlyRank,
            long allTimeRank
    ) {}
}
