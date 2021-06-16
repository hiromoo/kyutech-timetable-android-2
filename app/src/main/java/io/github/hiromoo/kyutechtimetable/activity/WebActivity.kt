package io.github.hiromoo.kyutechtimetable.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import io.github.hiromoo.kyutechtimetable.R
import io.github.hiromoo.kyutechtimetable.databinding.ActivityWebBinding
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.save
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.setSubjectsFromHTML

class WebActivity : AppCompatActivity() {

    lateinit var binding: ActivityWebBinding

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        WebView.setWebContentsDebuggingEnabled(true)

        supportActionBar?.run {
            title = null
            setDisplayHomeAsUpEnabled(true)
        }

        with(binding.webView) {
            with(settings) {
                javaScriptEnabled = true
                builtInZoomControls = true
            }
            addJavascriptInterface(this@WebActivity, "WebActivity")
            loadUrl("https://virginia.jimu.kyutech.ac.jp/")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.web_menu, menu)

        val updateItem = menu?.findItem(R.id.item_update)?.apply {
            isEnabled = false
        }

        with(binding.webView) {
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    setInitialScale(100)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    updateItem?.isEnabled = url.contains(
                        "https://virginia.jimu.kyutech.ac.jp/portal/jikanwariInit.do?"
                    )
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.item_update -> {
                item.isEnabled = false
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                with(binding) {
                    with(progressBar) {
                        visibility = View.VISIBLE
                        animate()
                    }
                    webView.loadUrl(
                        "javascript:window.WebActivity.viewSource(document.documentElement.outerHTML);"
                    )
                }
                return true
            }
        }
        return false
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!supportFragmentManager.popBackStackImmediate()) {
            finish()
        }
        return true
    }

    @JavascriptInterface
    fun viewSource(src: String) {
        setSubjectsFromHTML(src, this)
        save(this)
        setResult(RESULT_OK)
        finish()
    }
}