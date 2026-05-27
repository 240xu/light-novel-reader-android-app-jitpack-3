import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "io.github.dmzz_yyhyy.lnrplugin"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.dmzz_yyhyy.lnrplugin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("int", "PLUGIN_VERSION", "1")
        buildConfigField("String", "PLUGIN_NAME", "\"Legado Engine\"")
        buildConfigField("String", "PLUGIN_VERSION_NAME", "\"1.0.0\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach {
            val outputImpl = it as com.android.build.api.variant.impl.VariantOutputImpl
            val originalFileName = outputImpl.outputFileName.get()
            val newFileName = originalFileName.replace(".apk", ".apk.lnrp")
            outputImpl.outputFileName = newFileName
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.manifests.addStaticManifestFile(
            layout.buildDirectory.file("generated/ksp/${variant.name}/resources/auto_register_manifest.xml").get().toString()
        )
    }
}

afterEvaluate {
    listOf("Debug", "Release").forEach { variant ->
        tasks.findByName("process${variant}MainManifest")?.dependsOn("ksp${variant}Kotlin")
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.jsoup:jsoup:1.22.1")
    implementation("androidx.compose.runtime:runtime:1.10.3")
    implementation("androidx.navigation:navigation-runtime-ktx:2.9.7")
    implementation("org.mozilla:rhino:1.7.14")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(project(":legado-engine-core"))

    // LNR API
    compileOnly("io.nightfish.lightnovelreader:api:0.4-SNAPSHOT")
    ksp("io.nightfish.lightnovelreader:compiler:0.4-SNAPSHOT")
}


