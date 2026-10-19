package com.aniglow.app.data.remote

import com.aniglow.app.data.model.AgentCharacter
import com.aniglow.app.data.model.AgentChatRequest
import com.aniglow.app.data.model.AgentChatResponse
import com.aniglow.app.data.model.AnimeRating
import com.aniglow.app.data.model.AnimeRatingRequest
import com.aniglow.app.data.model.FireflyVoteRequest
import com.aniglow.app.data.model.FireflyVoteResponse
import com.aniglow.app.data.model.AnimePage
import com.aniglow.app.data.model.AnimeFilterOptions
import com.aniglow.app.data.model.AnimeSummary
import com.aniglow.app.data.model.ApiEnvelope
import com.aniglow.app.data.model.PublicStats
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscoveryApi {
    @GET("stats/public")
    suspend fun getPublicStats(): ApiEnvelope<PublicStats>

    @GET("anime/top-rated")
    suspend fun getTopRatedAnime(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 8,
    ): ApiEnvelope<AnimePage>

    @GET("anime/community-rated")
    suspend fun getCommunityRatedAnime(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30,
    ): ApiEnvelope<AnimePage>

    @GET("anime/firefly-ranking")
    suspend fun getFireflyRanking(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30,
    ): ApiEnvelope<AnimePage>

    @GET("anime/filter")
    suspend fun getAnime(
        @Query("keyword") keyword: String? = null,
        @Query("genre") genre: String? = null,
        @Query("type") type: String? = null,
        @Query("yearFrom") yearFrom: Int? = null,
        @Query("yearTo") yearTo: Int? = null,
        @Query("country") country: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<AnimePage>

    @GET("anime/filter-options")
    suspend fun getAnimeFilterOptions(): ApiEnvelope<AnimeFilterOptions>

    @GET("anime/search")
    suspend fun searchAnime(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<AnimePage>

    @GET("agent/characters")
    suspend fun getAgentCharacters(): ApiEnvelope<List<AgentCharacter>>

    @GET("anime/{id}")
    suspend fun getAnimeById(@Path("id") id: Long): ApiEnvelope<AnimeSummary>

    @POST("agent/chat")
    suspend fun chat(@Body request: AgentChatRequest): ApiEnvelope<AgentChatResponse>

    @GET("ratings/anime/{animeId}/reviews")
    suspend fun getAnimeReviews(
        @Path("animeId") animeId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<List<AnimeRating>>

    @POST("ratings")
    suspend fun submitRating(@Body request: AnimeRatingRequest): ApiEnvelope<AnimeRating>

    @POST("votes")
    suspend fun voteFirefly(@Body request: FireflyVoteRequest): ApiEnvelope<FireflyVoteResponse>
}
