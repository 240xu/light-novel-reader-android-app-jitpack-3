package io.github.dmzz_yyhyy.lnrplugin.util

import io.legado.engine.contract.IHttpHandler
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import java.util.concurrent.TimeUnit

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
        private val defaultUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

        override suspend fun get(url: String, headers: Map<String, String>): String {
            val requestBuilder = Request.Builder().url(url).get()
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
            if (!headers.containsKey("User-Agent")) requestBuilder.addHeader("User-Agent", defaultUA)
            val response = client.newCall(requestBuilder.build()).execute()
            return response.body?.string() ?: ""
        }

        override suspend fun post(url: String, body: String, headers: Map<String, String>): String {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = body.toRequestBody(mediaType)
            val requestBuilder = Request.Builder().url(url).post(requestBody)
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
            if (!headers.containsKey("User-Agent")) requestBuilder.addHeader("User-Agent", defaultUA)
            val response = client.newCall(requestBuilder.build()).execute()
            return response.body?.string() ?: ""
        }

        override fun resolveUrl(baseUrl: String, relativeUrl: String): String {
            return try { URL(URL(baseUrl), relativeUrl).toString() } catch (e: Exception) { relativeUrl }
        }
    }
}
