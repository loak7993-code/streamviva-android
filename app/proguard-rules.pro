# okhttp
-dontwarn okhttp3.**
-dontwarn okio.**
# bouncycastle (accounts crypto)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
# chicory wasm runtime (reflection-free but keep names)
-keep class com.dylibso.chicory.** { *; }
-dontwarn com.dylibso.chicory.**
# media3
-dontwarn androidx.media3.**
