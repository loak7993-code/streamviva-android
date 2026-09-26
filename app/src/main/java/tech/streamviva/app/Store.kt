package tech.streamviva.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local library: watch progress, favorites, history.
 * SharedPreferences + JSON — no deps, survives restarts.
 */
object Store {
    private lateinit var prefs: android.content.SharedPreferences

    // in-memory reactive state
    var progress by mutableStateOf<Map<String, ProgressEntry>>(emptyMap())
        private set
    var favorites by mutableStateOf<List<Tmdb.Media>>(emptyList())
        private set
    var history by mutableStateOf<List<HistoryEntry>>(emptyList())
        private set

    data class ProgressEntry(
        val tmdbId: String,
        val title: String,
        val poster: String?,
        val type: String,
        val positionMs: Long,
        val durationMs: Long,
        val season: Int? = null,
        val episode: Int? = null,
        val updatedAt: Long,
    ) {
        val pct: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    }

    data class HistoryEntry(
        val tmdbId: String,
        val title: String,
        val poster: String?,
        val type: String,
        val watchedAt: Long,
        val season: Int? = null,
        val episode: Int? = null,
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences("streamviva", Context.MODE_PRIVATE)
        load()
    }

    private fun load() {
        try {
            val p = prefs.getString("progress", null)
            if (p != null) {
                val arr = JSONArray(p)
                val map = mutableMapOf<String, ProgressEntry>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val e = ProgressEntry(
                        tmdbId = o.getString("tmdbId"),
                        title = o.getString("title"),
                        poster = o.optString("poster").takeIf { it.isNotBlank() },
                        type = o.getString("type"),
                        positionMs = o.getLong("positionMs"),
                        durationMs = o.getLong("durationMs"),
                        season = if (o.has("season")) o.getInt("season") else null,
                        episode = if (o.has("episode")) o.getInt("episode") else null,
                        updatedAt = o.getLong("updatedAt"),
                    )
                    map[key(e)] = e
                }
                progress = map
            }
        } catch (_: Exception) {}
        try {
            val f = prefs.getString("favorites", null)
            if (f != null) {
                val arr = JSONArray(f)
                val list = mutableListOf<Tmdb.Media>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        Tmdb.Media(
                            id = o.getLong("id"),
                            title = o.getString("title"),
                            overview = o.optString("overview"),
                            poster = o.optString("poster").takeIf { it.isNotBlank() },
                            backdrop = o.optString("backdrop").takeIf { it.isNotBlank() },
                            year = o.optString("year"),
                            type = o.getString("type"),
                            rating = o.optDouble("rating"),
                        )
                    )
                }
                favorites = list
            }
        } catch (_: Exception) {}
        try {
            val h = prefs.getString("history", null)
            if (h != null) {
                val arr = JSONArray(h)
                val list = mutableListOf<HistoryEntry>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        HistoryEntry(
                            tmdbId = o.getString("tmdbId"),
                            title = o.getString("title"),
                            poster = o.optString("poster").takeIf { it.isNotBlank() },
                            type = o.getString("type"),
                            watchedAt = o.getLong("watchedAt"),
                            season = if (o.has("season")) o.getInt("season") else null,
                            episode = if (o.has("episode")) o.getInt("episode") else null,
                        )
                    )
                }
                history = list
            }
        } catch (_: Exception) {}
    }

    private fun persist(key: String, json: JSONArray) {
        prefs.edit().putString(key, json.toString()).apply()
    }

    private fun key(e: ProgressEntry) =
        e.tmdbId + (e.season?.let { ":s$it" } ?: "") + (e.episode?.let { ":e$it" } ?: "")

    /* ------------------------- progress ------------------------- */

    fun saveProgress(media: Tmdb.Media, positionMs: Long, durationMs: Long, season: Int? = null, episode: Int? = null) {
        if (durationMs <= 0 || positionMs < 10_000L) return
        val entry = ProgressEntry(
            tmdbId = media.id.toString(),
            title = media.title,
            poster = media.poster,
            type = media.type,
            positionMs = positionMs,
            durationMs = durationMs,
            season = season,
            episode = episode,
            updatedAt = System.currentTimeMillis(),
        )
        progress = progress + (key(entry) to entry)
        val arr = JSONArray()
        for (e in progress.values) arr.put(progressJson(e))
        persist("progress", arr)
    }

    fun getProgress(media: Tmdb.Media, season: Int? = null, episode: Int? = null): ProgressEntry? =
        progress[key(ProgressEntry(media.id.toString(), "", null, "", 0, 0, season, episode, 0))]

    fun removeProgress(entry: ProgressEntry) {
        progress = progress - key(entry)
        val arr = JSONArray()
        for (e in progress.values) arr.put(progressJson(e))
        persist("progress", arr)
    }

    fun clearProgress(media: Tmdb.Media, season: Int? = null, episode: Int? = null) {
        val k = key(ProgressEntry(media.id.toString(), "", null, "", 0, 0, season, episode, 0))
        progress = progress - k
        val arr = JSONArray()
        for (e in progress.values) arr.put(progressJson(e))
        persist("progress", arr)
    }

    private fun progressJson(e: ProgressEntry) = JSONObject().apply {
        put("tmdbId", e.tmdbId); put("title", e.title); put("poster", e.poster ?: "")
        put("type", e.type); put("positionMs", e.positionMs); put("durationMs", e.durationMs)
        e.season?.let { put("season", it) }; e.episode?.let { put("episode", it) }
        put("updatedAt", e.updatedAt)
    }

    /* ------------------------- favorites ------------------------- */

    fun toggleFavorite(media: Tmdb.Media) {
        favorites = if (favorites.any { f -> f.id == media.id && f.type == media.type }) {
            favorites.filterNot { f -> f.id == media.id && f.type == media.type }
        } else {
            listOf(media) + favorites
        }
        persist("favorites", favoritesJson())
    }

    fun isFavorite(media: Tmdb.Media): Boolean =
        favorites.any { it.id == media.id && it.type == media.type }

    private fun favoritesJson(): JSONArray {
        val arr = JSONArray()
        favorites.forEach {
            arr.put(
                JSONObject().apply {
                    put("id", it.id); put("title", it.title); put("overview", it.overview)
                    put("poster", it.poster ?: ""); put("backdrop", it.backdrop ?: "")
                    put("year", it.year); put("type", it.type); put("rating", it.rating)
                }
            )
        }
        return arr
    }

    /* ------------------------- history ------------------------- */

    fun addHistory(media: Tmdb.Media, season: Int? = null, episode: Int? = null) {
        val entry = HistoryEntry(
            tmdbId = media.id.toString(),
            title = media.title,
            poster = media.poster,
            type = media.type,
            watchedAt = System.currentTimeMillis(),
            season = season,
            episode = episode,
        )
        history = (listOf(entry) + history.filterNot {
            it.tmdbId == entry.tmdbId && it.season == entry.season && it.episode == entry.episode
        }).take(60)
        val arr = JSONArray()
        history.forEach {
            arr.put(
                JSONObject().apply {
                    put("tmdbId", it.tmdbId); put("title", it.title); put("poster", it.poster ?: "")
                    put("type", it.type); put("watchedAt", it.watchedAt)
                    it.season?.let { s -> put("season", s) }; it.episode?.let { e -> put("episode", e) }
                }
            )
        }
        persist("history", arr)
    }
}
