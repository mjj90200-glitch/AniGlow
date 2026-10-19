package com.aniglow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aniglow.app.ui.app.AniGlowApp
import com.aniglow.app.data.auth.AuthSession
import com.aniglow.app.data.chat.ChatHistoryStore
import com.aniglow.app.data.daily.DailyStore
import com.aniglow.app.data.favorite.FavoriteStore
import com.aniglow.app.ui.theme.AniGlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthSession.initialize(applicationContext)
        ChatHistoryStore.initialize(applicationContext)
        DailyStore.initialize(applicationContext)
        FavoriteStore.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            AniGlowTheme {
                AniGlowApp()
            }
        }
    }
}
