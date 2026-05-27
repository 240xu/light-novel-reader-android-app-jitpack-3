package io.github.dmzz_yyhyy.lnrplugin.adapter

import io.github.dmzz_yyhyy.lnrplugin.LNRLegadoPlugin
import io.legado.engine.model.SearchBook
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Search adapter for multi-source book search.
 * Aggregates results from all enabled sources.
 */
class SearchAdapter(private val plugin: LNRLegadoPlugin) {

    data class SearchResult(
        val bookUrl: String,
        val title: String,
        val author: String,
        val coverUrl: String,
        val description: String,
        val tag: String,
        val sourceName: String,
        val sourceUrl: String
    )

    /**
     * Search across all enabled sources.
     */
    suspend fun searchAll(keyword: String, page: Int = 1): List<SearchResult> {
        val sources = plugin.getSourceManager().getEnabledSources()
        if (sources.isEmpty()) return emptyList()

        return coroutineScope {
            sources.map { source ->
                async {
                    try {
                        search(source.bookSourceUrl, keyword, page)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
    }

    /**
     * Search a specific source.
     */
    suspend fun search(sourceUrl: String, keyword: String, page: Int = 1): List<SearchResult> {
        val source = plugin.getSourceManager().getSource(sourceUrl) ?: return emptyList()
        val engine = plugin.getEngine(sourceUrl)
        return try {
            engine.search(source, keyword, page).map { book ->
                SearchResult(
                    bookUrl = book.bookUrl,
                    title = book.name,
                    author = book.author,
                    coverUrl = book.coverUrl,
                    description = book.intro,
                    tag = book.kind,
                    sourceName = book.originName,
                    sourceUrl = book.sourceUrl
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
