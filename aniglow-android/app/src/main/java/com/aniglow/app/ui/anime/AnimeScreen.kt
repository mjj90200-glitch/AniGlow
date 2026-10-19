package com.aniglow.app.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.aniglow.app.ui.common.AnimePosterCard
import com.aniglow.app.ui.common.ErrorContent
import com.aniglow.app.ui.common.LoadingContent
import com.aniglow.app.ui.theme.FireflyGreen
import com.aniglow.app.ui.theme.SkyBlue
import com.aniglow.app.ui.theme.WarmBackground

@Composable
fun AnimeRoute(
    onAnimeClick: (Long) -> Unit,
    onRankingClick: () -> Unit,
    focusSearchRequest: Int = 0,
    viewModel: AnimeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AnimeScreen(
        uiState = uiState,
        onQueryChange = viewModel::updateQuery,
        onSearch = viewModel::search,
        onClear = viewModel::clearSearch,
        onRetry = viewModel::retry,
        onAnimeClick = onAnimeClick,
        onRankingClick = onRankingClick,
        focusSearchRequest = focusSearchRequest,
        onSelectGenre = viewModel::selectGenre,
        onSelectType = viewModel::selectType,
        onSelectCountry = viewModel::selectCountry,
        onSelectYear = viewModel::selectYear,
        onResetFilters = viewModel::resetFilters,
        onPreviousPage = viewModel::previousPage,
        onNextPage = viewModel::nextPage,
    )
}

@Composable
private fun AnimeScreen(
    uiState: AnimeUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    onRankingClick: () -> Unit,
    focusSearchRequest: Int,
    onSelectGenre: (String) -> Unit,
    onSelectType: (String) -> Unit,
    onSelectCountry: (String) -> Unit,
    onSelectYear: (Int?) -> Unit,
    onResetFilters: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val searchFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val gridVisible = !(uiState.loading && uiState.anime.isEmpty()) && !(uiState.errorMessage != null && uiState.anime.isEmpty())
    LaunchedEffect(focusSearchRequest, gridVisible) {
        if (focusSearchRequest > 0 && gridVisible) {
            delay(120)
            searchFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(uiState.currentPage) {
        if (uiState.currentPage > 0) gridState.scrollToItem(0)
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFFBF8), WarmBackground, Color(0xFFF5FCF8))),
        ),
    ) {
        when {
            uiState.loading && uiState.anime.isEmpty() -> LoadingContent("正在加载番剧库")
            uiState.errorMessage != null && uiState.anime.isEmpty() -> ErrorContent(uiState.errorMessage, onRetry)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text("BROWSE ALL", color = Color(0xFF00A846), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("全部番剧", color = Color(0xFF202A38), fontSize = 31.sp, fontWeight = FontWeight.Black)
                                Text(
                                    "${if (uiState.totalElements > 0) uiState.totalElements else "6000+"} 部作品持续更新，挑一部今天想看的番。",
                                    color = Color(0xFF747D8B),
                                    fontSize = 13.sp,
                                    lineHeight = 21.sp,
                                )
                            }
                            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.78f), shadowElevation = 3.dp) {
                                TextButton(onClick = onRankingClick, modifier = Modifier.padding(horizontal = 3.dp)) {
                                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(19.dp))
                                    Text(" 排行", color = Color(0xFF59616E), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                            singleLine = true,
                            placeholder = { Text("搜索你喜欢的番剧...", color = Color(0xFF9AA3AE)) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF8E98A5)) },
                            trailingIcon = {
                                if (uiState.query.isNotBlank()) TextButton(onClick = onClear) { Text("清除", color = FireflyGreen) }
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.88f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.68f),
                                focusedBorderColor = FireflyGreen.copy(alpha = 0.45f),
                                unfocusedBorderColor = Color.White,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        )
                        Button(
                            onClick = onSearch,
                            enabled = !uiState.loading,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = FireflyGreen),
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  搜索番剧", fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(16.dp))
                        AnimeFilterPanel(
                            uiState = uiState,
                            onSelectGenre = onSelectGenre,
                            onSelectType = onSelectType,
                            onSelectCountry = onSelectCountry,
                            onSelectYear = onSelectYear,
                            onReset = onResetFilters,
                        )
                        Spacer(Modifier.height(16.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.68f)),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(40.dp).background(FireflyGreen.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = FireflyGreen)
                                }
                                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                                    Text(if (uiState.query.isBlank()) "番剧花园" else "搜索结果", fontWeight = FontWeight.Black, color = Color(0xFF303945))
                                    Text(
                                        if (uiState.query.isBlank()) "按热度与评分为你展示" else "找到 ${uiState.totalElements} 部相关作品",
                                        color = Color(0xFF8D96A2),
                                        fontSize = 12.sp,
                                    )
                                }
                                Text("${uiState.totalElements}", color = SkyBlue, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(uiState.anime, key = { it.id }) { anime ->
                    AnimePosterCard(anime, onClick = { onAnimeClick(anime.id) })
                }
                if (uiState.totalPages > 1) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PaginationBar(
                            currentPage = uiState.currentPage,
                            totalPages = uiState.totalPages,
                            loading = uiState.loading,
                            onPrevious = onPreviousPage,
                            onNext = onNextPage,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeFilterPanel(
    uiState: AnimeUiState,
    onSelectGenre: (String) -> Unit,
    onSelectType: (String) -> Unit,
    onSelectCountry: (String) -> Unit,
    onSelectYear: (Int?) -> Unit,
    onReset: () -> Unit,
) {
    val defaultGenres = listOf(
        "Action", "Adventure", "Comedy", "Romance", "Fantasy", "Sci-Fi", "Slice of Life", "School",
        "Iyashikei", "Isekai", "Music", "Sports", "Mystery", "Psychological", "Supernatural", "Horror",
        "Thriller", "Drama", "Mecha", "Mahou Shoujo",
    )
    val defaultTypes = listOf("TV", "Movie", "ONA", "OVA", "Special")
    val genres = defaultGenres
    val types = uiState.filterOptions.types.ifEmpty { defaultTypes }
    val countries = uiState.filterOptions.countries
    val years = uiState.filterOptions.years.distinct().sortedDescending()
    val hasFilters = uiState.genre.isNotBlank() || uiState.type.isNotBlank() || uiState.country.isNotBlank() || uiState.yearFrom != null || uiState.query.isNotBlank()
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.62f)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FILTER GARDEN", color = FireflyGreen, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    Text("筛选", color = Color(0xFF28313D), fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                if (hasFilters) {
                    TextButton(onClick = onReset) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" 重置")
                    }
                }
            }
            FilterRow("题材", listOf("") + genres, uiState.genre, onSelectGenre, ::genreLabel)
            FilterRow("格式", listOf("") + types, uiState.type, onSelectType, ::typeLabel)
            if (countries.isNotEmpty()) FilterRow("地区", listOf("") + countries, uiState.country, onSelectCountry) { it.ifBlank { "全部" } }
            if (years.isNotEmpty()) YearFilterRow(years, uiState.yearFrom, onSelectYear)
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    values: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    display: (String) -> String,
) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFF8A95A1), fontSize = 11.sp, fontWeight = FontWeight.Black)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(values.distinct(), key = { "${label}_$it" }) { value ->
                FilterChip(display(value), selected == value) { onSelect(value) }
            }
        }
    }
}

