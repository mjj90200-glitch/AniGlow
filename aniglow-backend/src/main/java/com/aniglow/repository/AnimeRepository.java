package com.aniglow.repository;

import com.aniglow.entity.Anime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long> {

    Optional<Anime> findByMalId(Long malId);

    boolean existsByMalId(Long malId);

    Optional<Anime> findByAnilistId(Long anilistId);

    // 存量封面迁移：定位仍指向外部图床（MyAnimeList CDN）的封面
    List<Anime> findByCoverImageContainingIgnoreCase(String fragment);

    // 贝叶斯评分排序
    Page<Anime> findAllByOrderByBayesianRatingDesc(Pageable pageable);

    // 中国用户站内评分排行：只展示已经产生站内评分的作品
    Page<Anime> findByCommunityRatingCountGreaterThanOrderByCommunityBayesianRatingDescFireflyVoteCountDesc(
            Long communityRatingCount,
            Pageable pageable
    );

    // 萤火投票排行：只展示已经被投票的作品
    Page<Anime> findByFireflyVoteCountGreaterThanOrderByFireflyVoteCountDescCommunityBayesianRatingDesc(
            Long fireflyVoteCount,
            Pageable pageable
    );

    // 按类型查询
    @Query("SELECT DISTINCT a FROM Anime a JOIN a.genres g WHERE g IN :genres ORDER BY a.bayesianRating DESC")
    Page<Anime> findByGenresInOrderByBayesianRatingDesc(@Param("genres") List<String> genres, Pageable pageable);

    // 搜索标题：展示仍使用原名，但搜索允许中文译名、简称、别名命中
    @Query("SELECT a FROM Anime a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.titleEnglish) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.titleJapanese) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.titleCn) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.searchAliases) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY a.bayesianRating DESC")
    Page<Anime> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    @Query(
            value = "SELECT a.* FROM anime a " +
                    "WHERE MATCH(a.title, a.title_english, a.title_cn, a.search_aliases) " +
                    "AGAINST (:query IN BOOLEAN MODE) " +
                    "ORDER BY a.bayesian_rating DESC",
            countQuery = "SELECT COUNT(*) FROM anime a " +
                    "WHERE MATCH(a.title, a.title_english, a.title_cn, a.search_aliases) " +
                    "AGAINST (:query IN BOOLEAN MODE)",
            nativeQuery = true
    )
    Page<Anime> searchByFullText(@Param("query") String query, Pageable pageable);

    // 更新贝叶斯评分
    @Modifying
    @Query("UPDATE Anime a SET a.bayesianRating = :bayesianRating, a.meanRating = :meanRating, " +
           "a.ratingCount = :ratingCount WHERE a.id = :animeId")
    void updateRatingStats(@Param("animeId") Long animeId,
                          @Param("bayesianRating") BigDecimal bayesianRating,
                          @Param("meanRating") BigDecimal meanRating,
                          @Param("ratingCount") Long ratingCount);

    // 更新站内用户评分统计，不影响 Jikan/MAL 抓取评分
    @Modifying
    @Query("UPDATE Anime a SET a.communityBayesianRating = :communityBayesianRating, " +
           "a.communityMeanRating = :communityMeanRating, " +
           "a.communityRatingCount = :communityRatingCount WHERE a.id = :animeId")
    void updateCommunityRatingStats(@Param("animeId") Long animeId,
                                    @Param("communityBayesianRating") BigDecimal communityBayesianRating,
                                    @Param("communityMeanRating") BigDecimal communityMeanRating,
                                    @Param("communityRatingCount") Long communityRatingCount);

    @Modifying
    @Query("UPDATE Anime a SET a.communityBayesianRating = null, " +
           "a.communityMeanRating = null, a.communityRatingCount = 0 WHERE a.id = :animeId")
    void clearCommunityRatingStats(@Param("animeId") Long animeId);

    @Modifying
    @Query("UPDATE Anime a SET a.fireflyVoteCount = :fireflyVoteCount WHERE a.id = :animeId")
    void updateFireflyVoteCount(@Param("animeId") Long animeId,
                                @Param("fireflyVoteCount") Long fireflyVoteCount);

    // 按年份和季节查询
    List<Anime> findByYearAndSeasonOrderByBayesianRatingDesc(Integer year, String season);

    // 按状态查询
    Page<Anime> findByStatusOrderByBayesianRatingDesc(String status, Pageable pageable);

    // 热门动漫（按 popularity 排序）
    Page<Anime> findAllByOrderByPopularityAsc(Pageable pageable);

    // 查找缺失中文标题的动漫
    List<Anime> findByTitleCnIsNull();

    // 查找缺失中文简介的动漫（有英文简介但无中文翻译）
    List<Anime> findBySynopsisCnIsNullAndSynopsisIsNotNull();

    // 按类型统计番剧数
    @Query("SELECT COUNT(DISTINCT a.id) FROM Anime a JOIN a.genres g WHERE g = :genre")
    long countByGenre(@Param("genre") String genre);

    // 综合筛选：关键词 + 题材 + 类型 + 年份（单年或区间）+ 国家/地区，全部可选，组合过滤
    @Query("""
           SELECT DISTINCT a FROM Anime a LEFT JOIN a.genres g
           WHERE (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.titleEnglish) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.titleJapanese) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.titleCn) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.searchAliases) LIKE LOWER(CONCAT('%', :keyword, '%')))
             AND (:genre IS NULL OR g = :genre)
             AND (:type IS NULL OR a.type = :type)
             AND (:country IS NULL OR a.country = :country)
             AND (:yearFrom IS NULL OR a.year >= :yearFrom)
             AND (:yearTo IS NULL OR a.year <= :yearTo)
           """)
    Page<Anime> filterAnime(@Param("keyword") String keyword,
                            @Param("genre") String genre,
                            @Param("type") String type,
                            @Param("yearFrom") Integer yearFrom,
                            @Param("yearTo") Integer yearTo,
                            @Param("country") String country,
                            Pageable pageable);

    // 筛选选项集
    @Query("SELECT DISTINCT a.year FROM Anime a WHERE a.year IS NOT NULL ORDER BY a.year DESC")
    List<Integer> findDistinctYears();

    @Query("SELECT DISTINCT a.country FROM Anime a WHERE a.country IS NOT NULL ORDER BY a.country")
    List<String> findDistinctCountries();

    @Query("SELECT DISTINCT a.type FROM Anime a WHERE a.type IS NOT NULL ORDER BY a.type")
    List<String> findDistinctTypes();

    @Query("SELECT DISTINCT g FROM Anime a JOIN a.genres g WHERE g IS NOT NULL ORDER BY g")
    List<String> findDistinctGenres();
}
