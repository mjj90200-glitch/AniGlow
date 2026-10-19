package com.aniglow.app.data.repository

import com.aniglow.app.data.auth.AuthSession
import com.aniglow.app.data.model.AuthUser
import com.aniglow.app.data.model.LoginRequest
import com.aniglow.app.data.model.LogoutRequest
import com.aniglow.app.data.model.RegisterRequest
import com.aniglow.app.data.model.ProfileUpdateRequest
import com.aniglow.app.data.remote.AuthApi

class AuthRepository(private val api: AuthApi) {
    suspend fun login(username: String, password: String): Result<AuthUser> = runCatching {
        val response = api.login(LoginRequest(username.trim(), password)).data ?: error("登录失败")
        require(!response.token.isNullOrBlank()) { "登录服务没有返回有效凭证" }
        AuthSession.save(response)
        response.toUser()
    }

    suspend fun register(username: String, displayName: String, password: String): Result<AuthUser> = runCatching {
        val response = api.register(
            RegisterRequest(username.trim(), displayName.trim().ifBlank { null }, password),
        ).data ?: error("注册失败")
        require(!response.token.isNullOrBlank()) { "注册服务没有返回有效凭证" }
        AuthSession.save(response)
        response.toUser()
    }

    suspend fun restore(): Result<AuthUser> = runCatching {
        val response = api.me().data ?: error("登录状态已失效")
        AuthSession.save(response)
        response.toUser()
    }

    suspend fun logout() {
        runCatching { api.logout(LogoutRequest(AuthSession.accessToken, AuthSession.refreshToken)) }
        AuthSession.clear()
    }

    suspend fun updateProfile(displayName: String, avatarUrl: String): Result<AuthUser> = runCatching {
        val response = api.updateProfile(ProfileUpdateRequest(displayName.trim(), avatarUrl.trim())).data
            ?: error("资料保存失败")
        AuthSession.save(response)
        response.toUser()
    }
}
