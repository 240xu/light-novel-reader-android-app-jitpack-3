plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "io.legado.engine"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "io.github.240xu"
                artifactId = "legado-engine-core"
                version = "1.0.0-core"

                pom {
                    name.set("Legado Engine Core")
                    description.set("Pure Kotlin parsing engine extracted from Legado, zero Android dependencies.")
                    url.set("https://github.com/240xu/legado")
                    licenses {
                        license {
                            name.set("GNU General Public License v3.0")
                            url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                        }
                    }
                }
            }
        }
    }
}

dependencies {
    // === Core parsing dependencies (matching lyc486 versions) ===
    // HTML parsing
    implementation("org.jsoup:jsoup:1.16.1")
    // XPath parsing
    implementation("org.seaborne:jsoup-xpath:2.7.0")
    // JSON path parsing
    implementation("com.jayway.jsonpath:json-path:2.8.0")
    // JavaScript engine (Rhino)
    implementation("org.mozilla:rhino:1.7.14")
    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // OkHttp for network (lightweight, no Android dependency)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Kotlin serialization for JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
