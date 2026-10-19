package com.aniglow.app.data.repository

import com.aniglow.app.data.model.AgentCharacter
import com.aniglow.app.data.model.AnimePage
import com.aniglow.app.data.model.PublicStats
import com.aniglow.app.data.model.AnimeSummary
import com.aniglow.app.data.model.AgentChatRequest
import com.aniglow.app.data.model.AgentChatResponse
import com.aniglow.app.data.model.AnimeRating
import com.aniglow.app.data.model.AnimeRatingRequest
import com.aniglow.app.data.model.AnimeFilterOptions
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.model.FireflyVoteRequest
import com.aniglow.app.data.model.FireflyVoteResponse
import com.aniglow.app.data.remote.DiscoveryApi

class DiscoveryRepository(
    private val api: DiscoveryApi,
) {
    suspend fun loadHome(): Result<Pair<PublicStats, AnimePage>> = runCatching {
        val stats = api.getPublicStats().data ?: error("站点数据暂时不可用")
        val anime = api.getTopRatedAnime().data ?: error("热门番剧暂时不可用")
        stats to anime
    }

    suspend fun loadAnime(
        keyword: String = "",
        genre: String = "",
        type: String = "",
        country: String = "",
        yearFrom: Int? = null,
        yearTo: Int? = null,
        page: Int = 0,
        size: Int = 60,
    ): Result<AnimePage> = runCatching {
        val response = api.getAnime(
            keyword = keyword.trim().ifBlank { null },
            genre = genre.ifBlank { null },
            type = type.ifBlank { null },
            country = country.ifBlank { null },
            yearFrom = yearFrom,
            yearTo = yearTo,
            page = page,
            size = size,
        )
        response.data ?: error(response.message?.takeIf(String::isNotBlank) ?: "番剧数据暂时不可用")
    }

    suspend fun loadAnimeFilterOptions(): Result<AnimeFilterOptions> = runCatching {
        api.getAnimeFilterOptions().data ?: AnimeFilterOptions()
    }

    suspend fun loadCharacters(): Result<List<AgentCharacter>> = runCatching {
        api.getAgentCharacters().data ?: error("角色列表暂时不可用")
    }

    suspend fun loadAnimeDetail(id: Long): Result<AnimeSummary> = runCatching {
        api.getAnimeById(id).data ?: error("番剧详情暂时不可用")
    }

    suspend fun chat(role: String, message: String): Result<AgentChatResponse> = runCatching {
        api.chat(AgentChatRequest(message = message.trim(), role = role)).data
            ?: error("角色暂时没有回应")
    }

    suspend fun loadReviews(animeId: Long): Result<List<AnimeRating>> = runCatching {
        api.getAnimeReviews(animeId).data.orEmpty()
    }

    suspend fun submitRating(
        animeId: Long,
        score: Double,
        review: String,
        user: AuthUser,
    ): Result<AnimeRating> = runCatching {
        api.submitRating(
            AnimeRatingRequest(
                animeId = animeId,
                score = score,
                review = review.trim().ifBlank { null },
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
            ),
        ).data ?: error("评分提交失败")
    }

    suspend fun voteFirefly(animeId: Long): Result<FireflyVoteResponse> = runCatching {
        api.voteFirefly(FireflyVoteRequest(animeId)).data ?: error("投票失败")
    }

    suspend fun loadRanking(firefly: Boolean): Result<List<AnimeSummary>> = runCatching {
        val response = if (firefly) api.getFireflyRanking() else api.getCommunityRatedAnime()
        response.data?.content ?: error("排行榜暂时不可用")
    }
}
