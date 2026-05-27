package io.github.dmzz_yyhyy.lnrplugin

import android.net.Uri
import android.util.Log
import io.github.dmzz_yyhyy.lnrplugin.source.BookSourceManager
import io.github.dmzz_yyhyy.lnrplugin.util.HttpClientFactory
import io.github.dmzz_yyhyy.lnrplugin.util.JsEngineImpl
import io.legado.engine.EngineFacade
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.book.ChapterInformation
import io.nightfish.lightnovelreader.api.book.MutableBookInformation
import io.nightfish.lightnovelreader.api.book.MutableChapterContent
import io.nightfish.lightnovelreader.api.book.Volume
import io.nightfish.lightnovelreader.api.book.WordCount
import io.nightfish.lightnovelreader.api.explore.ExploreBooksRow
import io.nightfish.lightnovelreader.api.explore.ExploreDisplayBook
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.AbstractDefaultExplorePageProvider
import io.nightfish.lightnovelreader.api.web.explore.ExploreExpandedPageDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.explore.ExploreTapPageDataSource
import io.nightfish.lightnovelreader.api.web.search.AbstractSearchProvider
import io.nightfish.lightnovelreader.api.web.search.SearchResult
import io.nightfish.lightnovelreader.api.web.search.SearchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDateTime

@WebDataSource(
    name = "LegadoWebDataSource",
    provider = "legado.engine"
)
class LegadoWebDataSource : WebBookDataSource {

    companion object {
        private const val TAG = "LegadoWebDataSource"
    }

    private val sourceManager = BookSourceManager()
    private val httpHandler = HttpClientFactory.create()
    private val jsEngine = JsEngineImpl()
    private val engine = EngineFacade(httpHandler, jsEngine)

    private val _isOffline = MutableStateFlow(false)

    override val id: Int = "legado-engine".hashCode()
    override val offLine: Boolean = false
    override val isOffLineFlow: StateFlow<Boolean> = _isOffline

    init {
        Log.i(TAG, "LegadoWebDataSource initialized")
    }

    fun loadSourcesFromJson(json: String): Int {
        val count = sourceManager.loadFromJson(json)
        Log.i(TAG, "Loaded $count sources")
        _isOffline.value = sourceManager.size() == 0
        return count
    }

    override suspend fun isOffLine(): Boolean = sourceManager.size() == 0

    // ==================== Explore ====================

    override val explorePageProvider: ExplorePageProvider = object : AbstractDefaultExplorePageProvider() {
        override val explorePageIdList: MutableList<String>
            get() = sourceManager.getEnabledSources()
                .filter { it.ruleExplore?.bookList?.isNotBlank() == true }
                .map { it.bookSourceUrl }
                .toMutableList()

        override val exploreTapPageDataSourceMap: MutableMap<String, ExploreTapPageDataSource>
            get() {
                val map = mutableMapOf<String, ExploreTapPageDataSource>()
                sourceManager.getEnabledSources()
                    .filter { it.ruleExplore?.bookList?.isNotBlank() == true }
                    .forEach { source ->
                        val exploreUrl = (source.searchUrl ?: "").substringBefore("{").ifBlank { source.bookSourceUrl }
                        map[source.bookSourceUrl] = LegadoExploreTapDataSource(
                            source.bookSourceName,
                            source.bookSourceUrl,
                            exploreUrl
                        )
                    }
                return map
            }

        override val exploreExpandedPageDataSourceMap: MutableMap<String, ExploreExpandedPageDataSource>
            get() = mutableMapOf()
    }

    private inner class LegadoExploreTapDataSource(
        private val sourceName: String,
        private val sourceUrl: String,
        private val exploreUrl: String
    ) : ExploreTapPageDataSource {

        override val title: String get() = sourceName

        override fun getRowsFlow(): Flow<List<ExploreBooksRow>> = flow {
            val source = sourceManager.getSource(sourceUrl) ?: run {
                emit(emptyList())
                return@flow
            }
            try {
                val results = engine.getExploreBooks(source, exploreUrl)
                val books = results.map { result ->
                    ExploreDisplayBook(
                        "${source.bookSourceUrl}|${result.bookUrl}",
                        result.name,
                        result.author,
                        Uri.parse(result.coverUrl.takeIf { it.isNotBlank() } ?: "")
                    )
                }
                val row = ExploreBooksRow(
                    sourceName,
                    books,
                    false,
                    ""
                )
                emit(listOf(row))
            } catch (e: Exception) {
                Log.e(TAG, "Explore failed for $sourceUrl", e)
                emit(emptyList())
            }
        }
    }

