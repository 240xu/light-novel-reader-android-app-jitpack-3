package io.legado.engine.model

import io.legado.engine.model.rule.BookInfoRule
import io.legado.engine.model.rule.ContentRule
import io.legado.engine.model.rule.ExploreRule
import io.legado.engine.model.rule.ReviewRule
import io.legado.engine.model.rule.SearchRule
import io.legado.engine.model.rule.TocRule

data class BookSource(
    var bookSourceUrl: String = "",
    var bookSourceName: String = "",
    var bookSourceGroup: String? = null,
    var bookSourceType: Int = 0,
    var bookUrlPattern: String? = null,
    var customOrder: Int = 0,
    var enabled: Boolean = true,
    var enabledExplore: Boolean = true,
    var jsLib: String? = null,
    var enabledCookieJar: Boolean? = false,
    var concurrentRate: String? = null,
    var header: String? = null,
    var loginUrl: String? = null,
    var loginUi: String? = null,
    var loginCheckJs: String? = null,
    var coverDecodeJs: String? = null,
    var bookSourceComment: String? = null,
    var variableComment: String? = null,
    var lastUpdateTime: Long = 0,
    var respondTime: Long = 180000L,
    var weight: Int = 0,
    var exploreUrl: String? = null,
    var exploreScreen: String? = null,
    var ruleExplore: ExploreRule? = null,
    var searchUrl: String? = null,
    var ruleSearch: SearchRule? = null,
    var ruleBookInfo: BookInfoRule? = null,
    var ruleToc: TocRule? = null,
    var ruleContent: ContentRule? = null,
    var ruleReview: ReviewRule? = null,
    var eventListener: Boolean = false
) {
    companion object {
        fun fromJsonMap(map: Map<String, Any?>): BookSource {
            return BookSource(
                bookSourceUrl = map["bookSourceUrl"] as? String ?: "",
                bookSourceName = map["bookSourceName"] as? String ?: "",
                bookSourceGroup = map["bookSourceGroup"] as? String,
                bookSourceType = (map["bookSourceType"] as? Number)?.toInt() ?: 0,
                bookUrlPattern = map["bookUrlPattern"] as? String,
                customOrder = (map["customOrder"] as? Number)?.toInt() ?: 0,
                enabled = map["enabled"] as? Boolean ?: true,
                enabledExplore = map["enabledExplore"] as? Boolean ?: true,
                jsLib = map["jsLib"] as? String,
                enabledCookieJar = map["enabledCookieJar"] as? Boolean ?: false,
                concurrentRate = map["concurrentRate"] as? String,
                header = map["header"] as? String,
                loginUrl = map["loginUrl"] as? String,
                loginUi = map["loginUi"] as? String,
                loginCheckJs = map["loginCheckJs"] as? String,
                coverDecodeJs = map["coverDecodeJs"] as? String,
                bookSourceComment = map["bookSourceComment"] as? String,
                variableComment = map["variableComment"] as? String,
                lastUpdateTime = (map["lastUpdateTime"] as? Number)?.toLong() ?: 0,
                respondTime = (map["respondTime"] as? Number)?.toLong() ?: 180000L,
                weight = (map["weight"] as? Number)?.toInt() ?: 0,
                exploreUrl = map["exploreUrl"] as? String,
                exploreScreen = map["exploreScreen"] as? String,
                searchUrl = map["searchUrl"] as? String,
                eventListener = map["eventListener"] as? Boolean ?: false,
                ruleExplore = parseExploreRule(map["ruleExplore"]),
                ruleSearch = parseSearchRule(map["ruleSearch"]),
                ruleBookInfo = parseBookInfoRule(map["ruleBookInfo"]),
                ruleToc = parseTocRule(map["ruleToc"]),
                ruleContent = parseContentRule(map["ruleContent"]),
                ruleReview = parseReviewRule(map["ruleReview"])
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseExploreRule(v: Any?): ExploreRule? {
            val m = v as? Map<String, Any?> ?: return null
            return ExploreRule(
                bookList = m["bookList"] as? String,
                name = m["name"] as? String,
                author = m["author"] as? String,
                intro = m["intro"] as? String,
                kind = m["kind"] as? String,
                lastChapter = m["lastChapter"] as? String,
                updateTime = m["updateTime"] as? String,
                bookUrl = m["bookUrl"] as? String,
                coverUrl = m["coverUrl"] as? String,
                wordCount = m["wordCount"] as? String
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseSearchRule(v: Any?): SearchRule? {
            val m = v as? Map<String, Any?> ?: return null
            return SearchRule(
                checkKeyWord = m["checkKeyWord"] as? String,
                bookList = m["bookList"] as? String,
                name = m["name"] as? String,
                author = m["author"] as? String,
                intro = m["intro"] as? String,
                kind = m["kind"] as? String,
                lastChapter = m["lastChapter"] as? String,
                updateTime = m["updateTime"] as? String,
                bookUrl = m["bookUrl"] as? String,
                coverUrl = m["coverUrl"] as? String,
                wordCount = m["wordCount"] as? String
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseBookInfoRule(v: Any?): BookInfoRule? {
            val m = v as? Map<String, Any?> ?: return null
            return BookInfoRule(
                init = m["init"] as? String,
                name = m["name"] as? String,
                author = m["author"] as? String,
                intro = m["intro"] as? String,
                kind = m["kind"] as? String,
                lastChapter = m["lastChapter"] as? String,
                updateTime = m["updateTime"] as? String,
                coverUrl = m["coverUrl"] as? String,
                tocUrl = m["tocUrl"] as? String,
                wordCount = m["wordCount"] as? String,
                canReName = m["canReName"] as? String,
                downloadUrls = m["downloadUrls"] as? String
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseTocRule(v: Any?): TocRule? {
            val m = v as? Map<String, Any?> ?: return null
            return TocRule(
                preUpdateJs = m["preUpdateJs"] as? String,
                chapterList = m["chapterList"] as? String,
                chapterName = m["chapterName"] as? String,
                chapterUrl = m["chapterUrl"] as? String,
                formatJs = m["formatJs"] as? String,
                isVolume = m["isVolume"] as? String,
                isVip = m["isVip"] as? String,
                isPay = m["isPay"] as? String,
                updateTime = m["updateTime"] as? String,
                nextTocUrl = m["nextTocUrl"] as? String
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseContentRule(v: Any?): ContentRule? {
            val m = v as? Map<String, Any?> ?: return null
            return ContentRule(
                content = m["content"] as? String,
                subContent = m["subContent"] as? String,
                title = m["title"] as? String,
                nextContentUrl = m["nextContentUrl"] as? String,
                webJs = m["webJs"] as? String,
                sourceRegex = m["sourceRegex"] as? String,
                replaceRegex = m["replaceRegex"] as? String,
                imageStyle = m["imageStyle"] as? String,
                imageDecode = m["imageDecode"] as? String,
                payAction = m["payAction"] as? String,
                callBackJs = m["callBackJs"] as? String
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseReviewRule(v: Any?): ReviewRule? {
            val m = v as? Map<String, Any?> ?: return null
            return ReviewRule(
                reviewUrl = m["reviewUrl"] as? String,
                avatarRule = m["avatarRule"] as? String,
                contentRule = m["contentRule"] as? String,
                postTimeRule = m["postTimeRule"] as? String,
                reviewQuoteUrl = m["reviewQuoteUrl"] as? String,
                voteUpUrl = m["voteUpUrl"] as? String,
                voteDownUrl = m["voteDownUrl"] as? String,
                postReviewUrl = m["postReviewUrl"] as? String,
                postQuoteUrl = m["postQuoteUrl"] as? String,
                deleteUrl = m["deleteUrl"] as? String
            )
        }
    }
}

