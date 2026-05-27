package io.github.dmzz_yyhyy.lnrplugin

import io.github.dmzz_yyhyy.lnrplugin.adapter.*
import io.github.dmzz_yyhyy.lnrplugin.source.BookSourceManager
import io.github.dmzz_yyhyy.lnrplugin.util.HttpClientFactory
import io.github.dmzz_yyhyy.lnrplugin.util.JsEngineImpl
import io.legado.engine.EngineFacade

/**
 * LNR Legado Plugin - Main Entry Point.
 *
 * This plugin integrates the Legado parsing engine into LightNovelReader,
 * providing book source-based content aggregation.
 *
 * Features:
 *   - Book source loading and management (Legado-compatible JSON format)
 *   - Search across multiple sources
 *   - Explore/discovery page with dynamic rule loading
 *   - Book info, chapter list, and content parsing
 *   - Paragraph-level comments via WebView injection
 *   - Backup and restore support
 */
class LNRLegadoPlugin {

    companion object {
        const val PLUGIN_ID = "legado-engine"
        const val PLUGIN_NAME = "Legado Engine Plugin"
        const val PLUGIN_VERSION = "1.0.0"
        const val PLUGIN_AUTHOR = "LNR Community"
        const val PLUGIN_DESCRIPTION = "Legado-compatible book source parsing engine for LightNovelReader"
    }

    // Lazy-initialized engine instances per source
    private val engines = mutableMapOf<String, EngineFacade>()
    private val sourceManager = BookSourceManager()

    /**
     * Get or create an engine instance for a given source.
     */
    fun getEngine(sourceUrl: String): EngineFacade {
        return engines.getOrPut(sourceUrl) {
            val httpHandler = HttpClientFactory.create()
            val jsEngine = JsEngineImpl()
            EngineFacade(httpHandler, jsEngine)
        }
    }

    /**
     * Get the book source manager.
     */
    fun getSourceManager(): BookSourceManager = sourceManager

    /**
     * Get the ExploreAdapter for discovery page.
     */
    fun getExploreAdapter(): ExploreAdapter = ExploreAdapter(this)

    /**
     * Get the SearchAdapter for search results.
     */
    fun getSearchAdapter(): SearchAdapter = SearchAdapter(this)

    /**
     * Get the BookInfoAdapter for book details.
     */
    fun getBookInfoAdapter(): BookInfoAdapter = BookInfoAdapter(this)

    /**
     * Get the ChapterListAdapter for table of contents.
     */
    fun getChapterListAdapter(): ChapterListAdapter = ChapterListAdapter(this)

    /**
     * Get the ContentAdapter for chapter content.
     */
    fun getContentAdapter(): ContentAdapter = ContentAdapter(this)

    /**
     * Get the BackupAdapter for backup/restore.
     */
    fun getBackupAdapter(): BackupAdapter = BackupAdapter(this)

    /**
     * Get the custom.js content for WebView injection.
     */
    fun getCustomJs(): String {
        return try {
            val inputStream = javaClass.classLoader?.getResourceAsStream("assets/js/custom.js")
            inputStream?.bufferedReader()?.readText() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Cleanup resources when plugin is unloaded.
     */
    fun destroy() {
        engines.clear()
        sourceManager.clear()
    }
}
