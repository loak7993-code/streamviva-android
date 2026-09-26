@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package tech.streamviva.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    data class Player(
        val title: String,
        val streamUrl: String,
        val media: Tmdb.Media? = null,
        val season: Int? = null,
        val episode: Int? = null,
    ) : Screen()
}

enum class HomeTab(val label: String) { HOME("Home"), MOVIES("Movies"), SHOWS("Shows"), LIST("My List") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        Store.init(this)
        setContent {
            SilkTheme { App() }
        }
    }
}

@Composable
fun App() {
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    var tab by remember { mutableStateOf(HomeTab.HOME) }
    val current = stack.last()
    val scope = rememberCoroutineScope()

    fun push(s: Screen) { stack = stack + s }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun openById(type: String, id: String) {
        scope.launch {
            try {
                val media = Tmdb.byId(type, id.toLong())
                if (media != null) push(Screen.Details(media))
            } catch (_: Exception) {}
        }
    }

    val onHomeRoot = current is Screen.Home

    Box(Modifier.fillMaxSize().background(Bg)) {
        androidx.compose.animation.Crossfade(targetState = current, label = "screen") { screen ->
            when (screen) {
                is Screen.Home -> NetflixHome(
                    tab = tab,
                    onTab = { tab = it },
                    onOpen = { push(Screen.Details(it)) },
                    onSearch = { push(Screen.Search) },
                    onOpenById = { t, i -> openById(t, i) },
                )
                is Screen.Search -> SearchScreen(onOpen = { push(Screen.Details(it)) })
                is Screen.Details -> DetailsScreen(
                    media = screen.media,
                    onPlay = { t, u, m, s, e -> push(Screen.Player(t, u, m, s, e)) },
                    onOpen = { push(Screen.Details(it)) },
                )
                is Screen.Player -> PlayerScreen(
                    title = screen.title,
                    streamUrl = screen.streamUrl,
                    media = screen.media,
                    season = screen.season,
                    episode = screen.episode,
                )
            }
        }
    }
    BackHandler(enabled = stack.size > 1) { pop() }
    // tab back handling resets to home tab
    BackHandler(enabled = stack.size == 1 && tab != HomeTab.HOME) { tab = HomeTab.HOME }
}

/* ============================ netflix home ============================ */

@Composable
fun NetflixHome(
    tab: HomeTab,
    onTab: (HomeTab) -> Unit,
    onOpen: (Tmdb.Media) -> Unit,
    onSearch: () -> Unit,
    onOpenById: (String, String) -> Unit,
) {
    var trending by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var topMovies by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var topTv by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var airing by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var upcoming by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try { trending = Tmdb.trendingMovies() } catch (_: Exception) {}
        try { topMovies = Tmdb.popularMovies() } catch (_: Exception) {}
        try { topTv = Tmdb.topRatedTv() } catch (_: Exception) {}
        try { airing = Tmdb.airingToday() } catch (_: Exception) {}
        try { upcoming = Tmdb.upcomingMovies() } catch (_: Exception) {}
        loaded = true
    }

    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 60.dp),
        ) {
            // billboard
            if (trending.isNotEmpty()) {
                item { Billboard(trending.take(5), onOpen) }
            } else {
                item { Shimmer(Modifier.fillMaxWidth().height(430.dp), radius = 0) }
            }

            when (tab) {
                HomeTab.HOME -> {
                    val cont = Store.progress.values.sortedByDescending { p -> p.updatedAt }.take(10)
                    if (cont.isNotEmpty()) {
                        item { Row("Continue watching", cont.mapNotNull { it.toMedia() }, onOpen, onOpenById, isContinue = true, contEntries = cont) }
                    }
                    if (topMovies.isNotEmpty()) {
                        item { Row("Top 10 movies today", topMovies.take(10), onOpen, onOpenById, ranked = true) }
                    }
                    if (topTv.isNotEmpty()) item { Row("Popular shows", topTv, onOpen, onOpenById) }
                    if (topMovies.isNotEmpty()) item { Row("Popular movies", topMovies, onOpen, onOpenById) }
                    if (airing.isNotEmpty()) item { Row("Airing today", airing, onOpen, onOpenById) }
                    if (upcoming.isNotEmpty()) item { Row("Coming soon", upcoming, onOpen, onOpenById) }
                    if (!loaded) item { HomeSkeleton() }
                }
                HomeTab.MOVIES -> {
                    item { Row("Top 10 movies", topMovies.take(10), onOpen, onOpenById, ranked = true) }
                    item { Row("Trending", trending.filter { it.type == "movie" }, onOpen, onOpenById) }
                    item { Row("Popular", topMovies, onOpen, onOpenById) }
                    item { Row("Coming soon", upcoming, onOpen, onOpenById) }
                }
                HomeTab.SHOWS -> {
                    item { Row("Top 10 shows", topTv.take(10), onOpen, onOpenById, ranked = true) }
                    item { Row("Popular", topTv, onOpen, onOpenById) }
                    item { Row("Airing today", airing, onOpen, onOpenById) }
                }
                HomeTab.LIST -> {
                    item {
                        MyListSection(onOpen, onOpenById)
                    }
                }
            }
        }

        // top bar: wordmark + tabs + search (netflix style, solidifies on scroll)
        TopNav(
            scrolled = scrolled,
            tab = tab,
            onTab = onTab,
            onSearch = onSearch,
        )
    }
}

