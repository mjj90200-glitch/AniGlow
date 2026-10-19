package com.aniglow.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aniglow.app.data.model.AnimeSummary
import com.aniglow.app.data.remote.resolveMediaUrl
import com.aniglow.app.ui.theme.FireflyGreen

@Composable
fun AnimePosterCard(
    anime: AnimeSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(Color(0xFFEDEBE5)),
            ) {
                AsyncImage(
                    model = resolveMediaUrl(anime.coverImage),
                    contentDescription = anime.displayTitle,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.72f),
                    contentScale = ContentScale.Crop,
                )
                anime.displayRating?.let { rating ->
                    Text(
                        text = "★ %.1f".format(rating),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.68f))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                    )
                }
            }
            Column(Modifier.padding(11.dp)) {
                Text(
                    text = anime.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = listOfNotNull(anime.type, anime.year?.toString()).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (anime.hasAgent) {
                        Text("AI", color = FireflyGreen, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeAnimeCard(anime: AnimeSummary, onClick: () -> Unit) {
    AnimePosterCard(anime = anime, modifier = Modifier.width(158.dp), onClick = onClick)
}
