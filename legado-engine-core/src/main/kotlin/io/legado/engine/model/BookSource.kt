package io.legado.engine.model

/**
 * Book source rule definition.
 * Compatible with Legado book source JSON format.
 */
data class BookSource(
    val bookSourceUrl: String = "",
    val bookSourceName: String = "",
    val bookSourceGroup: String = "",
    val bookSourceType: Int = 0,
    val enabled: Boolean = true,
    val bookUrlPattern: String = "",
    val header: String = "",
    val searchUrl: String = "",
    val ruleSearch: RuleSearch = RuleSearch(),
    val ruleBookInfo: RuleBookInfo = RuleBookInfo(),
    val ruleToc: RuleToc = RuleToc(),
    val ruleContent: RuleContent = RuleContent(),
    val ruleExplore: RuleExplore = RuleExplore(),
    // JS engine for custom rule execution
    val jsEngine: String = "rhino"
)

data class RuleSearch(
    val bookList: String = "",
    val name: String = "",
    val author: String = "",
    val bookUrl: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val kind: String = "",
    val lastChapterTitle: String = "",
    val wordCount: String = ""
)

data class RuleBookInfo(
    val name: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val kind: String = "",
    val lastChapterTitle: String = "",
    val wordCount: String = "",
    val canReName: String = ""
)

data class RuleToc(
    val chapterList: String = "",
    val chapterName: String = "",
    val chapterUrl: String = "",
    val isVip: String = "",
    val isPay: String = "",
    val updateTime: String = "",
    val volumeName: String = ""
)

data class RuleContent(
    val content: String = "",
    val title: String = "",
    val nextContentUrl: String = "",
    val replaceRegex: String = "",
    val imageStyle: String = "",
    val payAction: String = ""
)

data class RuleExplore(
    val bookList: String = "",
    val name: String = "",
    val author: String = "",
    val bookUrl: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val kind: String = "",
    val lastChapterTitle: String = "",
    val wordCount: String = ""
)
