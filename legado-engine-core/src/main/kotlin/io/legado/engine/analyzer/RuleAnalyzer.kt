package io.legado.engine.analyzer

import io.legado.engine.exception.RuleException
import io.legado.engine.interface.IJsEngine

/**
 * Unified rule analyzer - the core of the Legado parsing engine.
 * Extracted and adapted from Legado's RuleAnalyzer.
 *
 * Dispatches rules to the appropriate analyzer based on prefix:
 *   - @css: / @jsoup: → AnalyzeByJSoup (CSS selectors)
 *   - @XPath: → AnalyzeByXPath
 *   - @json: → AnalyzeByJSonPath
 *   - /regex/ → AnalyzeByRegex
 *   - @js: → JavaScript execution via IJsEngine
 *   - Plain string → direct text extraction
 *
 * Supports rule chains separated by '&&' or '||'.
 */
class RuleAnalyzer(
    private val html: String = "",
    private val json: String = "",
    private val baseUrl: String = "",
    private val jsEngine: IJsEngine? = null
) {
    private val jsoupAnalyzer: AnalyzeByJSoup? by lazy {
        if (html.isNotBlank()) AnalyzeByJSoup.parse(html, baseUrl) else null
    }

    private val xpathAnalyzer: AnalyzeByXPath? by lazy {
        if (html.isNotBlank()) AnalyzeByXPath.parse(html, baseUrl) else null
    }

    private val jsonPathAnalyzer: AnalyzeByJSonPath? by lazy {
        if (json.isNotBlank()) AnalyzeByJSonPath.parse(json) else null
    }

    /**
     * Analyze a rule and return a single string result.
     */
    suspend fun getString(rule: String): String {
        if (rule.isBlank()) return ""
        return try {
            val trimmedRule = rule.trim()
            when {
                trimmedRule.startsWith("@js:") -> {
                    executeJs(trimmedRule.removePrefix("@js:"))
                }
                trimmedRule.startsWith("@json:") || trimmedRule.startsWith("$.") -> {
                    getStringByJsonPath(trimmedRule)
                }
                trimmedRule.startsWith("@XPath:") || trimmedRule.startsWith("//") -> {
                    getStringByXPath(trimmedRule)
                }
                trimmedRule.startsWith("@css:") || trimmedRule.startsWith("@jsoup:") -> {
                    getStringByCss(trimmedRule)
                }
                AnalyzeByRegex.isRegexRule(trimmedRule) -> {
                    getStringByRegex(trimmedRule)
                }
                trimmedRule.contains("&&") -> {
                    // Chain: first non-empty result
                    trimmedRule.split("&&").firstNotNullOfOrNull { getString(it.trim()) } ?: ""
                }
                trimmedRule.contains("||") -> {
                    // Chain: first non-empty result
                    trimmedRule.split("||").firstNotNullOfOrNull {
                        val result = getString(it.trim())
                        result.ifBlank { null }
                    } ?: ""
                }
                else -> {
                    // Try CSS selector by default for HTML content
                    getStringByCss(trimmedRule)
                }
            }
        } catch (e: Exception) {
            if (e is RuleException) throw e
            ""
        }
    }

    /**
     * Analyze a rule and return a list of string results.
     */
    suspend fun getStringList(rule: String): List<String> {
        if (rule.isBlank()) return emptyList()
        return try {
            val trimmedRule = rule.trim()
            when {
                trimmedRule.startsWith("@js:") -> {
                    val result = executeJs(trimmedRule.removePrefix("@js:"))
                    result.split("\n").filter { it.isNotBlank() }
                }
                trimmedRule.startsWith("@json:") || trimmedRule.startsWith("$.") -> {
                    getStringListByJsonPath(trimmedRule)
                }
                trimmedRule.startsWith("@XPath:") || trimmedRule.startsWith("//") -> {
                    getStringListByXPath(trimmedRule)
                }
                trimmedRule.startsWith("@css:") || trimmedRule.startsWith("@jsoup:") -> {
                    getStringListByCss(trimmedRule)
                }
                else -> {
                    getStringListByCss(trimmedRule)
                }
            }
        } catch (e: Exception) {
            if (e is RuleException) throw e
            emptyList()
        }
    }

    private fun getStringByCss(rule: String): String {
        val analyzer = jsoupAnalyzer ?: return ""
        return analyzer.text(rule)
    }

    private fun getStringListByCss(rule: String): List<String> {
        val analyzer = jsoupAnalyzer ?: return emptyList()
        return analyzer.textList(rule)
    }

    private fun getStringByXPath(rule: String): String {
        val analyzer = xpathAnalyzer ?: return ""
        return analyzer.text(rule)
    }

    private fun getStringListByXPath(rule: String): List<String> {
        val analyzer = xpathAnalyzer ?: return emptyList()
        return analyzer.textList(rule)
    }

    private fun getStringByJsonPath(rule: String): String {
        val analyzer = jsonPathAnalyzer ?: return ""
        return analyzer.first(rule)
    }

    private fun getStringListByJsonPath(rule: String): List<String> {
        val analyzer = jsonPathAnalyzer ?: return emptyList()
        return analyzer.list(rule)
    }

    private fun getStringByRegex(rule: String): String {
        val source = if (html.isNotBlank()) html else json
        val (pattern, _) = AnalyzeByRegex.parseRegexRule(rule)
        return AnalyzeByRegex.regexFindFirst(source, pattern)
    }

    private suspend fun executeJs(script: String): String {
        val engine = jsEngine ?: return ""
        return engine.eval(script, mapOf(
            "java" to "",
            "src" to html.ifBlank { json },
            "baseUrl" to baseUrl
        )) ?: ""
    }
}
