package tech.streamviva.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Native account auth — the same movie-web protocol the website uses,
 * against the same backend. Passphrase -> PBKDF2 -> Ed25519.
 */
object Auth {
    const val BACKEND = "https://streamviva.satisfying-discovery.workers.dev"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class Session(
        val token: String,
        val userId: String,
        val device: String,
    )

    /* ------------------------------ crypto ------------------------------ */

    fun generateMnemonic(wordlist: List<String>): String {
        val entropy = ByteArray(16) // 128 bits -> 12 words
        SecureRandom().nextBytes(entropy)
        // entropy bits -> word indices (11 bits each)
        val bits = StringBuilder()
        for (b in entropy) bits.append(Integer.toBinaryString(b.toInt() and 0xFF).padStart(8, '0'))
        // checksum (4 bits): sha256 first byte high nibble
        val checksum = java.security.MessageDigest.getInstance("SHA-256").digest(entropy)[0].toInt() shr 4 and 0xF
        bits.append(Integer.toBinaryString(checksum).padStart(4, '0'))
        val words = mutableListOf<String>()
        for (i in 0 until 12) {
            val idx = Integer.parseInt(bits.substring(i * 11, i * 11 + 11), 2)
            words.add(wordlist[idx])
        }
        return words.joinToString(" ")
    }

    fun loadWordlist(context: Context): List<String> =
        context.assets.open("bip39-english.txt").bufferedReader().readLines().filter { it.isNotBlank() }

    /** passphrase -> 32-byte seed (identical to the web: pbkdf2-sha256, 2048 iters, salt "mnemonic") */
    fun seedFromMnemonic(mnemonic: String): ByteArray {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(
            mnemonic.toCharArray(),
            "mnemonic".toByteArray(Charsets.UTF_8),
            2048,
            256,
        )
        return factory.generateSecret(spec).encoded
    }

    data class Keys(val privateKey: ByteArray, val publicKey: ByteArray)

    fun keysFromSeed(seed: ByteArray): Keys {
        val priv = Ed25519PrivateKeyParameters(seed, 0)
        val pub = priv.generatePublicKey()
        return Keys(priv.encoded, pub.encoded)
    }

    fun sign(privateKey: ByteArray, message: String): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(privateKey, 0))
        signer.update(message.toByteArray(Charsets.UTF_8), 0, message.toByteArray(Charsets.UTF_8).size)
        return signer.generateSignature()
    }

    private fun b64url(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)

    /* ------------------------------- api ------------------------------- */

    private fun post(path: String, body: JSONObject): JSONObject {
        val req = Request.Builder()
            .url(BACKEND + path)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody())
            .build()
        client.newCall(req).execute().use { res ->
            val text = res.body?.string() ?: ""
            if (!res.isSuccessful) {
                val msg = try { JSONObject(text).optString("error", "http ${res.code}") } catch (e: Exception) { "http ${res.code}" }
                error(msg)
            }
            return JSONObject(text)
        }
    }

    data class AccountResult(val userId: String, val token: String, val profileColorA: String)

    suspend fun register(mnemonic: String, device: String, colorA: String, colorB: String, icon: String): AccountResult =
        withContext(Dispatchers.IO) {
            val keys = keysFromSeed(seedFromMnemonic(mnemonic))

            // 1. challenge
            val start = post("/auth/register/start", JSONObject().put("captchaToken", JSONObject.NULL))
            val challenge = start.getString("challenge")

            // 2. sign + complete
            val sig = sign(keys.privateKey, challenge)
            val complete = post(
                "/auth/register/complete",
                JSONObject()
                    .put("namespace", "movie-web")
                    .put("publicKey", b64url(keys.publicKey))
                    .put("challenge", JSONObject().put("code", challenge).put("signature", b64url(sig)))
                    .put("device", device)
                    .put("profile", JSONObject().put("colorA", colorA).put("colorB", colorB).put("icon", icon)),
            )
            AccountResult(
                userId = complete.getJSONObject("user").getString("id"),
                token = complete.getString("token"),
                profileColorA = complete.getJSONObject("user").getJSONObject("profile").optString("colorA", colorA),
            )
        }

    suspend fun login(mnemonic: String, device: String): AccountResult =
        withContext(Dispatchers.IO) {
            val keys = keysFromSeed(seedFromMnemonic(mnemonic))
            val pub = b64url(keys.publicKey)

            val start = post("/auth/login/start", JSONObject().put("publicKey", pub))
            val challenge = start.getString("challenge")

            val sig = sign(keys.privateKey, challenge)
            val complete = post(
                "/auth/login/complete",
                JSONObject()
                    .put("namespace", "movie-web")
                    .put("publicKey", pub)
                    .put("challenge", JSONObject().put("code", challenge).put("signature", b64url(sig)))
                    .put("device", device),
            )
            AccountResult(
                userId = complete.getJSONObject("session").getString("userId"),
                token = complete.getString("token"),
                profileColorA = "",
            )
        }

    /** validate the stored token */
    suspend fun me(token: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$BACKEND/users/@me")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                JSONObject(res.body?.string() ?: return@withContext null)
            }
        } catch (e: Exception) { null }
    }
}
