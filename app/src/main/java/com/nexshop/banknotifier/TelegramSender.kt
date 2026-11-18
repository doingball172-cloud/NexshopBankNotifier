package com.nexshop.banknotifier

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramSender {
    fun send(token: String, chatId: String, message: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://api.telegram.org/bot$token/sendMessage")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            val body = "chat_id=${enc(chatId)}&text=${enc(message)}&disable_web_page_preview=true"
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            connection.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }
    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}
