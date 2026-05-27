plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "io.github.dmzz_yyhyy.lnrplugin"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // === LNR Plugin API (provided by host) ===
    compileOnly("io.github.240xu:LightNovelReader-Plugin-API:1.0.0")

    // === Legado Engine Core (from JitPack) ===
    implementation("io.github.240xu:legado-engine-core:1.0.0-core")

    // === Kotlin ===
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // === OkHttp (for network within plugin) ===
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // === Gson (for source JSON parsing) ===
    implementation("com.google.code.gson:gson:2.10.1")
}
