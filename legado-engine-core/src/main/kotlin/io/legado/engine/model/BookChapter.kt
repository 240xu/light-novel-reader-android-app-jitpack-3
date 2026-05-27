package io.legado.engine.model

/**
 * Chapter data model for engine processing.
 */
data class BookChapter(
    val url: String = "",
    val title: String = "",
    val volume: String = "",
    val isVip: Boolean = false,
    val isPay: Boolean = false,
    val order: Int = 0
)
