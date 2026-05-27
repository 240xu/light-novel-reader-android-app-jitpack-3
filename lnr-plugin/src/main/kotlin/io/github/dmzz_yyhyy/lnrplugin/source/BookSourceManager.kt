package io.github.dmzz_yyhyy.lnrplugin.source

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.legado.engine.model.BookSource

class BookSourceManager {
    private val sources = mutableMapOf<String, BookSource>()
    private val gson = Gson()

    fun loadFromJson(json: String): Int {
        return try {
            val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
            val list: List<Map<String, Any?>> = gson.fromJson(json, type)
            list.forEach { map ->
                val source = BookSource.fromJsonMap(map)
                if (source.bookSourceUrl.isNotBlank()) {
                    sources[source.bookSourceUrl] = source
                }
            }
            list.size
        } catch (e: Exception) {
            try {
                val type = object : TypeToken<Map<String, Any?>>() {}.type
                val map: Map<String, Any?> = gson.fromJson(json, type)
                val source = BookSource.fromJsonMap(map)
                if (source.bookSourceUrl.isNotBlank()) {
                    sources[source.bookSourceUrl] = source
                    1
                } else 0
            } catch (e2: Exception) { 0 }
        }
    }

    suspend fun loadFromUrl(url: String, httpGet: suspend (String) -> String): Int {
        return try { loadFromJson(httpGet(url)) } catch (e: Exception) { 0 }
    }

    fun addSource(source: BookSource) {
        if (source.bookSourceUrl.isNotBlank()) sources[source.bookSourceUrl] = source
    }

    fun removeSource(sourceUrl: String) { sources.remove(sourceUrl) }
    fun getSource(sourceUrl: String): BookSource? = sources[sourceUrl]
    fun getAllSources(): List<BookSource> = sources.values.toList()
    fun getEnabledSources(): List<BookSource> = sources.values.filter { it.enabled }
    fun getSourcesByGroup(group: String): List<BookSource> = sources.values.filter { it.bookSourceGroup?.contains(group) == true }

    fun setSourceEnabled(sourceUrl: String, enabled: Boolean) {
        sources[sourceUrl]?.let { sources[sourceUrl] = it.copy(enabled = enabled) }
    }

    fun exportToJson(): String = gson.toJson(sources.values.toList())
    fun clear() { sources.clear() }
    fun size(): Int = sources.size
}
