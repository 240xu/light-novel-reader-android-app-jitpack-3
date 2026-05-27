plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

group = "io.github.240xu"
version = "1.0.4-core"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "io.github.240xu"
            artifactId = "legado-engine-core"
            version = "1.0.4-core"
            pom {
                name.set("Legado Engine Core")
                description.set("Pure Kotlin parsing engine for LNR plugin")
                url.set("https://github.com/240xu/light-novel-reader-android-app-jitpack-3")
            }
        }
    }
}

dependencies {
    api("org.jsoup:jsoup:1.16.1")
    api("com.jayway.jsonpath:json-path:2.8.0")
    api("org.mozilla:rhino:1.7.14")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    testImplementation("junit:junit:4.13.2")
}
