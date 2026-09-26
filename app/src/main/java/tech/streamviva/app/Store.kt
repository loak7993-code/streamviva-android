package tech.streamviva.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local library + profiles.
 * Each profile owns its own progress/favorites/history.
 * Accent color is app-wide customizable.
 */
object Store {
    private lateinit var prefs: android.content.SharedPreferences

    /* --------------------------- profiles --------------------------- */

    data class Profile(
        val id: String,
        val name: String,
        val icon: String,     // emoji
        val colorA: String,   // hex
        val colorB: String,   // hex
    )

    var profiles by mutableStateOf<List<Profile>>(listOf(Profile("p1", "Me", "🎬", "#8D6BE0", "#5B3FA8")))
        private set
    var activeProfileId by mutableStateOf("p1")
        private set
    val active: Profile get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()

    /* ---------------------------- accent ---------------------------- */

    val ACCENTS = listOf(
        "Iris" to ("#8D6BE0" to "#5B3FA8"),
        "Gold" to ("#D9A44E" to "#8A5D24"),
        "Rose" to ("#D96B8F" to "#8A3B55"),
        "Cyan" to ("#4EB8D9" to "#2E6B8A"),
        "Lime" to ("#8FD94E" to "#558A2E"),
        "Sunset" to ("#D96E4E" to "#8A3E2E"),
    )

    var accentHex by mutableStateOf("#8D6BE0")
        private set
    var accentDeepHex by mutableStateOf("#5B3FA8")
        private set

    // dynamic accent colors (read these in composables)
    var accent by mutableStateOf(androidx.compose.ui.graphics.Color(0xFF8D6BE0))
    var accentSoft by mutableStateOf(androidx.compose.ui.graphics.Color(0xFFAF97EB))
    var accentDeep by mutableStateOf(androidx.compose.ui.graphics.Color(0xFF5B3FA8))

    fun setAccent(hexA: String, hexB: String) {
        accentHex = hexA; accentDeepHex = hexB
        accent = parse(hexA)
        accentSoft = lighten(hexA)
        accentDeep = parse(hexB)
        prefs.edit().putString("accent", "$hexA|$hexB").apply()
    }

