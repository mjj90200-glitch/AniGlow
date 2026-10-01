package com.aniglow.service;

import com.aniglow.entity.Anime;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.storage.ImageStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AniList 数据源同步服务（替代 Jikan 定时任务）。
 *
 * 设计要点：
 * - GraphQL 分页拉取（POPULARITY_DESC，过滤成人内容），一次请求 50 部全字段
 * - 与既有 MAL 数据按「标准化标题」交叉匹配：命中则刷新旧记录（anime.id 不变，
 *   用户评分/投票/帖子关联全保留），未命中才插入新行 —— 同一部番永远只有一条记录
 * - 封面同步时转存自有 OSS（复用 ImageStore），不再依赖外部图床；下载失败回退外链
 * - AniList 限速 90 次/分，页间默认 800ms 延迟 + 失败跳过续跑
 */
@Slf4j
@Service
public class AniListSyncService {

    private static final String GRAPHQL_URL = "https://graphql.anilist.co";

    private static final String POPULAR_QUERY = """
            query ($page: Int, $perPage: Int) {
              Page(page: $page, perPage: $perPage) {
                pageInfo { hasNextPage }
                media(type: ANIME, sort: POPULARITY_DESC, isAdult: false) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large }
                  description
                  meanScore
                  episodes
                  duration
                  format
                  status
                  countryOfOrigin
                  season
                  seasonYear
                  startDate { year month day }
                  genres
                  popularity
                  favourites
                }
              }
            }
            """;

    private final WebClient webClient = WebClient.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
            .build();
    private final ObjectMapper objectMapper;
    private final AnimeRepository animeRepository;
    private final ImageStore imageStore;

    @Value("${aniglow.anilist.pages:100}")
    private int pages;
    @Value("${aniglow.anilist.per-page:50}")
    private int perPage;
    @Value("${aniglow.anilist.request-delay-ms:800}")
    private long requestDelay;
    @Value("${aniglow.anilist.cover-delay-ms:150}")
    private long coverDelay;
    @Value("${aniglow.anilist.transfer-covers:true}")
    private boolean transferCovers;

    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    /** 本轮同步内复用的「标准化标题 → 既有番剧」索引 */
    private Map<String, Anime> titleIndex = Map.of();

    public AniListSyncService(ObjectMapper objectMapper,
                              AnimeRepository animeRepository,
                              ImageStore imageStore) {
        this.objectMapper = objectMapper;
        this.animeRepository = animeRepository;
        this.imageStore = imageStore;
    }

    /** 拉取热门番剧（POPULARITY_DESC）并入库存，幂等可重复执行 */
    public void syncPopularAnime() {
        if (!syncRunning.compareAndSet(false, true)) {
            log.info("AniList 同步任务已在运行，跳过本次重复请求");
            return;
        }
        log.info("开始同步 AniList 番剧数据（{} 页 × {} 条）...", pages, perPage);
        try {
            titleIndex = buildTitleIndex();
            int inserted = 0;
            int merged = 0;
            for (int page = 1; page <= pages; page++) {
                try {
                    int[] counts = fetchAndSavePage(page);
                    inserted += counts[0];
                    merged += counts[1];
                    if (counts[2] == 0) {
                        log.info("AniList 第 {} 页无数据，同步提前完成", page);
                        break;
                    }
                } catch (Exception e) {
                    log.warn("AniList 第 {} 页失败，跳过续跑: {}", page, e.getMessage());
                }
                Thread.sleep(requestDelay);
            }
            log.info("AniList 同步完成: 新增 {} 部，合并进既有记录 {} 部", inserted, merged);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("AniList 同步被中断");
        } catch (Exception e) {
            log.error("同步 AniList 数据时出错", e);
        } finally {
            syncRunning.set(false);
        }
    }

