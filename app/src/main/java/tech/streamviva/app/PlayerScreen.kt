package tech.streamviva.app

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

/**
 * Native player — Media3/ExoPlayer with HLS + VDRK subtitles.
 * Auto-landscape, resume, progress saving, subtitle picker.
 */
@Composable
fun PlayerScreen(
    title: String,
    streamUrl: String,
    media: Tmdb.Media? = null,
    season: Int? = null,
    episode: Int? = null,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var subs by remember { mutableStateOf<List<Subtitles.Subtitle>>(emptyList()) }
    var subsLoading by remember { mutableStateOf(true) }
    var selectedSub by remember { mutableStateOf<Subtitles.Subtitle?>(null) }
    var showSubPicker by remember { mutableStateOf(false) }
    var playerVersion by remember { mutableStateOf(0) } // rebuild player when subs change

    // auto-rotate to landscape while playing; restore on exit
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation =
                previous ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val resumeMs = media?.let { Store.getProgress(it, season, episode)?.positionMs } ?: 0L

    // fetch subtitle list
    LaunchedEffect(media?.id) {
        subsLoading = true
        subs = media?.let {
            Subtitles.fetchAll(it.type, it.id, it._imdb, season, episode)
        } ?: emptyList()
        // auto-select last used language
        val pref = Store.subtitleLanguage
        if (pref != null) selectedSub = subs.firstOrNull { it.code == pref }
        subsLoading = false
    }

    val player = remember(playerVersion) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .build()
            .apply {
                val builder = MediaItem.Builder().setUri(streamUrl)
                selectedSub?.let { s ->
                    builder.setSubtitleConfigurations(
                        listOf(
                            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(s.url))
                                .setMimeType(
                                    if (s.format == "srt") MimeTypes.APPLICATION_SUBRIP
                                    else MimeTypes.TEXT_VTT
                                )
                                .setLanguage(s.code)
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .setLabel(s.label)
                                .build()
                        )
                    )
                }
                val item = builder.build()
                setMediaItem(item)
                if (resumeMs > 10_000L) seekTo(resumeMs)
                prepare()
                playWhenReady = true
            }
    }

    // toggle text track with selection
    LaunchedEffect(selectedSub, player) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, selectedSub == null)
            .apply {
                selectedSub?.let { setPreferredTextLanguage(it.code) }
            }
            .build()
    }

    // save progress periodically + record history
    LaunchedEffect(player, media) {
        media?.let { Store.addHistory(it, season, episode) }
        while (true) {
            kotlinx.coroutines.delay(5000)
            val m = media ?: continue
            val dur = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            val pos = player.currentPosition
            if (dur > 0 && pos > 0) Store.saveProgress(m, pos, dur, season, episode)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            media?.let {
                val dur = player.duration.takeIf { d -> d != C.TIME_UNSET } ?: 0L
                val pos = player.currentPosition
                if (dur > 0 && pos > 0) Store.saveProgress(it, pos, dur, season, episode)
            }
            player.release()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    useController = true
                    setShowSubtitleButton(true)
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // CC button (top right)
        Surface(
            color = Color(0x88000000),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
                .clickable { showSubPicker = true },
        ) {
            Text(
                if (selectedSub != null) "CC ✓" else "CC",
                color = if (selectedSub != null) Color.White else Color(0xFFB3B3B3),
                fontSize = 13.sp,
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        if (error != null) {
            Text(
                "⚠ ${error}",
                color = Rose,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }
    }

    // subtitle picker dialog
    if (showSubPicker) {
        SubtitlePickerDialog(
            subs = subs,
            loading = subsLoading,
            selected = selectedSub,
            onSelect = { sub ->
                selectedSub = sub
                Store.updateSubtitleLanguage(sub?.code)
                if (sub != null) playerVersion++ // rebuild with side-loaded track
                showSubPicker = false
            },
            onDismiss = { showSubPicker = false },
        )
    }
}

@Composable
fun SubtitlePickerDialog(
    subs: List<Subtitles.Subtitle>,
    loading: Boolean,
    selected: Subtitles.Subtitle?,
    onSelect: (Subtitles.Subtitle?) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xF215151A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(420.dp),
        ) {
            Column(Modifier.padding(vertical = 14.dp)) {
                Text(
                    "Subtitles",
                    color = White,
                    fontSize = 17.sp,
                    fontFamily = Sans,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Text(
                    "${subs.size} languages",
                    color = Text3,
                    fontSize = 11.sp,
                    fontFamily = Sans,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                )
                Spacer(Modifier.height(10.dp))

                if (loading) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Store.accent, modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                    }
                } else {
                    LazyColumn {
                        item {
                            SubRow("Off", "disable subtitles", selected == null) { onSelect(null) }
                        }
                        items(subs) { s ->
                            SubRow(
                                s.language,
                                (if (s.hearingImpaired) "hearing impaired · " else "") + s.source,
                                selected?.url == s.url,
                            ) { onSelect(s) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubRow(name: String, sub: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (active) Store.accent else Color.Transparent),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                name,
                color = if (active) White else Text1,
                fontSize = 14.sp,
                fontFamily = Sans,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(sub, color = Text3, fontSize = 10.5.sp, fontFamily = Sans)
        }
    }
}
