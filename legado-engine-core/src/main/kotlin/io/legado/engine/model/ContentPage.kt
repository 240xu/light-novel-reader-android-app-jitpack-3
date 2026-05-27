package io.legado.engine.model

data class ContentPage(
    val title: String = "",
    val contentLines: List<String> = emptyList(),
    val nextUrl: String? = null,
    val images: List<String> = emptyList(),
    val replaceRules: List<String> = emptyList()
) {
    val content: String get() = contentLines.joinToString("\n")
}