private fun Store.ProgressEntry.toMedia(): Tmdb.Media? = null // handled via onOpenById

/* ------------------------------ top nav ------------------------------ */

@Composable
fun TopNav(scrolled: Boolean, tab: HomeTab, onTab: (HomeTab) -> Unit, onSearch: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (scrolled) Bg else Color(0x66000000)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wordmark(size = 21)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.Search,
                contentDescription = "search",
                tint = White,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSearch() }
                    .padding(8.dp)
                    .size(19.dp),
            )
        }
        // tabs
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HomeTab.entries.forEach { t ->
                Text(
                    t.label,
                    color = if (tab == t) White else Text2,
                    fontSize = 13.sp,
                    fontFamily = Sans,
                    fontWeight = if (tab == t) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTab(t) }
                        .padding(vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

/* ----------------------------- billboard ----------------------------- */

@Composable
fun Billboard(items: List<Tmdb.Media>, onOpen: (Tmdb.Media) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        while (true) {
            delay(6000)
            if (!pagerState.isScrollInProgress) {
                scope.launch {
                    pagerState.animateScrollToPage(
                        (pagerState.currentPage + 1) % items.size,
                        animationSpec = tween(800),
                    )
                }
            }
        }
    }

    Box(Modifier.fillMaxWidth().height(440.dp)) {
        HorizontalPager(state = pagerState) { page ->
            val m = items[page]
            Box(Modifier.fillMaxSize().clickable { onOpen(m) }) {
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
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                ) {
                    Text(
                        m.title,
                        color = White,
                        fontSize = 34.sp,
                        fontFamily = Serif,
                        lineHeight = 38.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${m.year} · ★ ${"%.1f".format(m.rating)}",
                        color = Text2, fontSize = 12.sp, fontFamily = Sans,
                    )
                    Spacer(Modifier.height(12.dp))
                    // netflix action row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // play -> details (which resolves)
                        ActionPill(
                            icon = Icons.Rounded.PlayArrow, label = "Play",
                            filled = true, onClick = { onOpen(m) },
                        )
                        var faved by remember(m.id) { mutableStateOf(Store.isFavorite(m)) }
                        ActionPill(
                            icon = if (faved) Icons.Rounded.Check else Icons.Rounded.Add,
                            label = if (faved) "Added" else "My List",
                            filled = false,
                            onClick = {
                                Store.toggleFavorite(m)
                                faved = Store.isFavorite(m)
                            },
                        )
                        ActionPill(
                            icon = Icons.Rounded.Info, label = "Info",
                            filled = false, onClick = { onOpen(m) },
                        )
                    }
                }
            }
        }
        // dots
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(items.size) { i ->
                Box(
                    Modifier
                        .size(if (pagerState.currentPage == i) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == i) IrisSoft else Color(0x59FFFFFF)),
                )
            }
        }
    }
}

