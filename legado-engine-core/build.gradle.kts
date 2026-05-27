plugins {
    kotlin("jvm") version "1.9.22"
    `java-library`
    `maven-publish`
}

group = "io.github.240xu"
version = "1.0.0-core"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "io.github.240xu"
            artifactId = "legado-engine-core"
            version = "1.0.0-core"

            pom {
                name.set("Legado Engine Core")
                description.set("Pure Kotlin parsing engine extracted from Legado, zero Android dependencies.")
                url.set("https://github.com/240xu/light-novel-reader-android-app-jitpack-3")
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

dependencies {
    // HTML parsing
    api("org.jsoup:jsoup:1.16.1")
    // XPath parsing
    api("org.seaborne:jsoup-xpath:2.7.0")
    // JSON path parsing
    api("com.jayway.jsonpath:json-path:2.8.0")
    // JavaScript engine (Rhino)
    api("org.mozilla:rhino:1.7.14")
    // Kotlin coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // OkHttp for network
    api("com.squareup.okhttp3:okhttp:4.12.0")
    // Kotlin serialization for JSON
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
