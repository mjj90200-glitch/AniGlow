package com.aniglow.app.data.remote

import com.aniglow.app.data.model.ApiEnvelope
import com.aniglow.app.data.model.AuthResponse
import com.aniglow.app.data.model.LoginRequest
import com.aniglow.app.data.model.LogoutRequest
import com.aniglow.app.data.model.RegisterRequest
import com.aniglow.app.data.model.ProfileUpdateRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiEnvelope<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope<AuthResponse>

    @GET("auth/me")
    suspend fun me(): ApiEnvelope<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): ApiEnvelope<Any>

    @PUT("auth/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): ApiEnvelope<AuthResponse>
}
