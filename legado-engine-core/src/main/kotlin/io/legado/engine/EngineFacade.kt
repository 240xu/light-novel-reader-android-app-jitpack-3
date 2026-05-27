package io.legado.engine

import io.legado.engine.analyzer.AnalyzeByJSonPath
import io.legado.engine.analyzer.AnalyzeByJSoup
import io.legado.engine.analyzer.AnalyzeByRegex
import io.legado.engine.analyzer.RuleAnalyzer
import io.legado.engine.helper.ContentHelp
import io.legado.engine.interface.IHttpHandler
import io.legado.engine.interface.IJsEngine
import io.legado.engine.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Main engine facade.
 * This is the primary entry point for LNR plugin to call the parsing engine.
 * All methods are suspend functions for coroutine-based async processing.
 */
class EngineFacade(
    private val httpHandler: IHttpHandler,
    private val jsEngine: IJsEngine? = null
) {

    // ==================== Search ====================

    /**
     * Execute search and return book list.
     */
    suspend fun search(
        source: BookSource,
        keyword: String,
        page: Int = 1
    ): List<SearchBook> {
        if (source.searchUrl.isBlank()) return emptyList()
        val url = source.searchUrl
            .replace("searchKey", keyword)
            .replace("searchPage", page.toString())
        val html = httpHandler.get(url, parseHeader(source.header))
        val analyzer = RuleAnalyzer(html = html, baseUrl = source.bookSourceUrl, jsEngine = jsEngine)
        val rule = source.ruleSearch
        val bookList = analyzer.getStringList(rule.bookList)
        return bookList.mapNotNull { itemHtml ->
            try {
                val itemAnalyzer = RuleAnalyzer(html = itemHtml, baseUrl = source.bookSourceUrl, jsEngine = jsEngine)
                val name = itemAnalyzer.getString(rule.name)
                if (name.isBlank()) return@mapNotNull null
                SearchBook(
                    bookUrl = itemAnalyzer.getString(rule.bookUrl),
                    name = name,
                    author = itemAnalyzer.getString(rule.author),
                    coverUrl = itemAnalyzer.getString(rule.coverUrl),
                    intro = itemAnalyzer.getString(rule.intro),
                    kind = itemAnalyzer.getString(rule.kind),
                    latestChapterTitle = itemAnalyzer.getString(rule.lastChapterTitle),
                    sourceUrl = source.bookSourceUrl,
                    originName = source.bookSourceName
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    // ==================== Book Info ====================

    /**
     * Get detailed book information.
     */
    suspend fun getBookInfo(source: BookSource, bookUrl: String): Book {
        val html = httpHandler.get(bookUrl, parseHeader(source.header))
        val analyzer = RuleAnalyzer(html = html, baseUrl = bookUrl, jsEngine = jsEngine)
        val rule = source.ruleBookInfo
        return Book(
            bookUrl = bookUrl,
            name = analyzer.getString(rule.name),
            author = analyzer.getString(rule.author),
            coverUrl = analyzer.getString(rule.coverUrl),
            intro = analyzer.getString(rule.intro),
            kind = analyzer.getString(rule.kind),
            latestChapterTitle = analyzer.getString(rule.lastChapterTitle),
            wordCount = analyzer.getString(rule.wordCount),
            sourceUrl = source.bookSourceUrl
        )
    }

    // ==================== Table of Contents ====================

    /**
     * Get chapter list (table of contents).
     */
    suspend fun getChapterList(source: BookSource, bookUrl: String): List<BookChapter> {
        val html = httpHandler.get(bookUrl, parseHeader(source.header))
        val analyzer = RuleAnalyzer(html = html, baseUrl = bookUrl, jsEngine = jsEngine)
        val rule = source.ruleToc
        val chapters = analyzer.getStringList(rule.chapterList)
        var order = 0
        return chapters.mapNotNull { itemHtml ->
            try {
                val itemAnalyzer = RuleAnalyzer(html = itemHtml, baseUrl = bookUrl, jsEngine = jsEngine)
                val title = itemAnalyzer.getString(rule.chapterName)
                if (title.isBlank()) return@mapNotNull null
                val url = itemAnalyzer.getString(rule.chapterUrl)
                val volume = itemAnalyzer.getString(rule.volumeName)
                order++
                BookChapter(
                    url = url,
                    title = title,
                    volume = volume,
                    order = order
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    // ==================== Content ====================

    /**
     * Get chapter content.
     */
    suspend fun getContent(source: BookSource, chapterUrl: String): ContentPage {
        val html = httpHandler.get(chapterUrl, parseHeader(source.header))
        val analyzer = RuleAnalyzer(html = html, baseUrl = chapterUrl, jsEngine = jsEngine)
        val rule = source.ruleContent
        val title = analyzer.getString(rule.title)
        var content = analyzer.getString(rule.content)
        content = ContentHelp.formatContent(content)
        content = ContentHelp.replaceContent(content, rule.replaceRegex)
        content = ContentHelp.removeAds(content)
        val nextUrl = analyzer.getString(rule.nextContentUrl).ifBlank { null }
        return ContentPage(
            title = title,
            contentLines = content.split("\n").filter { it.isNotBlank() },
            nextUrl = nextUrl
        )
    }

    /**
     * Get content with multi-page concatenation support.
     */
    suspend fun getFullContent(
        source: BookSource,
        chapterUrl: String,
        maxPages: Int = 10
    ): ContentPage {
        val pages = mutableListOf<ContentPage>()
        var currentUrl: String? = chapterUrl
        var pageCount = 0

        while (currentUrl != null && pageCount < maxPages) {
            val page = getContent(source, currentUrl)
            pages.add(page)
            currentUrl = page.nextUrl
            pageCount++
        }

        val allContent = pages.flatMap { it.contentLines }
        return ContentPage(
            title = pages.firstOrNull()?.title ?: "",
            contentLines = allContent,
            nextUrl = null
        )
    }

    // ==================== Explore ====================

    /**
     * Get explore page books.
     */
    suspend fun getExploreBooks(
        source: BookSource,
        url: String
    ): List<ExploreBook> {
        val html = httpHandler.get(url, parseHeader(source.header))
        val analyzer = RuleAnalyzer(html = html, baseUrl = url, jsEngine = jsEngine)
        val rule = source.ruleExplore
        val bookList = analyzer.getStringList(rule.bookList)
        return bookList.mapNotNull { itemHtml ->
            try {
                val itemAnalyzer = RuleAnalyzer(html = itemHtml, baseUrl = url, jsEngine = jsEngine)
                val name = itemAnalyzer.getString(rule.name)
                if (name.isBlank()) return@mapNotNull null
                ExploreBook(
                    bookUrl = itemAnalyzer.getString(rule.bookUrl),
                    name = name,
                    author = itemAnalyzer.getString(rule.author),
                    coverUrl = itemAnalyzer.getString(rule.coverUrl),
                    intro = itemAnalyzer.getString(rule.intro),
                    kind = itemAnalyzer.getString(rule.kind),
                    latestChapterTitle = itemAnalyzer.getString(rule.lastChapterTitle)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    // ==================== Utilities ====================

    /**
     * Parse header string into map.
     * Format: "Key: Value\nKey2: Value2"
     */
    private fun parseHeader(header: String): Map<String, String> {
        if (header.isBlank()) return emptyMap()
        return header.split("\n").associate { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim()
            else parts[0].trim() to ""
        }.filter { it.key.isNotBlank() }
    }
}
