package com.aniglow.app.data.model

data class PublicStats(
    val agentCount: Int = 0,
    val animeCount: Int = 0,
    val memberCount: Int = 0,
)

data class AnimePage(
    val content: List<AnimeSummary> = emptyList(),
    val pageNumber: Int = 0,
    val pageSize: Int = 20,
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val last: Boolean = true,
)

data class AnimeFilterOptions(
    val genres: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
)

data class AnimeSummary(
    val id: Long,
    val title: String = "",
    val titleJapanese: String? = null,
    val titleEnglish: String? = null,
    val titleCn: String? = null,
    val synopsis: String? = null,
    val synopsisCn: String? = null,
    val coverImage: String? = null,
    val type: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val hasAgent: Boolean = false,
    val bayesianRating: Double? = null,
    val communityBayesianRating: Double? = null,
    val fireflyVoteCount: Long = 0,
    val genres: List<String> = emptyList(),
    val studio: String? = null,
    val source: String? = null,
    val season: String? = null,
    val airedFrom: String? = null,
    val airedTo: String? = null,
    val durationMinutes: Int? = null,
    val rating: String? = null,
    val meanRating: Double? = null,
    val ratingCount: Long = 0,
    val communityMeanRating: Double? = null,
    val communityRatingCount: Long = 0,
    val membersCount: Long = 0,
    val favoritesCount: Long = 0,
    val trailerUrl: String? = null,
    val year: Int? = null,
) {
    val displayTitle: String
        get() = titleCn?.takeIf(String::isNotBlank) ?: title

    val displayRating: Double?
        get() = communityBayesianRating ?: bayesianRating
}

data class AgentCharacter(
    val code: String,
    val displayName: String,
    val sourceTitle: String? = null,
    val avatarUrl: String? = null,
    val backgroundUrl: String? = null,
    val openingAudioUrl: String? = null,
    val sortOrder: Int = 0,
)

data class AgentChatRequest(
    val message: String,
    val role: String,
)

data class AgentChatResponse(
    val role: String,
    val roleName: String? = null,
    val reply: String,
    val emotion: String? = null,
    val cached: Boolean = false,
    val rateLimited: Boolean = false,
    val quotaLimit: Int? = null,
    val quotaRemaining: Int? = null,
    val quotaResetAt: String? = null,
)

data class AnimeRating(
    val id: Long,
    val userId: Long,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val animeId: Long,
    val score: Double,
    val review: String? = null,
    val likeCount: Long = 0,
    val createdAt: String = "",
) {
    val authorName: String get() = displayName?.takeIf(String::isNotBlank) ?: username
}

data class AnimeRatingRequest(
    val animeId: Long,
    val score: Double,
    val review: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
)

data class FireflyVoteRequest(val animeId: Long)

data class FireflyVoteResponse(
    val animeId: Long,
    val fireflyVoteCount: Long,
    val userVoteCount: Long = 0,
)
