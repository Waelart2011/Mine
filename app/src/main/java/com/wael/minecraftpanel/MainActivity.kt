package com.wael.minecraftpanel

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var address: EditText
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private val prefs by lazy { getSharedPreferences("panel", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(18, 25, 38)
        window.navigationBarColor = Color.rgb(18, 25, 38)
        showLauncher()
    }

    private fun showLauncher() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(16))
            setBackgroundColor(Color.rgb(13, 18, 29))
        }
        val title = TextView(this).apply {
            text = "Minecraft Panel"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }
        val subtitle = TextView(this).apply {
            text = "اتصال بلوحة إدارة سيرفر Minecraft"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(20))
        }
        address = EditText(this).apply {
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "http://192.168.100.92:8766"
            setText(prefs.getString("url", "http://192.168.100.92:8766"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(34, 43, 59))
            imeOptions = EditorInfo.IME_ACTION_GO
        }
        val open = Button(this).apply {
            text = "فتح لوحة التحكم"
            setOnClickListener { connect() }
        }
        status = TextView(this).apply {
            text = "أدخل عنوان اللوحة، ثم اضغط فتح. يجب أن تكون متصلًا بالشبكة المناسبة أو VPN."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(16), 0, 0)
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(address, LinearLayout.LayoutParams(-1, dp(54)))
        root.addView(open, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })
        root.addView(status)
        setContentView(root)
        address.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { connect(); true } else false
        }
    }

    private fun connect() {
        var url = address.text.toString().trim()
        if (url.isBlank()) {
            Toast.makeText(this, "أدخل رابط اللوحة أولًا", Toast.LENGTH_SHORT).show()
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
        prefs.edit().putString("url", url).apply()
        showWebView(url)
    }

    private fun showWebView(url: String) {
        val frame = FrameLayout(this)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.allowFileAccess = false
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    status.text = "جارٍ الاتصال..."
                    progress.visibility = View.VISIBLE
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    progress.visibility = View.GONE
                    status.text = "تم تحميل الصفحة"
                }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        progress.visibility = View.GONE
                        status.text = "تعذر الاتصال. تحقق من الرابط والشبكة وتشغيل لوحة الويب."
                    }
                }
            }
            webChromeClient = WebChromeClient()
        }
        progress = ProgressBar(this).apply { isIndeterminate = true }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.rgb(18, 25, 38))
        }
        val back = Button(this).apply {
            text = "رجوع"
            setOnClickListener {
                if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
                else showLauncher()
            }
        }
        val home = Button(this).apply {
            text = "العنوان"
            setOnClickListener { showLauncher() }
        }
        status = TextView(this).apply {
            text = "جارٍ الاتصال..."
            setTextColor(Color.DKGRAY)
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, 0, 0)
        }
        top.addView(back)
        top.addView(home)
        top.addView(status, LinearLayout.LayoutParams(0, dp(48), 1f))
        frame.addView(webView, FrameLayout.LayoutParams(-1, -1))
        frame.addView(progress, FrameLayout.LayoutParams(dp(42), dp(42), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(top, LinearLayout.LayoutParams(-1, dp(52)))
            addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        setContentView(root)
        webView.loadUrl(url)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.parent != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            showLauncher()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
