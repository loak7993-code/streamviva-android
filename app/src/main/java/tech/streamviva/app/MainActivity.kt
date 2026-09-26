@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package tech.streamviva.app


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ================================ app ================================ */

sealed class Screen {
    data object Home : Screen()
    data object Search : Screen()
    data class Details(val media: Tmdb.Media) : Screen()
    data class Player(val title: String, val streamUrl: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SilkTheme { App() }
        }
    }
}

@Composable
fun App() {
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val current = stack.last()
    fun push(s: Screen) { stack = stack + s }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }

    val showChrome = current !is Screen.Player

    Box(Modifier.fillMaxSize().background(Bg)) {
        // screens
        androidx.compose.animation.Crossfade(targetState = current, label = "screen") { screen ->
            when (screen) {
                is Screen.Home -> HomeScreen(onOpen = { push(Screen.Details(it)) }, onSearch = { push(Screen.Search) })
                is Screen.Search -> SearchScreen(onOpen = { push(Screen.Details(it)) })
                is Screen.Details -> DetailsScreen(media = screen.media, onPlay = { t, u -> push(Screen.Player(t, u)) })
                is Screen.Player -> PlayerScreen(title = screen.title, streamUrl = screen.streamUrl)
            }
        }

        // bottom bar (hidden in player)
        if (showChrome) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                BottomBar(
                    current = if (current is Screen.Search) 1 else 0,
                    onHome = { stack = listOf(Screen.Home) },
                    onSearch = { if (current !is Screen.Search) push(Screen.Search) },
                )
            }
        }
    }
    BackHandler(enabled = stack.size > 1) { pop() }
}

@Composable
fun BottomBar(current: Int, onHome: () -> Unit, onSearch: () -> Unit) {
    Surface(color = Color(0xF20D0D10), shadowElevation = 12.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 34.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomItem(icon = Icons.Rounded.Home, label = "Home", active = current == 0, onClick = onHome)
            BottomItem(icon = Icons.Rounded.Search, label = "Search", active = current == 1, onClick = onSearch)
        }
    }
}

@Composable
fun BottomItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = if (active) IrisSoft else Text3, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (active) IrisSoft else Text3,
            fontSize = 10.sp,
            fontFamily = Sans,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/* ================================ home ================================ */

@Composable
fun HomeScreen(onOpen: (Tmdb.Media) -> Unit, onSearch: () -> Unit) {
    var trending by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var topMovies by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var topTv by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var upcoming by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }

    LaunchedEffect(Unit) {
        try { trending = Tmdb.trendingMovies() } catch (_: Exception) {}
        try { topMovies = Tmdb.popularMovies() } catch (_: Exception) {}
        try { topTv = Tmdb.topRatedTv() } catch (_: Exception) {}
        try { upcoming = Tmdb.trendingTv() } catch (_: Exception) {}
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        // header: wordmark + search
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(size = 27)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .clickable { onSearch() }
                        .padding(9.dp),
                ) { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Text2, modifier = Modifier.size(18.dp)) }
            }
        }

        // hero pager
        if (trending.isNotEmpty()) {
            item { HeroPager(trending.take(5), onOpen) }
        } else {
            item { Shimmer(Modifier.fillMaxWidth().height(420.dp), radius = 0) }
        }

        if (trending.isEmpty()) {
            item { HomeSkeleton() }
        } else {
            item { Spacer(Modifier.height(26.dp)) }
            item {
                SectionHeader("Trending now")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) { items(trending) { m -> PosterCard(m, onOpen, wide = true) } }
            }
            item { Spacer(Modifier.height(28.dp)) }
            item {
                SectionHeader("Popular movies")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) { items(topMovies) { m -> PosterCard(m, onOpen) } }
            }
            item { Spacer(Modifier.height(28.dp)) }
            item {
                SectionHeader("Top rated shows")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) { items(topTv) { m -> PosterCard(m, onOpen) } }
            }
            item { Spacer(Modifier.height(28.dp)) }
            item {
                SectionHeader("Trending shows")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) { items(upcoming) { m -> PosterCard(m, onOpen) } }
            }
        }
    }
}

@Composable
fun HeroPager(items: List<Tmdb.Media>, onOpen: (Tmdb.Media) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    // auto-advance
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5200)
            if (!pagerState.isScrollInProgress) {
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size, animationSpec = tween(700))
                }
            }
        }
    }

    Box {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(430.dp),
        ) { page ->
            val m = items[page]
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { onOpen(m) },
            ) {
                AsyncImage(
                    model = m.backdrop?.let { IMG_W780 + it } ?: m.poster?.let { IMG + it },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(bottomFade()))
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    Kicker("now showing")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        m.title,
                        color = White,
                        fontSize = 36.sp,
                        fontFamily = Serif,
                        lineHeight = 40.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${m.year} · ${if (m.type == "tv") "show" else "film"}",
                            color = Text2, fontSize = 12.sp, fontFamily = Sans,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("★ ${"%.1f".format(m.rating)}", color = Gold, fontSize = 12.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                    }
                    if (m.overview.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            m.overview,
                            color = Text1,
                            fontSize = 12.5.sp,
                            fontFamily = Sans,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // dots
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(items.size) { i ->
                Box(
                    Modifier
                        .size(if (pagerState.currentPage == i) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == i) IrisSoft else Color(0x59FFFFFF)),
                )
            }
        }
    }
}

/* =============================== search =============================== */

@Composable
fun SearchScreen(onOpen: (Tmdb.Media) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.length >= 2) {
            searching = true
            try { results = Tmdb.search(query) } catch (_: Exception) { results = emptyList() }
            searching = false
        } else results = emptyList()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
            ) {
                if (query.isEmpty()) {
                    Text("Search movies, shows…", color = Text3, fontSize = 14.5.sp, fontFamily = Sans)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(color = White, fontSize = 14.5.sp, fontFamily = Sans),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            Spacer(Modifier.width(12.dp))
            Wordmark(size = 21)
        }

        if (searching) {
            Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Iris, strokeWidth = 2.5.dp, modifier = Modifier.size(30.dp))
            }
        } else if (query.length >= 2 && results.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                Text("nothing found", color = Text3, fontSize = 13.sp, fontFamily = Sans)
            }
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            items(results) { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(m) }
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = m.poster?.let { IMG + it },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(60.dp).height(90.dp).clip(RoundedCornerShape(11.dp)),
                    )
                    Spacer(Modifier.width(15.dp))
                    Column {
                        Text(
                            m.title,
                            color = Text1, fontSize = 15.sp, fontFamily = Sans,
                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${m.year} · ${if (m.type == "tv") "show" else "film"} · ★ ${"%.1f".format(m.rating)}",
                            color = Text3, fontSize = 11.5.sp, fontFamily = Sans,
                        )
                        if (m.overview.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                m.overview, color = Text2, fontSize = 11.sp, fontFamily = Sans,
                                maxLines = 2, lineHeight = 15.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
