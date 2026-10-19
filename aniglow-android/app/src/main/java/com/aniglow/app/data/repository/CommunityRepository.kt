package com.aniglow.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.aniglow.app.data.model.CommunityPostPage
import com.aniglow.app.data.model.CommunityPost
import com.aniglow.app.data.model.CommunityReply
import com.aniglow.app.data.model.CommunityInfo
import com.aniglow.app.data.model.CreatePostRequest
import com.aniglow.app.data.model.CreateReplyRequest
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.remote.CommunityApi
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

class CommunityRepository(
    private val api: CommunityApi,
) {
    suspend fun loadAllPosts(): Result<CommunityPostPage> = runCatching {
        val response = api.getPosts(slug = "all")
        response.data ?: error(response.message?.takeIf(String::isNotBlank) ?: "后端没有返回帖子数据")
    }

    suspend fun loadPost(postId: Long): Result<Pair<CommunityPost, List<CommunityReply>>> = runCatching {
        val post = api.getPost(postId).data ?: error("帖子详情暂时不可用")
        val replies = api.getReplies(postId).data.orEmpty()
        post to replies
    }

    suspend fun loadCommunities(): Result<List<CommunityInfo>> = runCatching {
        api.getCommunities().data.orEmpty().filterNot { it.slug == "all" }
    }

    suspend fun createPost(
        slug: String,
        title: String,
        content: String,
        images: List<String>,
        user: AuthUser,
    ): Result<CommunityPost> = runCatching {
        api.createPost(
            slug,
            CreatePostRequest(
                title = title.trim(),
                content = content.trim(),
                images = images,
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
            ),
        ).data ?: error("发布失败")
    }

    suspend fun uploadImages(contentResolver: ContentResolver, imageUris: List<Uri>): Result<List<String>> = runCatching {
        require(imageUris.size <= MAX_IMAGES) { "最多选择 $MAX_IMAGES 张图片" }
        if (imageUris.isEmpty()) return@runCatching emptyList()

        val parts = imageUris.mapIndexed { index, uri ->
            val contentType = contentResolver.getType(uri)?.lowercase()
                ?: error("无法识别第 ${index + 1} 张图片的格式")
            require(contentType in SUPPORTED_IMAGE_TYPES) { "仅支持 JPG、PNG、GIF 或 WebP 图片" }

            val contentLength = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            require(contentLength < 0 || contentLength <= MAX_IMAGE_BYTES) { "第 ${index + 1} 张图片超过 10MB" }

            val fileName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            } ?: "image_${index + 1}.${contentType.substringAfter('/')}"

            MultipartBody.Part.createFormData(
                "files",
                fileName,
                ContentUriRequestBody(contentResolver, uri, contentType.toMediaType(), contentLength),
            )
        }
        val response = api.uploadImages(parts)
        response.data ?: error(response.message?.takeIf(String::isNotBlank) ?: "图片上传失败")
    }

    suspend fun likePost(postId: Long): Result<CommunityPost> = runCatching {
        api.likePost(postId).data ?: error("操作失败")
    }

    suspend fun createReply(postId: Long, content: String, user: AuthUser): Result<CommunityReply> = runCatching {
        api.createReply(
            postId,
            CreateReplyRequest(content.trim(), user.displayName, user.avatarUrl),
        ).data ?: error("回复失败")
    }

    suspend fun loadMyPosts(): Result<List<CommunityPost>> = runCatching {
        api.getMyPosts().data?.items ?: error("我的帖子暂时不可用")
    }

    suspend fun deletePost(postId: Long): Result<Unit> = runCatching {
        val response = api.deletePost(postId)
        check(response.success) { response.message?.takeIf(String::isNotBlank) ?: "删除失败" }
    }

    private companion object {
        const val MAX_IMAGES = 9
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
        val SUPPORTED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
    }
}

private class ContentUriRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType,
    private val length: Long,
) : RequestBody() {
    override fun contentType(): MediaType = mediaType

    override fun contentLength(): Long = length

    override fun writeTo(sink: BufferedSink) {
        val input = contentResolver.openInputStream(uri) ?: error("无法读取所选图片")
        input.use { sink.writeAll(it.source()) }
    }
}
