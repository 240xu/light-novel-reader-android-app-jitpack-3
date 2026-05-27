package io.github.dmzz_yyhyy.lnrplugin.adapter

import io.github.dmzz_yyhyy.lnrplugin.LNRLegadoPlugin

/**
 * Content adapter for fetching chapter text content.
 * Supports multi-page concatenation and content cleaning.
 */
class ContentAdapter(private val plugin: LNRLegadoPlugin) {

    data class ChapterContent(
        val title: String,
        val contentLines: List<String>,
        val hasNextPage: Boolean
    )

    /**
     * Get chapter content from a specific source.
     * @param fullContent If true, auto-follows nextContentUrl to get full text.
     */
    suspend fun getContent(
        sourceUrl: String,
        chapterUrl: String,
        fullContent: Boolean = true
    ): ChapterContent? {
        val source = plugin.getSourceManager().getSource(sourceUrl) ?: return null
        val engine = plugin.getEngine(sourceUrl)
        return try {
            if (fullContent) {
                val page = engine.getFullContent(source, chapterUrl)
                ChapterContent(
                    title = page.title,
                    contentLines = page.contentLines,
                    hasNextPage = false
                )
            } else {
                val page = engine.getContent(source, chapterUrl)
                ChapterContent(
                    title = page.title,
                    contentLines = page.contentLines,
                    hasNextPage = page.nextUrl != null
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
