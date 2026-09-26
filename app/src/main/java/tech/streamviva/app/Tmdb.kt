package tech.streamviva.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Native TMDB client — browse/search/details. */
object Tmdb {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // same public read token the deployed instance uses
    private const val TOKEN =
        "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJlN2NjZjNhZDYyN2M4ZTA3MmQ3NjQ3YWFlNDRmNGU3ZiIsIm5iZiI6MTc3Mjg1MjkxMS40MjcsInN1YiI6IjY5YWI5NmFmNzgyNzRlMTFmMThmYWYxOCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.bUX3sQru8sCYqTKO-SB9YqXfLvoa88bwc9g3AJdfst8"

    private const val API = "https://api.themoviedb.org/3"
    const val IMG = "https://image.tmdb.org/t/p/w500"

    data class Media(
        val id: Long,
        val title: String,
        val overview: String,
        val poster: String?,
        val backdrop: String?,
        val year: String,
        val type: String, // "movie" | "tv"
        val rating: Double,
    )

    private fun get(path: String, params: Map<String, String> = emptyMap()): JSONObject =
        withContextBridge(
            Request.Builder()
                .url(
                    buildString {
                        append(API).append(path)
                        if (params.isNotEmpty()) {
                            append('?')
                            append(params.entries.joinToString("&") {
                                "${java.net.URLEncoder.encode(it.key, "UTF-8")}=${java.net.URLEncoder.encode(it.value, "UTF-8")}"
                            })
                        }
                    },
                )
                .header("Authorization", "Bearer $TOKEN")
                .header("Accept", "application/json")
                .build(),
        )

    private fun withContextBridge(req: Request): JSONObject {
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("tmdb http ${res.code}")
            return JSONObject(res.body!!.string())
        }
    }

    suspend fun trendingMovies(): List<Media> = list("/trending/movie/week")
    suspend fun trendingTv(): List<Media> = list("/trending/tv/week")
    suspend fun popularMovies(): List<Media> = list("/movie/popular")
    suspend fun topRatedTv(): List<Media> = list("/tv/top_rated")

    private suspend fun list(path: String): List<Media> = withContext(Dispatchers.IO) {
        val j = get(path)
        val arr = j.optJSONArray("results") ?: org.json.JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val type = if (path.contains("/tv")) "tv" else "movie"
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

    suspend fun search(query: String): List<Media> = withContext(Dispatchers.IO) {
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

    data class Season(val number: Int, val id: Long, val episodeCount: Int)

    /** returns imdb id + seasons (tv only) */
    suspend fun details(type: String, tmdbId: Long): Pair<String, List<Season>> =
        withContext(Dispatchers.IO) {
            val j = get("/$type/$tmdbId", mapOf("language" to "en-US"))
            val imdb = j.optString("imdb_id", "")
            val seasons = mutableListOf<Season>()
            if (type == "tv") {
                val arr = j.optJSONArray("seasons") ?: org.json.JSONArray()
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    val sn = s.optInt("season_number")
                    if (sn > 0) seasons.add(Season(sn, s.getLong("id"), s.optInt("episode_count")))
                }
            }
            imdb to seasons
        }

    /** returns episode imdb ids for a season (needed for per-episode streams) */
    suspend fun episodes(tvId: Long, seasonNumber: Int): List<Pair<Int, String>> =
        withContext(Dispatchers.IO) {
            val j = get("/tv/$tvId/season/$seasonNumber")
            val arr = j.optJSONArray("episodes") ?: org.json.JSONArray()
            (0 until arr.length()).mapNotNull { i ->
                val e = arr.getJSONObject(i)
                val imdb = e.optString("imdb_id").takeIf { it.isNotBlank() && it != "null" }
                    ?: return@mapNotNull null
                e.optInt("episode_number") to imdb
            }
        }
}
