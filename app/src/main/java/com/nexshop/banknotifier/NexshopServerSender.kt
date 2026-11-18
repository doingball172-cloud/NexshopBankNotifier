package com.nexshop.banknotifier

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object NexshopServerSender {
    fun send(
        endpoint: String,
        apiToken: String,
        eventId: String,
        sourcePackage: String,
        appName: String,
        title: String,
        body: String,
        postedAt: Long
    ): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(endpoint)
            require(url.protocol.equals("https", ignoreCase = true)) {
                "Only HTTPS endpoints are allowed"
            }
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (apiToken.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $apiToken")
                }
            }
            val payload = JSONObject().apply {
                put("event_id", eventId)
                put("source_package", sourcePackage)
                put("source_app", appName)
                put("title", title)
                put("body", body)
                put("posted_at_unix_ms", postedAt)
            }
            connection.outputStream.use {
                it.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            connection.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }
}
