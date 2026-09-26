package tech.streamviva.app

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.Parser

/**
 * Native runner for the vidsrc.sh stream-urls decryptor.
 *
 * The stream API hands back an encrypted `stream_urls` blob plus a
 * per-5-minute-window WASM module (exports: alloc, decrypt, memory).
 * We execute that exact WASM bytecode natively with Chicory — no
 * WebView, no JS engine.
 */
object WasmDecryptor {

    fun decrypt(wasmBytes: ByteArray, encrypted: ByteArray): ByteArray {
        val module = Parser.parse(wasmBytes)
        val instance = Instance.builder(module).build()
        val memory = instance.memory()
        val alloc = instance.export("alloc")
        val decrypt = instance.export("decrypt")

        // allocate and copy ciphertext into wasm memory
        val ptr = alloc.apply(encrypted.size.toLong())[0].toInt()
        memory.write(ptr, encrypted)

        // run the decryptor: returns plaintext length
        val outLen = decrypt.apply(ptr.toLong(), encrypted.size.toLong())[0].toInt()

        // plaintext lives at ptr+12 (12-byte header/nonce prefix)
        return memory.readBytes(ptr + 12, outLen)
    }
}
