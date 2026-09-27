plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "tech.streamviva.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "tech.streamviva.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 32
        versionName = "2.8.1"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../streamviva.keystore")
            storePassword = "streamviva2026"
            keyAlias = "streamviva"
            keyPassword = "streamviva2026"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += "META-INF/*"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // video player — native HLS
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // images
    implementation("io.coil-kt:coil-compose:2.6.0")

    // WASM runtime (pure java) — runs the vidsrc decryptor natively
    implementation("com.dylibso.chicory:runtime:1.7.5")

    // crypto — ed25519 + pbkdf2 for accounts
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