    // ==================== Search ====================

    override val searchProvider = object : AbstractSearchProvider() {
        override fun search(searchType: SearchType, keyword: String): Flow<SearchResult> = flow {
            for (source in sourceManager.getEnabledSources()) {
                try {
                    val results = engine.search(source, keyword)
                    results.forEach { result ->
                        val bookId = "${source.bookSourceUrl}|${result.bookUrl}"
                        val bookInfo = MutableBookInformation(
                            bookId,
                            result.name,
                            "",
                            Uri.parse(result.coverUrl.takeIf { it.isNotBlank() } ?: ""),
                            result.author,
                            result.intro,
                            emptyList(),
                            "",
                            WordCount(0),
                            LocalDateTime.now(),
                            false
                        )
                        emit(SearchResult.MultipleBook(bookInfo))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Search failed for ${source.bookSourceName}", e)
                }
            }
            emit(SearchResult.End())
        }
    }

    // ==================== Book Info ====================

    override suspend fun getBookInformation(id: String): BookInformation {
        val parsed = parseBookId(id)
        if (parsed == null) return MutableBookInformation.empty()
        val (sourceUrl, bookUrl) = parsed
        val source = sourceManager.getSource(sourceUrl) ?: return MutableBookInformation.empty()

        return try {
            val book = engine.getBookInfo(source, bookUrl)
            MutableBookInformation(
                id,
                book.name,
                "",
                Uri.parse(book.coverUrl.takeIf { it.isNotBlank() } ?: ""),
                book.author,
                book.intro,
                book.kind.split(",").map { it.trim() }.filter { it.isNotBlank() },
                "",
                WordCount(book.wordCount.toIntOrNull() ?: 0),
                LocalDateTime.now(),
                false
            )
        } catch (e: Exception) {
            Log.e(TAG, "getBookInformation failed", e)
            MutableBookInformation.empty()
        }
    }

    // ==================== Book Volumes ====================

    override suspend fun getBookVolumes(id: String): BookVolumes {
        val parsed = parseBookId(id)
        if (parsed == null) return BookVolumes.empty()
        val (sourceUrl, bookUrl) = parsed
        val source = sourceManager.getSource(sourceUrl) ?: return BookVolumes.empty()

        return try {
            val chapters = engine.getChapterList(source, bookUrl)
            val volume = Volume(
                "${id}_vol_0",
                "Chapters",
                chapters.map { chapter ->
                    ChapterInformation(
                        "${sourceUrl}|${chapter.url}",
                        chapter.title
                    )
                }
            )
            BookVolumes(id, listOf(volume))
        } catch (e: Exception) {
            Log.e(TAG, "getBookVolumes failed", e)
            BookVolumes.empty(id)
        }
    }

    // ==================== Chapter Content ====================

    override suspend fun getChapterContent(chapterId: String, bookId: String): ChapterContent {
        val parsed = parseBookId(chapterId)
        if (parsed == null) return ChapterContent.empty(chapterId)
        val (sourceUrl, chapterUrl) = parsed
        val source = sourceManager.getSource(sourceUrl) ?: return ChapterContent.empty(chapterId)

        return try {
            val contentPage = engine.getFullContent(source, chapterUrl)
            val contentJson = buildJsonObject {
                put("text", contentPage.contentLines.joinToString("\n"))
            }
            MutableChapterContent(
                chapterId,
                contentPage.title,
                contentJson,
                "",
                contentPage.nextUrl?.let { "${sourceUrl}|$it" } ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "getChapterContent failed", e)
            ChapterContent.empty(chapterId)
        }
    }

    // ==================== Utilities ====================

    private fun parseBookId(bookId: String): Pair<String, String>? {
        val parts = bookId.split("|", limit = 2)
        return if (parts.size == 2) Pair(parts[0], parts[1]) else null
    }
}