    /** 拉取并保存一页，返回 {新增数, 合并数, 本页条目数} */
    private int[] fetchAndSavePage(int page) throws Exception {
        Map<String, Object> body = Map.of(
                "query", POPULAR_QUERY,
                "variables", Map.of("page", page, "perPage", perPage)
        );
        String response = webClient.post()
                .uri(GRAPHQL_URL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .bodyValue(objectMapper.writeValueAsString(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> {
                    String errBody = r.bodyToMono(String.class).block(Duration.ofSeconds(10));
                    log.error("AniList HTTP {}: {}", r.statusCode(), errBody);
                    return reactor.core.publisher.Mono.error(
                            new IllegalStateException("ANILIST_HTTP_" + r.statusCode().value()));
                })
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        JsonNode root = objectMapper.readTree(response);
        if (root.has("errors")) {
            throw new IllegalStateException("AniList GraphQL 错误: "
                    + root.path("errors").get(0).path("message").asText(""));
        }
        JsonNode pageNode = root.path("data").path("Page");
        JsonNode media = pageNode.path("media");

        int inserted = 0;
        int merged = 0;
        for (JsonNode m : media) {
            try {
                if (saveOrUpdateAniList(m)) inserted++;
                else merged++;
            } catch (Exception e) {
                log.warn("保存 AniList 条目 {} 失败: {}", m.path("id").asLong(), e.getMessage());
            }
        }
        log.info("AniList 第 {} 页同步完成: {} 条（新增 {}，合并 {}）",
                page, media.size(), inserted, merged);
        return new int[]{inserted, merged, media.size()};
    }

    /** @return true=新增，false=合并进既有记录 */
    private boolean saveOrUpdateAniList(JsonNode m) {
        long anilistId = m.path("id").asLong();

        Anime anime = animeRepository.findByAnilistId(anilistId).orElse(null);
        boolean inserted;
        if (anime == null) {
            Anime matched = matchByTitles(m.path("title"));
            if (matched != null) {
                anime = matched;
                inserted = false;
            } else {
                anime = new Anime();
                inserted = true;
                anime.setTitle(firstNonBlank(
                        m.path("title").path("romaji").asText(null),
                        m.path("title").path("english").asText(null),
                        m.path("title").path("native").asText(null),
                        "Unknown #" + anilistId));
            }
        } else {
            inserted = false;
        }

        anime.setAnilistId(anilistId);

        // 标题：不覆盖既有值（保留中文翻译等资产），缺失才补
        String romaji = m.path("title").path("romaji").asText(null);
        String english = m.path("title").path("english").asText(null);
        String nativeTitle = m.path("title").path("native").asText(null);
        if (!hasText(anime.getTitle())) {
            anime.setTitle(firstNonBlank(romaji, english, nativeTitle, "Unknown #" + anilistId));
        }
        if (!hasText(anime.getTitleJapanese())) anime.setTitleJapanese(nativeTitle);
        if (!hasText(anime.getTitleEnglish())) anime.setTitleEnglish(english);

        // 简介：去除 HTML 标签，限长
        String desc = stripHtml(m.path("description").asText(null));
        if (hasText(desc)) {
            anime.setSynopsis(truncate(desc, 2000));
        }

        // 评分：AniList 百分制 → 十分制
        JsonNode meanScore = m.path("meanScore");
        if (!meanScore.isNull() && meanScore.canConvertToInt()) {
            BigDecimal score = BigDecimal.valueOf(meanScore.asInt() / 10.0).setScale(2, RoundingMode.HALF_UP);
            anime.setMeanRating(score);
            if (anime.getBayesianRating() == null) {
                anime.setBayesianRating(score);
            }
        }

        if (m.path("episodes").asInt(0) > 0) anime.setEpisodes(m.path("episodes").asInt());
        if (m.path("duration").asInt(0) > 0) anime.setDurationMinutes(m.path("duration").asInt());
        anime.setType(mapFormat(m.path("format").asText(null)));
        anime.setStatus(mapStatus(m.path("status").asText(null)));
        // 国家/地区以 AniList 官方字段为权威来源，新增与合并记录都写入
        anime.setCountry(mapCountry(m.path("countryOfOrigin").asText(null)));

        JsonNode start = m.path("startDate");
        if (start.path("year").canConvertToInt()) {
            anime.setAiredFrom(LocalDate.of(
                    start.path("year").asInt(),
                    Math.max(start.path("month").asInt(1), 1),
                    Math.max(start.path("day").asInt(1), 1)));
        }

        if (m.path("popularity").asLong(0) > 0) anime.setPopularity(m.path("popularity").asLong());
        if (m.path("favourites").asLong(0) > 0) anime.setFavoritesCount(m.path("favourites").asLong());

        int year = m.path("seasonYear").asInt(0);
        if (year > 0) {
            anime.setYear(year);
            String season = m.path("season").asText("");
            anime.setSeason(season.isBlank() ? String.valueOf(year)
                    : season.charAt(0) + season.substring(1).toLowerCase(Locale.ROOT) + " " + year);
        }

        Set<String> genres = new HashSet<>();
        for (JsonNode g : m.path("genres")) {
            if (hasText(g.asText())) genres.add(g.asText());
        }
        if (!genres.isEmpty()) anime.setGenres(genres);

        if (anime.getHasAgent() == null) anime.setHasAgent(false);
        anime.setSyncedAt(LocalDateTime.now());

        // 封面：下载 AniList 图 → 转存自有存储；失败回退外链
        String aniCover = firstNonBlank(
                m.path("coverImage").path("extraLarge").asText(null),
                m.path("coverImage").path("large").asText(null));
        if (hasText(aniCover)) {
            if (transferCovers) {
                try {
                    byte[] img = download(aniCover);
                    anime.setCoverImage(imageStore.store(img, "covers/anilist/" + anilistId + ".jpg"));
                    Thread.sleep(coverDelay);
                } catch (Exception e) {
                    log.warn("封面转存失败，回退外链: anilistId={} - {}", anilistId, e.getMessage());
                    anime.setCoverImage(aniCover);
                }
            } else {
                anime.setCoverImage(aniCover);
            }
        }

        animeRepository.save(anime);
        return inserted;
    }

    // ================================================================
    // 跨源标题匹配
    // ================================================================

    private Map<String, Anime> buildTitleIndex() {
        Map<String, Anime> index = new HashMap<>();
        for (Anime a : animeRepository.findAll()) {
            putTitle(index, a.getTitle(), a);
            putTitle(index, a.getTitleJapanese(), a);
            putTitle(index, a.getTitleEnglish(), a);
            putTitle(index, a.getTitleCn(), a);
        }
        log.info("标题匹配索引构建完成: {} 部既有番剧", animeRepository.count());
        return index;
    }

    private void putTitle(Map<String, Anime> index, String title, Anime anime) {
        if (hasText(title)) index.putIfAbsent(normalize(title), anime);
    }

    /** AniList 标题（romaji/english/native）逐一与既有库匹配 */
    private Anime matchByTitles(JsonNode titleNode) {
        for (String field : List.of("romaji", "english", "native")) {
            String t = titleNode.path(field).asText(null);
            if (hasText(t)) {
                Anime hit = titleIndex.get(normalize(t));
                if (hit != null) return hit;
            }
        }
        return null;
    }

    private String normalize(String title) {
        if (!hasText(title)) return "";
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。！？、：；（）【】《》「」『』·・~～]+", "");
    }

    // ================================================================
    // 工具方法
    // ================================================================

    private byte[] download(String url) {
        byte[] data = webClient.get().uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofSeconds(30));
        if (data == null || data.length == 0) throw new IllegalStateException("空图片: " + url);
        return data;
    }

