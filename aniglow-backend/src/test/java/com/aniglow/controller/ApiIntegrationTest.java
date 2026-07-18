package com.aniglow.controller;

import com.aniglow.entity.Anime;
import com.aniglow.entity.Community;
import com.aniglow.repository.AnimeRepository;
import com.aniglow.repository.CommunityRepository;
import com.aniglow.repository.UserRepository;
import com.aniglow.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
@DisplayName("API 集成测试")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private UserRepository userRepository;

    private Anime savedAnime;
    private Community savedCommunity;

    @BeforeEach
    void setUp() {
        communityRepository.deleteAll();
        animeRepository.deleteAll();
        userRepository.deleteAll();

        savedAnime = animeRepository.save(Anime.builder()
                .malId(10001L + System.nanoTime() % 1000000)
                .title("Steins;Gate")
                .titleJapanese("シュタインズ・ゲート")
                .titleEnglish("Steins;Gate")
                .type("TV")
                .status("Finished")
                .episodes(24)
                .synopsis("A group of friends discover time travel.")
                .bayesianRating(BigDecimal.valueOf(9.07))
                .meanRating(BigDecimal.valueOf(9.12))
                .ratingCount(500_000L)
                .genres(new java.util.HashSet<>(Set.of("Sci-Fi", "Thriller")))
                .build());

        animeRepository.save(Anime.builder()
                .malId(20002L + System.nanoTime() % 1000000)
                .title("Attack on Titan")
                .type("TV")
                .status("Finished")
                .episodes(87)
                .synopsis("Humanity fights for survival.")
                .bayesianRating(BigDecimal.valueOf(8.55))
                .meanRating(BigDecimal.valueOf(8.60))
                .ratingCount(300_000L)
                .genres(new java.util.HashSet<>(Set.of("Action", "Drama")))
                .build());

        savedCommunity = communityRepository.save(Community.builder()
                .slug("anime-chat")
                .name("动漫闲聊")
                .description("畅聊一切动漫相关")
                .category("General")
                .tags("闲聊,讨论,推荐")
                .postCount(0L)
                .memberCount(0L)
                .heatScore(0L)
                .featured(true)
                .build());
    }

    // ═══════════════════════════════════════════════════════════════
    // 动漫接口 (公开)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/anime — 动漫列表")
    class AnimeList {

        @Test
        @DisplayName("返回分页动漫列表")
        void returnsPaginatedAnimeList() throws Exception {
            mockMvc.perform(get("/anime")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(1));
        }

        @Test
        @DisplayName("支持分页参数")
        void supportsPagination() throws Exception {
            mockMvc.perform(get("/anime")
                            .param("page", "0")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.pageSize").value(1));
        }

        @Test
        @DisplayName("默认按贝叶斯评分降序排列")
        void defaultsToBayesianRatingDesc() throws Exception {
            mockMvc.perform(get("/anime"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("Steins;Gate"));
        }
    }

    @Nested
    @DisplayName("GET /api/anime/{id} — 动漫详情")
    class AnimeDetail {

        @Test
        @DisplayName("返回动漫完整信息")
        void returnsFullAnimeDetail() throws Exception {
            mockMvc.perform(get("/anime/{id}", savedAnime.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Steins;Gate"))
                    .andExpect(jsonPath("$.data.titleJapanese").value("シュタインズ・ゲート"))
                    .andExpect(jsonPath("$.data.type").value("TV"))
                    .andExpect(jsonPath("$.data.episodes").value(24))
                    .andExpect(jsonPath("$.data.bayesianRating").value(9.07))
                    .andExpect(jsonPath("$.data.genres").isArray())
                    .andExpect(jsonPath("$.data.genres", hasItems("Sci-Fi", "Thriller")));
        }

        @Test
        @DisplayName("不存在的ID → 404")
        void notFoundReturns404() throws Exception {
            mockMvc.perform(get("/anime/{id}", 99999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isString());
        }
    }

    @Nested
    @DisplayName("GET /api/anime/search — 动漫搜索")
    class AnimeSearch {

        @Test
        @DisplayName("按关键词搜索标题")
        void searchesByKeyword() throws Exception {
            mockMvc.perform(get("/anime/search")
                            .param("keyword", "Steins"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Steins;Gate"));
        }

        @Test
        @DisplayName("无匹配结果返回空列表")
        void noResultsReturnsEmpty() throws Exception {
            mockMvc.perform(get("/anime/search")
                            .param("keyword", "XYZNonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/anime/top-rated — 高分排行")
    class TopRated {

        @Test
        @DisplayName("按贝叶斯评分降序")
        void returnsTopRatedAnime() throws Exception {
            mockMvc.perform(get("/anime/top-rated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("Steins;Gate"));
        }
    }

    @Nested
    @DisplayName("GET /api/anime/genre/{genre} — 按类型筛选")
    class GenreFilter {

        @Test
        @DisplayName("按类型筛选动漫")
        void filtersByGenre() throws Exception {
            mockMvc.perform(get("/anime/genre/{genre}", "Sci-Fi"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Steins;Gate"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 评分接口 (部分公开 + 需认证)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/ratings — 提交评分 (需认证)")
    class CreateRating {

        @Test
        @DisplayName("未登录 → 401")
        void unauthenticatedReturns401() throws Exception {
            mockMvc.perform(post("/ratings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"animeId": %d, "score": 9.0}
                                    """.formatted(savedAnime.getId())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/ratings/anime/{animeId} — 评分列表 (公开)")
    class GetRatings {

        @Test
        @DisplayName("返回动漫评分列表")
        void returnsRatingsForAnime() throws Exception {
            mockMvc.perform(get("/ratings/anime/{animeId}", savedAnime.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 社区接口 (GET 公开, POST 需认证)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/communities — 社区列表 (公开)")
    class CommunityList {

        @Test
        @DisplayName("返回所有社区")
        void returnsAllCommunities() throws Exception {
            mockMvc.perform(get("/communities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].slug").value("anime-chat"))
                    .andExpect(jsonPath("$.data[0].name").value("动漫闲聊"));
        }
    }

    @Nested
    @DisplayName("GET /api/communities/{slug} — 社区详情 (公开)")
    class CommunityDetail {

        @Test
        @DisplayName("返回社区详细信息")
        void returnsCommunityDetail() throws Exception {
            mockMvc.perform(get("/communities/{slug}", "anime-chat"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.slug").value("anime-chat"))
                    .andExpect(jsonPath("$.data.description").value("畅聊一切动漫相关"))
                    .andExpect(jsonPath("$.data.tags").isArray());
        }

        @Test
        @DisplayName("不存在的社区 → 404")
        void notFoundReturns404() throws Exception {
            mockMvc.perform(get("/communities/{slug}", "nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/communities/{slug}/posts — 社区帖子列表 (公开)")
    class CommunityPosts {

        @Test
        @DisplayName("返回帖子分页")
        void returnsCommunityPosts() throws Exception {
            mockMvc.perform(get("/communities/{slug}/posts", "anime-chat"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/communities/{slug}/posts — 发布帖子 (需认证)")
    class CreatePost {

        @Test
        @DisplayName("未登录 → 401")
        void unauthenticatedReturns401() throws Exception {
            mockMvc.perform(post("/communities/{slug}/posts", "anime-chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "测试帖", "content": "测试内容"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 异常处理
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("全局异常处理")
    class ExceptionHandling {

        @Test
        @DisplayName("404 → 统一响应格式")
        void notFoundReturnsUnifiedFormat() throws Exception {
            mockMvc.perform(get("/anime/99999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isString())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("无认证访问需认证资源 → 401")
        void unauthorizedAccessReturns401() throws Exception {
            mockMvc.perform(delete("/ratings/1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
