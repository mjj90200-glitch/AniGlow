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
import java.time.Duration;
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
                        .retrieve().bodyToMono(JikanResponse.class).retryWhen(jikanRetry()).block();
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
            // 分页获取 Top Anime（每页 25 条，获取 40 页 = 1000 条，覆盖更多非头部番剧）
            for (int page = 1; page <= 40; page++) {
                fetchAndSaveTopAnime(page);
                Thread.sleep(requestDelay);
            }
            log.info("Jikan Top Anime 同步完成");

            // 同步当季新番（各3页）
            fetchAndSaveSeasonal("now", 3);
            Thread.sleep(requestDelay);
            fetchAndSaveSeasonal("upcoming", 3);
            log.info("当季新番同步完成");

            // 按评分排序：每类型8页，深入非热门区域
            for (int gid : GENRE_TAG.keySet()) {
                final int genreId = gid;
                for (int p = 1; p <= 8; p++) {
                    fetchAndSaveGenre(genreId, p, "score", "desc");
                    Thread.sleep(requestDelay);
                }
            }
            log.info("全部类型（评分排序）番剧同步完成");

            // 同步近6年历史季度番剧（每季10页=250部，覆盖当季所有番剧包括冷门）
            String[][] historicalSeasons = {
                {"2020", "winter"}, {"2020", "spring"}, {"2020", "summer"}, {"2020", "fall"},
                {"2021", "winter"}, {"2021", "spring"}, {"2021", "summer"}, {"2021", "fall"},
                {"2022", "winter"}, {"2022", "spring"}, {"2022", "summer"}, {"2022", "fall"},
                {"2023", "winter"}, {"2023", "spring"}, {"2023", "summer"}, {"2023", "fall"},
                {"2024", "winter"}, {"2024", "spring"}, {"2024", "summer"}, {"2024", "fall"},
                {"2025", "winter"}, {"2025", "spring"}, {"2025", "summer"}, {"2025", "fall"},
                {"2026", "winter"}, {"2026", "spring"},
            };
            for (String[] s : historicalSeasons) {
                fetchAndSaveSeason(s[0], s[1], 10);
                Thread.sleep(requestDelay);
            }
            log.info("历史季度番剧同步完成");

            // --- 深度挖掘（仅库存不足时执行，每次只跑一个维度避免触发限流） ---
            long currentCount = animeRepository.count();
            if (currentCount < 2500) {
                log.info("当前库存 {} < 2500，启动深度挖掘...", currentCount);

                // 剧场版/OVA/特别篇（各5页，按会员数排序）
                for (String type : new String[]{"movie", "ova", "special"}) {
                    for (int p = 1; p <= 5; p++) {
                        fetchAndSaveByType(type, p);
                        Thread.sleep(requestDelay);
                    }
                }
                log.info("剧场版/OVA/特别篇同步完成");
            }

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
        fetchAndSaveSeasonal(seasonPath, 1);
    }

    /**
     * 获取并保存当季新番（支持分页）
     */
    private void fetchAndSaveSeasonal(String seasonPath, int pages) {
        for (int page = 1; page <= pages; page++) {
            final int p = page;
            try {
                JikanResponse response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/seasons/" + seasonPath)
                                .queryParam("page", p)
                                .queryParam("limit", 25)
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, clientResponse -> {
                            log.error("Jikan 当季 API 请求失败: {}", clientResponse.statusCode());
                            return Mono.error(new RuntimeException("JIKAN_RETRYABLE_" + clientResponse.statusCode().value()));
                        })
                        .bodyToMono(JikanResponse.class).retryWhen(jikanRetry())
                        .block();

                if (response == null || response.data == null) continue;

                for (JikanAnime anime : response.data) {
                    try {
                        saveOrUpdateAnime(anime);
                    } catch (Exception e) {
                        log.error("保存当季动漫 {} 时出错", anime.title, e);
                    }
                }
                log.info("已同步当季新番({})第{}页: {} 条", seasonPath, page, response.data.size());
            } catch (Exception e) {
                log.error("获取当季新番({})第{}页时出错", seasonPath, page, e);
            }
        }
    }

    /**
     * 同步指定年份/季度番剧
     */
    private void fetchAndSaveSeason(String year, String season) {
        fetchAndSaveSeason(year, season, 1);
    }

    /**
     * 同步指定年份/季度番剧（支持分页）
     */
    private void fetchAndSaveSeason(String year, String season, int pages) {
        for (int page = 1; page <= pages; page++) {
            final int p = page;
            try {
                JikanResponse response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/seasons/" + year + "/" + season)
                                .queryParam("page", p)
                                .queryParam("limit", 25)
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, clientResponse -> {
                            log.error("Jikan 季度 API 请求失败: {}", clientResponse.statusCode());
                            return Mono.error(new RuntimeException("JIKAN_RETRYABLE_" + clientResponse.statusCode().value()));
                        })
                        .bodyToMono(JikanResponse.class).retryWhen(jikanRetry())
                        .block();

                if (response == null || response.data == null) continue;

                for (JikanAnime anime : response.data) {
                    try { saveOrUpdateAnime(anime); } catch (Exception e) {}
                }
                log.info("已同步 {} {} 第{}页: {} 条", year, season, page, response.data.size());
            } catch (Exception e) {
                log.error("获取 {} {} 第{}页数据时出错", year, season, page, e);
            }
        }
    }

    /**
     * 按年份同步番剧（按会员数排序，挖掘各年份忠实粉丝向作品）
     */
    private void fetchAndSaveYear(int year, int page) {
        try {
            JikanResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/anime")
                            .queryParam("start_date", year + "-01-01")
                            .queryParam("end_date", year + "-12-31")
                            .queryParam("order_by", "members")
                            .queryParam("sort", "desc")
                            .queryParam("page", page)
                            .queryParam("limit", 25)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Jikan 年份 API 请求失败: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("JIKAN_RETRYABLE_" + clientResponse.statusCode().value()));
                    })
                    .bodyToMono(JikanResponse.class).retryWhen(jikanRetry())
                    .block();

            if (response == null || response.data == null) return;

            for (JikanAnime anime : response.data) {
                try { saveOrUpdateAnime(anime); } catch (Exception e) {}
            }
            log.info("已同步 {} 年第{}页: {} 条", year, page, response.data.size());
        } catch (Exception e) {
            log.error("获取 {} 年第{}页数据时出错", year, page, e);
        }
    }

    /**
     * 按类型（tv/movie/ova/special）同步番剧，按会员数排序
     */
    private void fetchAndSaveByType(String type, int page) {
        try {
            JikanResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/anime")
                            .queryParam("type", type)
                            .queryParam("order_by", "members")
                            .queryParam("sort", "desc")
                            .queryParam("page", page)
                            .queryParam("limit", 25)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Jikan 类型({}) API 请求失败: {}", type, clientResponse.statusCode());
                        return Mono.error(new RuntimeException("JIKAN_RETRYABLE_" + clientResponse.statusCode().value()));
                    })
                    .bodyToMono(JikanResponse.class).retryWhen(jikanRetry())
                    .block();

            if (response == null || response.data == null) return;

            for (JikanAnime anime : response.data) {
                try { saveOrUpdateAnime(anime); } catch (Exception e) {}
            }
            log.info("已同步 type={} 第{}页: {} 条", type, page, response.data.size());
        } catch (Exception e) {
            log.error("获取 type={} 第{}页数据时出错", type, page, e);
        }
    }

    /**
     * 无过滤全量同步（仅排序，命中 Jikan 简单索引，速度最快）
     */
    private void fetchAndSaveAllAnime(int page, String orderBy, String sort) {
        try {
            JikanResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/anime")
                            .queryParam("order_by", orderBy)
                            .queryParam("sort", sort)
                            .queryParam("page", page)
                            .queryParam("limit", 25)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Jikan 全量 API 请求失败: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("JIKAN_RETRYABLE_" + clientResponse.statusCode().value()));
                    })
                    .bodyToMono(JikanResponse.class).retryWhen(jikanRetry())
                    .block();

            if (response == null || response.data == null) return;

            for (JikanAnime anime : response.data) {
                try { saveOrUpdateAnime(anime); } catch (Exception e) {}
            }
        } catch (Exception e) {
            log.error("获取全量第{}页({}/{})数据时出错", page, orderBy, sort, e);
        }
    }

    /**
     * 按首字母同步番剧（按会员数排序，正交维度挖掘隐藏作品）
     */
    private void fetchAndSaveByLetter(String letter, int page) {
        try {
            JikanResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/anime")
                            .queryParam("letter", letter)
                            .queryParam("order_by", "members")
                            .queryParam("sort", "desc")
                            .queryParam("page", page)
                            .queryParam("limit", 25)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Jikan 字母({}) API 请求失败: {}", letter, clientResponse.statusCode());
                        return Mono.error(new RuntimeException("JIKAN_RETRYABLE_" + clientResponse.statusCode().value()));
                    })
                    .bodyToMono(JikanResponse.class).retryWhen(jikanRetry())
                    .block();

            if (response == null || response.data == null) return;

            for (JikanAnime anime : response.data) {
                try { saveOrUpdateAnime(anime); } catch (Exception e) {}
            }
            log.info("已同步 letter={} 第{}页: {} 条", letter, page, response.data.size());
        } catch (Exception e) {
            log.error("获取 letter={} 第{}页数据时出错", letter, page, e);
        }
    }

    /** MAL genre ID → 补充标签名（Jikan 按 ID 筛选时可能不返回该类型名，需补标） */
    private static final Map<Integer, String> GENRE_TAG = Map.ofEntries(
        Map.entry(1, "Action"),         // 动作
        Map.entry(2, "Adventure"),      // 冒险
        Map.entry(4, "Comedy"),         // 喜剧
        Map.entry(7, "Mystery"),        // 悬疑
        Map.entry(8, "Drama"),          // 剧情
        Map.entry(10, "Fantasy"),       // 奇幻
        Map.entry(14, "Horror"),        // 恐怖
        Map.entry(18, "Mecha"),         // 机甲
        Map.entry(22, "Romance"),       // 恋爱
        Map.entry(23, "School"),        // 校园
        Map.entry(24, "Sci-Fi"),        // 科幻
        Map.entry(27, "Shounen"),       // 热血
        Map.entry(30, "Sports"),        // 运动
        Map.entry(36, "Slice of Life"), // 日常
        Map.entry(37, "Supernatural"),  // 超自然
        Map.entry(40, "Psychological"), // 心理
        Map.entry(41, "Thriller"),      // 惊悚
        Map.entry(42, "Seinen"),        // 青年
        Map.entry(43, "Josei"),         // 女性向
        Map.entry(62, "Isekai")         // 异世界
    );

    /**
     * 按类型同步番剧（评分排序），并自动补上 Jikan 不返回的类型标签
     */
    private void fetchAndSaveGenre(int genreId, int page) {
        fetchAndSaveGenre(genreId, page, "score", "desc");
    }

    /**
     * 按类型同步番剧（自定义排序），并自动补上 Jikan 不返回的类型标签
     */
    private void fetchAndSaveGenre(int genreId, int page, String orderBy, String sort) {
        try {
            JikanResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/anime")
                            .queryParam("genres", genreId)
                            .queryParam("order_by", orderBy)
                            .queryParam("sort", sort)
                            .queryParam("page", page)
                            .queryParam("limit", 25)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("Jikan 类型 API 请求失败: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("JIKAN_RETRYABLE_" + clientResponse.statusCode().value()));
                    })
                    .bodyToMono(JikanResponse.class).retryWhen(jikanRetry())
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
            log.info("已同步类型({})第{}页({}/{}): {} 条", genreId, page, orderBy, sort, response.data.size());
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
    /** Jikan 5xx/429 瞬时错误自动重试：一次 504 不再废掉整页同步 */
    private reactor.util.retry.Retry jikanRetry() {
        return reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(5))
                .filter(e -> {
                    String m = String.valueOf(e.getMessage());
                    return m.startsWith("JIKAN_RETRYABLE_");
                });
    }

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
                        return Mono.error(new RuntimeException("JIKAN_RETRYABLE_" + clientResponse.statusCode().value()));
                    })
                    .bodyToMono(JikanResponse.class).retryWhen(jikanRetry())
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
