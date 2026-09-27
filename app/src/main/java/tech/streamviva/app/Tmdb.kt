package tech.streamviva.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Session TMDB cache — rows load instantly on tab switches and
 * back-navigation; first load kicks off in parallel and is shared
 * with the splash so the app renders the moment data lands.
 */
object TmdbCache {
    private val mem = ConcurrentHashMap<String, Any>()
    private val inflight = ConcurrentHashMap<String, Deferred<Any>>()
    private val scope = MainScope()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> cached(key: String, fetcher: suspend () -> T): T {
        try {
            (mem[key] as? T)?.let { return it }
            @Suppress("UNCHECKED_CAST")
            val deferred = inflight.getOrPut(key) {
                scope.async<Any> {
                    try {
                        val v = fetcher()
                        mem[key] = v as Any
                        v
                    } finally {
                        inflight.remove(key)
                    }
                }
            }
            return deferred.await() as T
        } catch (e: Exception) {
            // cache machinery failed — fall through to a direct fetch
            return fetcher()
        }
    }

    fun clear() {
        mem.clear()
    }
}

/** Native TMDB client — browse/search/details/discover/credits. */
object Tmdb {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val TOKEN =
        "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJlN2NjZjNhZDYyN2M4ZTA3MmQ3NjQ3YWFlNDRmNGU3ZiIsIm5iZiI6MTc3Mjg1MjkxMS40MjcsInN1YiI6IjY5YWI5NmFmNzgyNzRlMTFmMThmYWYxOCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.bUX3sQru8sCYqTKO-SB9YqXfLvoa88bwc9g3AJdfst8"

    private const val API = "https://api.themoviedb.org/3"

    data class Media(
        val id: Long,
        val title: String,
        val overview: String,
        val poster: String?,
        val backdrop: String?,
        val year: String,
        val type: String,
        val rating: Double,
        var _imdb: String = "",
    )

    data class Season(val number: Int, val id: Long, val episodeCount: Int)
    data class Genre(val id: Int, val name: String)

    data class Episode(
        val number: Int,
        val name: String,
        val still: String?,
        val imdbId: String?,
        val overview: String,
        val rating: Double,
    )

    data class CastMember(
        val name: String,
        val character: String,
        val profile: String?,
    )

    data class DetailsResult(
        val imdb: String,
        val seasons: List<Season>,
        val genres: List<Genre>,
        val tagline: String?,
    )

