package com.aniglow.service;

import com.aniglow.entity.Anime;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 贝叶斯平均分算法服务
 *
 * 贝叶斯平均公式：
 * weighted_rating = (v / (v + m)) * R + (m / (v + m)) * C
 *
 * 其中：
 * - R = 该动漫的平均分
 * - v = 该动漫的评分人数
 * - m = 最小评分阈值（置信权重）
 * - C = 所有动漫的全局平均分
 *
 * 这个算法的优势：
 * 1. 小众动漫（评分人数少）会向全局平均分回归，避免刷榜
 * 2. 热门动漫（评分人数多）更依赖自身真实评分
 * 3. 提供了一个平滑的过渡区间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BayesianRatingService {

    private final AnimeRepository animeRepository;
    private final RatingRepository ratingRepository;

    @Value("${aniglow.bayesian.global-mean:7.0}")
    private double globalMean;

    @Value("${aniglow.bayesian.confidence-weight:25}")
    private double confidenceWeight; // m 参数

    /**
     * 计算贝叶斯平均分
     *
     * @param meanRating   该动漫的平均分 R
     * @param ratingCount  该动漫的评分人数 v
     * @return 贝叶斯加权评分
     */
    public double calculateBayesianAverage(Double meanRating, Long ratingCount) {
        if (meanRating == null || ratingCount == null || ratingCount == 0) {
            return globalMean;
        }

        // (v / (v + m)) * R + (m / (v + m)) * C
        double v = ratingCount.doubleValue();
        double m = confidenceWeight;
        double R = meanRating;
        double C = globalMean;

        double weightedRating = (v / (v + m)) * R + (m / (v + m)) * C;

        // 保留两位小数
        return Math.round(weightedRating * 100.0) / 100.0;
    }

    /**
     * 更新单个动漫的评分统计
     *
     * @deprecated 用户评分不再写回原始抓取评分。保留该方法仅用于旧管理入口兼容。
     */
    @Deprecated
    @Transactional
    public void updateAnimeRatingStats(Long animeId) {
        List<Object[]> result = ratingRepository.calculateAverageByAnimeId(animeId);

        if (result == null || result.isEmpty() || result.get(0)[0] == null) {
            log.debug("动漫 {} 暂无评分", animeId);
            return;
        }

        Double meanRating = ((Number) result.get(0)[0]).doubleValue();
        Long ratingCount = ((Number) result.get(0)[1]).longValue();

        // 计算贝叶斯平均分
        double bayesianRating = calculateBayesianAverage(meanRating, ratingCount);

        // 更新数据库
        animeRepository.updateRatingStats(animeId,
                BigDecimal.valueOf(bayesianRating),
                BigDecimal.valueOf(meanRating),
                ratingCount);

        log.debug("更新动漫 {} 评分: 平均分={}, 贝叶斯分={}, 评分人数={}",
                animeId, meanRating, bayesianRating, ratingCount);
    }

    /**
     * 更新站内用户评分统计。
     *
     * 这里专门写入 community_* 字段，不触碰 bayesian_rating/mean_rating/rating_count，
     * 这样 Jikan/MAL 抓取后的原始排行可以长期保持稳定。
     */
    @Transactional
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public void updateCommunityRatingStats(Long animeId) {
        List<Object[]> result = ratingRepository.calculateAverageByAnimeId(animeId);

        if (result == null || result.isEmpty() || result.get(0)[0] == null) {
            animeRepository.clearCommunityRatingStats(animeId);
            log.debug("动漫 {} 暂无站内评分，已清空 community 统计", animeId);
            return;
        }

        Double meanRating = ((Number) result.get(0)[0]).doubleValue();
        Long ratingCount = ((Number) result.get(0)[1]).longValue();
        double bayesianRating = calculateBayesianAverage(meanRating, ratingCount);

        animeRepository.updateCommunityRatingStats(
                animeId,
                BigDecimal.valueOf(bayesianRating),
                BigDecimal.valueOf(meanRating),
                ratingCount
        );

        log.debug("更新动漫 {} 站内评分: 平均分={}, 贝叶斯分={}, 评分人数={}",
                animeId, meanRating, bayesianRating, ratingCount);
    }

    /**
     * 重新计算所有动漫的贝叶斯评分
     * 用于定时任务或数据迁移
     */
    @Transactional
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public void recalculateAllRatings() {
        log.info("开始重新计算所有动漫的站内用户评分...");

        List<Anime> allAnime = animeRepository.findAll();
        int updatedCount = 0;

        for (Anime anime : allAnime) {
            try {
                updateCommunityRatingStats(anime.getId());
                updatedCount++;
            } catch (Exception e) {
                log.error("计算动漫 {} 评分时出错", anime.getId(), e);
            }
        }

        log.info("站内用户评分重算完成，共更新 {} 个动漫", updatedCount);
    }

    /**
     * 获取全局平均分（动态计算）
     */
    public double calculateGlobalMean() {
        // 这里可以从数据库动态计算全局平均分
        // 暂时使用配置值
        return globalMean;
    }

    /**
     * 获取评分分布直方图数据
     */
    public double[] getRatingDistribution(Long animeId) {
        double[] distribution = new double[10]; // 1-10分
        List<Object[]> result = ratingRepository.getRatingDistribution(animeId);

        for (Object[] row : result) {
            Double score = ((Number) row[0]).doubleValue();
            Long count = ((Number) row[1]).longValue();
            int index = (int) Math.round(score) - 1;
            if (index >= 0 && index < 10) {
                distribution[index] = count;
            }
        }

        return distribution;
    }
}
