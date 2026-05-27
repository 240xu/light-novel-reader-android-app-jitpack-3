package io.github.dmzz_yyhyy.lnrplugin.adapter

import io.github.dmzz_yyhyy.lnrplugin.LNRLegadoPlugin

/**
 * Book info adapter for fetching detailed book information.
 */
class BookInfoAdapter(private val plugin: LNRLegadoPlugin) {

    data class BookInfo(
        val bookUrl: String,
        val title: String,
        val author: String,
        val coverUrl: String,
        val description: String,
        val tag: String,
        val wordCount: String,
        val latestChapter: String,
        val sourceName: String,
        val sourceUrl: String
    )

    /**
     * Get book info from a specific source.
     */
    suspend fun getBookInfo(sourceUrl: String, bookUrl: String): BookInfo? {
        val source = plugin.getSourceManager().getSource(sourceUrl) ?: return null
        val engine = plugin.getEngine(sourceUrl)
        return try {
            val book = engine.getBookInfo(source, bookUrl)
            BookInfo(
                bookUrl = book.bookUrl,
                title = book.name,
                author = book.author,
                coverUrl = book.coverUrl,
                description = book.intro,
                tag = book.kind,
                wordCount = book.wordCount,
                latestChapter = book.latestChapterTitle,
                sourceName = source.bookSourceName,
                sourceUrl = source.bookSourceUrl
            )
        } catch (e: Exception) {
            null
        }
    }
}
