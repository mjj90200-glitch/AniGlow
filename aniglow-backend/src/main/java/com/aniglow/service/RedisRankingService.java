package com.aniglow.service;

import com.aniglow.entity.Anime;
import com.aniglow.repository.AnimeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis 排行榜服务
 *
 * 使用 Redis Sorted Set (ZSet) 实现高性能排行榜
 * - Score: 贝叶斯加权评分
 * - Member: 动漫ID
 *
 * 支持三个维度：
 * - weekly: 本周热门
 * - monthly: 本月热门
 * - yearly: 年度热门
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRankingService {

    private final StringRedisTemplate redisTemplate;
    private final AnimeRepository animeRepository;
    private final BayesianRatingService bayesianRatingService;

    // Redis Key 前缀
    private static final String RANKING_KEY_PREFIX = "aniglow:ranking:";
    private static final String RANKING_WEEKLY = RANKING_KEY_PREFIX + "weekly";
    private static final String RANKING_MONTHLY = RANKING_KEY_PREFIX + "monthly";
    private static final String RANKING_YEARLY = RANKING_KEY_PREFIX + "yearly";
    private static final String RANKING_ALL_TIME = RANKING_KEY_PREFIX + "all_time";

    /**
     * 更新动漫在排行榜中的分数
     *
     * @param animeId       动漫ID
     * @param bayesianScore 贝叶斯评分
     */
    public void updateRankingScore(Long animeId, BigDecimal bayesianScore) {
        double score = bayesianScore != null ? bayesianScore.doubleValue() : 0.0;
        String member = animeId.toString();

        // 更新各维度排行榜
        redisTemplate.opsForZSet().add(RANKING_ALL_TIME, member, score);
        redisTemplate.opsForZSet().add(RANKING_WEEKLY, member, score);
        redisTemplate.opsForZSet().add(RANKING_MONTHLY, member, score);
        redisTemplate.opsForZSet().add(RANKING_YEARLY, member, score);

        log.debug("更新排行榜分数: animeId={}, score={}", animeId, score);
    }

    /**
     * 获取排行榜
     *
     * @param type   排行榜类型: weekly, monthly, yearly, all_time
     * @param topN   前N名
     * @return 动漫ID列表（按排名排序）
     */
    public List<Long> getRanking(String type, int topN) {
        String key = RANKING_KEY_PREFIX + type;
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);

        if (tuples == null || tuples.isEmpty()) {
            log.warn("排行榜 {} 为空，尝试从数据库加载", type);
            return loadRankingFromDatabase(type, topN);
        }

        return tuples.stream()
                .map(tuple -> Long.parseLong(Objects.requireNonNull(tuple.getValue())))
                .collect(Collectors.toList());
    }

    /**
     * 获取排行榜（带动漫详情）
     */
    public List<RankingItem> getRankingWithDetails(String type, int topN) {
        List<Long> animeIds = getRanking(type, topN);

        if (animeIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询动漫详情
        List<Anime> animes = animeRepository.findAllById(animeIds);

        // 构建 ID -> Anime 映射
        var animeMap = animes.stream()
                .collect(Collectors.toMap(Anime::getId, anime -> anime));

        // 按排名顺序组装结果
        List<RankingItem> result = new ArrayList<>();
        int rank = 1;
        for (Long animeId : animeIds) {
            Anime anime = animeMap.get(animeId);
            if (anime != null) {
                result.add(new RankingItem(
                        rank++,
                        anime.getId(),
                        anime.getTitle(),
                        anime.getCoverImage(),
                        anime.getBayesianRating() != null ? anime.getBayesianRating().doubleValue() : null,
                        anime.getRatingCount()
                ));
            }
        }

        return result;
    }

    /**
     * 从数据库加载排行榜（Redis 未命中时的 fallback）
     */
    public List<Long> loadRankingFromDatabase(String type, int topN) {
        List<Anime> animes;

        switch (type) {
            case "weekly" -> {
                // 本周新增评分较多的动漫
                LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
                animes = animeRepository.findAllByOrderByBayesianRatingDesc(
                        org.springframework.data.domain.PageRequest.of(0, topN)
                ).getContent();
            }
            case "monthly" -> {
                animes = animeRepository.findAllByOrderByBayesianRatingDesc(
                        org.springframework.data.domain.PageRequest.of(0, topN)
                ).getContent();
            }
            case "yearly" -> {
                animes = animeRepository.findAllByOrderByBayesianRatingDesc(
                        org.springframework.data.domain.PageRequest.of(0, topN)
                ).getContent();
            }
            default -> {
                // all_time
                animes = animeRepository.findAllByOrderByBayesianRatingDesc(
                        org.springframework.data.domain.PageRequest.of(0, topN)
                ).getContent();
            }
        }

        // 同步到 Redis
        for (Anime anime : animes) {
            if (anime.getBayesianRating() != null) {
                updateRankingScore(anime.getId(), anime.getBayesianRating());
            }
        }

        return animes.stream().map(Anime::getId).collect(Collectors.toList());
    }

    /**
     * 全量重建排行榜
     * 定时任务：每天凌晨 2 点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @PostConstruct
    public void rebuildAllRankings() {
        log.info("开始重建排行榜...");

        // 清理旧排行榜
        redisTemplate.delete(RANKING_WEEKLY);
        redisTemplate.delete(RANKING_MONTHLY);
        redisTemplate.delete(RANKING_YEARLY);
        redisTemplate.delete(RANKING_ALL_TIME);

        // 从数据库加载所有动漫
        List<Anime> allAnime = animeRepository.findAll();

        // 批量更新到 Redis
        for (Anime anime : allAnime) {
            if (anime.getBayesianRating() != null && anime.getBayesianRating().compareTo(BigDecimal.ZERO) > 0) {
                updateRankingScore(anime.getId(), anime.getBayesianRating());
            }
        }

        // 设置过期时间（周榜 7 天，月榜 30 天）
        redisTemplate.expire(RANKING_WEEKLY, 7, java.util.concurrent.TimeUnit.DAYS);
        redisTemplate.expire(RANKING_MONTHLY, 30, java.util.concurrent.TimeUnit.DAYS);

        log.info("排行榜重建完成，共 {} 个动漫", allAnime.size());
    }

    /**
     * 获取动漫排名
     *
     * @param animeId 动漫ID
     * @param type    排行榜类型
     * @return 排名（从1开始），未上榜返回 -1
     */
    public long getAnimeRank(Long animeId, String type) {
        String key = RANKING_KEY_PREFIX + type;
        Long rank = redisTemplate.opsForZSet().reverseRank(key, animeId.toString());
        return rank != null ? rank + 1 : -1;
    }

    /**
     * 获取动漫在当前排行榜中的分数
     */
    public Double getAnimeScore(Long animeId, String type) {
        String key = RANKING_KEY_PREFIX + type;
        return redisTemplate.opsForZSet().score(key, animeId.toString());
    }

    /**
     * 排行榜项 DTO
     */
    public record RankingItem(
            int rank,
            Long animeId,
            String title,
            String coverImage,
            Double score,
            Long ratingCount
    ) {}
}
