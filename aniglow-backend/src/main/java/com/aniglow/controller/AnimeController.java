package com.aniglow.controller;

import com.aniglow.dto.ApiResponse;
import com.aniglow.dto.anime.AnimeDto;
import com.aniglow.dto.anime.AnimeListResponse;
import com.aniglow.dto.anime.AnimeSearchRequest;
import com.aniglow.entity.Anime;
import com.aniglow.exception.ResourceNotFoundException;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.service.BayesianRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/anime")
@RequiredArgsConstructor
@Tag(name = "动漫", description = "动漫相关接口")
public class AnimeController {

    private final AnimeRepository animeRepository;
    private final BayesianRatingService bayesianRatingService;

    @GetMapping
    @Operation(summary = "获取动漫列表", description = "支持分页和排序")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getAllAnime(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bayesianRating") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Anime> animePage = animeRepository.findAll(pageable);

        AnimeListResponse response = buildAnimeListResponse(animePage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取动漫详情", description = "根据ID获取动漫详细信息")
    public ResponseEntity<ApiResponse<AnimeDto>> getAnimeById(
            @Parameter(description = "动漫ID") @PathVariable Long id) {
        Anime anime = animeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("动漫", "id", id));
        return ResponseEntity.ok(ApiResponse.success(convertToDto(anime)));
    }

    @GetMapping("/mal/{malId}")
    @Operation(summary = "根据MAL ID获取动漫", description = "通过 MyAnimeList ID 查询")
    public ResponseEntity<ApiResponse<AnimeDto>> getAnimeByMalId(
            @Parameter(description = "MAL ID") @PathVariable Long malId) {
        Anime anime = animeRepository.findByMalId(malId)
                .orElseThrow(() -> new ResourceNotFoundException("动漫", "malId", malId));
        return ResponseEntity.ok(ApiResponse.success(convertToDto(anime)));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索动漫", description = "按标题关键词搜索")
    public ResponseEntity<ApiResponse<AnimeListResponse>> searchAnime(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Anime> animePage = animeRepository.searchByTitle(keyword, pageable);

        AnimeListResponse response = buildAnimeListResponse(animePage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/top-rated")
    @Operation(summary = "高分动漫", description = "按贝叶斯评分排序")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getTopRated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Anime> animePage = animeRepository.findAllByOrderByBayesianRatingDesc(pageable);

        AnimeListResponse response = buildAnimeListResponse(animePage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/community-rated")
    @Operation(summary = "中国用户评分排行", description = "基于萤火番舍用户真实评分，不影响原始抓取排行")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getCommunityRated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Anime> animePage = animeRepository
                .findByCommunityRatingCountGreaterThanOrderByCommunityBayesianRatingDescFireflyVoteCountDesc(0L, pageable);

        AnimeListResponse response = buildAnimeListResponse(animePage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/firefly-ranking")
    @Operation(summary = "萤火投票排行", description = "基于全站用户真实投票的番剧排行")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getFireflyRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Anime> animePage = animeRepository
                .findByFireflyVoteCountGreaterThanOrderByFireflyVoteCountDescCommunityBayesianRatingDesc(0L, pageable);

        AnimeListResponse response = buildAnimeListResponse(animePage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/genre/{genre}")
    @Operation(summary = "按类型筛选", description = "获取特定类型的动漫")
    public ResponseEntity<ApiResponse<AnimeListResponse>> getByGenre(
            @PathVariable String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Anime> animePage = animeRepository.findByGenresInOrderByBayesianRatingDesc(
                List.of(genre), pageable);

        AnimeListResponse response = buildAnimeListResponse(animePage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/season/{year}/{season}")
    @Operation(summary = "按季度查询", description = "如 /season/2024/spring")
    public ResponseEntity<ApiResponse<List<AnimeDto>>> getBySeason(
            @PathVariable Integer year,
            @PathVariable String season) {

        String seasonStr = season.substring(0, 1).toUpperCase() + season.substring(1).toLowerCase();
        List<Anime> animes = animeRepository.findByYearAndSeasonOrderByBayesianRatingDesc(
                year, seasonStr + " " + year);

        List<AnimeDto> dtos = animes.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}/rating-distribution")
    @Operation(summary = "评分分布", description = "获取动漫的评分分布直方图")
    public ResponseEntity<ApiResponse<double[]>> getRatingDistribution(
            @PathVariable Long id) {
        // 验证动漫存在
        if (!animeRepository.existsById(id)) {
            throw new ResourceNotFoundException("动漫", "id", id);
        }
        double[] distribution = bayesianRatingService.getRatingDistribution(id);
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    private AnimeDto convertToDto(Anime anime) {
        return AnimeDto.builder()
                .id(anime.getId())
                .malId(anime.getMalId())
                .title(anime.getTitle())
                .titleJapanese(anime.getTitleJapanese())
                .titleEnglish(anime.getTitleEnglish())
                .titleCn(anime.getTitleCn())
                .searchAliases(anime.getSearchAliases())
                .synopsis(anime.getSynopsis())
                .synopsisCn(anime.getSynopsisCn())
                .coverImage(anime.getCoverImage())
                .trailerUrl(anime.getTrailerUrl())
                .type(anime.getType())
                .status(anime.getStatus())
                .airedFrom(anime.getAiredFrom())
                .airedTo(anime.getAiredTo())
                .episodes(anime.getEpisodes())
                .durationMinutes(anime.getDurationMinutes())
                .rating(anime.getRating())
                .popularity(anime.getPopularity())
                .membersCount(anime.getMembersCount())
                .favoritesCount(anime.getFavoritesCount())
                .hasAgent(Boolean.TRUE.equals(anime.getHasAgent()))
                .bayesianRating(anime.getBayesianRating() != null ? anime.getBayesianRating().doubleValue() : null)
                .meanRating(anime.getMeanRating() != null ? anime.getMeanRating().doubleValue() : null)
                .ratingCount(anime.getRatingCount())
                .communityBayesianRating(anime.getCommunityBayesianRating() != null ? anime.getCommunityBayesianRating().doubleValue() : null)
                .communityMeanRating(anime.getCommunityMeanRating() != null ? anime.getCommunityMeanRating().doubleValue() : null)
                .communityRatingCount(anime.getCommunityRatingCount() != null ? anime.getCommunityRatingCount() : 0L)
                .fireflyVoteCount(anime.getFireflyVoteCount() != null ? anime.getFireflyVoteCount() : 0L)
                .genres(anime.getGenres())
                .studio(anime.getStudio())
                .source(anime.getSource())
                .season(anime.getSeason())
                .year(anime.getYear())
                .createdAt(anime.getCreatedAt())
                .updatedAt(anime.getUpdatedAt())
                .build();
    }

    private AnimeListResponse buildAnimeListResponse(Page<Anime> page) {
        List<AnimeDto> content = page.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return AnimeListResponse.builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
