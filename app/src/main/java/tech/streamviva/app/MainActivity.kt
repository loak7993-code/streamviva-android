package tech.streamviva.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/* ---------- silk palette (matches the web design) ---------- */
private val Bg = Color(0xFF0E0E11)
private val Surface = Color(0xFF17171B)
private val SurfaceHi = Color(0xFF1D1D23)
private val Text1 = Color(0xFFC7C7CE)
private val Text2 = Color(0xFF8B8B96)
private val Iris = Color(0xFF9D7FE8)
private val IrisSoft = Color(0xFFB49BF1)
private val White = Color(0xFFF7F7FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}

sealed class Screen {
    data object Home : Screen()
    data class Search(val query: String = "") : Screen()
    data class Details(val media: Tmdb.Media) : Screen()
    data class Player(val title: String, val streamUrl: String) : Screen()
}

@Composable
fun App() {
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val current = stack.last()

    fun push(s: Screen) { stack = stack + s }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }

    androidx.compose.animation.Crossfade(targetState = current, label = "screen") { screen ->
        when (screen) {
            is Screen.Home -> HomeScreen(onOpen = { push(Screen.Details(it)) }, onSearch = { push(Screen.Search()) })
            is Screen.Search -> SearchScreen(onOpen = { push(Screen.Details(it)) })
            is Screen.Details -> DetailsScreen(media = screen.media, onPlay = { title, url -> push(Screen.Player(title, url)) })
            is Screen.Player -> PlayerScreen(title = screen.title, streamUrl = screen.streamUrl)
        }
    }

    BackHandler(enabled = stack.size > 1) { pop() }
}



/* ------------------------------ home ------------------------------ */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(onOpen: (Tmdb.Media) -> Unit, onSearch: () -> Unit) {
    var movies by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var tv by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var topTv by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }

    LaunchedEffect(Unit) {
        try { movies = Tmdb.trendingMovies() } catch (_: Exception) {}
        try { tv = Tmdb.trendingTv() } catch (_: Exception) {}
        try { topTv = Tmdb.topRatedTv() } catch (_: Exception) {}
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(
                    "Stream",
                    color = White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Viva",
                        color = IrisSoft,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("now showing", color = Text2, fontSize = 11.sp, letterSpacing = 3.sp)
                }
            }
        }
        item {
            // search bar
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .clickable { onSearch() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔍", fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Text("Search movies, shows…", color = Text2, fontSize = 15.sp)
            }
            Spacer(Modifier.height(22.dp))
        }
        if (movies.isNotEmpty()) item { RowSection("Trending movies", movies, onOpen) }
        if (tv.isNotEmpty()) item { RowSection("Trending shows", tv, onOpen) }
        if (topTv.isNotEmpty()) item { RowSection("Top rated shows", topTv, onOpen) }
        if (movies.isEmpty() && tv.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Iris)
                }
            }
        }
    }
}

