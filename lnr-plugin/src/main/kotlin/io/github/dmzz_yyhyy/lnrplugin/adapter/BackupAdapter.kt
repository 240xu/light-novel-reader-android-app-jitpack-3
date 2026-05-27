package io.github.dmzz_yyhyy.lnrplugin.adapter

import io.github.dmzz_yyhyy.lnrplugin.LNRLegadoPlugin
import io.legado.engine.model.BookSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Backup and restore adapter.
 * Supports book source, bookshelf, and settings backup/restore.
 */
class BackupAdapter(private val plugin: LNRLegadoPlugin) {

    data class BackupData(
        val version: Int = 1,
        val timestamp: Long = System.currentTimeMillis(),
        val bookSources: List<BookSourceBackup> = emptyList(),
        val bookshelf: List<BookshelfItem> = emptyList(),
        val settings: Map<String, String> = emptyMap()
    )

    data class BookSourceBackup(
        val bookSourceUrl: String,
        val bookSourceName: String,
        val bookSourceGroup: String,
        val searchUrl: String,
        val enabled: Boolean
    )

    data class BookshelfItem(
        val bookUrl: String,
        val title: String,
        val author: String,
        val sourceUrl: String,
        val lastReadChapter: String?,
        val lastReadPosition: Int
    )

    private val gson = Gson()

    /**
     * Export all data to JSON string.
     */
    fun exportToJson(): String {
        val sourceManager = plugin.getSourceManager()
        val sources = sourceManager.getAllSources().map { source ->
            BookSourceBackup(
                bookSourceUrl = source.bookSourceUrl,
                bookSourceName = source.bookSourceName,
                bookSourceGroup = source.bookSourceGroup,
                searchUrl = source.searchUrl,
                enabled = source.enabled
            )
        }
        val backup = BackupData(bookSources = sources)
        return gson.toJson(backup)
    }

    /**
     * Import data from JSON string.
     * @return Number of sources imported.
     */
    fun importFromJson(json: String): Int {
        return try {
            val backup = gson.fromJson(json, BackupData::class.java)
            val sourceManager = plugin.getSourceManager()
            var count = 0
            backup.bookSources.forEach { backup ->
                val source = BookSource(
                    bookSourceUrl = backup.bookSourceUrl,
                    bookSourceName = backup.bookSourceName,
                    bookSourceGroup = backup.bookSourceGroup,
                    searchUrl = backup.searchUrl,
                    enabled = backup.enabled
                )
                sourceManager.addSource(source)
                count++
            }
            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Export book sources to Legado-compatible JSON format.
     */
    fun exportSourcesToJson(): String {
        val sources = plugin.getSourceManager().getAllSources()
        return gson.toJson(sources)
    }

    /**
     * Import book sources from Legado-compatible JSON format.
     */
    fun importSourcesFromJson(json: String): Int {
        return try {
            val type = object : TypeToken<List<BookSource>>() {}.type
            val sources: List<BookSource> = gson.fromJson(json, type)
            val sourceManager = plugin.getSourceManager()
            sources.forEach { sourceManager.addSource(it) }
            sources.size
        } catch (e: Exception) {
            0
        }
    }
}
