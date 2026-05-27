package io.legado.engine

import io.legado.engine.analyzer.AnalyzeByRegex
import io.legado.engine.helper.ContentHelp
import io.legado.engine.contract.IHttpHandler
import io.legado.engine.contract.IJsEngine
import io.legado.engine.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class EngineFacade(
    private val httpHandler: IHttpHandler,
    private val jsEngine: IJsEngine? = null
) {
    // ==================== Search ====================
    suspend fun search(source: BookSource, keyword: String, page: Int = 1): List<SearchBook> {
        val searchUrl = source.searchUrl ?: return emptyList()
        if (searchUrl.isBlank()) return emptyList()
        val url = searchUrl.replace("searchKey", keyword).replace("searchPage", page.toString())
        val html = httpHandler.get(url, parseHeader(source.header))
        val analyzer = AnalyzeRule(source = source, jsEngine = jsEngine)
            .setContent(html, source.bookSourceUrl)
        val rule = source.ruleSearch ?: return emptyList()
        val bookList = analyzer.getStringList(rule.bookList) ?: return emptyList()
        return bookList.mapNotNull { itemHtml ->
            try {
                val itemAnalyzer = AnalyzeRule(source = source, jsEngine = jsEngine)
                    .setContent(itemHtml, source.bookSourceUrl)
                val name = itemAnalyzer.getString(rule.name)
                if (name.isBlank()) return@mapNotNull null
                SearchBook(
                    bookUrl = itemAnalyzer.getString(rule.bookUrl, isUrl = true),
                    name = name,
                    author = itemAnalyzer.getString(rule.author),
                    coverUrl = itemAnalyzer.getString(rule.coverUrl, isUrl = true),
                    intro = itemAnalyzer.getString(rule.intro),
                    kind = itemAnalyzer.getString(rule.kind),
                    latestChapterTitle = itemAnalyzer.getString(rule.lastChapter),
                    wordCount = itemAnalyzer.getString(rule.wordCount),
                    sourceUrl = source.bookSourceUrl,
                    origin = source.bookSourceUrl,
                    originName = source.bookSourceName
                )
            } catch (e: Exception) { null }
        }
    }

    // ==================== Book Info ====================
    suspend fun getBookInfo(source: BookSource, bookUrl: String): Book {
        val html = httpHandler.get(bookUrl, parseHeader(source.header))
        val analyzer = AnalyzeRule(source = source, jsEngine = jsEngine)
            .setContent(html, bookUrl)
        val rule = source.ruleBookInfo
        return Book(
            bookUrl = bookUrl,
            name = analyzer.getString(rule?.name),
            author = analyzer.getString(rule?.author),
            coverUrl = analyzer.getString(rule?.coverUrl, isUrl = true),
            intro = analyzer.getString(rule?.intro),
            kind = analyzer.getString(rule?.kind),
            latestChapterTitle = analyzer.getString(rule?.lastChapter),
            wordCount = analyzer.getString(rule?.wordCount),
            tocUrl = analyzer.getString(rule?.tocUrl, isUrl = true).ifBlank { bookUrl },
            origin = source.bookSourceUrl,
            originName = source.bookSourceName
        )
    }

    // ==================== Table of Contents ====================
    suspend fun getChapterList(source: BookSource, bookUrl: String): List<BookChapter> {
        val tocUrl = bookUrl
        val html = httpHandler.get(tocUrl, parseHeader(source.header))
        val analyzer = AnalyzeRule(source = source, jsEngine = jsEngine)
            .setContent(html, tocUrl)
        val rule = source.ruleToc ?: return emptyList()
        // preUpdateJs
        rule.preUpdateJs?.takeIf { it.isNotBlank() }?.let { analyzer.evalJS(it) }
        val chapters = analyzer.getStringList(rule.chapterList) ?: return emptyList()
        var index = 0
        return chapters.mapNotNull { itemHtml ->
            try {
                val itemAnalyzer = AnalyzeRule(source = source, jsEngine = jsEngine)
                    .setContent(itemHtml, tocUrl)
                val title = itemAnalyzer.getString(rule.chapterName)
                if (title.isBlank()) return@mapNotNull null
                val url = itemAnalyzer.getString(rule.chapterUrl, isUrl = true)
                val volume = itemAnalyzer.getString(rule.isVolume)
                val isVip = itemAnalyzer.getString(rule.isVip).let { it == "true" || it == "1" }
                val isPay = itemAnalyzer.getString(rule.isPay).let { it == "true" || it == "1" }
                index++
                BookChapter(
                    url = url,
                    title = title,
                    volume = volume,
                    baseUrl = tocUrl,
                    bookUrl = bookUrl,
                    index = index,
                    isVip = isVip,
                    isPay = isPay
                )
            } catch (e: Exception) { null }
        }
    }

    // ==================== Content ====================
    suspend fun getContent(source: BookSource, chapterUrl: String): ContentPage {
        val html = httpHandler.get(chapterUrl, parseHeader(source.header))
        val analyzer = AnalyzeRule(source = source, jsEngine = jsEngine)
            .setContent(html, chapterUrl)
        val rule = source.ruleContent
        val title = analyzer.getString(rule?.title)
        var content = analyzer.getString(rule?.content)
        // webJs
        rule?.webJs?.takeIf { it.isNotBlank() }?.let {
            content = analyzer.evalJS(it, content)?.toString() ?: content
        }
        content = ContentHelp.formatContent(content)
        content = ContentHelp.replaceContent(content, rule?.replaceRegex ?: "")
        content = ContentHelp.removeAds(content)
        // subContent
        val subContent = analyzer.getString(rule?.subContent)
        if (subContent.isNotBlank()) {
            content = content + "\n" + ContentHelp.formatContent(subContent)
        }
        val nextUrl = analyzer.getString(rule?.nextContentUrl, isUrl = true).ifBlank { null }
        return ContentPage(
            title = title,
            contentLines = content.split("\n").filter { it.isNotBlank() },
            nextUrl = nextUrl
        )
    }

    suspend fun getFullContent(source: BookSource, chapterUrl: String, maxPages: Int = 10): ContentPage {
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
    suspend fun getExploreBooks(source: BookSource, url: String): List<ExploreBook> {
        val html = httpHandler.get(url, parseHeader(source.header))
        val analyzer = AnalyzeRule(source = source, jsEngine = jsEngine)
            .setContent(html, url)
        val rule = source.ruleExplore ?: return emptyList()
        val bookList = analyzer.getStringList(rule.bookList) ?: return emptyList()
        return bookList.mapNotNull { itemHtml ->
            try {
                val itemAnalyzer = AnalyzeRule(source = source, jsEngine = jsEngine)
                    .setContent(itemHtml, url)
                val name = itemAnalyzer.getString(rule.name)
                if (name.isBlank()) return@mapNotNull null
                ExploreBook(
                    bookUrl = itemAnalyzer.getString(rule.bookUrl, isUrl = true),
                    name = name,
                    author = itemAnalyzer.getString(rule.author),
                    coverUrl = itemAnalyzer.getString(rule.coverUrl, isUrl = true),
                    intro = itemAnalyzer.getString(rule.intro),
                    kind = itemAnalyzer.getString(rule.kind),
                    latestChapterTitle = itemAnalyzer.getString(rule.lastChapter),
                    wordCount = itemAnalyzer.getString(rule.wordCount),
                    sourceUrl = source.bookSourceUrl,
                    origin = source.bookSourceUrl
                )
            } catch (e: Exception) { null }
        }
    }

    // ==================== Review (段评) ====================
    suspend fun getReviews(source: BookSource, chapterUrl: String): List<Map<String, String>> {
        val html = httpHandler.get(chapterUrl, parseHeader(source.header))
        val analyzer = AnalyzeRule(source = source, jsEngine = jsEngine)
            .setContent(html, chapterUrl)
        return analyzer.getReviewList(source.ruleReview)
    }

    // ==================== Utilities ====================
    private fun parseHeader(header: String?): Map<String, String> {
        if (header.isNullOrBlank()) return emptyMap()
        return header.split("\n").associate { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim()
            else parts[0].trim() to ""
        }.filter { it.key.isNotBlank() }
    }
}
