package com.aniglow.app.data.remote

import com.aniglow.app.data.model.ApiEnvelope
import com.aniglow.app.data.model.CommunityPostPage
import com.aniglow.app.data.model.CommunityPost
import com.aniglow.app.data.model.CommunityReply
import com.aniglow.app.data.model.CommunityInfo
import com.aniglow.app.data.model.CreatePostRequest
import com.aniglow.app.data.model.CreateReplyRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody

interface CommunityApi {
    @Multipart
    @POST("communities/upload/images")
    suspend fun uploadImages(@Part files: List<MultipartBody.Part>): ApiEnvelope<List<String>>

    @GET("communities")
    suspend fun getCommunities(): ApiEnvelope<List<CommunityInfo>>

    @GET("communities/{slug}/posts")
    suspend fun getPosts(
        @Path("slug") slug: String,
        @Query("sort") sort: String = "latest",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<CommunityPostPage>

    @GET("communities/posts/{postId}")
    suspend fun getPost(@Path("postId") postId: Long): ApiEnvelope<CommunityPost>

    @GET("communities/posts/{postId}/replies")
    suspend fun getReplies(
        @Path("postId") postId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): ApiEnvelope<List<CommunityReply>>

    @POST("communities/{slug}/posts")
    suspend fun createPost(
        @Path("slug") slug: String,
        @Body request: CreatePostRequest,
    ): ApiEnvelope<CommunityPost>

    @POST("communities/posts/{postId}/like")
    suspend fun likePost(@Path("postId") postId: Long): ApiEnvelope<CommunityPost>

    @POST("communities/posts/{postId}/replies")
    suspend fun createReply(
        @Path("postId") postId: Long,
        @Body request: CreateReplyRequest,
    ): ApiEnvelope<CommunityReply>

    @GET("communities/posts/my")
    suspend fun getMyPosts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): ApiEnvelope<CommunityPostPage>

    @DELETE("communities/posts/{postId}")
    suspend fun deletePost(@Path("postId") postId: Long): ApiEnvelope<String>
}
