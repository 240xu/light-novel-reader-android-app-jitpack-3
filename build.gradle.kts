plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.10" apply false
}

// Android plugins are only available when building the full project locally
plugins.withId("com.android.application") {
    // already applied
}
