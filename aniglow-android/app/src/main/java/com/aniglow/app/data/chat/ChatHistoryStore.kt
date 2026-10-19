package com.aniglow.app.data.chat

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class StoredChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val content: String,
)

object ChatHistoryStore {
    private const val PREFS_NAME = "aniglow_chat_history"
    private const val MAX_MESSAGES_PER_ROLE = 100
    private val gson = Gson()
    private lateinit var context: Context

    fun initialize(applicationContext: Context) {
        if (!::context.isInitialized) context = applicationContext
    }

    @Synchronized
    fun load(userId: Long, role: String): List<StoredChatMessage> {
        val json = preferences().getString(historyKey(userId, role), null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<StoredChatMessage>>(
                json,
                object : TypeToken<List<StoredChatMessage>>() {}.type,
            )
        }.getOrNull().orEmpty()
    }

    @Synchronized
    fun save(userId: Long, role: String, messages: List<StoredChatMessage>) {
        preferences().edit()
            .putString(historyKey(userId, role), gson.toJson(messages.takeLast(MAX_MESSAGES_PER_ROLE)))
            .apply()
    }

    @Synchronized
    fun clear(userId: Long, role: String) {
        preferences().edit().remove(historyKey(userId, role)).apply()
    }

    private fun historyKey(userId: Long, role: String) = "user_${userId}_role_${role.lowercase()}"

    private fun preferences() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
