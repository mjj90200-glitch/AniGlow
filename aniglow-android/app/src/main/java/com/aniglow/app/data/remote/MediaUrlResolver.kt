package com.aniglow.app.data.remote

import com.aniglow.app.BuildConfig
import com.aniglow.app.data.model.AgentCharacter

/**
 * 浏览器能自动解析 `/api/images/...`，原生图片加载器需要完整 URL。
 * 这里从当前环境的 API 地址推导服务端根地址，避免把开发机地址写死在界面层。
 */
fun resolveMediaUrl(value: String?): String? {
    val url = value?.trim().orEmpty()
    if (url.isEmpty()) return null
    if (url.startsWith("http://") || url.startsWith("https://")) return url

    val serverRoot = BuildConfig.API_BASE_URL.substringBefore("/api/").trimEnd('/')
    return "$serverRoot/${url.trimStart('/')}"
}

fun resolveCharacterImageUrl(character: AgentCharacter): String {
    val configured = character.avatarUrl?.trim().orEmpty()
    if (configured.isNotEmpty()) return resolveMediaUrl(configured).orEmpty()
    return "${BuildConfig.WEB_BASE_URL.trimEnd('/')}/images/${character.code}.png"
}
