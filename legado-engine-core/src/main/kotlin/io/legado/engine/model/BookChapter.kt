package io.legado.engine.model

data class BookChapter(
    var url: String = "",
    var title: String = "",
    var volume: String = "",
    var baseUrl: String = "",
    var bookUrl: String = "",
    var index: Int = 0,
    var isVip: Boolean = false,
    var isPay: Boolean = false,
    var resourceUrl: String? = null,
    var tag: String? = null,
    var start: Long = 0,
    var end: Long = 0,
    var variableMap: HashMap<String, String>? = null
) {
    fun putVariable(key: String, value: String?) {
        if (variableMap == null) variableMap = HashMap()
        if (value == null) variableMap!!.remove(key) else variableMap!![key] = value
    }

    fun getVariable(key: String): String = variableMap?.get(key) ?: ""
}