@Composable
fun RowSection(title: String, items: List<Tmdb.Media>, onOpen: (Tmdb.Media) -> Unit) {
    Column {
        Text(
            title,
            color = White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items) { m ->
                Column(
                    Modifier
                        .width(128.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .clickable { onOpen(m) },
                ) {
                    AsyncImage(
                        model = m.poster?.let { Tmdb.IMG + it } ?: R.drawable.placeholder,
                        contentDescription = m.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                    )
                    Column(Modifier.padding(8.dp)) {
                        Text(
                            m.title,
                            color = Text1,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                        Text(
                            "${m.year} · ${if (m.type == "tv") "show" else "movie"}",
                            color = Text2,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

/* ----------------------------- search ----------------------------- */

@Composable
fun SearchScreen(onOpen: (Tmdb.Media) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        if (query.length >= 2) {
            searching = true
            try { results = Tmdb.search(query) } catch (_: Exception) { results = emptyList() }
            searching = false
        } else {
            results = emptyList()
        }
    }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(color = White, fontSize = 15.sp),
                modifier = Modifier.weight(1f),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search movies, shows…", color = Text2, fontSize = 15.sp)
                    inner()
                },
            )
        }
        if (searching) {
            Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Iris)
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
            items(results) { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(m) }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = m.poster?.let { Tmdb.IMG + it },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(56.dp).height(84.dp).clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(m.title, color = Text1, fontSize = 15.sp)
                        Text(
                            "${m.year} · ${if (m.type == "tv") "show" else "movie"} · ★ ${"%.1f".format(m.rating)}",
                            color = Text2,
                            fontSize = 12.sp,
                        )
                        if (m.overview.isNotBlank()) {
                            Text(
                                m.overview,
                                color = Text2,
                                fontSize = 11.sp,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ----------------------------- details ---------------------------- */

@Composable
fun DetailsScreen(media: Tmdb.Media, onPlay: (String, String) -> Unit) {
    var imdb by remember { mutableStateOf("") }
    var seasons by remember { mutableStateOf<List<Tmdb.Season>>(emptyList()) }
    var episodes by remember { mutableStateOf<Map<Int, List<Pair<Int, String>>>>(emptyMap()) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(media.id) {
        try {
            val (id, ss) = Tmdb.details(media.type, media.id)
            imdb = id
            seasons = ss
        } catch (e: Exception) {
            error = e.message
        }
    }

    LaunchedEffect(selectedSeason) {
        val s = selectedSeason ?: return@LaunchedEffect
        if (episodes[s] == null) {
            try {
                episodes = episodes + (s to Tmdb.episodes(media.id, s))
            } catch (_: Exception) {}
        }
    }

    fun resolve(imdbId: String) {
        resolving = true
        error = null
        scope.launch {
            try {
                val r = if (media.type == "movie") StreamResolver.resolveMovie(imdbId)
                else {
                    val eps = episodes[selectedSeason] ?: emptyList()
                    val ep = eps.firstOrNull() ?: error("no episode selected")
                    StreamResolver.resolveShow(imdbId, selectedSeason ?: 1, ep.first)
                }
                onPlay(r.title, r.masterUrl)
            } catch (e: Exception) {
                error = e.message
            } finally {
                resolving = false
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        item {
            Box {
                AsyncImage(
                    model = media.backdrop?.let { "https://image.tmdb.org/t/p/w780" + it },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Bg))),
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(media.title, color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${media.year} · ${if (media.type == "tv") "show" else "movie"} · ★ ${"%.1f".format(media.rating)}",
                    color = Text2,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    media.overview,
                    color = Text1,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Spacer(Modifier.height(18.dp))

                if (media.type == "tv" && seasons.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        items(seasons) { s ->
                            Text(
                                "Season ${s.number}",
                                color = if (selectedSeason == s.number) White else Text2,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedSeason == s.number) Iris.copy(alpha = 0.25f) else Surface)
                                    .clickable { selectedSeason = s.number }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                when {
                    resolving -> Box(
                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = Iris) }
                    error != null -> Text(
                        "⚠ ${error}",
                        color = Color(0xFFE88383),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    else -> {
                        val canPlay = imdb.isNotBlank() && (media.type == "movie" || (selectedSeason != null && episodes[selectedSeason]?.isNotEmpty() == true))
                        Button(
                            onClick = { resolve(imdb) },
                            enabled = canPlay,
                            colors = ButtonDefaults.buttonColors(containerColor = Iris, contentColor = White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Text(if (resolving) "Resolving stream…" else "▶  Play", fontSize = 16.sp)
                        }
                    }
                }

                // episode list for tv
                selectedSeason?.let { s ->
                    episodes[s]?.let { eps ->
                        Spacer(Modifier.height(14.dp))
                        Text("Episodes", color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // episodes rendered as list items
        selectedSeason?.let { s ->
            val eps = episodes[s] ?: emptyList()
            items(eps) { (epNum, epImdb) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            resolving = true
                            error = null
                            scope.launch {
                                try {
                                    val r = StreamResolver.resolveShow(imdb, s, epNum)
                                    onPlay(r.title, r.masterUrl)
                                } catch (e: Exception) { error = e.message } finally { resolving = false }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "S$s·E$epNum",
                        color = IrisSoft,
                        fontSize = 13.sp,
                        modifier = Modifier.width(64.dp),
                    )
                    Text("Episode $epNum", color = Text1, fontSize = 14.sp)
                }
            }
        }
    }
}
