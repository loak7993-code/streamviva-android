package tech.streamviva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    media: Tmdb.Media,
    onPlay: (String, String, Tmdb.Media?, Int?, Int?) -> Unit,
    onOpen: (Tmdb.Media) -> Unit = {},
) {
    var imdb by remember { mutableStateOf("") }
    var seasons by remember { mutableStateOf<List<Tmdb.Season>>(emptyList()) }
    var episodes by remember { mutableStateOf<Map<Int, List<Tmdb.Episode>>>(emptyMap()) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var cast by remember { mutableStateOf<List<Tmdb.CastMember>>(emptyList()) }
    var similar by remember { mutableStateOf<List<Tmdb.Media>>(emptyList()) }
    var resolving by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isFav by remember { mutableStateOf(Store.isFavorite(media)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(media.id) {
        try {
            val (id, ss) = Tmdb.details(media.type, media.id)
            imdb = id
            seasons = ss
            if (ss.isNotEmpty()) selectedSeason = ss.first().number
        } catch (e: Exception) { error = e.message }
        try { cast = Tmdb.credits(media.type, media.id) } catch (_: Exception) {}
        try { similar = Tmdb.recommendations(media.type, media.id) } catch (_: Exception) {}
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
                onPlay(r.title, r.masterUrl, media, season, episode)
            } catch (e: Exception) { error = e.message } finally { resolving = null }
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
        ) {
            // backdrop
            item {
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    AsyncImage(
                        model = media.backdrop?.let { IMG_W780 + it } ?: media.poster?.let { IMG + it },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(Modifier.fillMaxSize().background(bottomFade()))
                }
            }

            // title + favorite
            item {
                Row(
                    Modifier.padding(horizontal = 20.dp).offset(y = (-60).dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AsyncImage(
                        model = media.poster?.let { IMG + it },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(110.dp).height(165.dp).clip(RoundedCornerShape(14.dp)),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Spacer(Modifier.height(58.dp))
                        Kicker(if (media.type == "tv") "series" else "feature")
                        Spacer(Modifier.height(5.dp))
                        Text(
                            media.title,
                            color = White,
                            fontSize = 27.sp,
                            fontFamily = Serif,
                            lineHeight = 30.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(media.year, color = Text2, fontSize = 12.sp, fontFamily = Sans)
                            Text("  ·  ★ ", color = Text3, fontSize = 12.sp)
                            Text(
                                "%.1f".format(media.rating),
                                color = Gold, fontSize = 12.sp, fontFamily = Sans,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            Store.toggleFavorite(media)
                            isFav = Store.isFavorite(media)
                        },
                        modifier = Modifier.offset(y = (-8).dp),
                    ) {
                        Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "favorite",
                            tint = if (isFav) Iris else Text2,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // play / resume
            item {
                Column(Modifier.padding(horizontal = 20.dp).offset(y = (-40).dp)) {
                    val resumeEntry = Store.getProgress(
                        media,
                        if (media.type == "tv") selectedSeason else null,
                        if (media.type == "tv") episodes[selectedSeason]?.firstOrNull()?.number else null,
                    )

                    when {
                        resolving != null -> Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(color = Iris, modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("resolving ${resolving}…", color = Text2, fontSize = 13.sp, fontFamily = Sans)
                        }
                        error != null -> Text(
                            "⚠ ${error}", color = Rose, fontSize = 13.sp, fontFamily = Sans,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        else -> {
                            val canPlay = imdb.isNotBlank() && (
                                media.type == "movie" ||
                                    (selectedSeason != null && episodes[selectedSeason]?.isNotEmpty() == true)
                                )
                            Button(
                                onClick = {
                                    if (media.type == "movie") resolve(imdb)
                                    else episodes[selectedSeason]?.firstOrNull()?.let { resolve(imdb, selectedSeason, it.number) }
                                },
                                enabled = canPlay,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Iris, contentColor = White,
                                    disabledContainerColor = SurfaceHi, disabledContentColor = Text3,
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (imdb.isBlank()) "loading…"
                                    else if (resumeEntry != null && resumeEntry.positionMs > 10_000)
                                        "Resume · ${(resumeEntry.positionMs / 60000)} min"
                                    else "Play now",
                                    fontSize = 15.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        media.overview,
                        color = Text1, fontSize = 13.sp, fontFamily = Sans, lineHeight = 19.sp,
                    )
                }
            }

            // cast
            if (cast.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp).offset(y = (-16).dp)) {
                        Kicker("cast")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(cast) { c ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(72.dp),
                                ) {
                                    AsyncImage(
                                        model = c.profile?.let { "https://image.tmdb.org/t/p/w185" + it },
                                        contentDescription = c.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(50)).background(SurfaceHi),
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        c.name, color = Text1, fontSize = 10.5.sp, fontFamily = Sans,
                                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                                    )
                                    Text(
                                        c.character, color = Text3, fontSize = 9.5.sp, fontFamily = Sans,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // seasons
            if (media.type == "tv" && seasons.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp).offset(y = (-8).dp)) {
                        Kicker("seasons")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons) { s ->
                                Text(
                                    "Season ${s.number}",
                                    color = if (selectedSeason == s.number) White else Text2,
                                    fontSize = 13.sp, fontFamily = Sans,
                                    fontWeight = if (selectedSeason == s.number) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (selectedSeason == s.number) Iris.copy(alpha = 0.3f) else Surface)
                                        .clickable { selectedSeason = s.number }
                                        .padding(horizontal = 15.dp, vertical = 9.dp),
                                )
                            }
                        }
                    }
                }
            }

            // episodes — with stills + names + progress
            selectedSeason?.let { s ->
                val eps = episodes[s] ?: emptyList()
                item {
                    Column(Modifier.padding(horizontal = 20.dp).offset(y = (0).dp)) {
                        Spacer(Modifier.height(14.dp))
                        Kicker("episodes · season $s")
                    }
                }
                items(eps) { ep ->
                    val epImdb = ep.imdbId
                    val epProgress = Store.getProgress(media, s, ep.number)
                    Row(
                        Modifier
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (epProgress != null && epProgress.pct > 0.9f) Surface.copy(alpha = 0.3f)
                                else Surface.copy(alpha = 0.55f)
                            )
                            .clickable {
                                if (imdb.isBlank()) return@clickable
                                if (epImdb == null) {
                                    // fall back to show imdb id
                                    resolve(imdb, s, ep.number)
                                } else resolve(imdb, s, ep.number)
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            AsyncImage(
                                model = ep.still?.let { IMG + it },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(107.dp).height(60.dp).clip(RoundedCornerShape(9.dp)).background(SurfaceHi),
                            )
                            if (epProgress != null) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .width(107.dp)
                                        .height(3.dp)
                                        .background(Color(0x66000000)),
                                ) {
                                    Box(Modifier.fillMaxWidth(epProgress.pct).height(3.dp).background(Iris))
                                }
                            }
                            Box(
                                Modifier
                                    .align(Alignment.Center)
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x990D0D10)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.PlayArrow, "play", tint = White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${ep.number}. ${ep.name}",
                                color = Text1, fontSize = 13.sp, fontFamily = Sans,
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                            )
                            Text(
                                "★ ${"%.1f".format(ep.rating)} · ${epProgress?.let { "${(it.pct * 100).toInt()}%" } ?: "not watched"}",
                                color = Text3, fontSize = 10.5.sp, fontFamily = Sans,
                            )
                        }
                    }
                }
            }

            // more like this
            if (similar.isNotEmpty()) {
                item {
                    Column(Modifier.padding(top = 10.dp)) {
                        SectionHeader("More like this")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(similar) { m ->
                                CompactCard(m, onOpen)
                            }
                        }
                    }
                }
            }
        }
    }
}
