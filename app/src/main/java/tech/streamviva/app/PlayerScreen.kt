package tech.streamviva.app

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Native player — Media3/ExoPlayer with HLS.
 * Saves watch progress every 5s and on exit; resumes from last position.
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

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val isHls = streamUrl.contains(".m3u8") || streamUrl.contains("m3u8")
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)

            val source = if (isHls) {
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(streamUrl))
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(streamUrl))
            }
            setMediaSource(source)
            if (resumeMs > 10_000L) seekTo(resumeMs)
            prepare()
            playWhenReady = true
        }
    }

    // save progress periodically + record history
    LaunchedEffect(player, media) {
        media?.let { Store.addHistory(it, season, episode) }
        while (true) {
            delay(5000)
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
        if (error != null) {
            Text(
                "⚠ ${error}",
                color = Rose,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }
    }
}
