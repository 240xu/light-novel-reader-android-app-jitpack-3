package io.github.dmzz_yyhyy.lnrplugin.adapter

import io.github.dmzz_yyhyy.lnrplugin.LNRLegadoPlugin

/**
 * Chapter list adapter for fetching table of contents.
 */
class ChapterListAdapter(private val plugin: LNRLegadoPlugin) {

    data class ChapterInfo(
        val url: String,
        val title: String,
        val volume: String,
        val order: Int
    )

    /**
     * Get chapter list from a specific source.
     */
    suspend fun getChapterList(sourceUrl: String, bookUrl: String): List<ChapterInfo> {
        val source = plugin.getSourceManager().getSource(sourceUrl) ?: return emptyList()
        val engine = plugin.getEngine(sourceUrl)
        return try {
            engine.getChapterList(source, bookUrl).map { chapter ->
                ChapterInfo(
                    url = chapter.url,
                    title = chapter.title,
                    volume = chapter.volume,
                    order = chapter.order
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
