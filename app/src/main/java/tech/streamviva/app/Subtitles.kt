package tech.streamviva.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Subtitles via VDRK — https://sub.vdrk.site
 *   movie: /v1/movie/{tmdbId}
 *   tv:    /v1/tv/{tmdbId}/{season}/{episode}
 * Returns direct VTT URLs, no key needed.
 */
object Subtitles {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class Subtitle(
        val label: String,       // display: "English", "Arabic (2)"
        val language: String,    // full name
        val code: String,        // ISO 639-1 for player matching
        val url: String,
        val hearingImpaired: Boolean,
    )

    suspend fun fetch(type: String, tmdbId: Long, season: Int? = null, episode: Int? = null): List<Subtitle> =
        withContext(Dispatchers.IO) {
            try {
                val url = if (season != null && episode != null) {
                    "https://sub.vdrk.site/v1/tv/$tmdbId/$season/$episode"
                } else {
                    "https://sub.vdrk.site/v1/movie/$tmdbId"
                }
                val req = Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    .build()
                client.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return@withContext emptyList()
                    val arr = JSONArray(res.body!!.string())
                    val raw = mutableListOf<Subtitle>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val file = o.optString("file").takeIf { it.isNotBlank() } ?: continue
                        val label = o.optString("label").takeIf { it.isNotBlank() } ?: continue
                        val hi = label.contains("Hi")
                        // strip numbering/Hi suffix for the language name
                        val langName = label
                            .replace(Regex("\\s*Hi\\d*\\s*$"), "")
                            .replace(Regex("\\s*\\d+$"), "")
                            .trim()
                        val code = LANG_CODES[langName.lowercase()] ?: continue
                        raw.add(Subtitle(label, langName, code, file, hi))
                    }
                    // one entry per language: prefer non-HI, keep first
                    val seen = mutableMapOf<String, Subtitle>()
                    for (s in raw.sortedBy { it.hearingImpaired }) {
                        if (!seen.containsKey(s.language)) seen[s.language] = s
                    }
                    seen.values.sortedBy { it.language }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** common language name -> ISO 639-1 */
    val LANG_CODES: Map<String, String> = mapOf(
        "english" to "en", "arabic" to "ar", "albanian" to "sq", "bulgarian" to "bg",
        "croatian" to "hr", "czech" to "cs", "danish" to "da", "dutch" to "nl",
        "estonian" to "et", "finnish" to "fi", "french" to "fr", "german" to "de",
        "greek" to "el", "hebrew" to "he", "hindi" to "hi", "hungarian" to "hu",
        "icelandic" to "is", "indonesian" to "id", "italian" to "it", "japanese" to "ja",
        "korean" to "ko", "latvian" to "lv", "lithuanian" to "lt", "macedonian" to "mk",
        "malay" to "ms", "norwegian" to "no", "polish" to "pl", "portuguese" to "pt",
        "brazilian portuguese" to "pt-BR", "romanian" to "ro", "russian" to "ru",
        "serbian" to "sr", "slovak" to "sk", "slovenian" to "sl", "spanish" to "es",
        "latin american spanish" to "es-419", "swedish" to "sv", "thai" to "th",
        "turkish" to "tr", "ukrainian" to "uk", "vietnamese" to "vi", "persian" to "fa",
        "farsi" to "fa", "bengali" to "bn", "urdu" to "ur", "tamil" to "ta",
        "telugu" to "te", "chinese" to "zh", "simplified chinese" to "zh-Hans",
        "traditional chinese" to "zh-Hant", "cantonese" to "yue", "catalan" to "ca",
        "basque" to "eu", "galician" to "gl", "welsh" to "cy", "irish" to "ga",
        "afrikaans" to "af", "swahili" to "sw", "armenian" to "hy", "azerbaijani" to "az",
        "georgian" to "ka", "kazakh" to "kk", "mongolian" to "mn", "nepali" to "ne",
        "sinhala" to "si", "somali" to "so", "tagalog" to "tl", "uzbek" to "uz",
        "esperanto" to "eo", "bosnian" to "bs", "montenegrin" to "sr-ME",
    )
}