    private String stripHtml(String text) {
        if (!hasText(text)) return null;
        String t = text.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&quot;", "\"").replace("&#039;", "'").replace("&apos;", "'")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&ldquo;", "“").replace("&rdquo;", "”").replace("&mdash;", "—");
        return t.trim();
    }

    private String mapFormat(String format) {
        if (!hasText(format)) return null;
        return switch (format.toUpperCase(Locale.ROOT)) {
            case "TV" -> "TV";
            case "MOVIE" -> "Movie";
            case "OVA" -> "OVA";
            case "ONA" -> "ONA";
            case "TV_SHORT", "SPECIAL", "PV" -> "Special";
            case "MUSIC" -> "Music";
            default -> null;
        };
    }

    private String mapStatus(String status) {
        if (!hasText(status)) return null;
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "FINISHED" -> "Finished";
            case "RELEASING" -> "Airing";
            case "NOT_YET_RELEASED" -> "Not yet aired";
            case "CANCELLED" -> "Cancelled";
            case "HIATUS" -> "Airing";
            default -> null;
        };
    }

    /** AniList 国家代码 → 中文地区名 */
    private String mapCountry(String code) {
        if (!hasText(code)) return null;
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "JP" -> "日本";
            case "CN" -> "中国";
            case "TW" -> "台湾";
            case "HK" -> "香港";
            case "KR" -> "韩国";
            case "US" -> "美国";
            case "GB" -> "英国";
            case "FR" -> "法国";
            case "DE" -> "德国";
            case "IT" -> "意大利";
            case "ES" -> "西班牙";
            case "IN" -> "印度";
            case "TH" -> "泰国";
            case "AU" -> "澳大利亚";
            case "CA" -> "加拿大";
            default -> null;
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) if (hasText(v)) return v;
        return null;
    }

    private String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }
}
