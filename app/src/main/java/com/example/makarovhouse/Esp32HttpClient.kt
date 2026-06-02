package com.example.makarovhouse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Esp32HttpClient {
    private const val DEFAULT_TIMEOUT_MS = 3000

    suspend fun sendCommand(host: String, command: String): String {
        return request(host, "/command?value=$command")
    }

    suspend fun fetchStatus(host: String): String {
        return request(host, "/status")
    }

    private suspend fun request(host: String, path: String): String = withContext(Dispatchers.IO) {
        val url = URL("http://$host$path")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = DEFAULT_TIMEOUT_MS
            readTimeout = DEFAULT_TIMEOUT_MS
            doInput = true
            useCaches = false
        }

        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (responseCode in 200..299) {
                body.ifBlank { "OK" }
            } else {
                throw IOException(body.ifBlank { "HTTP $responseCode" })
            }
        } finally {
            connection.disconnect()
        }
    }
}
