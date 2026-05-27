package io.github.dmzz_yyhyy.lnrplugin.util

import io.legado.engine.interface.IHttpHandler
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * HTTP client factory.
 * Creates IHttpHandler implementations using OkHttp.
 */
object HttpClientFactory {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun create(): IHttpHandler = OkHttpHandler()

    private class OkHttpHandler : IHttpHandler {

        override suspend fun get(url: String, headers: Map<String, String>): String {
            val requestBuilder = Request.Builder().url(url).get()
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
            // Default headers to mimic a browser
            if (!headers.containsKey("User-Agent")) {
                requestBuilder.addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
            }
            val response = client.newCall(requestBuilder.build()).execute()
            return response.body?.string() ?: ""
        }

        override suspend fun post(url: String, body: String, headers: Map<String, String>): String {
            val requestBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                body
            )
            val requestBuilder = Request.Builder().url(url).post(requestBody)
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
            if (!headers.containsKey("User-Agent")) {
                requestBuilder.addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            }
            val response = client.newCall(requestBuilder.build()).execute()
            return response.body?.string() ?: ""
        }

        override fun resolveUrl(baseUrl: String, relativeUrl: String): String {
            return try {
                URL(URL(baseUrl), relativeUrl).toString()
            } catch (e: Exception) {
                relativeUrl
            }
        }
    }
}
