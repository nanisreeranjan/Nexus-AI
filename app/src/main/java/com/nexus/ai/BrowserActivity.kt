package com.nexus.ai

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.*
import android.view.ViewGroup
import android.graphics.Color

class BrowserActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val address = EditText(this).apply {
            singleLine = true
            hint = "https://website.com"
            setText(intent.getStringExtra("url") ?: "https://www.google.com")
        }
        val go = Button(this).apply { text = "Go" }
        bar.addView(address, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(go)
        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }
        go.setOnClickListener {
            var url = address.text.toString().trim()
            if (!url.startsWith("https://") && !url.startsWith("http://")) url = "https://$url"
            web.loadUrl(url)
        }
        web.setDownloadListener { url, _, _, _, _ ->
            Toast.makeText(this, "Download link opened by website: $url", Toast.LENGTH_LONG).show()
        }
        root.addView(bar)
        root.addView(web, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
        web.loadUrl(address.text.toString())
    }

}
