package tech.streamviva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
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

/* ---------------- download quality picker ---------------- */

@Composable
fun DownloadQualityDialog(
    qualities: List<Downloader.Quality>,
    onPick: (Downloader.Quality) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color(0xF215151A), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(vertical = 14.dp)) {
                Text("Download quality", color = White, fontSize = 17.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp))
                Text("${qualities.size} options", color = Text3, fontSize = 11.sp, fontFamily = Sans, modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp))
                Spacer(Modifier.height(10.dp))
                LazyColumn {
                    items(qualities) { q ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(q) }.padding(horizontal = 18.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(q.label, color = Text1, fontSize = 15.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            if (q.width > 0) {
                                Text("${q.width}×${q.height}", color = Text3, fontSize = 11.sp, fontFamily = Sans)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("↓", color = Store.accent, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- episode multi-select for download ---------------- */

@Composable
fun EpisodeSelectDialog(
    media: Tmdb.Media,
    seasons: List<Tmdb.Season>,
    episodes: Map<Int, List<Tmdb.Episode>>,
    onConfirm: (List<Pair<Int, Int>>) -> Unit, // (season, episode) pairs
    onDismiss: () -> Unit,
) {
    var selectedSeason by remember { mutableStateOf(seasons.firstOrNull()?.number ?: 1) }
    val eps = episodes[selectedSeason] ?: emptyList()
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    var loadedSeasons by remember { mutableStateOf(setOf<Int>(selectedSeason)) }

    LaunchedEffect(selectedSeason) {
        if (selectedSeason !in loadedSeasons) {
            try {
                val list = Tmdb.episodes(media.id, selectedSeason)
                episodes // no-op; caller holds the map
            } catch (_: Exception) {}
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color(0xF215151A), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(vertical = 14.dp).heightIn(max = 480.dp)) {
                Text("Download episodes", color = White, fontSize = 17.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp))
                Text("${selected.count { it.value }} selected", color = Store.accent, fontSize = 12.sp, fontFamily = Sans, modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp))

                // season chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(seasons) { s ->
                        Text(
                            "S${s.number}",
                            color = if (selectedSeason == s.number) White else Text3,
                            fontSize = 13.sp, fontFamily = Sans,
                            fontWeight = if (selectedSeason == s.number) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedSeason == s.number) Store.accent.copy(alpha = 0.3f) else SurfaceColor)
                                .clickable { selectedSeason = s.number }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                // select all for this season
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Select all (S$selectedSeason)",
                        color = Store.accent, fontSize = 12.sp, fontFamily = Sans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                eps.forEach { selected["s$selectedSeason:e${it.number}"] = true }
                            }
                            .padding(vertical = 4.dp),
                    )
                    Text(
                        "Clear",
                        color = Text3, fontSize = 12.sp, fontFamily = Sans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected.clear() }
                            .padding(vertical = 4.dp),
                    )
                }

                // episodes
                LazyColumn(Modifier.weight(1f)) {
                    items(eps) { ep ->
                        val key = "s$selectedSeason:e${ep.number}"
                        val checked = selected[key] == true
                        val already = Downloader.isDownloaded("${media.type}-${media.id}-s${selectedSeason}e${ep.number}")
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { if (!already) selected[key] = !checked }
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked || already,
                                onCheckedChange = { if (!already) selected[key] = it },
                                enabled = !already,
                                colors = CheckboxDefaults.colors(checkedColor = Store.accent, uncheckedColor = Text3),
                            )
                            Column(Modifier.weight(1f)) {
                                Text("${ep.number}. ${ep.name}", color = if (already) Text3 else Text1, fontSize = 13.sp, fontFamily = Sans, maxLines = 1)
                                if (already) Text("downloaded", color = Text3, fontSize = 9.sp, fontFamily = Sans)
                            }
                        }
                    }
                }

                // confirm
                Button(
                    onClick = {
                        val pairs = selected.filter { it.value }.keys.mapNotNull { k ->
                            val m = Regex("s(\\d+):e(\\d+)").find(k) ?: return@mapNotNull null
                            m.groupValues[1].toInt() to m.groupValues[2].toInt()
                        }
                        onConfirm(pairs)
                    },
                    enabled = selected.any { it.value },
                    colors = ButtonDefaults.buttonColors(containerColor = Store.accent, contentColor = White, disabledContainerColor = SurfaceColor, disabledContentColor = Text3),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).height(44.dp),
                ) { Text("Continue", fontSize = 13.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

private val SurfaceColor = SurfaceHi

/* ---------------- downloads screen ---------------- */

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlay: (Downloader.DownloadItem) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back",
                tint = Text2,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onBack() }.padding(6.dp),
            )
            Spacer(Modifier.width(14.dp))
            Wordmark(size = 21)
            Spacer(Modifier.width(10.dp))
            Kicker("downloads", color = Text2)
        }

        val items = Downloader.items
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                Text(
                    "no downloads yet —\npress ↓ on any movie or episode",
                    color = Text3, fontSize = 13.sp, fontFamily = Sans, lineHeight = 20.sp,
                )
            }
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
            items(items, key = { it.id }) { item ->
                val status = Downloader.statuses[item.id] ?: "done"
                val p = Downloader.progress[item.id] ?: 1f
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = item.poster?.let { IMG + it },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(64.dp).height(40.dp).clip(RoundedCornerShape(5.dp)).background(SurfaceColor),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = Text1, fontSize = 13.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        when {
                            status == "downloading" -> {
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { p },
                                    color = Store.accent,
                                    trackColor = SurfaceColor,
                                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                )
                                Text("${(p * 100).toInt()}%", color = Text3, fontSize = 10.sp, fontFamily = Sans)
                            }
                            status.startsWith("failed") -> Text(status, color = Rose, fontSize = 10.sp, fontFamily = Sans)
                            else -> Text("ready · ${(java.io.File(Downloader.dir(), item.file).length() / 1_000_000)} MB", color = Text3, fontSize = 10.sp, fontFamily = Sans)
                        }
                    }
                    if (status == "done") {
                        Icon(
                            Icons.Rounded.PlayArrow, contentDescription = "play",
                            tint = Store.accent,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onPlay(item) }.padding(6.dp),
                        )
                    }
                    Icon(
                        Icons.Rounded.Delete, contentDescription = "delete",
                        tint = Text3,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { Downloader.delete(item.id) }.padding(6.dp),
                    )
                }
            }
        }
    }
}
