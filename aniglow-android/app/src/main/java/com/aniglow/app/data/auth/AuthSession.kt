package com.aniglow.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.aniglow.app.data.model.AuthResponse
import com.aniglow.app.data.model.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthSession {
    private const val PREFS_NAME = "aniglow_session"
    private lateinit var preferences: SharedPreferences
    private val mutableUser = MutableStateFlow<AuthUser?>(null)
    val user: StateFlow<AuthUser?> = mutableUser.asStateFlow()

    val accessToken: String?
        get() = if (::preferences.isInitialized) preferences.getString("access_token", null) else null

    val refreshToken: String?
        get() = if (::preferences.isInitialized) preferences.getString("refresh_token", null) else null

    fun initialize(context: Context) {
        if (::preferences.isInitialized) return
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = preferences.getLong("user_id", -1L)
        val username = preferences.getString("username", null)
        if (id > 0 && !username.isNullOrBlank() && !accessToken.isNullOrBlank()) {
            mutableUser.value = AuthUser(
                id = id,
                username = username,
                displayName = preferences.getString("display_name", null),
                avatarUrl = preferences.getString("avatar_url", null),
                phone = preferences.getString("phone", null),
                email = preferences.getString("email", null),
            )
        }
    }

    fun save(response: AuthResponse) {
        val savedAccessToken = response.token ?: accessToken
        val savedRefreshToken = response.refreshToken ?: refreshToken
        preferences.edit()
            .putString("access_token", savedAccessToken)
            .putString("refresh_token", savedRefreshToken)
            .putLong("user_id", response.id)
            .putString("username", response.username)
            .putString("display_name", response.displayName)
            .putString("avatar_url", response.avatarUrl)
            .putString("phone", response.phone)
            .putString("email", response.email)
            .apply()
        mutableUser.value = response.toUser()
    }

    fun clear() {
        if (::preferences.isInitialized) preferences.edit().clear().apply()
        mutableUser.value = null
    }
}
