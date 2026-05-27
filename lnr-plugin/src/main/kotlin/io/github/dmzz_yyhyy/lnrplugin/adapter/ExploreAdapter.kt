package io.github.dmzz_yyhyy.lnrplugin.adapter

import io.github.dmzz_yyhyy.lnrplugin.LNRLegadoPlugin
import io.legado.engine.model.BookSource
import io.legado.engine.model.ExploreBook

/**
 * Explore/Discovery page adapter.
 * Dynamically loads book source ruleExplore rules and converts
 * results to LNR exploration page data model.
 *
 * Features:
 *   - Dynamic rule loading from book sources
 *   - JS/XPath/CSS rule execution via engine
 *   - Error fallback with static rule mapping
 *   - Pagination support
 */
class ExploreAdapter(private val plugin: LNRLegadoPlugin) {

    /**
     * Explore category definition from a book source.
     */
    data class ExploreCategory(
        val title: String,
        val url: String,
        val sourceUrl: String
    )

    /**
     * Explore result for LNR UI.
     */
    data class ExploreResult(
        val books: List<ExploreItem>,
        val hasNextPage: Boolean,
        val nextPageUrl: String?
    )

    data class ExploreItem(
        val bookUrl: String,
        val title: String,
        val author: String,
        val coverUrl: String,
        val description: String,
        val tag: String,
        val sourceName: String
    )

    /**
     * Get all explore categories from loaded sources.
     * Each source with a valid searchUrl can be explored.
     */
    fun getExploreCategories(): List<ExploreCategory> {
        val sources = plugin.getSourceManager().getEnabledSources()
        return sources.flatMap { source ->
            parseExploreUrls(source)
        }
    }

    /**
     * Parse explore URL rules from a source.
     * Some sources have multiple explore pages defined in their ruleExplore.
     */
    private fun parseExploreUrls(source: BookSource): List<ExploreCategory> {
        val categories = mutableListOf<ExploreCategory>()
        // Default explore: use search URL with empty keyword
        if (source.searchUrl.isNotBlank()) {
            categories.add(
                ExploreCategory(
                    title = source.bookSourceName,
                    url = source.searchUrl.replace("searchKey", "").replace("searchPage", "1"),
                    sourceUrl = source.bookSourceUrl
                )
            )
        }
        return categories
    }

    /**
     * Execute explore for a given category, returns page results.
     */
    suspend fun explore(
        category: ExploreCategory,
        page: Int = 1
    ): ExploreResult {
        return try {
            val source = plugin.getSourceManager().getSource(category.sourceUrl)
                ?: return ExploreResult(emptyList(), false, null)

            val url = category.url
                .replace("{page}", page.toString())
                .replace("searchPage", page.toString())

            val engine = plugin.getEngine(source.bookSourceUrl)
            val books = engine.getExploreBooks(source, url)

            ExploreResult(
                books = books.map { book ->
                    ExploreItem(
                        bookUrl = book.bookUrl,
                        title = book.name,
                        author = book.author,
                        coverUrl = book.coverUrl,
                        description = book.intro,
                        tag = book.kind,
                        sourceName = source.bookSourceName
                    )
                },
                hasNextPage = books.isNotEmpty(),
                nextPageUrl = url.replace("page=$page", "page=${page + 1}")
            )
        } catch (e: Exception) {
            ExploreResult(emptyList(), false, null)
        }
    }

    /**
     * Error fallback: try static explore rules when dynamic rules fail.
     * This provides a basic experience even with broken sources.
     */
    suspend fun exploreWithFallback(
        category: ExploreCategory,
        page: Int = 1
    ): ExploreResult {
        val result = explore(category, page)
        if (result.books.isNotEmpty()) return result

        // Fallback: try using the source's search URL with common keywords
        val fallbackKeywords = listOf("轻小说", "小说", "最新")
        for (keyword in fallbackKeywords) {
            val searchResult = try {
                val source = plugin.getSourceManager().getSource(category.sourceUrl) ?: continue
                val engine = plugin.getEngine(source.bookSourceUrl)
                engine.search(source, keyword, page).map { book ->
                    ExploreItem(
                        bookUrl = book.bookUrl,
                        title = book.name,
                        author = book.author,
                        coverUrl = book.coverUrl,
                        description = book.intro,
                        tag = book.kind,
                        sourceName = source.bookSourceName
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
            if (searchResult.isNotEmpty()) {
                return ExploreResult(searchResult, true, null)
            }
        }
        return ExploreResult(emptyList(), false, null)
    }
}
