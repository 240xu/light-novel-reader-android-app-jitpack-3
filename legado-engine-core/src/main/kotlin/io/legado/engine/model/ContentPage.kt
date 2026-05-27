package io.legado.engine.model

/**
 * Content page result model.
 */
data class ContentPage(
    val title: String = "",
    val contentLines: List<String> = emptyList(),
    val nextUrl: String? = null
)
