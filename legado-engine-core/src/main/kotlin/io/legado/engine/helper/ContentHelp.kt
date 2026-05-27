package io.legado.engine.helper

/**
 * Content processing helper.
 * Extracted from Legado ContentHelp, pure regex, zero external dependencies.
 * Handles text cleanup, format normalization, and content filtering.
 */
object ContentHelp {

    /**
     * Format content for display.
     * Normalizes whitespace, removes empty lines, trims paragraphs.
     */
    fun formatContent(content: String): String {
        if (content.isBlank()) return ""
        return content
            .replace("\\r\\n".toRegex(), "\n")
            .replace("\\r".toRegex(), "\n")
            .replace("[\\u00a0\\u3000]+".toRegex(), " ")
            .replace("\\s*\\n\\s*".toRegex(), "\n")
            .replace("\\n{3,}".toRegex(), "\n\n")
            .trim()
    }

    /**
     * Replace content using regex rules from source.
     * @param replaceRegex Format: "/pattern/replacement/\n/pattern2/replacement2/"
     */
    fun replaceContent(content: String, replaceRegex: String): String {
        if (replaceRegex.isBlank()) return content
        var result = content
        replaceRegex.split("\n").forEach { rule ->
            val trimmed = rule.trim()
            if (trimmed.startsWith("/")) {
                val parts = trimmed.removePrefix("/").split("/")
                if (parts.size >= 2) {
                    try {
                        result = result.replace(Regex(parts[0]), parts.drop(1).joinToString("/"))
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return result
    }

    /**
     * Remove HTML tags from content, preserving text.
     */
    fun removeHtmlTags(html: String): String {
        if (html.isBlank()) return ""
        return html
            .replace("<br\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
            .replace("<p[^>]*>".toRegex(RegexOption.IGNORE_CASE), "\n")
            .replace("</p>".toRegex(RegexOption.IGNORE_CASE), "\n")
            .replace("<[^>]+>".toRegex(), "")
            .replace("&nbsp;".toRegex(), " ")
            .replace("&lt;".toRegex(), "<")
            .replace("&gt;".toRegex(), ">")
            .replace("&amp;".toRegex(), "&")
            .replace("&quot;".toRegex(), "\"")
            .replace("&#39;".toRegex(), "'")
            .replace("\\n{3,}".toRegex(), "\n\n")
            .trim()
    }

    /**
     * Remove advertising content patterns.
     */
    fun removeAds(content: String, patterns: List<String> = defaultAdPatterns()): String {
        if (content.isBlank() || patterns.isEmpty()) return content
        var result = content
        patterns.forEach { pattern ->
            try {
                result = result.replace(Regex(pattern, RegexOption.IGNORE_CASE), "")
            } catch (_: Exception) {
            }
        }
        return result
    }

    /**
     * Default advertisement patterns commonly found in Chinese light novel sites.
     */
    fun defaultAdPatterns(): List<String> {
        return listOf(
            "(百度搜索|手机阅读).+?最新章节",
            "最新章节.+?无弹窗",
            "天才一秒记住.+?地址",
            "手机用户请浏览.+?阅读",
            "本章未完，请点击.*继续阅读",
            "(一秒记住|笔趣阁).+?\\.",
            "【.*?】.*?最快更新",
            "请收藏.*?\\.com",
            "亲,点击进去,给个好评呗.*?分数越高更新越快",
            "天才壹秒記住.*?網"
        )
    }

    /**
     * Truncate content to a maximum number of characters.
     */
    fun truncate(content: String, maxLength: Int): String {
        if (content.length <= maxLength) return content
        return content.take(maxLength) + "..."
    }

    /**
     * Get the first N characters of content as an excerpt.
     */
    fun getExcerpt(content: String, length: Int = 100): String {
        val cleaned = removeHtmlTags(content).trim()
        return truncate(cleaned, length)
    }
}
