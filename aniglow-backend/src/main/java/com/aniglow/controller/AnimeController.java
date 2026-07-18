package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.anime.AnimeDto;
import com.aniglow.dto.anime.AnimeListResponse;
import com.aniglow.service.AnimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/anime")
@RequiredArgsConstructor
@Tag(name = "动漫", description = "动漫相关接口")
public class AnimeController {

    private final AnimeService animeService;

    @GetMapping
    @Operation(summary = "获取动漫列表", description = "支持分页和排序")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getAllAnime(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bayesianRating") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ok(animeService.list(page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取动漫详情")
    public ResponseEntity<ApiResponse<AnimeDto>> getAnimeById(
            @Parameter(description = "动漫ID") @PathVariable Long id) {
        return ok(animeService.getById(id));
    }

    @GetMapping("/mal/{malId}")
    @Operation(summary = "根据MAL ID获取动漫")
    public ResponseEntity<ApiResponse<AnimeDto>> getAnimeByMalId(@PathVariable Long malId) {
        return ok(animeService.getByMalId(malId));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索动漫", description = "支持中文译名、原名和别名")
    public ResponseEntity<ApiResponse<AnimeListResponse>> searchAnime(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(animeService.search(keyword, page, size));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getTopRated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(animeService.topRated(page, size));
    }

    @GetMapping("/community-rated")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getCommunityRated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(animeService.communityRated(page, size));
    }

    @GetMapping("/firefly-ranking")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getFireflyRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(animeService.fireflyRanking(page, size));
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getByGenre(
            @PathVariable String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(animeService.byGenre(genre, page, size));
    }

    @GetMapping("/season/{year}/{season}")
    public ResponseEntity<ApiResponse<List<AnimeDto>>> getBySeason(
            @PathVariable Integer year, @PathVariable String season) {
        return ok(animeService.bySeason(year, season));
    }

    @GetMapping("/{id}/rating-distribution")
    public ResponseEntity<ApiResponse<double[]>> getRatingDistribution(@PathVariable Long id) {
        return ok(animeService.ratingDistribution(id));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
