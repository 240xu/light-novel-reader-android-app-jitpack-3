package io.legado.engine.model

data class Book(
    var bookUrl: String = "",
    var tocUrl: String = "",
    var origin: String = "",
    var originName: String = "",
    var originOrder: Int = 0,
    var name: String = "",
    var author: String = "",
    var kind: String = "",
    var coverUrl: String = "",
    var intro: String = "",
    var customIntro: String? = null,
    var charset: String? = null,
    var type: Int = 0,
    var group: Int = 0,
    var latestChapterTitle: String = "",
    var latestChapterTime: Long = 0,
    var lastCheckTime: Long = 0,
    var lastCheckCount: Int = 0,
    var totalChapterNum: Int = 0,
    var durChapterTitle: String = "",
    var durChapterIndex: Int = 0,
    var durChapterPos: Int = 0,
    var durChapterTime: Long = 0,
    var wordCount: String = "",
    var canUpdate: Boolean = true,
    var variableMap: HashMap<String, String>? = null,
    var kindList: List<String> = emptyList()
) {
    fun putVariable(key: String, value: String?) {
        if (variableMap == null) variableMap = HashMap()
        if (value == null) variableMap!!.remove(key) else variableMap!![key] = value
    }

    fun getVariable(key: String): String = variableMap?.get(key) ?: ""
}
