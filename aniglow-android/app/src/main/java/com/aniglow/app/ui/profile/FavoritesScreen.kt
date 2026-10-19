package com.aniglow.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aniglow.app.data.favorite.FavoriteStore
import com.aniglow.app.ui.common.AnimePosterCard
import com.aniglow.app.ui.theme.WarmBackground
import com.aniglow.app.ui.common.GlassBackTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onAnimeClick: (Long) -> Unit,
) {
    val favorites by FavoriteStore.items.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            GlassBackTopBar("收藏的番剧", onBack, "MY FAVORITES")
        },
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding).background(
                    Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF2FBF7))),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有收藏番剧\n可以在番剧详情页点击“收藏”")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding).background(
                    Brush.verticalGradient(listOf(Color(0xFFFFFAF8), WarmBackground, Color(0xFFF2FBF7))),
                ),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(favorites, key = { it.id }) { anime ->
                    AnimePosterCard(anime, onClick = { onAnimeClick(anime.id) })
                }
            }
        }
    }
}
