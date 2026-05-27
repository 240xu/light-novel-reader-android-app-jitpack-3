package io.legado.engine

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import io.legado.engine.analyzer.AnalyzeByJSonPath
import io.legado.engine.analyzer.AnalyzeByJSoup
import io.legado.engine.analyzer.AnalyzeByRegex
import io.legado.engine.analyzer.AnalyzeByXPath
import io.legado.engine.contract.IJsEngine
import io.legado.engine.model.Book
import io.legado.engine.model.BookChapter
import io.legado.engine.model.BookSource
import io.legado.engine.model.rule.BookListRule
import io.legado.engine.model.rule.ReviewRule
import org.mozilla.javascript.NativeObject
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

@Suppress("MemberVisibilityCanBePrivate", "unused")
class AnalyzeRule(
    private var book: Book? = null,
    private var chapter: BookChapter? = null,
    private var source: BookSource? = null,
    private var jsEngine: IJsEngine? = null
) {
    private var content: Any? = null
    private var baseUrl: String? = null
    private var redirectUrl: URL? = null
    private var nextChapterUrl: String? = null
    private var isFromBookInfo: Boolean = false
    private var isRegex: Boolean = false
    private var ruleData: HashMap<String, String>? = null

    private var jsoupAnalyzer: AnalyzeByJSoup? = null
    private var xpathAnalyzer: AnalyzeByXPath? = null
    private var jsonPathAnalyzer: AnalyzeByJSonPath? = null

    private val stringRuleCache = ConcurrentHashMap<String, List<SourceRule>>(16)

    enum class Mode { XPath, Json, Default, Js, Regex, WebJs }

    // ==================== Setup ====================

    fun setBook(book: Book?): AnalyzeRule { this.book = book; return this }
    fun setChapter(chapter: BookChapter?): AnalyzeRule { this.chapter = chapter; return this }
    fun setSource(source: BookSource?): AnalyzeRule { this.source = source; return this }
    fun setJsEngine(engine: IJsEngine?): AnalyzeRule { this.jsEngine = engine; return this }
    fun setBaseUrl(url: String?): AnalyzeRule { this.baseUrl = url; return this }
    fun setNextChapterUrl(url: String?): AnalyzeRule { this.nextChapterUrl = url; return this }
    fun setFromBookInfo(v: Boolean): AnalyzeRule { this.isFromBookInfo = v; return this }
    fun setRedirectUrl(url: String): URL? {
        redirectUrl = try { URL(url) } catch (e: Exception) { null }
        return redirectUrl
    }

    fun setContent(content: Any?, baseUrl: String? = null): AnalyzeRule {
        this.content = content
        this.baseUrl = baseUrl ?: this.baseUrl
        this.redirectUrl = null
        jsoupAnalyzer = null
        xpathAnalyzer = null
        jsonPathAnalyzer = null
        isRegex = false
        stringRuleCache.clear()
        if (content is String) {
            val trimmed = content.trim()
            if (trimmed.startsWith("<") || trimmed.startsWith("<?")) {
                jsoupAnalyzer = AnalyzeByJSoup.parse(content, this.baseUrl ?: "")
            } else if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                jsonPathAnalyzer = AnalyzeByJSonPath.parse(content)
            }
        }
        return this
    }

    // ==================== Core getString ====================

    fun getString(ruleStr: String?, mContent: Any? = null, isUrl: Boolean = false): String {
        if (ruleStr.isNullOrEmpty()) return ""
        val ruleList = splitSourceRuleCacheString(ruleStr)
        return getString(ruleList, mContent, isUrl)
    }

    fun getString(
        ruleList: List<SourceRule>,
        mContent: Any? = null,
        isUrl: Boolean = false,
        unescape: Boolean = true
    ): String {
        var result: Any? = null
        val content = mContent ?: this.content
        if (content != null && ruleList.isNotEmpty()) {
            result = content
            if (result is NativeObject) {
                val sourceRule = ruleList.first()
                putRule(sourceRule.putMap)
                result = if (sourceRule.getParamSize() > 1) {
                    sourceRule.rule
                } else {
                    (result as Map<*, *>)[sourceRule.rule]?.toString()
                }?.let { replaceRegex(it, sourceRule) }
            } else if (result is LinkedTreeMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                result = (result as Map<Any?, Any?>)[ruleList.first().rule]?.toString()
            } else {
                for (sourceRule in ruleList) {
                    putRule(sourceRule.putMap)
                    result ?: continue
                    val rule = sourceRule.rule
                    if (rule.isNotBlank() || sourceRule.replaceRegex.isEmpty()) {
                        result = when (sourceRule.mode) {
                            Mode.WebJs -> getWebJsResult(rule, result!!)
                            Mode.Js -> evalJS(rule, result)
                            Mode.Json -> getJsonAnalyzer(result!!)?.getString(rule)
                            Mode.XPath -> getXPathAnalyzer(result!!)?.getString(rule)
                            Mode.Regex -> {
                                val src = if (result is String) result else result.toString()
                                AnalyzeByRegex.regexFindFirst(src, rule)
                            }
                            Mode.Default -> getJsoupAnalyzer(result!!)?.getString(rule)
                        }
                    }
                    if (result != null && sourceRule.replaceRegex.isNotEmpty()) {
                        result = replaceRegex(result.toString(), sourceRule)
                    }
                }
            }
        }
        if (result == null) result = ""
        val resultStr = result.toString()
        val str = if (unescape && resultStr.indexOf('&') > -1) {
            unescapeHtml(resultStr)
        } else {
            resultStr
        }
        if (isUrl) {
            return if (str.isBlank()) baseUrl ?: ""
            else resolveUrl(redirectUrl?.toString() ?: baseUrl, str)
        }
        return str
    }

    // ==================== getStringList ====================

    fun getStringList(ruleStr: String?, mContent: Any? = null, isUrl: Boolean = false): List<String>? {
        if (ruleStr.isNullOrEmpty()) return null
        val ruleList = splitSourceRuleCacheString(ruleStr)
        return getStringList(ruleList, mContent, isUrl)
    }

    fun getStringList(
        ruleList: List<SourceRule>,
        mContent: Any? = null,
        isUrl: Boolean = false
    ): List<String>? {
        val content = mContent ?: this.content ?: return null
        if (ruleList.isEmpty()) return null
        val sourceRule = ruleList.first()
        putRule(sourceRule.putMap)
        val result = when (sourceRule.mode) {
            Mode.Js -> {
                val jsResult = evalJS(sourceRule.rule, content)
                (jsResult as? List<*>)?.map { it.toString() }
                    ?: jsResult?.toString()?.split("\n")?.filter { it.isNotBlank() }
            }
            Mode.Json -> getJsonAnalyzer(content)?.getStringList(sourceRule.rule)
            Mode.XPath -> getXPathAnalyzer(content)?.getStringList(sourceRule.rule)
            Mode.Regex -> {
                val src = if (content is String) content else content.toString()
                AnalyzeByRegex.regexFindAll(src, sourceRule.rule)
            }
            Mode.Default -> getJsoupAnalyzer(content)?.getStringList(sourceRule.rule)
            else -> null
        } ?: return null
        if (ruleList.size == 1) return result
        return result.map { item ->
            getString(ruleList.subList(1, ruleList.size), item, isUrl)
        }
    }

    // ==================== getElements ====================

    fun getElement(ruleStr: String): Any? {
        if (ruleStr.isBlank()) return null
        val content = this.content ?: return null
        val sourceRule = splitSourceRule(ruleStr).firstOrNull() ?: return null
        return when (sourceRule.mode) {
            Mode.XPath -> getXPathAnalyzer(content)?.getElement(sourceRule.rule)
            Mode.Default -> getJsoupAnalyzer(content)?.getElement(sourceRule.rule)
            else -> null
        }
    }

    fun getElements(ruleStr: String): List<Any> {
        if (ruleStr.isBlank()) return emptyList()
        val content = this.content ?: return emptyList()
        val sourceRule = splitSourceRule(ruleStr).firstOrNull() ?: return emptyList()
        return when (sourceRule.mode) {
            Mode.XPath -> getXPathAnalyzer(content)?.getElements(sourceRule.rule) ?: emptyList()
            Mode.Default -> getJsoupAnalyzer(content)?.getElements(sourceRule.rule) ?: emptyList()
            else -> emptyList()
        }
    }

    // ==================== Review/Paragraph Comments ====================

    fun getReviewList(reviewRule: ReviewRule?): List<Map<String, String>> {
        if (reviewRule?.reviewUrl.isNullOrBlank()) return emptyList()
        val result = mutableListOf<Map<String, String>>()
        try {
            val reviewHtml = getString(reviewRule!!.reviewUrl) ?: return emptyList()
            if (reviewHtml.isBlank()) return emptyList()
            setContent(reviewHtml, baseUrl)
            val avatarList = getStringList(reviewRule.avatarRule)
            val contentList = getStringList(reviewRule.contentRule)
            val timeList = getStringList(reviewRule.postTimeRule)
            val size = maxOf(avatarList?.size ?: 0, contentList?.size ?: 0, timeList?.size ?: 0)
            for (i in 0 until size) {
                result.add(buildMap {
                    avatarList?.getOrNull(i)?.let { put("avatar", it) }
                    contentList?.getOrNull(i)?.let { put("content", it) }
                    timeList?.getOrNull(i)?.let { put("time", it) }
                })
            }
        } catch (e: Exception) {
            // ignore
        }
        return result
    }

    // ==================== JavaScript ====================

    fun evalJS(jsStr: String, result: Any? = null): Any? {
        val engine = jsEngine ?: return null
        val bindings = mutableMapOf<String, Any?>(
            "java" to this,
            "source" to source,
            "book" to book,
            "result" to result,
            "baseUrl" to baseUrl,
            "chapter" to chapter,
            "title" to chapter?.title,
            "src" to content,
            "nextChapterUrl" to nextChapterUrl,
            "fromBookInfo" to isFromBookInfo,
            "chapterIndex" to (chapter?.index ?: 0),
            "chapterTitle" to (chapter?.title ?: ""),
            "bookUrl" to (book?.bookUrl ?: ""),
            "bookName" to (book?.name ?: ""),
            "bookAuthor" to (book?.author ?: ""),
            "sourceUrl" to (source?.bookSourceUrl ?: ""),
            "redirectUrl" to (redirectUrl?.toString() ?: baseUrl ?: "")
        )
        return try {
            kotlinx.coroutines.runBlocking { engine.eval(jsStr, bindings) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getWebJsResult(jsStr: String, result: Any): String {
        return evalJS(jsStr, result)?.toString() ?: ""
    }

    // ==================== Variable Substitution ====================

    fun put(key: String, value: String): String {
        chapter?.putVariable(key, value)
            ?: book?.putVariable(key, value)
            ?: run { if (ruleData == null) ruleData = HashMap(); ruleData!![key] = value }
        return value
    }

    fun get(key: String): String {
        return chapter?.getVariable(key)
            ?: book?.getVariable(key)
            ?: ruleData?.get(key)
            ?: source?.let { "" }
            ?: ""
    }

    private fun putRule(map: Map<String, String>) {
        map.forEach { (k, v) -> put(k, v) }
    }

    // ==================== Rule Parsing ====================

    private fun splitSourceRuleCacheString(ruleStr: String?): List<SourceRule> {
        if (ruleStr.isNullOrEmpty()) return emptyList()
        return stringRuleCache.getOrPut(ruleStr) { splitSourceRule(ruleStr) }
    }

    fun splitSourceRule(ruleStr: String?, allInOne: Boolean = false): List<SourceRule> {
        if (ruleStr.isNullOrEmpty()) return emptyList()
        val ruleList = ArrayList<SourceRule>()
        var mMode: Mode = Mode.Default
        var start = 0
        if (allInOne && ruleStr.startsWith(":")) {
            mMode = Mode.Regex; isRegex = true; start = 1
        } else if (isRegex) {
            mMode = Mode.Regex
        }
        // Extract <js>...</js> and @js:...
        val jsMatcher = JS_PATTERN.matcher(ruleStr)
        while (jsMatcher.find()) {
            if (jsMatcher.start() > start) {
                val tmp = ruleStr.substring(start, jsMatcher.start()).trim()
                if (tmp.isNotEmpty()) ruleList.add(SourceRule(tmp, mMode))
            }
            ruleList.add(SourceRule(jsMatcher.group(2) ?: jsMatcher.group(1) ?: "", Mode.Js))
            start = jsMatcher.end()
        }
        // Extract @webjs:...
        val webJsMatcher = WebJS_PATTERN.matcher(ruleStr)
        while (webJsMatcher.find()) {
            if (webJsMatcher.start() > start) {
                val tmp = ruleStr.substring(start, webJsMatcher.start()).trim()
                if (tmp.isNotEmpty()) ruleList.add(SourceRule(tmp, mMode))
            }
            ruleList.add(SourceRule(webJsMatcher.group(1) ?: "", Mode.WebJs))
            start = webJsMatcher.end()
        }
        if (ruleStr.length > start) {
            val tmp = ruleStr.substring(start).trim()
            if (tmp.isNotEmpty()) ruleList.add(SourceRule(tmp, mMode))
        }
        return ruleList
    }

    private fun splitPutRule(ruleStr: String, putMap: HashMap<String, String>): String {
        val matcher = PUT_PATTERN.matcher(ruleStr)
        if (!matcher.find()) return ruleStr
        var result = ruleStr
        do {
            val group = matcher.group(1) ?: continue
            val kv = group.removePrefix("{").removeSuffix("}").split(",", limit = 2)
            if (kv.size == 2) {
                putMap[kv[0].trim()] = kv[1].trim()
            }
            result = result.replace(matcher.group(), "")
        } while (matcher.find())
        return result.trim()
    }

    private fun replaceRegex(result: String, rule: SourceRule): String {
        if (rule.replaceRegex.isEmpty()) return result
        return try {
            val regex = Regex(rule.replaceRegex)
            if (rule.replaceFirst) {
                regex.replaceFirst(result, rule.replacement)
            } else {
                regex.replace(result, rule.replacement)
            }
        } catch (e: Exception) {
            result
        }
    }

    private fun substituteVariables(str: String): String {
        var result = str
        val matcher = EVAL_PATTERN.matcher(result)
        while (matcher.find()) {
            val key = matcher.group()?.let {
                if (it.startsWith("{{")) it.substring(2, it.length - 2)
                else it.substring(6, it.length - 1)
            } ?: continue
            val value = get(key)
            result = result.replace(matcher.group(), value)
        }
        return result
    }

    // ==================== Analyzer Getters ====================

    private fun getJsoupAnalyzer(o: Any): AnalyzeByJSoup? {
        if (jsoupAnalyzer != null) return jsoupAnalyzer
        val html = o as? String ?: return null
        jsoupAnalyzer = AnalyzeByJSoup.parse(html, baseUrl ?: "")
        return jsoupAnalyzer
    }

    private fun getXPathAnalyzer(o: Any): AnalyzeByXPath? {
        if (xpathAnalyzer != null) return xpathAnalyzer
        val html = o as? String ?: return null
        xpathAnalyzer = AnalyzeByXPath.parse(html, baseUrl ?: "")
        return xpathAnalyzer
    }

    private fun getJsonAnalyzer(o: Any): AnalyzeByJSonPath? {
        if (jsonPathAnalyzer != null) return jsonPathAnalyzer
        val json = o as? String ?: return null
        jsonPathAnalyzer = AnalyzeByJSonPath.parse(json)
        return jsonPathAnalyzer
    }

    // ==================== SourceRule Inner Class ====================

    inner class SourceRule internal constructor(
        ruleStr: String,
        internal var mode: Mode = Mode.Default
    ) {
        internal var rule: String
        internal var replaceRegex = ""
        internal var replacement = ""
        internal var replaceFirst = false
        internal val putMap = HashMap<String, String>()
        private val ruleParam = ArrayList<String>()

        init {
            rule = when {
                mode == Mode.Js || mode == Mode.Regex -> ruleStr
                ruleStr.startsWith("@CSS:", ignoreCase = true) -> { mode = Mode.Default; ruleStr }
                ruleStr.startsWith("@@") -> { mode = Mode.Default; ruleStr.substring(2) }
                ruleStr.startsWith("@XPath:", ignoreCase = true) -> { mode = Mode.XPath; ruleStr.substring(7) }
                ruleStr.startsWith("@Json:", ignoreCase = true) -> { mode = Mode.Json; ruleStr.substring(6) }
                ruleStr.startsWith("$.") || ruleStr.startsWith("$[") -> { mode = Mode.Json; ruleStr }
                ruleStr.startsWith("//") -> { mode = Mode.XPath; ruleStr }
                ruleStr.startsWith("/") && ruleStr.endsWith("/") -> { mode = Mode.Regex; ruleStr }
                else -> ruleStr
            }
            // Split @put:{}
            rule = splitPutRule(rule, putMap)
            // Handle {{}} variable substitution
            rule = substituteVariables(rule)
            // Split replacement rules (##)
            val hashIdx = rule.indexOf("##")
            if (hashIdx >= 0) {
                val parts = rule.substring(hashIdx + 2).split("#", limit = 3)
                rule = rule.substring(0, hashIdx)
                if (parts.isNotEmpty()) replaceRegex = parts[0]
                if (parts.size > 1) replacement = parts[1]
                if (parts.size > 2) replaceFirst = parts[2] == "1"
            }
        }

        fun getParamSize(): Int = ruleParam.size

        override fun toString(): String = "SourceRule(mode=$mode, rule=$rule)"
    }

    // ==================== Utilities ====================

    companion object {
        private val JS_PATTERN = Pattern.compile("<js>([\\w\\W]*?)</js>|@js:([\\w\\W]*)", Pattern.CASE_INSENSITIVE)
        private val WebJS_PATTERN = Pattern.compile("@webjs:([\\w\\W]{5,})", Pattern.CASE_INSENSITIVE)
        private val PUT_PATTERN = Pattern.compile("@put:(\\{[^}]+?\\})", Pattern.CASE_INSENSITIVE)
        private val EVAL_PATTERN = Pattern.compile("@get:\\{[^}]+?\\}|\\{\\{[\\w\\W]*?\\}\\}", Pattern.CASE_INSENSITIVE)

        private fun resolveUrl(baseUrl: String?, relativeUrl: String): String {
            if (baseUrl.isNullOrBlank()) return relativeUrl
            return try { URL(URL(baseUrl), relativeUrl).toString() } catch (e: Exception) { relativeUrl }
        }

        private fun unescapeHtml(str: String): String {
            return str.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
        }
    }
}

