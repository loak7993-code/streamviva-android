package tech.streamviva.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.fontResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/* ---------- silk palette (the web design) ---------- */
private val Bg = Color(0xFF0E0E11)
private val BgDeep = Color(0xFF0B0B0E)
private val Surface = Color(0xFF17171C)
private val SurfaceHi = Color(0xFF1E1E25)
private val Text1 = Color(0xFFC9C9D2)
private val Text2 = Color(0xFF8B8B98)
private val Iris = Color(0xFF8D6BE0)
private val IrisSoft = Color(0xFFAF97EB)
private val White = Color(0xFFF7F7FA)

/* ---------- typography: Instrument family ---------- */
private val Serif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, androidx.compose.ui.text.font.FontStyle.Italic),
)
private val Sans = FontFamily(
    Font(R.font.instrument_sans, FontWeight.Normal),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
)

private val IMG = "https://image.tmdb.org/t/p/w500"
private val IMG_W780 = "https://image.tmdb.org/t/p/w780"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

sealed class Screen {
    data object Home : Screen()
    data object Search : Screen()
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
            is Screen.Home -> HomeScreen(onOpen = { push(Screen.Details(it)) }, onSearch = { push(Screen.Search) })
            is Screen.Search -> SearchScreen(onOpen = { push(Screen.Details(it)) })
            is Screen.Details -> DetailsScreen(media = screen.media, onPlay = { t, u -> push(Screen.Player(t, u)) })
            is Screen.Player -> PlayerScreen(title = screen.title, streamUrl = screen.streamUrl)
        }
    }
    BackHandler(enabled = stack.size > 1) { pop() }
}

/* --------------------------- shared bits --------------------------- */

@Composable
fun Wordmark(fontSize: Int = 30) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("Stream", color = White, fontSize = fontSize.sp, fontFamily = Serif)
        Text(
            "Viva",
            color = IrisSoft,
            fontSize = fontSize.sp,
            fontFamily = Serif,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        )
    }
}

@Composable
fun Kicker(text: String, color: Color = IrisSoft) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.6.sp,
    )
}

@Composable
fun SectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Iris)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            color = White,
            fontSize = 16.sp,
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun PosterCard(m: Tmdb.Media, onOpen: (Tmdb.Media) -> Unit) {
    Column(
        Modifier
            .width(124.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable { onOpen(m) },
    ) {
        Box {
            AsyncImage(
                model = m.poster?.let { IMG + it } ?: R.drawable.placeholder,
                contentDescription = m.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(186.dp),
            )
            if (m.rating > 0) {
                Text(
                    "★ ${"%.1f".format(m.rating)}",
                    color = White,
                    fontSize = 10.sp,
                    fontFamily = Sans,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(m.title, color = Text1, fontSize = 12.5.sp, fontFamily = Sans, maxLines = 1)
            Text(
                "${m.year} · ${if (m.type == "tv") "show" else "movie"}",
                color = Text2, fontSize = 10.5.sp, fontFamily = Sans,
            )
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "a",
    )
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceHi.copy(alpha = alpha)))
}

@Composable
fun SkeletonRows() {
    Column(Modifier.padding(top = 130.dp)) {
        repeat(2) {
            ShimmerBox(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).width(150.dp).height(16.dp))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(horizontal = 20.dp)) {
                repeat(3) {
                    ShimmerBox(Modifier.size(width = 124.dp, height = 220.dp))
                    Spacer(Modifier.width(12.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

/* ------------------------------ home ------------------------------ */

@Composable
fun HomeScreen(onOpen: (Tmdb.Media) -> Unit, onSearch: () -> Unit) {
    var movies by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var tv by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var popular by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }

    LaunchedEffect(Unit) {
        try { movies = Tmdb.trendingMovies() } catch (_: Exception) {}
        try { tv = Tmdb.trendingTv() } catch (_: Exception) {}
        try { popular = Tmdb.popularMovies() } catch (_: Exception) {}
    }

    val hero = movies.firstOrNull()

    Box(Modifier.fillMaxSize().background(Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            // top bar
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Wordmark()
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface)
                            .clickable { onSearch() }
                            .padding(10.dp),
                    ) { Text("🔍", fontSize = 15.sp) }
                }
            }

            // hero — first trending movie
            if (hero != null) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(430.dp)
                            .clickable { onOpen(hero) },
                    ) {
                        AsyncImage(
                            model = hero.backdrop?.let { IMG_W780 + it },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    0f to Color(0x66000000),
                                    0.45f to Color.Transparent,
                                    1f to Bg,
                                )
                            )
                        )
                        Column(
                            Modifier.align(Alignment.BottomStart).padding(20.dp)
                        ) {
                            Kicker("now showing")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                hero.title,
                                color = White,
                                fontSize = 34.sp,
                                fontFamily = Serif,
                                lineHeight = 38.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${hero.year} · ${if (hero.type == "tv") "show" else "movie"} · ★ ${"%.1f".format(hero.rating)}",
                                color = Text2, fontSize = 12.sp, fontFamily = Sans,
                            )
                            if (hero.overview.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    hero.overview,
                                    color = Text1,
                                    fontSize = 12.5.sp,
                                    fontFamily = Sans,
                                    lineHeight = 18.sp,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }

            if (movies.isEmpty()) {
                item { SkeletonRows() }
            } else {
                item {
                    Column(Modifier.padding(top = 24.dp)) {
                        SectionTitle("Trending movies")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(movies) { m -> PosterCard(m, onOpen) }
                        }
                    }
                }
                item {
                    Column(Modifier.padding(top = 24.dp)) {
                        SectionTitle("Trending shows")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(tv) { m -> PosterCard(m, onOpen) }
                        }
                    }
                }
                item {
                    Column(Modifier.padding(top = 24.dp)) {
                        SectionTitle("Popular movies")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(popular) { m -> PosterCard(m, onOpen) }
                        }
                    }
                }
            }
        }
    }
}

