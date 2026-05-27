package io.legado.engine.contract

/**
 * Abstraction for HTTP requests.
 * The LNR host app implements this using its own network stack.
 */
interface IHttpHandler {
    /**
     * Execute GET request and return HTML/JSON body string.
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String

    /**
     * Execute POST request and return HTML/JSON body string.
     */
    suspend fun post(
        url: String,
        body: String = "",
        headers: Map<String, String> = emptyMap()
    ): String

    /**
     * Get an absolute URL by resolving relative path against baseUrl.
     */
    fun resolveUrl(baseUrl: String, relativeUrl: String): String
}

