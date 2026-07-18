package com.aniglow.service;

import com.aniglow.entity.Anime;
import com.aniglow.repository.AnimeRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Jikan API 同步服务
 *
 * Jikan API (https://jikan.moe/) 是 MyAnimeList 的非官方 API
 * 提供丰富的动漫数据，免费且无需 API Key
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JikanSyncService {

    private final AnimeRepository animeRepository;
    private final RedisRankingService rankingService;
    private final TranslationService translationService;
    private final AtomicBoolean topSyncRunning = new AtomicBoolean(false);

    @Value("${aniglow.jikan.base-url:https://api.jikan.moe/v4}")
    private String baseUrl;

    @Value("${aniglow.jikan.request-delay:1000}")
    private long requestDelay;

    @Transactional
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public void tagGenreFromJikan(int genreId, String tag) {
        try {
            long existing = animeRepository.countByGenre(tag);
            if (existing > 20) return;
            log.info("补充{}标签: 当前{}条 -> 目标 >20", tag, existing);
            for (int p = 1; p <= 10; p++) {
                final int page = p;
                JikanResponse response = webClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/anime")
                                .queryParam("genres", genreId)
                                .queryParam("order_by", "score")
                                .queryParam("sort", "desc")
                                .queryParam("page", page)
                                .queryParam("limit", 25)
                                .build())
                        .retrieve().bodyToMono(JikanResponse.class).block();
                if (response == null || response.data == null) continue;
                for (JikanAnime anime : response.data) {
                    try {
                        Anime saved = saveOrUpdateAnime(anime);
                        if (saved != null) {
                            saved.getGenres().add(tag);
                            animeRepository.save(saved);
                        }
                    } catch (Exception ignored) {}
                }
                try { Thread.sleep(requestDelay); } catch (InterruptedException ignored) {}
            }
            log.info("{}标签补充完成: {}", tag, animeRepository.countByGenre(tag));
        } catch (Exception e) { log.warn("标签补充({})失败", tag, e); }
    }

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.jikan.moe/v4")
            .build();

    /**
     * 同步热门动漫（Top Anime）
     * 定时任务：每小时执行一次，获取 Top 625 + 当季新番 + 热门类型
     */
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public void syncTopAnime() {
        if (!topSyncRunning.compareAndSet(false, true)) {
            log.info("Jikan 同步任务已在运行，跳过本次重复请求");
            return;
        }
        log.info("开始同步 Jikan 番剧数据...");

        try {
            // 分页获取 Top Anime（每页 25 条，获取 25 页 = 625 条）
            for (int page = 1; page <= 25; page++) {
                fetchAndSaveTopAnime(page);
                Thread.sleep(requestDelay);
            }
            log.info("Jikan Top Anime 同步完成");

            // 同步当季新番
            fetchAndSaveSeasonal("now");
            Thread.sleep(requestDelay);
            fetchAndSaveSeasonal("upcoming");
            log.info("当季新番同步完成");

            // 热门类型：校园(23)、热血(27)、异世界(62) — 每次5页
            for (int gid : new int[]{23, 27, 62}) {
                final int genreId = gid;
                for (int p = 1; p <= 5; p++) {
                    fetchAndSaveGenre(genreId, p);
                    Thread.sleep(requestDelay);
                }
            }
            log.info("热门类型番剧同步完成");

        } catch (Exception e) {
            log.error("同步 Jikan 数据时出错", e);
        } finally {
            topSyncRunning.set(false);
        }
    }

    /**
     * 同步当季新番 + 即将上映
     */
    @Transactional
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public void syncSeasonalAnime() {
        log.info("开始同步当季新番数据...");
        try {
            // 当前季度 + 即将上映
            fetchAndSaveSeasonal("now");
            Thread.sleep(requestDelay);
            fetchAndSaveSeasonal("upcoming");
            Thread.sleep(requestDelay);
            log.info("当季新番同步完成");
        } catch (Exception e) {
            log.error("同步当季新番时出错", e);
        }
    }

    /**
     * 获取并保存当季新番
     */
    private void fetchAndSaveSeasonal(String seasonPath) {
        try {
            JikanResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/seasons/" + seasonPath)
                            .queryParam("limit", 25)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Jikan 当季 API 请求失败: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("API 请求失败"));
                    })
                    .bodyToMono(JikanResponse.class)
                    .block();

            if (response == null || response.data == null) return;

            for (JikanAnime anime : response.data) {
                try {
                    saveOrUpdateAnime(anime);
                } catch (Exception e) {
                    log.error("保存当季动漫 {} 时出错", anime.title, e);
                }
            }
            log.info("已同步当季新番({}): {} 条", seasonPath, response.data.size());
        } catch (Exception e) {
            log.error("获取当季新番({})时出错", seasonPath, e);
        }
    }

    /**
     * 同步指定年份/季度番剧
     */
    private void fetchAndSaveSeason(String year, String season) {
        try {
            JikanResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/seasons/" + year + "/" + season)
                            .queryParam("limit", 25)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Jikan 季度 API 请求失败: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("API 请求失败"));
                    })
                    .bodyToMono(JikanResponse.class)
                    .block();

            if (response == null || response.data == null) return;

            for (JikanAnime anime : response.data) {
                try { saveOrUpdateAnime(anime); } catch (Exception e) {}
            }
            log.info("已同步 {} {}: {} 条", year, season, response.data.size());
        } catch (Exception e) {
            log.error("获取 {} {} 数据时出错", year, season, e);
        }
    }

    /** MAL genre ID → 补充标签名 */
    private static final Map<Integer, String> GENRE_TAG = Map.of(
        23, "School",     // 校园
        27, "Shounen",    // 热血
        62, "Isekai"      // 异世界
    );

    /**
     * 按类型同步番剧，并自动补上 Jikan 不返回的类型标签
     */
    private void fetchAndSaveGenre(int genreId, int page) {
        try {
            JikanResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/anime")
                            .queryParam("genres", genreId)
                            .queryParam("order_by", "score")
                            .queryParam("sort", "desc")
                            .queryParam("page", page)
                            .queryParam("limit", 25)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Jikan 类型 API 请求失败: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("API 请求失败"));
                    })
                    .bodyToMono(JikanResponse.class)
                    .block();

            if (response == null || response.data == null) return;

            String tag = GENRE_TAG.get(genreId);
            for (JikanAnime anime : response.data) {
                try {
                    Anime saved = saveOrUpdateAnime(anime);
                    if (tag != null && saved != null && !saved.getGenres().contains(tag)) {
                        saved.getGenres().add(tag);
                        animeRepository.save(saved);
                    }
                } catch (Exception e) {}
            }
            log.info("已同步类型({})第{}页: {} 条", genreId, page, response.data.size());
        } catch (Exception e) {
            log.error("获取类型({})第{}页时出错", genreId, page, e);
        }
    }

    /**
     * 全量同步：Top 榜单 + 当季新番（手动触发用）
     */
    @Transactional
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public void fullSync() {
        log.info("=== 开始全量番剧同步 ===");
        syncTopAnime();
        try { Thread.sleep(requestDelay); } catch (InterruptedException ignored) {}
        syncSeasonalAnime();
        log.info("=== 全量番剧同步完成 ===");
    }

    /**
     * 获取并保存 Top Anime
     */
    private void fetchAndSaveTopAnime(int page) {
        try {
            JikanResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/top/anime")
                            .queryParam("page", page)
                            .queryParam("limit", 25)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Jikan API 请求失败: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("API 请求失败"));
                    })
                    .bodyToMono(JikanResponse.class)
                    .block();

            if (response == null || response.data == null) {
                log.warn("Jikan API 返回空数据");
                return;
            }

            for (JikanAnime jikanAnime : response.data) {
                try {
                    saveOrUpdateAnime(jikanAnime);
                } catch (Exception e) {
                    log.error("保存动漫 {} 时出错", jikanAnime.title, e);
                }
            }

            log.info("已同步第 {} 页，共 {} 条数据", page, response.data.size());

        } catch (Exception e) {
            log.error("获取第 {} 页数据时出错", page, e);
        }
    }

    /**
     * 根据 MAL ID 同步单个动漫详情
     */
    @Transactional
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public Anime syncAnimeByMalId(Long malId) {
        log.info("同步 MAL ID: {}", malId);

        try {
            JikanAnimeResponse response = webClient.get()
                    .uri("/anime/{id}/full", malId)
                    .retrieve()
                    .bodyToMono(JikanAnimeResponse.class)
                    .block();

            if (response == null || response.data == null) {
                log.warn("未找到 MAL ID: {}", malId);
                return null;
            }

            Anime anime = saveOrUpdateAnime(response.data);
            Thread.sleep(requestDelay); // 遵守速率限制

            return anime;

        } catch (Exception e) {
            log.error("同步 MAL ID {} 时出错", malId, e);
            return null;
        }
    }

    /**
     * 批量修复缺失的中文简介
     * 查找所有 synopsisCn 为 null 但 synopsis 存在的动漫，调用 AI 翻译填补
     * @return 成功修复的数量
     */
    @Transactional
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public int repairMissingChineseSynopses() {
        var animes = animeRepository.findBySynopsisCnIsNullAndSynopsisIsNotNull();
        if (animes.isEmpty()) {
            log.info("所有动漫已有中文简介，跳过修复");
            return 0;
        }

        log.info("发现 {} 条缺失中文简介的动漫，开始修复...", animes.size());
        int repaired = 0;

        for (var anime : animes) {
            String synopsis = anime.getSynopsis();
            if (synopsis == null || synopsis.length() < 30 || !isEnglishText(synopsis)) {
                continue;
            }

            try {
                String cn = translationService.translateToChinese(synopsis);
                if (cn != null && !cn.isEmpty()) {
                    anime.setSynopsisCn(cn);
                    animeRepository.save(anime);
                    repaired++;
                    log.debug("已翻译简介: {} ({}/{})", anime.getTitle(), repaired, animes.size());
                    Thread.sleep(500); // 控制翻译 API 调用频率
                }
            } catch (Exception e) {
                log.warn("修复简介失败 {}: {}", anime.getTitle(), e.getMessage());
            }
        }

        log.info("中文简介修复完成: 成功翻译 {} / {} 条", repaired, animes.size());
        return repaired;
    }

    /**
     * 保存或更新动漫
     */
    private Anime saveOrUpdateAnime(JikanAnime jikanAnime) {
        Optional<Anime> existingAnime = animeRepository.findByMalId(jikanAnime.malId);

        Anime anime = existingAnime.orElse(new Anime());

        // 基础信息
        anime.setMalId(jikanAnime.malId);
        anime.setTitle(jikanAnime.title);
        anime.setTitleJapanese(jikanAnime.titleJapanese);
        anime.setTitleEnglish(jikanAnime.titleEnglish);
        anime.setSearchAliases(mergeAliases(
                anime.getSearchAliases(),
                jikanAnime.titleSynonyms,
                jikanAnime.titles
        ));
        anime.setSynopsis(jikanAnime.synopsis);
        anime.setCoverImage(jikanAnime.images != null ?
                jikanAnime.images.jpg().largeImageUrl : null);
        anime.setTrailerUrl(jikanAnime.trailer != null ?
                jikanAnime.trailer.url : null);

        // 元数据
        anime.setType(jikanAnime.type);
        anime.setStatus(jikanAnime.status);
        anime.setEpisodes(jikanAnime.episodes);
        anime.setDurationMinutes(parseDuration(jikanAnime.duration));
        anime.setRating(jikanAnime.rating);
        anime.setSource(jikanAnime.source);

        // 日期
        if (jikanAnime.aired != null) {
            anime.setAiredFrom(parseDate(jikanAnime.aired.from));
            anime.setAiredTo(parseDate(jikanAnime.aired.to));
        }

        // 统计数据
        anime.setPopularity(jikanAnime.popularity != null ?
                jikanAnime.popularity.longValue() : null);
        anime.setMembersCount(jikanAnime.members != null ?
                jikanAnime.members.longValue() : null);
        anime.setFavoritesCount(jikanAnime.favorites != null ?
                jikanAnime.favorites.longValue() : null);
        if (anime.getHasAgent() == null) {
            anime.setHasAgent(false);
        }

        // Jikan 原始评分
        if (jikanAnime.score != null) {
            anime.setMeanRating(BigDecimal.valueOf(jikanAnime.score.doubleValue()));
            // 初始化贝叶斯评分为原始评分
            if (anime.getBayesianRating() == null) {
                anime.setBayesianRating(BigDecimal.valueOf(jikanAnime.score.doubleValue()));
            }
        }
        if (jikanAnime.scoredBy != null) {
            anime.setRatingCount(jikanAnime.scoredBy.longValue());
        }

        // 季节和年份
        if (jikanAnime.season != null && jikanAnime.year != null) {
            anime.setSeason(jikanAnime.season + " " + jikanAnime.year);
            anime.setYear(jikanAnime.year);
        }

        // 类型
        if (jikanAnime.genres != null) {
            Set<String> genres = new HashSet<>();
            for (Producer genre : jikanAnime.genres) {
                genres.add(genre.name);
            }
            anime.setGenres(genres);
        }

        // 制作公司
        if (jikanAnime.studios != null && !jikanAnime.studios.isEmpty()) {
            anime.setStudio(jikanAnime.studios.get(0).name);
        }

        anime.setSyncedAt(LocalDateTime.now());

        Anime savedAnime = animeRepository.save(anime);

        // 同步更新 Redis 排行榜
        if (savedAnime.getBayesianRating() != null) {
            rankingService.updateRankingScore(savedAnime.getId(),
                    savedAnime.getBayesianRating());
        }

        // 自动翻译英文简介为中文（仅首次，已有中文则跳过）
        if (savedAnime.getSynopsisCn() == null
                && savedAnime.getSynopsis() != null
                && savedAnime.getSynopsis().length() > 30
                && isEnglishText(savedAnime.getSynopsis())) {
            try {
                String cn = translationService.translateToChinese(savedAnime.getSynopsis());
                if (cn != null && !cn.isEmpty()) {
                    savedAnime.setSynopsisCn(cn);
                    animeRepository.save(savedAnime);
                    log.debug("已翻译简介: {}", savedAnime.getTitle());
                }
            } catch (Exception e) {
                log.warn("翻译简介失败 {}: {}", savedAnime.getTitle(), e.getMessage());
            }
        }

        log.debug("已保存动漫: {} (MAL ID: {})", savedAnime.getTitle(), savedAnime.getMalId());

        return savedAnime;
    }

    /**
     * 解析时长字符串（如 "23 min per ep"）
     */
    private Integer parseDuration(String duration) {
        if (duration == null) return null;
        try {
            String[] parts = duration.split(" ");
            if (parts.length >= 1) {
                return Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            log.debug("无法解析时长: {}", duration);
        }
        return null;
    }

    /**
     * 解析日期字符串
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("null")) {
            return null;
        }
        try {
            // 尝试多种日期格式
            List<DateTimeFormatter> formatters = List.of(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            );

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(dateStr.substring(0, Math.min(dateStr.length(), 10)),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (DateTimeParseException ignored) {
                }
            }
        } catch (Exception e) {
            log.debug("无法解析日期: {}", dateStr);
        }
        return null;
    }

    private String mergeAliases(String existing, List<String> titleSynonyms, List<Title> titles) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        addAliases(aliases, existing);

        if (titleSynonyms != null) {
            titleSynonyms.forEach(alias -> addAlias(aliases, alias));
        }

        if (titles != null) {
            titles.forEach(title -> {
                if (title != null) {
                    addAlias(aliases, title.title());
                }
            });
        }

        return String.join("，", aliases);
    }

    private void addAliases(Set<String> aliases, String rawAliases) {
        if (rawAliases == null || rawAliases.isBlank()) {
            return;
        }
        for (String alias : rawAliases.split("[,，、;；|/\\n]+")) {
            addAlias(aliases, alias);
        }
    }

    private void addAlias(Set<String> aliases, String alias) {
        if (alias == null) {
            return;
        }
        String normalized = alias.trim();
        if (!normalized.isEmpty() && normalized.length() <= 120) {
            aliases.add(normalized);
        }
    }

    // ========== Jikan API 响应 DTOs ==========

    public record JikanResponse(
            List<JikanAnime> data,
            Pagination pagination
    ) {}

    public record JikanAnimeResponse(
            JikanAnime data
    ) {}

    public record Pagination(
            int lastVisiblePage,
            boolean hasNextPage,
            int currentPage,
            Items items
    ) {}

    public record Items(
            int count,
            int total,
            int perPage
    ) {}

    public record JikanAnime(
            @JsonProperty("mal_id") Long malId,
            String url,
            Images images,
            Trailer trailer,
            boolean approved,
            List<Title> titles,
            String title,
            @JsonProperty("title_english") String titleEnglish,
            @JsonProperty("title_japanese") String titleJapanese,
            List<String> titleSynonyms,
            String type,
            String source,
            Integer episodes,
            String status,
            boolean airing,
            Aired aired,
            String duration,
            String rating,
            Double score,
            @JsonProperty("scored_by") Integer scoredBy,
            Integer rank,
            Integer popularity,
            Integer members,
            Integer favorites,
            String synopsis,
            String background,
            String season,
            Integer year,
            Broadcast broadcast,
            List<Producer> producers,
            List<Producer> licensors,
            List<Producer> studios,
            List<Producer> genres,
            List<Producer> explicitGenres,
            List<Producer> themes,
            List<Producer> demographics
    ) {}

    public record Images(
            Jpg jpg,
            Webp webp
    ) {}

    public record Jpg(
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("small_image_url") String smallImageUrl,
            @JsonProperty("large_image_url") String largeImageUrl
    ) {}

    public record Webp(
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("small_image_url") String smallImageUrl,
            @JsonProperty("large_image_url") String largeImageUrl
    ) {}

    public record Trailer(
            @JsonProperty("youtube_id") String youtubeId,
            String url,
            @JsonProperty("embed_url") String embedUrl
    ) {}

    public record Title(
            String type,
            String title
    ) {}

    public record Aired(
            String from,
            String to,
            Prop prop
    ) {}

    public record Prop(
            DateParts from,
            DateParts to
    ) {}

    public record DateParts(
            Integer day,
            Integer month,
            Integer year
    ) {}

    public record Broadcast(
            String day,
            String time,
            String timezone,
            String string
    ) {}

    public record Producer(
            @JsonProperty("mal_id") Integer malId,
            String type,
            String name,
            String url
    ) {}

    public record Genre(
            @JsonProperty("mal_id") Integer malId,
            String type,
            String name,
            String url
    ) {}

    /**
     * 判断文本是否主要为英文（ASCII 字符占比 > 70%）
     */
    private boolean isEnglishText(String text) {
        if (text == null || text.isEmpty()) return false;
        long ascii = text.chars().filter(c -> c < 128).count();
        return (double) ascii / text.length() > 0.7;
    }
}
