package com.nexshop.banknotifier

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private lateinit var token: EditText
    private lateinit var chatId: EditText
    private lateinit var serverUrl: EditText
    private lateinit var serverToken: EditText
    private lateinit var apps: Spinner
    private var appPackages: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
        }
        fun addLabel(text: String) {
            layout.addView(TextView(this).apply {
                this.text = text
                textSize = 15f
                setPadding(0, 16, 0, 6)
            })
        }
        fun addEdit(hintText: String, value: String = "", secret: Boolean = false): EditText {
            return EditText(this).apply {
                hint = hintText
                setSingleLine(true)
                if (secret) inputType = 129
                setText(value)
                layout.addView(this)
            }
        }

        layout.addView(TextView(this).apply {
            text = "NEXSHOP BANK NOTIFIER"
            textSize = 22f
            setPadding(0, 0, 0, 12)
        })

        addLabel("Telegram Bot Token")
        token = addEdit("Bot token", prefs.getString("token", "") ?: "")
        addLabel("Telegram Chat ID")
        chatId = addEdit("Например: 123456789", prefs.getString("chat_id", "") ?: "")

        addLabel("Сервер NEXSHOP HTTPS URL")
        serverUrl = addEdit(
            "https://your-domain.tld/api/bank-notifications",
            prefs.getString("server_url", "") ?: ""
        )
        addLabel("Ключ доступа NEXSHOP (Bearer token)")
        serverToken = addEdit(
            "Секретный ключ сервера",
            prefs.getString("server_token", "") ?: "",
            secret = true
        )

        addLabel("Приложение, уведомления которого пересылать")
        apps = Spinner(this)
        layout.addView(apps)
        loadApps(prefs.getString("source_package", "") ?: "")

        val save = Button(this).apply { text = "Сохранить настройки" }
        layout.addView(save)
        save.setOnClickListener {
            val url = serverUrl.text.toString().trim()
            if (url.isNotBlank() && !url.startsWith("https://", true)) {
                Toast.makeText(this, "Сервер должен использовать HTTPS", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            prefs.edit()
                .putString("token", token.text.toString().trim())
                .putString("chat_id", chatId.text.toString().trim())
                .putString("server_url", url)
                .putString("server_token", serverToken.text.toString().trim())
                .putString("source_package", appPackages.getOrNull(apps.selectedItemPosition) ?: "")
                .apply()
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
        }

        val permission = Button(this).apply { text = "1. Разрешить доступ к уведомлениям" }
        layout.addView(permission)
        permission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        val testTelegram = Button(this).apply { text = "2. Отправить тест в Telegram" }
        layout.addView(testTelegram)
        testTelegram.setOnClickListener {
            save.performClick()
            val t = token.text.toString().trim()
            val c = chatId.text.toString().trim()
            if (t.isBlank() || c.isBlank()) {
                Toast.makeText(this, "Заполни Telegram Token и Chat ID", Toast.LENGTH_LONG).show()
            } else {
                Thread {
                    val ok = TelegramSender.send(
                        t, c, "✅ NEXSHOP BANK NOTIFIER подключён. Тестовое сообщение."
                    )
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            if (ok) "Тест Telegram отправлен" else "Ошибка Telegram. Проверь настройки.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.start()
            }
        }

        val testServer = Button(this).apply { text = "3. Отправить тест на сервер NEXSHOP" }
        layout.addView(testServer)
        testServer.setOnClickListener {
            save.performClick()
            val url = serverUrl.text.toString().trim()
            val key = serverToken.text.toString().trim()
            if (url.isBlank()) {
                Toast.makeText(this, "Укажи HTTPS URL сервера", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            Thread {
                val ok = NexshopServerSender.send(
                    endpoint = url,
                    apiToken = key,
                    eventId = "test-${System.currentTimeMillis()}",
                    sourcePackage = "com.nexshop.banknotifier.test",
                    appName = "NEXSHOP BANK NOTIFIER TEST",
                    title = "Тестовое уведомление",
                    body = "Это тест. Не является подтверждением банковского платежа.",
                    postedAt = System.currentTimeMillis()
                )
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (ok) "Тест отправлен на сервер" else "Не удалось отправить. Проверь URL и ключ.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.start()
        }

        layout.addView(TextView(this).apply {
            text = "Уведомления отправляются одновременно в Telegram и на сервер. " +
                "Для сервера используй только HTTPS. Само уведомление не является банковским API: " +
                "сервер обязан сверять сумму с ожидающим пополнением и блокировать повторы."
            textSize = 13f
            setPadding(0, 18, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun loadApps(selected: String) {
        val pm = packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                try { pm.getApplicationLabel(app).toString().isNotBlank() }
                catch (_: Exception) { false }
            }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
        appPackages = installed.map { it.packageName }
        val labels = installed.map { "${pm.getApplicationLabel(it)} (${it.packageName})" }
        apps.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        val index = appPackages.indexOf(selected)
        if (index >= 0) apps.setSelection(index)
        else {
            val dushanbeIndex = installed.indexOfFirst {
                pm.getApplicationLabel(it).toString().contains("dushanbe", true)
            }
            if (dushanbeIndex >= 0) apps.setSelection(dushanbeIndex)
        }
    }
}
