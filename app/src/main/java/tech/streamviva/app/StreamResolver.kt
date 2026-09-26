package tech.streamviva.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Native resolver for vidsrc.sh — the universal stream source.
 * Chain: api.php -> encrypted urls + WASM -> Chicory decrypt ->
 * mirror host -> IP-bound JWT via /generate.php -> HLS master.
 */
object StreamResolver {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private const val API = "https://data.vidsrc.sh/api.php"
    const val SITE = "https://streamviva.satisfying-discovery.workers.dev"

    data class Resolved(
        val title: String,
        val masterUrl: String,
        val host: String,
    )

    suspend fun resolveMovie(imdbId: String): Resolved =
        resolve(mapOf("type" to "movie", "imdb" to imdbId, "stream_urls" to ""))

    suspend fun resolveShow(imdbId: String, season: Int, episode: Int): Resolved =
        resolve(
            mapOf(
                "type" to "tv",
                "imdb" to imdbId,
                "season" to season.toString(),
                "episode" to episode.toString(),
                "stream_urls" to "",
            )
        )

    private suspend fun resolve(params: Map<String, String>): Resolved =
        withContext(Dispatchers.IO) {
            val url = buildString {
                append(API)
                append('?')
                append(params.entries.joinToString("&") {
                    "${java.net.URLEncoder.encode(it.key, "UTF-8")}=${java.net.URLEncoder.encode(it.value, "UTF-8")}"
                })
            }
            val body = fetchJson(url)

            val data = body.getJSONObject("data")
            val title = data.optString("title", "")
            val su = data.opt("stream_urls")

            val urls: List<String> = when (su) {
                is JSONArray -> (0 until su.length()).map { su.getString(it) }.filter { it.isNotBlank() }
                is String -> {
                    if (su.isBlank()) error("no stream urls")
                    val vs = body.getJSONObject("vs")
                    val wasmUrl = vs.optString("wasm_url")
                    val wasmB64 = vs.optString("wasm")
                    val wasm = if (wasmUrl.isNotBlank()) fetchBytes(wasmUrl) else b64decode(wasmB64)
                    val plain = WasmDecryptor.decrypt(wasm, b64decode(su))
                    String(plain, Charsets.UTF_8).split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                }
                else -> error("no stream urls")
            }
            if (urls.isEmpty()) error("no mirrors")

            // walk mirrors until one issues an IP-bound token
            var result: Resolved? = null
            loop@ for (raw in urls) {
                if (result != null) break@loop
                val host = try {
                    java.net.URI(raw).let { "${it.scheme}://${it.host}" }
                } catch (e: Exception) {
                    continue@loop
                }
                val token = fetchToken(host)
                if (token.isNotBlank()) {
                    val master = if (raw.contains("__TOKEN__")) raw.replace("__TOKEN__", token)
                    else raw + (if (raw.contains('?')) "&" else "?") + "token=" + token
                    result = Resolved(title, master, host)
                }
            }
            // fall back to first mirror raw
            result ?: Resolved(title, urls[0], "")
        }

    private fun fetchJson(url: String): JSONObject {
        val req = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("api http ${res.code}")
            return JSONObject(res.body!!.string())
        }
    }

    private fun fetchBytes(url: String): ByteArray {
        val req = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("wasm http ${res.code}")
            return res.body!!.bytes()
        }
    }

    private fun fetchToken(host: String): String {
        try {
            // short-lived IP-bound tokens; retry once on rate limit
            for (attempt in 0 until 2) {
                val req = Request.Builder().url("$host/generate.php")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    .build()
                val token: String? = client.newCall(req).execute().use { res ->
                    when {
                        res.code == 429 || res.code >= 500 -> null
                        !res.isSuccessful -> ""
                        else -> {
                            var t = res.body!!.string().trim()
                            if (t.startsWith("{") || t.startsWith("[")) {
                                t = try {
                                    val j = JSONObject(t)
                                    when (val v = j.opt("token") ?: j.opt("data") ?: j.opt("string") ?: j.opt("result")) {
                                        is String -> v
                                        else -> ""
                                    }
                                } catch (e: Exception) { "" }
                            }
                            if (t.startsWith("<")) "" else t
                        }
                    }
                }
                if (token != null) return token
                Thread.sleep(1200L * (attempt + 1))
            }
        } catch (e: Exception) {
        }
        return ""
    }

    private fun b64decode(s: String): ByteArray {
        return android.util.Base64.decode(
            s.replace("-", "+").replace("_", "/"),
            android.util.Base64.DEFAULT,
        )
    }
}
