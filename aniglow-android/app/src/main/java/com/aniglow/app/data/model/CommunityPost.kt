package com.aniglow.app.data.model

data class CommunityPostPage(
    val items: List<CommunityPost> = emptyList(),
    val total: Int = 0,
    val page: Int = 0,
    val size: Int = 20,
)

data class CommunityInfo(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String = "",
    val category: String? = null,
    val relatedAnimeTitle: String? = null,
)

data class CreatePostRequest(
    val title: String,
    val content: String,
    val images: List<String> = emptyList(),
    val displayName: String? = null,
    val avatarUrl: String? = null,
)

data class CreateReplyRequest(
    val content: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
)

data class CommunityPost(
    val id: Long,
    val communityId: Long,
    val communitySlug: String,
    val communityName: String,
    val userId: Long,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val title: String,
    val content: String,
    val coverImage: String? = null,
    val images: List<String> = emptyList(),
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    val replyCount: Int = 0,
    val viewCount: Int = 0,
    val pinned: Boolean = false,
    val featured: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String? = null,
) {
    val authorName: String
        get() = displayName?.takeIf(String::isNotBlank) ?: username
}

data class CommunityReply(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val content: String,
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String? = null,
) {
    val authorName: String
        get() = displayName?.takeIf(String::isNotBlank) ?: username
}
