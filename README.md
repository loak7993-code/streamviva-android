# StreamViva for Android

Fully native Android streaming client: Kotlin + Jetpack Compose, Media3/ExoPlayer for playback, zero WebView. Browse TMDB, resolve streams natively, and watch with subtitles.

Part of the StreamViva family: [streamviva](https://github.com/loak7993-code/streamviva) (web) · [streamviva-webos](https://github.com/loak7993-code/streamviva-webos) (LG TVs) · [streamviva-providers](https://github.com/loak7993-code/streamviva-providers) (sources).

## Install

Grab the signed APK from [Releases](https://github.com/loak7993-code/streamviva-android/releases). Sideload: allow "install unknown apps" for your browser or file manager, then open the APK. Requires Android 8.0 (API 26) or newer.

## What it does

- **TMDB browsing** — trending, popular, top rated, search, with posters and backdrops through Coil
- **Native playback** — Media3/ExoPlayer with the HLS module, no WebView involved anywhere
- **Native stream resolution** — vidsrc.sh streams resolved in-app; the decryptor runs on [Chicory](https://github.com/dylibso/chicory), a pure-Java WASM runtime, so no JS bridge exists
- **Subtitles** — external subtitle loading in the player
- **Downloads** — download manager with progress UI for offline watching
- **Accounts** — ed25519 keypairs via BouncyCastle, sync across devices
- **Profiles and settings** — avatars, theme, playback preferences, all stored locally

## Build

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Requirements: JDK 17, Android SDK (compileSdk 34, minSdk 26). Release builds sign with the repo keystore config in `app/build.gradle.kts`.

## Stack

| piece | what it is |
| --- | --- |
| UI | Jetpack Compose (Material 3), compose BOM 2024.05.00 |
| player | androidx.media3 ExoPlayer 1.3.1 + HLS |
| network | OkHttp 4.12 |
| WASM | Chicory runtime 1.7.5 (pure Java, no JNI) |
| crypto | BouncyCastle 1.78.1 (ed25519, pbkdf2) |
| async | kotlinx-coroutines 1.8.1 |

## License

MIT.
