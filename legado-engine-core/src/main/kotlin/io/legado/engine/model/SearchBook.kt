package io.legado.engine.model

/**
 * Search result item model.
 */
data class SearchBook(
    val bookUrl: String = "",
    val name: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val kind: String = "",
    val latestChapterTitle: String = "",
    val sourceUrl: String = "",
    val originName: String = ""
)
