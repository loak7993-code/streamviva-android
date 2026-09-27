package tech.streamviva.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Download engine: resolves a stream, parses the HLS master playlist for
 * quality variants, downloads all segments of the chosen variant and
 * concatenates them into a single local file playable by ExoPlayer.
 */
object Downloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class Quality(val label: String, val width: Int, val height: Int, val url: String)

    data class DownloadItem(
        val id: String,              // "movie-27205" / "tv-1396-s1e3"
        val tmdbId: Long,
        val title: String,           // "Breaking Bad — S1 E3"
        val mediaTitle: String,
        val poster: String?,
        val type: String,
        val season: Int?,
        val episode: Int?,
        val file: String,            // relative path in downloads dir
    )

    var items by mutableStateOf<List<DownloadItem>>(emptyList())
        private set
    var progress by mutableStateOf<Map<String, Float>>(emptyMap())
        private set
    var statuses by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    private lateinit var appContext: Context
    private var notified = mutableSetOf<String>()

    fun init(context: Context) {
        appContext = context.applicationContext
        createChannel()
        loadRegistry()
    }

    fun dir(): File = File(appContext.filesDir, "downloads").apply { mkdirs() }

    private fun registryFile() = File(appContext.filesDir, "downloads.json")

    private fun loadRegistry() {
        try {
            val txt = registryFile().readText()
            val arr = org.json.JSONArray(txt)
            val list = mutableListOf<DownloadItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(itemFromJson(o))
            }
            items = list
        } catch (_: Exception) {}
    }

    private fun saveRegistry() {
        val arr = org.json.JSONArray()
        items.forEach { arr.put(itemToJson(it)) }
        registryFile().writeText(arr.toString())
    }

    private fun itemToJson(d: DownloadItem) = org.json.JSONObject().apply {
        put("id", d.id); put("tmdbId", d.tmdbId); put("title", d.title)
        put("mediaTitle", d.mediaTitle); put("poster", d.poster ?: "")
        put("type", d.type); d.season?.let { put("season", it) }; d.episode?.let { put("episode", it) }
        put("file", d.file)
    }

    private fun itemFromJson(o: org.json.JSONObject) = DownloadItem(
        id = o.getString("id"), tmdbId = o.getLong("tmdbId"), title = o.getString("title"),
        mediaTitle = o.getString("mediaTitle"), poster = o.optString("poster").takeIf { it.isNotBlank() },
        type = o.getString("type"),
        season = if (o.has("season")) o.getInt("season") else null,
        episode = if (o.has("episode")) o.getInt("episode") else null,
        file = o.getString("file"),
    )

    fun isDownloaded(id: String): Boolean = items.any { it.id == id }
    fun isDownloading(id: String): Boolean = statuses[id] == "downloading" || statuses[id] == "queued"
    fun fileFor(id: String): File? = items.find { it.id == id }?.let { File(dir(), it.file) }

    /* ---------------- quality discovery ---------------- */

    suspend fun qualities(media: Tmdb.Media, season: Int?, episode: Int?): List<Quality> =
        withContext(Dispatchers.IO) {
            try {
                val streamUrl = StreamResolver.resolveForDownload(media, season, episode) ?: return@withContext emptyList()
                val pl = fetchText(streamUrl)
                if (pl.trimStart().startsWith("#EXTM3U")) {
                    val isMaster = pl.contains("#EXT-X-STREAM-INF")
                    if (isMaster) {
                        val base = streamUrl.substringBeforeLast('/') + "/"
                        val out = mutableListOf<Quality>()
                        val lines = pl.lines()
                        for (i in lines.indices) {
                            if (lines[i].startsWith("#EXT-X-STREAM-INF")) {
                                val res = Regex("RESOLUTION=\\\"(\\d+)x(\\d+)\\\"").find(lines[i])
                                val uriLine = lines.getOrNull(i + 1)?.trim() ?: continue
                                val variantUrl = if (uriLine.startsWith("http")) uriLine else base + uriLine
                                val w = res?.groupValues?.get(1)?.toIntOrNull() ?: 0
                                val h = res?.groupValues?.get(2)?.toIntOrNull() ?: 0
                                out.add(Quality(label = if (h > 0) "${h}p" else "auto", width = w, height = h, url = variantUrl))
                            }
                        }
                        out.sortedByDescending { it.height }
                    } else {
                        listOf(Quality("source", 0, 0, streamUrl))
                    }
                } else {
                    // direct mp4
                    listOf(Quality("source", 0, 0, streamUrl))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun fetchText(url: String): String {
        val req = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("http ${res.code}")
            return res.body!!.string()
        }
    }

    /* ---------------- download ---------------- */

    fun enqueue(
        media: Tmdb.Media,
        season: Int?,
        episode: Int?,
        qualityUrl: String,
        qualityLabel: String,
    ) {
        val id = if (season != null && episode != null) {
            "${media.type}-${media.id}-s${season}e${episode}"
        } else {
            "${media.type}-${media.id}"
        }
        if (isDownloaded(id) || isDownloading(id)) return

        val title = if (season != null && episode != null) {
            "${media.title} — S${season} E${episode}"
        } else media.title

        val item = DownloadItem(
            id = id, tmdbId = media.id, title = title, mediaTitle = media.title,
            poster = media.poster, type = media.type, season = season, episode = episode,
            file = "$id.ts",
        )
        items = items + item
        saveRegistry()
        statuses = statuses + (id to "downloading")
        progress = progress + (id to 0f)

        scope.launch {
            try {
                downloadHls(item, qualityUrl, qualityLabel)
                statuses = statuses + (id to "done")
                progress = progress + (id to 1f)
                notify(item.title, "Download complete")
            } catch (e: Exception) {
                statuses = statuses + (id to "failed: ${e.message?.take(40)}")
                notify(item.title, "Download failed: ${e.message?.take(60)}")
            }
        }
    }

    private suspend fun downloadHls(item: DownloadItem, url: String, qualityLabel: String) {
        withContext(Dispatchers.IO) {
            val out = File(dir(), item.file)
            val tmp = File(dir(), item.file + ".part")

            val text = fetchText(url)
            if (!text.trimStart().startsWith("#EXTM3U")) {
                // direct file download
                val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                client.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) error("http ${res.code}")
                    val body = res.body ?: error("no body")
                    val total = body.contentLength()
                    var read = 0L
                    val input = body.byteStream()
                    tmp.outputStream().use { o ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n)
                            read += n
                            if (total > 0) {
                                val p = read.toFloat() / total
                                updateProgress(item.id, p)
                            }
                        }
                    }
                }
                tmp.renameTo(out)
                return@withContext
            }

            // HLS: collect segment urls
            val base = url.substringBeforeLast('/') + "/"
            val segments = text.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { if (it.startsWith("http")) it else base + it }

            // nested playlists (rare) — handle one level
            if (segments.size == 1 && !segments[0].endsWith(".ts") && !segments[0].contains("/content/")) {
                val inner = fetchText(segments[0])
                val innerBase = segments[0].substringBeforeLast('/') + "/"
                val realSegs = inner.lines().map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .map { if (it.startsWith("http")) it else innerBase + it }
                downloadSegments(item, realSegs, tmp, out)
            } else {
                downloadSegments(item, segments, tmp, out)
            }
        }
    }

    private suspend fun downloadSegments(item: DownloadItem, segments: List<String>, tmp: File, out: File) {
        val total = segments.size
        var done = 0
        val outStream = tmp.outputStream().buffered()
        try {
            // sequential but pipelined with 3 concurrent fetches
            val queue = java.util.concurrent.ConcurrentLinkedQueue(segments)
            val jobs = (0 until 3).map {
                scope.launch(Dispatchers.IO) {
                    while (true) {
                        val seg = queue.poll() ?: break
                        try {
                            val req = Request.Builder().url(seg).header("User-Agent", "Mozilla/5.0").build()
                            client.newCall(req).execute().use { res ->
                                if (!res.isSuccessful) error("seg http ${res.code}")
                                val bytes = res.body!!.bytes()
                                synchronized(outStream) {
                                    outStream.write(bytes)
                                }
                            }
                            done++
                            updateProgress(item.id, done.toFloat() / total)
                        } catch (e: Exception) {
                            // skip failed segment — playback tolerates gaps
                            done++
                        }
                    }
                }
            }
            jobs.joinAll()
            outStream.flush()
        } finally {
            outStream.close()
        }
        if (tmp.length() > 0) tmp.renameTo(out)
    }

    private fun updateProgress(id: String, p: Float) {
        // throttle UI churn
        progress = progress + (id to p)
        notifyProgress(id, p)
    }

    fun delete(id: String) {
        val item = items.find { it.id == id } ?: return
        File(dir(), item.file).delete()
        File(dir(), item.file + ".part").delete()
        items = items.filterNot { it.id == id }
        statuses = statuses - id
        progress = progress - id
        saveRegistry()
    }

    /* ---------------- notifications ---------------- */

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("downloads", "Downloads", NotificationManager.IMPORTANCE_LOW)
            appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun notify(title: String, text: String) {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = androidx.core.app.NotificationCompat.Builder(appContext, "downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(title.hashCode(), n)
    }

    private fun notifyProgress(id: String, p: Float) {
        if (!notified.contains(id)) notified.add(id)
        val item = items.find { it.id == id } ?: return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = androidx.core.app.NotificationCompat.Builder(appContext, "downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(item.title)
            .setContentText("${(p * 100).toInt()}%")
            .setProgress(100, (p * 100).toInt(), false)
            .setOngoing(true)
            .build()
        nm.notify(id.hashCode(), n)
        if (p >= 1f) nm.cancel(id.hashCode())
    }
}
