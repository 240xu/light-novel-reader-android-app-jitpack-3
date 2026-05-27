package io.legado.engine.model

/**
 * Exploration page result item.
 */
data class ExploreBook(
    val bookUrl: String = "",
    val name: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val kind: String = "",
    val latestChapterTitle: String = ""
)
