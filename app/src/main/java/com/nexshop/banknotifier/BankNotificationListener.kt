package com.nexshop.banknotifier

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Bundle
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BankNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
        val selectedPackage = prefs.getString("source_package", "") ?: ""
        val token = prefs.getString("token", "") ?: ""
        val chatId = prefs.getString("chat_id", "") ?: ""
        val serverUrl = prefs.getString("server_url", "")?.trim().orEmpty()
        val serverToken = prefs.getString("server_token", "")?.trim().orEmpty()

        if (selectedPackage.isBlank() || sbn.packageName != selectedPackage) return

        val extras: Bundle = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString()?.trim().orEmpty()
        val lines = extras.getCharSequenceArray("android.textLines")
            ?.map { it.toString() }?.filter { it.isNotBlank() }?.joinToString("\n").orEmpty()
        val body = listOf(title, bigText.ifBlank { text }, lines)
            .filter { it.isNotBlank() }.distinct().joinToString("\n")
        if (body.isBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (_: Exception) { sbn.packageName }

        val postedAt = sbn.postTime
        val time = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            .format(Date(postedAt))
        val message = "🔔 $appName\n$body\n\n🕒 $time"

        // Stable ID allows the server to reject duplicate delivery attempts.
        val rawId = "${sbn.packageName}|${sbn.key}|$postedAt"
        val eventId = sha256(rawId)

        if (token.isNotBlank() && chatId.isNotBlank()) {
            Thread { TelegramSender.send(token, chatId, message) }.start()
        }

        if (serverUrl.isNotBlank()) {
            Thread {
                NexshopServerSender.send(
                    endpoint = serverUrl,
                    apiToken = serverToken,
                    eventId = eventId,
                    sourcePackage = sbn.packageName,
                    appName = appName,
                    title = title,
                    body = body,
                    postedAt = postedAt
                )
            }.start()
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
