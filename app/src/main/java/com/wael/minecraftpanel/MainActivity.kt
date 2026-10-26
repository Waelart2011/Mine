
package com.waelalwaely.minecraftpanel

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ConsoleMessage
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var address: EditText
    private lateinit var tokenInput: EditText
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var loadingBar: ProgressBar
    private lateinit var historySpinner: Spinner

    private val prefs by lazy {
        getSharedPreferences("panel", Context.MODE_PRIVATE)
    }

    private val history = mutableListOf<JSONObject>()
    private var currentUrl = ""
    private var currentToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(18, 25, 38)
        window.navigationBarColor = Color.rgb(18, 25, 38)

        loadHistory()
        showLauncher()
    }

    private fun loadHistory() {
        history.clear()

        try {
            val array = JSONArray(
                prefs.getString("connection_history", "[]")
            )

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val url = item.optString("url", "")

                if (url.isNotBlank()) {
                    history.add(
                        JSONObject()
                            .put("url", url)
                            .put("token", item.optString("token", ""))
                    )
                }
            }
        } catch (_: Exception) {
            history.clear()
        }
    }

    private fun saveHistory() {
        val array = JSONArray()

        history.forEach { array.put(it) }

        prefs.edit()
            .putString("connection_history", array.toString())
            .apply()
    }

    private fun rememberConnection(url: String, token: String) {
        history.removeAll {
            it.optString("url") == url
        }

        history.add(
            0,
            JSONObject()
                .put("url", url)
                .put("token", token)
        )

        while (history.size > 30) {
            history.removeAt(history.lastIndex)
        }

        saveHistory()
    }

    private fun showLauncher() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(16))
            setBackgroundColor(Color.rgb(13, 18, 29))
        }

        val title = TextView(this).apply {
            text = "Minecraft Panel"
            textSize = 26f
            setTextColor(Color.WHITE)
        }

        val subtitle = TextView(this).apply {
            text = "إدارة الاتصال بلوحة Minecraft"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(6), 0, dp(16))
        }

        address = EditText(this).apply {
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "رابط لوحة التحكم"
            setText(
                prefs.getString(
                    "last_url",
                    "http://192.168.100.92:8766"
                )
            )
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.rgb(34, 43, 59))
            imeOptions = EditorInfo.IME_ACTION_GO
        }

        tokenInput = EditText(this).apply {
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "توكن لوحة التحكم"
            inputType = 129
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.rgb(34, 43, 59))
        }

        val historyLabel = TextView(this).apply {
            text = "الاتصالات المحفوظة"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, dp(14), 0, dp(4))
        }

        historySpinner = Spinner(this)
        refreshHistorySpinner()

        historySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position in history.indices) {
                        val item = history[position]
                        address.setText(item.optString("url"))
                        tokenInput.setText(item.optString("token"))
                    }
                }
            }

        val openButton = Button(this).apply {
            text = "اتصال وحفظ"
            setOnClickListener { connect() }
        }

        val deleteButton = Button(this).apply {
            text = "حذف الاتصال المحدد"
            setOnClickListener {
                val position = historySpinner.selectedItemPosition

                if (position in history.indices) {
                    history.removeAt(position)
                    saveHistory()
                    refreshHistorySpinner()
                    Toast.makeText(
                        this@MainActivity,
                        "تم حذف الاتصال من السجل",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val developerButton = Button(this).apply {
            text = "حول التطبيق والمطوّر"
            setOnClickListener { showDeveloperInfo() }
        }

        status = TextView(this).apply {
            text = "اختر اتصالًا سابقًا أو أدخل رابطًا جديدًا."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(12), 0, 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(address, fieldParams())
        root.addView(tokenInput, fieldParams(dp(10)))
        root.addView(historyLabel)
        root.addView(
            historySpinner,
            LinearLayout.LayoutParams(-1, dp(48))
        )
        root.addView(openButton, buttonParams())
        root.addView(deleteButton, buttonParams())
        root.addView(developerButton, buttonParams())
        root.addView(status)

        setContentView(root)

        address.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                connect()
                true
            } else {
                false
            }
        }
    }

    private fun refreshHistorySpinner() {
        val items = history.map {
            it.optString("url")
        }.toMutableList()

        if (items.isEmpty()) {
            items.add("لا توجد اتصالات محفوظة")
        }

        historySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            items
        )
    }

    private fun connect() {
        var url = address.text.toString().trim()
        val token = tokenInput.text.toString().trim()

        if (url.isBlank()) {
            Toast.makeText(
                this,
                "أدخل رابط اللوحة أولًا",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!url.startsWith("http://") &&
            !url.startsWith("https://")) {
            url = "http://$url"
        }

        currentUrl = url
        currentToken = token

        rememberConnection(url, token)

        prefs.edit()
            .putString("last_url", url)
            .apply()

        showWebView(url)
    }

    private fun showWebView(url: String) {
        val frame = FrameLayout(this)

        loadingBar = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = View.VISIBLE
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.allowFileAccess = false

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: android.graphics.Bitmap?
                ) {
                    loadingBar.visibility = View.VISIBLE
                    status.text = "جارٍ الاتصال..."
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    loadingBar.visibility = View.GONE
                    status.text = "تم تحميل الصفحة"
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        loadingBar.visibility = View.GONE
                        status.text =
                            "تعذر الاتصال. تحقق من الرابط والشبكة."
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult?
                ): Boolean {
                    if (currentToken.isNotBlank()) {
                        result?.confirm(currentToken)
                        return true
                    }

                    result?.cancel()
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "أدخل توكن اللوحة في الشاشة السابقة ثم أعد الاتصال.",
                            Toast.LENGTH_LONG
                        ).show()
                        showLauncher()
                    }
                    return true
                }
            }
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(3), dp(6), dp(3))
            setBackgroundColor(Color.rgb(18, 25, 38))
        }

        val back = Button(this).apply {
            text = "رجوع"
            setOnClickListener {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    showLauncher()
                }
            }
        }

        val home = Button(this).apply {
            text = "الاتصالات"
            setOnClickListener { showLauncher() }
        }

        status = TextView(this).apply {
            text = "جارٍ الاتصال..."
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), 0, 0, 0)
        }

        top.addView(back)
        top.addView(home)
        top.addView(
            status,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        frame.addView(
            webView,
            FrameLayout.LayoutParams(-1, -1)
        )

        frame.addView(
            loadingBar,
            FrameLayout.LayoutParams(
                dp(42),
                dp(42),
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            )
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(top, LinearLayout.LayoutParams(-1, dp(52)))
            addView(
                frame,
                LinearLayout.LayoutParams(-1, 0, 1f)
            )
        }

        setContentView(root)
        webView.loadUrl(url)
    }

    private fun showDeveloperInfo() {
        android.app.AlertDialog.Builder(this)
            .setTitle("حول التطبيق")
            .setMessage(
                "Minecraft Panel\n\n" +
                "المطوّر: waelalwaely\n" +
                "معرّف الحزمة: com.waelalwaely.minecraftpanel\n\n" +
                "واجهة اتصال بلوحة إدارة Minecraft."
            )
            .setPositiveButton("حسنًا", null)
            .show()
    }

    private fun fieldParams(top: Int = 0) =
        LinearLayout.LayoutParams(-1, dp(52)).apply {
            topMargin = top
        }

    private fun buttonParams() =
        LinearLayout.LayoutParams(-1, dp(50)).apply {
            topMargin = dp(6)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    @Deprecated("Deprecated in Android API 33")
    override fun onBackPressed() {
        if (::webView.isInitialized &&
            webView.parent != null &&
            webView.canGoBack()) {
            webView.goBack()
        } else {
            showLauncher()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            (webView.parent as? android.view.ViewGroup)
                ?.removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }
}
