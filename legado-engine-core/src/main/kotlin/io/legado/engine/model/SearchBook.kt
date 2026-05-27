package io.legado.engine.model

data class SearchBook(
    var bookUrl: String = "",
    var name: String = "",
    var author: String = "",
    var kind: String = "",
    var coverUrl: String = "",
    var intro: String = "",
    var latestChapterTitle: String = "",
    var wordCount: String = "",
    var sourceUrl: String = "",
    var origin: String = "",
    var originName: String = "",
    var originOrder: Int = 0,
    var type: Int = 0,
    var variableMap: HashMap<String, String>? = null
) {
    fun putVariable(key: String, value: String?) {
        if (variableMap == null) variableMap = HashMap()
        if (value == null) variableMap!!.remove(key) else variableMap!![key] = value
    }

    fun getVariable(key: String): String = variableMap?.get(key) ?: ""
}
