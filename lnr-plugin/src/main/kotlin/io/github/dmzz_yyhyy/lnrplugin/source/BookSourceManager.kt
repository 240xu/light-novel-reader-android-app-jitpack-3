package io.github.dmzz_yyhyy.lnrplugin.source

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.legado.engine.model.BookSource

/**
 * Book source manager.
 * Handles loading, saving, and managing Legado-format book sources.
 *
 * Supports:
 *   - Loading from JSON string (Legado export format)
 *   - Loading from URL (online source repositories)
 *   - CRUD operations on sources
 *   - Enable/disable individual sources
 *   - Group-based filtering
 */
class BookSourceManager {

    private val sources = mutableMapOf<String, BookSource>()
    private val gson = Gson()

    /**
     * Load sources from a JSON string (Legado export format).
     * @return Number of sources loaded.
     */
    fun loadFromJson(json: String): Int {
        return try {
            val type = object : TypeToken<List<BookSource>>() {}.type
            val sourceList: List<BookSource> = gson.fromJson(json, type)
            sourceList.forEach { source ->
                if (source.bookSourceUrl.isNotBlank()) {
                    sources[source.bookSourceUrl] = source
                }
            }
            sourceList.size
        } catch (e: Exception) {
            // Try single source format
            try {
                val source = gson.fromJson(json, BookSource::class.java)
                if (source.bookSourceUrl.isNotBlank()) {
                    sources[source.bookSourceUrl] = source
                    1
                } else {
                    0
                }
            } catch (e2: Exception) {
                0
            }
        }
    }

    /**
     * Load sources from a URL.
     * The URL should point to a JSON array of BookSource objects.
     */
    suspend fun loadFromUrl(url: String, httpGet: suspend (String) -> String): Int {
        return try {
            val json = httpGet(url)
            loadFromJson(json)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Add or update a single source.
     */
    fun addSource(source: BookSource) {
        if (source.bookSourceUrl.isNotBlank()) {
            sources[source.bookSourceUrl] = source
        }
    }

    /**
     * Remove a source by URL.
     */
    fun removeSource(sourceUrl: String) {
        sources.remove(sourceUrl)
    }

    /**
     * Get a source by URL.
     */
    fun getSource(sourceUrl: String): BookSource? {
        return sources[sourceUrl]
    }

    /**
     * Get all loaded sources.
     */
    fun getAllSources(): List<BookSource> {
        return sources.values.toList()
    }

    /**
     * Get only enabled sources.
     */
    fun getEnabledSources(): List<BookSource> {
        return sources.values.filter { it.enabled }
    }

    /**
     * Get sources by group.
     */
    fun getSourcesByGroup(group: String): List<BookSource> {
        return sources.values.filter { it.bookSourceGroup.contains(group) }
    }

    /**
     * Enable or disable a source.
     */
    fun setSourceEnabled(sourceUrl: String, enabled: Boolean) {
        sources[sourceUrl]?.let { source ->
            sources[sourceUrl] = source.copy(enabled = enabled)
        }
    }

    /**
     * Export all sources to JSON string.
     */
    fun exportToJson(): String {
        return gson.toJson(sources.values.toList())
    }

    /**
     * Clear all loaded sources.
     */
    fun clear() {
        sources.clear()
    }

    /**
     * Get count of loaded sources.
     */
    fun size(): Int = sources.size
}