@Composable
private fun YearFilterRow(years: List<Int>, selected: Int?, onSelect: (Int?) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text("年份", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFF8A95A1), fontSize = 11.sp, fontWeight = FontWeight.Black)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip("全部", selected == null) { onSelect(null) } }
            items(years, key = { "year_$it" }) { year -> FilterChip(year.toString(), selected == year) { onSelect(year) } }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) FireflyGreen else Color.White.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (selected) FireflyGreen else Color(0xFFE8ECE8)),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = if (selected) Color.White else Color(0xFF596473), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    loading: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onPrevious, enabled = currentPage > 0 && !loading, shape = CircleShape) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(17.dp))
            Text(" 上一页")
        }
        Text("${currentPage + 1} / $totalPages", modifier = Modifier.padding(horizontal = 18.dp), color = Color(0xFF596473), fontWeight = FontWeight.Black)
        OutlinedButton(onClick = onNext, enabled = currentPage + 1 < totalPages && !loading, shape = CircleShape) {
            Text("下一页 ")
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
        }
    }
}

private fun genreLabel(value: String): String = when (value) {
    "" -> "全部"
    "Action" -> "热血"
    "Adventure" -> "冒险"
    "Comedy" -> "搞笑"
    "Romance" -> "恋爱"
    "Fantasy" -> "奇幻"
    "Sci-Fi" -> "科幻"
    "Slice of Life" -> "日常"
    "School" -> "校园"
    "Iyashikei" -> "治愈"
    "Isekai" -> "异世界"
    "Music" -> "音乐"
    "Sports" -> "运动"
    "Mystery" -> "悬疑"
    "Psychological" -> "心理"
    "Supernatural" -> "超自然"
    "Horror" -> "恐怖"
    "Thriller" -> "惊悚"
    "Drama" -> "剧情"
    "Mecha" -> "机甲"
    "Mahou Shoujo" -> "魔法少女"
    else -> value
}

private fun typeLabel(value: String): String = when (value) {
    "" -> "全部"
    "TV" -> "TV动画"
    "Movie" -> "剧场版"
    "Special" -> "特别篇"
    "Music" -> "音乐"
    else -> value
}
