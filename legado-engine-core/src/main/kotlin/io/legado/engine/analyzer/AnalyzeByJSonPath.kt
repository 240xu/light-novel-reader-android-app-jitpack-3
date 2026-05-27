package io.legado.engine.analyzer

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.PathNotFoundException

/**
 * JSON path analyzer using json-path library.
 * Extracted from Legado AnalyzeByJSonPath.
 *
 * Supports Legado's JSON path syntax:
 *   - $.store.book[0].title
 *   - @json: prefix
 *   - Filter expressions
 */
class AnalyzeByJSonPath(private val json: String) {

    companion object {
        private val conf: Configuration = Configuration.builder()
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS)
            .build()

        fun parse(json: String): AnalyzeByJSonPath {
            return AnalyzeByJSonPath(json)
        }
    }

    private val document: Any by lazy {
        JsonPath.using(conf).parse(json)
    }

    /**
     * Get the first result of the JSON path expression.
     */
    fun first(jsonPath: String): String {
        if (jsonPath.isBlank()) return ""
        return try {
            val path = jsonPath.removePrefix("@json:")
            val result = JsonPath.using(conf).parse(json).read<Any>(path)
            when (result) {
                is List<*> -> result.firstOrNull()?.toString() ?: ""
                is String -> result
                else -> result?.toString() ?: ""
            }
        } catch (e: PathNotFoundException) {
            ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get list of results from JSON path expression.
     */
    fun list(jsonPath: String): List<String> {
        if (jsonPath.isBlank()) return emptyList()
        return try {
            val path = jsonPath.removePrefix("@json:")
            val result = JsonPath.using(conf).parse(json).read<List<Any>>(path)
            result.mapNotNull { it?.toString() }
        } catch (e: PathNotFoundException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get list of JSON objects as strings.
     */
    fun objectList(jsonPath: String): List<String> {
        if (jsonPath.isBlank()) return emptyList()
        return try {
            val path = jsonPath.removePrefix("@json:")
            val result = JsonPath.using(conf).parse(json).read<List<Map<String, Any>>>(path)
            result.map { it.toString() }
        } catch (e: PathNotFoundException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get raw JSON at path (for nested analysis).
     */
    fun raw(jsonPath: String): String {
        if (jsonPath.isBlank()) return json
        return try {
            val path = jsonPath.removePrefix("@json:")
            val result = JsonPath.using(conf).parse(json).read<Any>(path)
            when (result) {
                is String -> result
                else -> result?.toString() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get a sub-document for chained analysis.
     */
    fun subDocument(jsonPath: String): AnalyzeByJSonPath {
        val sub = raw(jsonPath)
        return AnalyzeByJSonPath(sub)
    }
}