    private fun parse(hex: String): androidx.compose.ui.graphics.Color =
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))

    private fun lighten(hex: String): androidx.compose.ui.graphics.Color {
        val c = android.graphics.Color.parseColor(hex)
        val r = (android.graphics.Color.red(c) + 40).coerceAtMost(255)
        val g = (android.graphics.Color.green(c) + 40).coerceAtMost(255)
        val b = (android.graphics.Color.blue(c) + 40).coerceAtMost(255)
        return androidx.compose.ui.graphics.Color(r, g, b, 255)
    }

    /* ------------------------- library state ------------------------- */

    data class ProgressEntry(
        val tmdbId: String, val title: String, val poster: String?, val type: String,
        val positionMs: Long, val durationMs: Long,
        val season: Int? = null, val episode: Int? = null, val updatedAt: Long,
    ) {
        val pct: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    }

    data class HistoryEntry(
        val tmdbId: String, val title: String, val poster: String?, val type: String,
        val watchedAt: Long, val season: Int? = null, val episode: Int? = null,
    )

    var progress by mutableStateOf<Map<String, ProgressEntry>>(emptyMap())
        private set
    var favorites by mutableStateOf<List<Tmdb.Media>>(emptyList())
        private set
    var history by mutableStateOf<List<HistoryEntry>>(emptyList())
        private set

    /* --------------------------- session --------------------------- */

    data class SavedSession(val token: String, val userId: String, val device: String)
    var session by mutableStateOf<SavedSession?>(null)
        private set

    var onboardingDone by mutableStateOf(false)
        private set

    /* --------------------------- settings --------------------------- */

    var autoplayNext by mutableStateOf(true)
    var showRatings by mutableStateOf(true)
    var skipProfilePicker by mutableStateOf(false)
    var defaultTab by mutableStateOf("HOME")

    /* ----------------------------- init ----------------------------- */

    fun init(context: Context) {
        prefs = context.getSharedPreferences("streamviva", Context.MODE_PRIVATE)

        // profiles
        try {
            val p = prefs.getString("profiles", null)
            if (p != null) {
                val arr = JSONArray(p)
                val list = mutableListOf<Profile>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(Profile(o.getString("id"), o.getString("name"), o.getString("icon"), o.getString("colorA"), o.getString("colorB")))
                }
                if (list.isNotEmpty()) profiles = list
            }
        } catch (_: Exception) {}
        activeProfileId = prefs.getString("activeProfile", profiles.first().id) ?: profiles.first().id

        // migrate legacy unprefixed data into the first profile once
        migrateLegacy()

        loadProfileData()

        // accent
        prefs.getString("accent", null)?.let {
            val parts = it.split("|")
            if (parts.size == 2) setAccent(parts[0], parts[1])
        }

        onboardingDone = prefs.getBoolean("onboardingDone", false)
        autoplayNext = prefs.getBoolean("autoplayNext", true)
        showRatings = prefs.getBoolean("showRatings", true)
        skipProfilePicker = prefs.getBoolean("skipProfilePicker", false)
        defaultTab = prefs.getString("defaultTab", "HOME") ?: "HOME"

        session = prefs.getString("sessionToken", null)?.let { t ->
            SavedSession(t, prefs.getString("sessionUserId", "") ?: "", prefs.getString("sessionDevice", "StreamViva Android") ?: "StreamViva Android")
        }
    }

    private fun migrateLegacy() {
        val legacyKeys = listOf("progress", "favorites", "history")
        val pid = profiles.first().id
        val hasPrefixed = prefs.contains("p:$pid:progress")
        val hasLegacy = legacyKeys.any { prefs.contains(it) }
        if (hasLegacy && !hasPrefixed) {
            val e = prefs.edit()
            for (k in legacyKeys) {
                val v = prefs.getString(k, null)
                if (v != null) e.putString("p:$pid:$k", v)
                e.remove(k)
            }
            e.apply()
        }
    }

    private fun loadProfileData() {
        val pid = activeProfileId
        progress = loadProgress("p:$pid:progress")
        favorites = loadFavorites("p:$pid:favorites")
        history = loadHistory("p:$pid:history")
    }

    /* ------------------------- profile mgmt ------------------------- */

    fun switchProfile(id: String) {
        activeProfileId = id
        prefs.edit().putString("activeProfile", id).apply()
        loadProfileData()
    }

    fun saveProfile(profile: Profile) {
        profiles = if (profiles.any { it.id == profile.id }) {
            profiles.map { if (it.id == profile.id) profile else it }
        } else {
            profiles + profile
        }
        persistProfiles()
        // sync to backend account if logged in (nickname + colors)
        session?.let { s ->
            MainScope().launch(Dispatchers.IO) {
                try { Auth.updateProfile(s.token, profile.name, profile.colorA, profile.colorB, profile.icon) } catch (_: Exception) {}
            }
        }
    }

    fun deleteProfile(id: String) {
        if (profiles.size <= 1) return
        profiles = profiles.filterNot { it.id == id }
        persistProfiles()
        prefs.edit().remove("p:$id:progress").remove("p:$id:favorites").remove("p:$id:history").apply()
        if (activeProfileId == id) switchProfile(profiles.first().id)
    }

    private fun persistProfiles() {
        val arr = JSONArray()
        profiles.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("name", it.name); put("icon", it.icon)
                put("colorA", it.colorA); put("colorB", it.colorB)
            })
        }
        prefs.edit().putString("profiles", arr.toString()).apply()
    }

    /* ------------------------- onboarding/session ------------------------- */

    fun completeOnboarding() {
        onboardingDone = true
        prefs.edit().putBoolean("onboardingDone", true).apply()
    }

    fun saveSession(s: SavedSession) {
        session = s
        prefs.edit()
            .putString("sessionToken", s.token)
            .putString("sessionUserId", s.userId)
            .putString("sessionDevice", s.device)
            .apply()
    }

    fun clearSession() {
        session = null
        prefs.edit().remove("sessionToken").remove("sessionUserId").remove("sessionDevice").apply()
    }

    fun updateAutoplayNext(v: Boolean) { autoplayNext = v; prefs.edit().putBoolean("autoplayNext", v).apply() }
    fun updateShowRatings(v: Boolean) { showRatings = v; prefs.edit().putBoolean("showRatings", v).apply() }
    fun updateSkipProfilePicker(v: Boolean) { skipProfilePicker = v; prefs.edit().putBoolean("skipProfilePicker", v).apply() }
    fun updateDefaultTab(v: String) { defaultTab = v; prefs.edit().putString("defaultTab", v).apply() }

    /* --------------------------- persistence --------------------------- */

    private fun persistList(key: String, json: JSONArray) {
        prefs.edit().putString(key, json.toString()).apply()
    }

    private fun key(e: ProgressEntry) =
        e.tmdbId + (e.season?.let { ":s$it" } ?: "") + (e.episode?.let { ":e$it" } ?: "")

    private fun loadProgress(key: String): Map<String, ProgressEntry> {
        val out = mutableMapOf<String, ProgressEntry>()
        try {
            val p = prefs.getString(key, null) ?: return out
            val arr = JSONArray(p)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val e = ProgressEntry(
                    o.getString("tmdbId"), o.getString("title"),
                    o.optString("poster").takeIf { it.isNotBlank() }, o.getString("type"),
                    o.getLong("positionMs"), o.getLong("durationMs"),
                    if (o.has("season")) o.getInt("season") else null,
                    if (o.has("episode")) o.getInt("episode") else null,
                    o.getLong("updatedAt"),
                )
                out[key(e)] = e
            }
        } catch (_: Exception) {}
        return out
    }

    private fun loadFavorites(key: String): List<Tmdb.Media> {
        val out = mutableListOf<Tmdb.Media>()
        try {
            val f = prefs.getString(key, null) ?: return out
            val arr = JSONArray(f)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Tmdb.Media(
                        o.getLong("id"), o.getString("title"), o.optString("overview"),
                        o.optString("poster").takeIf { it.isNotBlank() },
                        o.optString("backdrop").takeIf { it.isNotBlank() },
                        o.optString("year"), o.getString("type"), o.optDouble("rating"),
                    )
                )
            }
        } catch (_: Exception) {}
        return out
    }

    private fun loadHistory(key: String): List<HistoryEntry> {
        val out = mutableListOf<HistoryEntry>()
        try {
            val h = prefs.getString(key, null) ?: return out
            val arr = JSONArray(h)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    HistoryEntry(
                        o.getString("tmdbId"), o.getString("title"),
                        o.optString("poster").takeIf { it.isNotBlank() },
                        o.getString("type"), o.getLong("watchedAt"),
                        if (o.has("season")) o.getInt("season") else null,
                        if (o.has("episode")) o.getInt("episode") else null,
                    )
                )
            }
        } catch (_: Exception) {}
        return out
    }

    /* ------------------------- progress api ------------------------- */

    fun saveProgress(media: Tmdb.Media, positionMs: Long, durationMs: Long, season: Int? = null, episode: Int? = null) {
        if (durationMs <= 0 || positionMs < 10_000L) return
        val entry = ProgressEntry(
            media.id.toString(), media.title, media.poster, media.type,
            positionMs, durationMs, season, episode, System.currentTimeMillis(),
        )
        progress = progress + (key(entry) to entry)
        val arr = JSONArray()
        for (e in progress.values) arr.put(progressJson(e))
        persistList("p:$activeProfileId:progress", arr)
    }

    fun getProgress(media: Tmdb.Media, season: Int? = null, episode: Int? = null): ProgressEntry? =
        progress[key(ProgressEntry(media.id.toString(), "", null, "", 0, 0, season, episode, 0))]

    fun removeProgress(entry: ProgressEntry) {
        progress = progress - key(entry)
        val arr = JSONArray()
        for (e in progress.values) arr.put(progressJson(e))
        persistList("p:$activeProfileId:progress", arr)
    }

    fun clearAllProgress() {
        progress = emptyMap()
        persistList("p:$activeProfileId:progress", JSONArray())
    }

    private fun progressJson(e: ProgressEntry) = JSONObject().apply {
        put("tmdbId", e.tmdbId); put("title", e.title); put("poster", e.poster ?: "")
        put("type", e.type); put("positionMs", e.positionMs); put("durationMs", e.durationMs)
        e.season?.let { put("season", it) }; e.episode?.let { put("episode", it) }
        put("updatedAt", e.updatedAt)
    }

    /* ------------------------- favorites api ------------------------- */

    fun toggleFavorite(media: Tmdb.Media) {
        favorites = if (favorites.any { f -> f.id == media.id && f.type == media.type }) {
            favorites.filterNot { f -> f.id == media.id && f.type == media.type }
        } else {
            listOf(media) + favorites
        }
        persistList("p:$activeProfileId:favorites", favoritesJson())
    }

    fun isFavorite(media: Tmdb.Media): Boolean =
        favorites.any { f -> f.id == media.id && f.type == media.type }

    fun clearFavorites() {
        favorites = emptyList()
        persistList("p:$activeProfileId:favorites", JSONArray())
    }

    private fun favoritesJson(): JSONArray {
        val arr = JSONArray()
        for (f in favorites) {
            arr.put(
                JSONObject().apply {
                    put("id", f.id); put("title", f.title); put("overview", f.overview)
                    put("poster", f.poster ?: ""); put("backdrop", f.backdrop ?: "")
                    put("year", f.year); put("type", f.type); put("rating", f.rating)
                }
            )
        }
        return arr
    }

    /* ------------------------- history api ------------------------- */

    fun addHistory(media: Tmdb.Media, season: Int? = null, episode: Int? = null) {
        val entry = HistoryEntry(
            media.id.toString(), media.title, media.poster, media.type,
            System.currentTimeMillis(), season, episode,
        )
        history = (listOf(entry) + history.filterNot {
            it.tmdbId == entry.tmdbId && it.season == entry.season && it.episode == entry.episode
        }).take(60)
        val arr = JSONArray()
        for (h in history) {
            arr.put(
                JSONObject().apply {
                    put("tmdbId", h.tmdbId); put("title", h.title); put("poster", h.poster ?: "")
                    put("type", h.type); put("watchedAt", h.watchedAt)
                    h.season?.let { s -> put("season", s) }; h.episode?.let { e -> put("episode", e) }
                }
            )
        }
        persistList("p:$activeProfileId:history", arr)
    }

    fun clearHistory() {
        history = emptyList()
        persistList("p:$activeProfileId:history", JSONArray())
    }
}
