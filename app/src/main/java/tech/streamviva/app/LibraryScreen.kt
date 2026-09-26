package tech.streamviva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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

/* ------------------------ continue watching row ------------------------ */

@Composable
fun ContinueRow(
    entries: List<Store.ProgressEntry>,
    onOpen: (String, String) -> Unit, // (type, tmdbId)
    onRemove: (Store.ProgressEntry) -> Unit,
) {
    if (entries.isEmpty()) return
    Column {
        SectionHeader("Continue watching")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries.size) { idx ->
                val e = entries[idx]
                Box(
                    Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .clickable { onOpen(e.type, e.tmdbId) },
                ) {
                    Column {
                        Box {
                            AsyncImage(
                                model = e.poster?.let { IMG + it },
                                contentDescription = e.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(124.dp),
                            )
                            if (e.season != null) {
                                Text(
                                    "S${e.season}·E${e.episode}",
                                    color = White,
                                    fontSize = 10.sp,
                                    fontFamily = Sans,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xB30D0D10))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                            IconButton(
                                onClick = { onRemove(e) },
                                modifier = Modifier.align(Alignment.TopEnd).size(30.dp),
                            ) {
                                Text("✕", color = White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                            // progress bar
                            Box(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(Color(0x66000000)),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(e.pct)
                                        .height(3.dp)
                                        .background(Iris),
                                )
                            }
                        }
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                e.title,
                                color = Text1,
                                fontSize = 12.5.sp,
                                fontFamily = Sans,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Text(
                                if (e.season != null) "season ${e.season} · episode ${e.episode}" else "${(e.pct * 100).toInt()}% watched",
                                color = Text3,
                                fontSize = 10.5.sp,
                                fontFamily = Sans,
                            )
                        }
                    }
                }
            }
        }
    }
}

/* --------------------------- library screen --------------------------- */

@Composable
fun LibraryScreen(
    onOpen: (Tmdb.Media) -> Unit,
    onOpenById: (String, String) -> Unit,
) {
    val favs = Store.favorites
    val cont = Store.progress.values.sortedByDescending { it.updatedAt }
    val hist = Store.history

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(size = 25)
                Spacer(Modifier.width(10.dp))
                Kicker("your library", color = Text2)
            }
        }

        // continue watching
        if (cont.isNotEmpty()) {
            item {
                ContinueRow(
                    entries = cont,
                    onOpen = { type, id -> onOpenById(type, id) },
                    onRemove = { e ->
                        Store.removeProgress(e)
                    },
                )
                Spacer(Modifier.height(26.dp))
            }
        }

        // favorites
        item {
            if (favs.isNotEmpty()) {
                SectionHeader("Favorites", "${favs.size}")
            } else {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    SectionHeader("Favorites")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface.copy(alpha = 0.5f))
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = Text3, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Tap the heart on any movie or show to save it here",
                            color = Text3, fontSize = 13.sp, fontFamily = Sans,
                        )
                    }
                }
            }
        }
        if (favs.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(favs.size) { i -> PosterCard(favs[i], onOpen) }
                }
                Spacer(Modifier.height(26.dp))
            }
        }

        // history
        if (hist.isNotEmpty()) {
            item { SectionHeader("Recently watched") }
            items(hist.size) { idx ->
                val h = hist[idx]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenById(h.type, h.tmdbId) }
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = h.poster?.let { IMG + it },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(52.dp).height(78.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            h.title,
                            color = Text1, fontSize = 14.5.sp, fontFamily = Sans,
                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                        )
                        Text(
                            when {
                                h.season != null -> "S${h.season} · E${h.episode}"
                                else -> if (h.type == "tv") "show" else "film"
                            } + "  ·  " + timeAgo(h.watchedAt),
                            color = Text3, fontSize = 11.5.sp, fontFamily = Sans,
                        )
                    }
                }
            }
        } else if (favs.isEmpty() && cont.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = Text3, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "nothing here yet —\nwatch something and it'll show up",
                        color = Text3, fontSize = 13.5.sp, fontFamily = Sans,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

private fun timeAgo(ts: Long): String {
    val mins = (System.currentTimeMillis() - ts) / 60000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 60 * 24 -> "${mins / 60}h ago"
        else -> "${mins / (60 * 24)}d ago"
    }
}
