package com.coda.music.data.provider

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

class HttpDownloader private constructor(
    private val client: OkHttpClient
) : Downloader() {

    override fun execute(request: NewPipeRequest): Response {
        val httpMethod = request.httpMethod()
        val url        = request.url()
        val headers    = request.headers()
        val body       = request.dataToSend()

        val requestBody = body?.toRequestBody()

        val builder = Request.Builder().url(url)

        headers.forEach { (key, values) ->
            values.forEach { value -> builder.addHeader(key, value) }
        }

        when (httpMethod) {
            "GET"  -> builder.get()
            "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody())
            "HEAD" -> builder.head()
            else   -> builder.method(httpMethod, requestBody)
        }

        val response = client.newCall(builder.build()).execute()

        if (response.code == 429) {
            throw ReCaptchaException("Rate-limited by YouTube (429)", url)
        }

        val responseBodyText = response.body?.string() ?: ""

        val responseHeaders: Map<String, List<String>> =
            response.headers.toMultimap()

        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBodyText,
            response.request.url.toString()
        )
    }

    companion object {
        @Volatile
        private var instance: HttpDownloader? = null

        fun getInstance(): HttpDownloader =
            instance ?: synchronized(this) {
                instance ?: HttpDownloader(
                    OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build()
                ).also { instance = it }
            }
    }
}