@Composable
fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (filled) White else Color(0xCC2A2A32))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Icon(
            icon, contentDescription = label,
            tint = if (filled) Bg else White,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (filled) Bg else White,
            fontSize = 12.5.sp,
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ------------------------------ content row ------------------------------ */

@Composable
fun Row(
    title: String,
    items: List<Tmdb.Media>,
    onOpen: (Tmdb.Media) -> Unit,
    onOpenById: (String, String) -> Unit,
    ranked: Boolean = false,
    isContinue: Boolean = false,
    contEntries: List<Store.ProgressEntry> = emptyList(),
) {
    Column(Modifier.padding(vertical = 10.dp)) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isContinue) {
                itemsIndexed(contEntries) { _, e ->
                    ContinueCard(e, onOpenById)
                }
            } else if (ranked) {
                itemsIndexed(items) { i, m ->
                    Top10Card(m, i + 1, onOpen)
                }
            } else {
                items(items) { m -> CompactCard(m, onOpen) }
            }
        }
    }
}

@Composable
fun ContinueCard(e: Store.ProgressEntry, onOpenById: (String, String) -> Unit) {
    Column(
        Modifier
            .width(150.dp)
            .clickable { onOpenById(e.type, e.tmdbId) },
    ) {
        Box {
            AsyncImage(
                model = e.poster?.let { IMG + it },
                contentDescription = e.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Surface),
            )
            // play badge
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xB30D0D10)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, "play", tint = White, modifier = Modifier.size(16.dp))
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0x66000000)),
            ) {
                Box(Modifier.fillMaxWidth(e.pct).height(3.dp).background(Iris))
            }
            if (e.season != null) {
                Text(
                    "S${e.season}·E${e.episode}",
                    color = White, fontSize = 9.sp, fontFamily = Sans,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xB30D0D10))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            e.title, color = Text2, fontSize = 10.5.sp, fontFamily = Sans,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ------------------------------ my list ------------------------------ */

@Composable
fun MyListSection(onOpen: (Tmdb.Media) -> Unit, onOpenById: (String, String) -> Unit) {
    val favs = Store.favorites
    val cont = Store.progress.values.sortedByDescending { p -> p.updatedAt }
    val hist = Store.history

    Column(Modifier.padding(top = 8.dp)) {
        if (cont.isNotEmpty()) {
            Row("Continue watching", emptyList(), onOpen, onOpenById, isContinue = true, contEntries = cont)
        }
        if (favs.isNotEmpty()) {
            SectionHeader("My list · ${favs.size}", "tap hearts to add")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(favs) { m -> CompactCard(m, onOpen) }
            }
        }
        if (hist.isNotEmpty()) {
            SectionHeader("Recently watched")
            Column {
                hist.take(12).forEach { h ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenById(h.type, h.tmdbId) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = h.poster?.let { IMG + it },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(64.dp).height(40.dp).clip(RoundedCornerShape(4.dp)).background(Surface),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                h.title, color = Text1, fontSize = 13.sp,
                                fontFamily = Sans, maxLines = 1,
                            )
                            Text(
                                (if (h.season != null) "S${h.season}·E${h.episode}" else if (h.type == "tv") "show" else "film"),
                                color = Text3, fontSize = 10.5.sp, fontFamily = Sans,
                            )
                        }
                    }
                }
            }
        }
        if (favs.isEmpty() && cont.isEmpty() && hist.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                Text(
                    "your list is empty —\ntap ♥ on anything you like",
                    color = Text3, fontSize = 13.sp, fontFamily = Sans,
                    lineHeight = 20.sp,
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
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                if (query.isEmpty()) Text("Search movies, shows…", color = Text3, fontSize = 14.sp, fontFamily = Sans)
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 14.sp, fontFamily = Sans),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            Spacer(Modifier.width(10.dp))
            Wordmark(size = 20)
        }
        if (searching) {
            Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Iris, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
            }
        } else if (query.length >= 2 && results.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                Text("nothing found", color = Text3, fontSize = 13.sp, fontFamily = Sans)
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
            items(results) { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(m) }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = m.poster?.let { IMG + it },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(48.dp).height(72.dp).clip(RoundedCornerShape(5.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(m.title, color = Text1, fontSize = 14.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${m.year} · ${if (m.type == "tv") "show" else "film"} · ★ ${"%.1f".format(m.rating)}",
                            color = Text3, fontSize = 11.sp, fontFamily = Sans,
                        )
                    }
                }
            }
        }
    }
}
