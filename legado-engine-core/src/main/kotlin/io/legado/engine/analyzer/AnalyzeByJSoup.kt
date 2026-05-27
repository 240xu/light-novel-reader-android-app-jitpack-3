package io.legado.engine.analyzer

import io.legado.engine.exception.RuleException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * HTML/CSS selector analyzer using Jsoup.
 * Extracted from Legado AnalyzeByJSoup, stripped of Android dependencies.
 *
 * Supports Legado's CSS selector syntax:
 *   - Standard CSS: div.class > span
 *   - @css: prefixed rules
 *   - Attribute extraction: div@src, div@text, div@href
 *   - Chain selectors separated by @
 */
class AnalyzeByJSoup(private val doc: Document) {

    companion object {
        /**
         * Parse HTML content into an AnalyzeByJSoup instance.
         */
        fun parse(html: String, baseUrl: String = ""): AnalyzeByJSoup {
            val doc = Jsoup.parse(html, baseUrl)
            return AnalyzeByJSoup(doc)
        }
    }

    /**
     * Get the root element wrapped for further analysis.
     */
    fun root(): Element {
        return doc
    }

    /**
     * Get the first element matching the CSS selector.
     */
    fun firstElement(selectors: String, isRegex: Boolean = false): Element? {
        if (selectors.isBlank()) return null
        return try {
            val cssSelector = if (selectors.startsWith("@css:")) {
                selectors.removePrefix("@css:")
            } else {
                selectors
            }
            doc.selectFirst(cssSelector)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all elements matching the CSS selector.
     */
    fun elements(selectors: String): List<Element> {
        if (selectors.isBlank()) return emptyList()
        return try {
            val cssSelector = if (selectors.startsWith("@css:")) {
                selectors.removePrefix("@css:")
            } else {
                selectors
            }
            doc.select(cssSelector)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get text content from the first matching element.
     */
    fun text(selectors: String): String {
        if (selectors.isBlank()) return ""
        return try {
            val element = firstElement(selectors) ?: return ""
            when {
                selectors.endsWith("@text") -> element.text()
                selectors.endsWith("@textNodes") -> element.text()
                selectors.endsWith("@src") -> element.attr("src")
                selectors.endsWith("@href") -> element.attr("href")
                selectors.endsWith("@content") -> element.attr("content")
                selectors.contains("@") -> {
                    val attrName = selectors.substringAfterLast("@")
                    element.attr(attrName)
                }
                else -> element.text()
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get list of text strings from matching elements.
     */
    fun textList(selectors: String): List<String> {
        if (selectors.isBlank()) return emptyList()
        return try {
            elements(selectors).map { element ->
                when {
                    selectors.endsWith("@text") -> element.text()
                    selectors.endsWith("@src") -> element.attr("src")
                    selectors.endsWith("@href") -> element.attr("href")
                    selectors.contains("@") -> {
                        val attrName = selectors.substringAfterLast("@")
                        element.attr(attrName)
                    }
                    else -> element.text()
                }
            }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get the outer HTML of matching element.
     */
    fun outerHtml(selectors: String): String {
        if (selectors.isBlank()) return ""
        return try {
            firstElement(selectors)?.outerHtml() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get all outer HTML of matching elements.
     */
    fun outerHtmlList(selectors: String): List<String> {
        if (selectors.isBlank()) return emptyList()
        return try {
            elements(selectors).map { it.outerHtml() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
