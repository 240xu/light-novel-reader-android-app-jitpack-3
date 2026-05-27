package io.legado.engine.analyzer

/**
 * Regex-based analyzer.
 * Extracted from Legado AnalyzeByRegex, zero external dependencies.
 *
 * Supports:
 *   - Simple regex matching: regex pattern
 *   - Group extraction: regex(pattern) or regex(pattern)(groupIndex)
 *   - Replacement: regex(replacement)
 */
class AnalyzeByRegex {

    companion object {
        /**
         * Check if a rule is a regex rule.
         */
        fun isRegexRule(rule: String): Boolean {
            return rule.startsWith("/") || rule.matches(Regex("^/(.*)/$"))
        }

        /**
         * Apply regex to content and return matched groups.
         */
        fun regexFind(content: String, pattern: String): List<String> {
            return try {
                val regex = pattern.trim().toRegex()
                val match = regex.find(content) ?: return emptyList()
                match.groupValues
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Apply regex and return the first group or full match.
         */
        fun regexFindFirst(content: String, pattern: String): String {
            val groups = regexFind(content, pattern)
            return if (groups.size > 1) groups[1] else groups.firstOrNull() ?: ""
        }

        /**
         * Apply regex and return all matches.
         */
        fun regexFindAll(content: String, pattern: String): List<String> {
            return try {
                val regex = pattern.trim().toRegex()
                regex.findAll(content).map { it.value }.toList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Replace content using regex.
         */
        fun regexReplace(content: String, pattern: String, replacement: String): String {
            return try {
                content.replace(Regex(pattern), replacement)
            } catch (e: Exception) {
                content
            }
        }

        /**
         * Parse Legado regex rule format: /pattern/replacement/
         * Returns (pattern, replacement) pair.
         */
        fun parseRegexRule(rule: String): Pair<String, String> {
            val trimmed = rule.trim()
            if (!trimmed.startsWith("/")) return Pair(trimmed, "")
            val parts = trimmed.removePrefix("/").split("/")
            return when {
                parts.size >= 2 -> Pair(parts[0], parts.drop(1).joinToString("/"))
                else -> Pair(parts[0], "")
            }
        }
    }
}
