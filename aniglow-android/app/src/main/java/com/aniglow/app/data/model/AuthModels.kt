package com.aniglow.app.data.model

data class LoginRequest(val username: String, val password: String)

data class RegisterRequest(
    val username: String,
    val displayName: String? = null,
    val password: String,
)

data class ProfileUpdateRequest(
    val displayName: String,
    val avatarUrl: String,
)

data class LogoutRequest(
    val accessToken: String?,
    val refreshToken: String?,
)

data class AuthUser(
    val id: Long,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val roles: List<String> = emptyList(),
) {
    val visibleName: String
        get() = displayName?.takeIf { it.isNotBlank() && !it.contains('\uFFFD') } ?: username
}

data class AuthResponse(
    val token: String? = null,
    val refreshToken: String? = null,
    val type: String = "Bearer",
    val id: Long,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val roles: List<String> = emptyList(),
) {
    fun toUser() = AuthUser(id, username, displayName, avatarUrl, phone, email, roles)
}