    private fun get(path: String, params: Map<String, String> = emptyMap()): JSONObject {
        val url = buildString {
            append(API).append(path)
            if (params.isNotEmpty()) {
                append('?')
                append(params.entries.joinToString("&") {
                    "${java.net.URLEncoder.encode(it.key, "UTF-8")}=${java.net.URLEncoder.encode(it.value, "UTF-8")}"
                })
            }
        }
        val req = Request.Builder().url(url)
            .header("Authorization", "Bearer $TOKEN")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("tmdb http ${res.code}")
            return JSONObject(res.body!!.string())
        }
    }

    private fun parseList(j: JSONObject, type: String): List<Media> {
        val arr = j.optJSONArray("results") ?: org.json.JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val date = o.optString(if (type == "tv") "first_air_date" else "release_date")
            Media(
                id = o.getLong("id"),
                title = o.optString(if (type == "tv") "name" else "title", "?"),
                overview = o.optString("overview", ""),
                poster = o.optString("poster_path").takeIf { it.isNotBlank() && it != "null" },
                backdrop = o.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" },
                year = date.take(4),
                type = type,
                rating = o.optDouble("vote_average", 0.0),
            )
        }
    }

    // cached row data — instant on tab switch, single flight on first load
    suspend fun trendingMovies(): List<Media> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        TmdbCache.cached("trendM") { parseList(get("/trending/movie/week"), "movie") }
    }
    suspend fun trendingTv(): List<Media> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        TmdbCache.cached("trendT") { parseList(get("/trending/tv/week"), "tv") }
    }
    suspend fun popularMovies(): List<Media> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        TmdbCache.cached("popularM") { parseList(get("/movie/popular"), "movie") }
    }
    suspend fun topRatedTv(): List<Media> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        TmdbCache.cached("topTv") { parseList(get("/tv/top_rated"), "tv") }
    }
    suspend fun airingToday(): List<Media> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        TmdbCache.cached("airing") { parseList(get("/tv/airing_today"), "tv") }
    }
    suspend fun upcomingMovies(): List<Media> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        TmdbCache.cached("upcoming") { parseList(get("/movie/upcoming"), "movie") }
    }

    suspend fun search(query: String): List<Media> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val j = get("/search/multi", mapOf("query" to query, "include_adult" to "false"))
        val arr = j.optJSONArray("results") ?: org.json.JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val mt = o.optString("media_type")
            if (mt != "movie" && mt != "tv") return@mapNotNull null
            val date = o.optString(if (mt == "tv") "first_air_date" else "release_date")
            Media(
                id = o.getLong("id"),
                title = o.optString(if (mt == "tv") "name" else "title", "?"),
                overview = o.optString("overview", ""),
                poster = o.optString("poster_path").takeIf { it.isNotBlank() && it != "null" },
                backdrop = o.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" },
                year = date.take(4),
                type = mt,
                rating = o.optDouble("vote_average", 0.0),
            )
        }
    }

    /** fetch a Media by type + tmdb id (for continue watching / history) */
    suspend fun byId(type: String, tmdbId: Long): Media? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val j = get("/$type/$tmdbId")
            val date = j.optString(if (type == "tv") "first_air_date" else "release_date")
            Media(
                id = tmdbId,
                title = j.optString(if (type == "tv") "name" else "title", "?"),
                overview = j.optString("overview", ""),
                poster = j.optString("poster_path").takeIf { it.isNotBlank() && it != "null" },
                backdrop = j.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" },
                year = date.take(4),
                type = type,
                rating = j.optDouble("vote_average", 0.0),
            )
        } catch (e: Exception) { null }
    }

    /** imdb id + seasons + genres. TMDB stopped returning imdb_id on /tv/{id}
     *  for shows — fall back to /external_ids when it's blank. */
    suspend fun details(type: String, tmdbId: Long): DetailsResult =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            val j = get("/$type/$tmdbId")
            var imdb = j.optString("imdb_id", "")
            if (imdb.isBlank()) {
                try {
                    val ext = get("/$type/$tmdbId/external_ids")
                    imdb = ext.optString("imdb_id", "")
                } catch (_: Exception) {}
            }
            val seasons = mutableListOf<Season>()
            if (type == "tv") {
                val arr = j.optJSONArray("seasons") ?: org.json.JSONArray()
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    val sn = s.optInt("season_number")
                    if (sn > 0) seasons.add(Season(sn, s.getLong("id"), s.optInt("episode_count")))
                }
            }
            val genres = mutableListOf<Genre>()
            val garr = j.optJSONArray("genres") ?: org.json.JSONArray()
            for (i in 0 until garr.length()) {
                val g = garr.getJSONObject(i)
                genres.add(Genre(g.getInt("id"), g.optString("name", "")))
            }
            DetailsResult(
                imdb,
                seasons,
                genres,
                j.optString("tagline").takeIf { it.isNotBlank() && it != "null" },
            )
        }

    /** full episodes with names + stills */
    suspend fun episodes(tvId: Long, seasonNumber: Int): List<Episode> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            val j = get("/tv/$tvId/season/$seasonNumber")
            val arr = j.optJSONArray("episodes") ?: org.json.JSONArray()
            (0 until arr.length()).map { i ->
                val e = arr.getJSONObject(i)
                Episode(
                    number = e.optInt("episode_number"),
                    name = e.optString("name", "Episode"),
                    still = e.optString("still_path").takeIf { it.isNotBlank() && it != "null" },
                    imdbId = e.optString("imdb_id").takeIf { it.isNotBlank() && it != "null" },
                    overview = e.optString("overview", ""),
                    rating = e.optDouble("vote_average", 0.0),
                )
            }
        }

    /** more like this */
    suspend fun recommendations(type: String, tmdbId: Long): List<Media> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                parseList(get("/$type/$tmdbId/recommendations"), type)
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** top cast */
    suspend fun credits(type: String, tmdbId: Long): List<CastMember> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val j = get("/$type/$tmdbId/credits")
                val arr = j.optJSONArray("cast") ?: org.json.JSONArray()
                (0 until minOf(arr.length(), 15)).map { i ->
                    val c = arr.getJSONObject(i)
                    CastMember(
                        name = c.optString("name", ""),
                        character = c.optString("character", ""),
                        profile = c.optString("profile_path").takeIf { it.isNotBlank() && it != "null" },
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** genre lists */
    private var movieGenres: List<Genre> = emptyList()
    private var tvGenres: List<Genre> = emptyList()

    suspend fun genres(type: String): List<Genre> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val cachedGenres = if (type == "tv") tvGenres else movieGenres
        if (cachedGenres.isNotEmpty()) return@withContext cachedGenres
        try {
            val j = get("/genre/$type/list")
            val arr = j.optJSONArray("genres") ?: org.json.JSONArray()
            val list = (0 until arr.length()).map { i ->
                val g = arr.getJSONObject(i)
                Genre(g.getInt("id"), g.getString("name"))
            }
            if (type == "tv") tvGenres = list else movieGenres = list
            list
        } catch (e: Exception) { emptyList() }
    }
}