/* ----------------------------- search ----------------------------- */

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

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .padding(16.dp),
            ) {
                if (query.isEmpty()) Text("Search movies, shows…", color = Text2, fontSize = 15.sp, fontFamily = Sans)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(color = White, fontSize = 15.sp, fontFamily = Sans),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            Spacer(Modifier.width(10.dp))
            Wordmark(fontSize = 22)
        }
        if (searching) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
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
                        model = m.poster?.let { IMG + it },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(58.dp).height(87.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(m.title, color = Text1, fontSize = 15.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${m.year} · ${if (m.type == "tv") "show" else "movie"} · ★ ${"%.1f".format(m.rating)}",
                            color = Text2, fontSize = 12.sp, fontFamily = Sans,
                        )
                        if (m.overview.isNotBlank()) Text(
                            m.overview, color = Text2, fontSize = 11.sp, fontFamily = Sans,
                            maxLines = 2, lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
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
    var resolving by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(media.id) {
        try {
            val (id, ss) = Tmdb.details(media.type, media.id)
            imdb = id
            seasons = ss
        } catch (e: Exception) { error = e.message }
    }
    LaunchedEffect(selectedSeason) {
        val s = selectedSeason ?: return@LaunchedEffect
        if (episodes[s] == null) {
            try { episodes = episodes + (s to Tmdb.episodes(media.id, s)) } catch (_: Exception) {}
        }
    }

    fun resolve(imdbId: String, season: Int? = null, episode: Int? = null) {
        resolving = if (episode != null) "S$season · E$episode" else "stream"
        error = null
        scope.launch {
            try {
                val r = if (media.type == "movie") StreamResolver.resolveMovie(imdbId)
                else StreamResolver.resolveShow(imdbId, season ?: 1, episode ?: 1)
                onPlay(r.title, r.masterUrl)
            } catch (e: Exception) { error = e.message } finally { resolving = null }
        }
    }

    LazyColumn(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        item {
            Box(Modifier.fillMaxWidth().height(320.dp)) {
                AsyncImage(
                    model = media.backdrop?.let { IMG_W780 + it },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Bg)))
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp).offset(y = (-70).dp)) {
                AsyncImage(
                    model = media.poster?.let { IMG + it },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(112.dp).height(168.dp).clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.padding(top = 80.dp)) {
                    Kicker(if (media.type == "tv") "series" else "feature")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        media.title, color = White, fontSize = 26.sp, fontFamily = Serif, lineHeight = 30.sp,
                    )
                    Text(
                        "${media.year} · ★ ${"%.1f".format(media.rating)}",
                        color = Text2, fontSize = 12.sp, fontFamily = Sans,
                    )
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).offset(y = (-52).dp)) {
                Text(
                    media.overview, color = Text1, fontSize = 13.sp, fontFamily = Sans, lineHeight = 19.sp,
                )
                Spacer(Modifier.height(18.dp))

                if (media.type == "tv" && seasons.isNotEmpty()) {
                    Kicker("seasons")
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(seasons) { s ->
                            Text(
                                "Season ${s.number}",
                                color = if (selectedSeason == s.number) White else Text2,
                                fontSize = 13.sp, fontFamily = Sans,
                                fontWeight = if (selectedSeason == s.number) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedSeason == s.number) Iris.copy(alpha = 0.28f) else Surface)
                                    .clickable { selectedSeason = s.number }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                when {
                    resolving != null -> Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(color = Iris, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("resolving ${resolving}…", color = Text2, fontSize = 13.sp, fontFamily = Sans)
                    }
                    error != null -> Text("⚠ ${error}", color = Color(0xFFE88383), fontSize = 13.sp, fontFamily = Sans)
                    else -> {
                        val canPlay = imdb.isNotBlank() && (media.type == "movie" || (selectedSeason != null && episodes[selectedSeason]?.isNotEmpty() == true))
                        Button(
                            onClick = {
                                if (media.type == "movie") resolve(imdb)
                                else episodes[selectedSeason]?.firstOrNull()?.let { (epNum, _) -> resolve(imdb, selectedSeason, epNum) }
                            },
                            enabled = canPlay,
                            colors = ButtonDefaults.buttonColors(containerColor = Iris, contentColor = White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) { Text("▶  Play now", fontSize = 15.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        selectedSeason?.let { s ->
            val eps = episodes[s] ?: emptyList()
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(14.dp))
                    Kicker("episodes · season $s")
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(eps) { (epNum, _) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { resolve(imdb, s, epNum) }
                        .padding(horizontal = 20.dp, vertical = 9.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("E$epNum", color = IrisSoft, fontSize = 13.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(14.dp))
                    Text("Episode $epNum", color = Text1, fontSize = 14.sp, fontFamily = Sans)
                    Spacer(Modifier.weight(1f))
                    Text("▶", color = Text2, fontSize = 12.sp)
                }
            }
        }
    }
}
