package com.aniglow.repository;

import com.aniglow.entity.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndAnimeId(Long userId, Long animeId);

    boolean existsByUserIdAndAnimeId(Long userId, Long animeId);

    // 数据库分页，避免先加载整部作品的全部评分再在内存截取。
    @EntityGraph(attributePaths = {"user", "anime"})
    Page<Rating> findByAnimeIdOrderByCreatedAtDesc(Long animeId, Pageable pageable);

    // 评分统计服务仍需要完整评分集合。
    List<Rating> findByAnimeId(Long animeId);

    // 获取用户的所有评分
    Page<Rating> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 计算动漫的平均分
    @Query("SELECT AVG(r.score), COUNT(r) FROM Rating r WHERE r.anime.id = :animeId")
    List<Object[]> calculateAverageByAnimeId(@Param("animeId") Long animeId);

    // 获取动漫的评分统计
    @Query("SELECT r.score, COUNT(r) FROM Rating r WHERE r.anime.id = :animeId GROUP BY r.score ORDER BY r.score DESC")
    List<Object[]> getRatingDistribution(@Param("animeId") Long animeId);

    // 时间段内的评分（用于排行榜计算）
    @Query("SELECT r.anime.id, AVG(r.score), COUNT(r) FROM Rating r " +
           "WHERE r.createdAt >= :startTime GROUP BY r.anime.id ORDER BY AVG(r.score) DESC")
    List<Object[]> findTopRatedByTimeRange(@Param("startTime") LocalDateTime startTime, Pageable pageable);

    // 最近评论
    @EntityGraph(attributePaths = {"user", "anime"})
    @Query("SELECT r FROM Rating r WHERE r.review IS NOT NULL AND r.review != '' ORDER BY r.createdAt DESC")
    Page<Rating> findRecentReviews(Pageable pageable);

    // 某动漫的热门评论
    @EntityGraph(attributePaths = {"user", "anime"})
    @Query("SELECT r FROM Rating r WHERE r.anime.id = :animeId AND r.review IS NOT NULL " +
           "AND r.review != '' ORDER BY r.likeCount DESC, r.updatedAt DESC, r.createdAt DESC")
    Page<Rating> findPopularReviewsByAnimeId(@Param("animeId") Long animeId, Pageable pageable);

    // 删除用户的评分
    void deleteByUserIdAndAnimeId(Long userId, Long animeId);

    // 统计用户的评分数量
    long countByUserId(Long userId);
}
