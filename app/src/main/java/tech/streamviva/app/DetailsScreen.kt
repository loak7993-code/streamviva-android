package tech.streamviva.app

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

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

    Box(Modifier.fillMaxSize().background(Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
        ) {
            // backdrop header
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

            // title row
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
                    Column {
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
                            Text(
                                media.year,
                                color = Text2, fontSize = 12.sp, fontFamily = Sans,
                            )
                            Text("  ·  ", color = Text3, fontSize = 12.sp)
                            Icon(
                                androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                                contentDescription = null, tint = Text3,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "%.1f".format(media.rating),
                                color = Gold, fontSize = 12.sp, fontFamily = Sans,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // action + overview
            item {
                Column(Modifier.padding(horizontal = 20.dp).offset(y = (-40).dp)) {
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
                            "⚠ ${error}",
                            color = Rose, fontSize = 13.sp, fontFamily = Sans,
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
                                    else episodes[selectedSeason]?.firstOrNull()?.let { (epNum, _) ->
                                        resolve(imdb, selectedSeason, epNum)
                                    }
                                },
                                enabled = canPlay,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Iris,
                                    contentColor = White,
                                    disabledContainerColor = SurfaceHi,
                                    disabledContentColor = Text3,
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (imdb.isBlank()) "loading…" else "Play now",
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

            // seasons
            if (media.type == "tv" && seasons.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp).offset(y = (-24).dp)) {
                        Kicker("seasons")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons) { s ->
                                Text(
                                    "Season ${s.number}",
                                    color = if (selectedSeason == s.number) White else Text2,
                                    fontSize = 13.sp,
                                    fontFamily = Sans,
                                    fontWeight = if (selectedSeason == s.number) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(
                                            if (selectedSeason == s.number) Iris.copy(alpha = 0.3f) else Surface
                                        )
                                        .clickable { selectedSeason = s.number }
                                        .padding(horizontal = 15.dp, vertical = 9.dp),
                                )
                            }
                        }
                    }
                }
            }

            // episodes
            selectedSeason?.let { s ->
                val eps = episodes[s] ?: emptyList()
                item {
                    Column(Modifier.padding(horizontal = 20.dp).offset(y = (-14).dp)) {
                        Kicker("episodes · season $s")
                    }
                }
                items(eps) { (epNum, _) ->
                    Row(
                        Modifier
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(Surface.copy(alpha = 0.55f))
                            .clickable { resolve(imdb, s, epNum) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "E$epNum",
                            color = IrisSoft,
                            fontSize = 13.sp,
                            fontFamily = Sans,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Episode $epNum",
                            color = Text1, fontSize = 14.sp, fontFamily = Sans,
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "play",
                            tint = Text3,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
