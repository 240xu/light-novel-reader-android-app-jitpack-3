package io.legado.engine.model

/**
 * Minimal book data model for engine processing.
 * No Android/Room dependencies - pure Kotlin data class.
 */
data class Book(
    val bookUrl: String = "",
    val name: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val kind: String = "",
    val latestChapterTitle: String = "",
    val wordCount: String = "",
    val sourceUrl: String = ""
)
