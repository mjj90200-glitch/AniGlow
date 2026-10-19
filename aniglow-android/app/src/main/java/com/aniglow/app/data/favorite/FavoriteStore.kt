package com.aniglow.app.data.favorite

import android.content.Context
import com.aniglow.app.data.model.AnimeSummary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoriteStore {
    private const val PREFS_NAME = "aniglow_favorites"
    private const val ITEMS_KEY = "anime_items"
    private val gson = Gson()
    private lateinit var context: Context
    private val mutableItems = MutableStateFlow<List<AnimeSummary>>(emptyList())
    val items: StateFlow<List<AnimeSummary>> = mutableItems.asStateFlow()

    fun initialize(applicationContext: Context) {
        if (::context.isInitialized) return
        context = applicationContext
        val json = preferences().getString(ITEMS_KEY, null)
        mutableItems.value = runCatching {
            gson.fromJson<List<AnimeSummary>>(json, object : TypeToken<List<AnimeSummary>>() {}.type)
        }.getOrNull().orEmpty()
    }

    fun isFavorite(animeId: Long): Boolean = mutableItems.value.any { it.id == animeId }

    fun toggle(anime: AnimeSummary): Boolean {
        val alreadyFavorite = isFavorite(anime.id)
        mutableItems.value = if (alreadyFavorite) {
            mutableItems.value.filterNot { it.id == anime.id }
        } else {
            listOf(anime) + mutableItems.value
        }
        preferences().edit().putString(ITEMS_KEY, gson.toJson(mutableItems.value)).apply()
        return !alreadyFavorite
    }

    private fun preferences() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
