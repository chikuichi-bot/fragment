package jp.lagado.literaryfragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private lateinit var tts: TextToSpeech
    private lateinit var bridge: AndroidBridge

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyNativeTheme()

        setContentView(R.layout.activity_main)
        TicketManager.checkDailyReset(this)

        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) { e.printStackTrace() }

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        bridge = AndroidBridge(this, webView)
        webView.addJavascriptInterface(bridge, "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                applyWebViewTheme()
            }
        }

        webView.loadUrl("file:///android_asset/index.html")

        handleIntent(intent)
    }

    private fun applyNativeTheme() {
        try {
            val prefs = getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
            when (prefs.getInt("themePreference", 0)) {
                1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onResume() {
        super.onResume()
        applyNativeTheme()
        applyWebViewTheme()
    }

    private fun applyWebViewTheme() {
        try {
            val prefs = getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
            val theme = prefs.getInt("themePreference", 0)

            val isDark = when (theme) {
                1 -> false
                2 -> true
                else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }

            // 💎 上質なマットカラー（Android Material Design基準）
            val bgColor = if (isDark) Color.parseColor("#121212") else Color.parseColor("#F8F9FA")
            webView.setBackgroundColor(bgColor)
            window.decorView.setBackgroundColor(bgColor)

            val themeJS = """
                try {
                    if ($theme === 1) { document.documentElement.removeAttribute('data-theme'); }
                    else if ($theme === 2) { document.documentElement.setAttribute('data-theme', 'dark'); }
                    else { 
                        if (window.matchMedia('(prefers-color-scheme: dark)').matches) { 
                            document.documentElement.setAttribute('data-theme', 'dark'); 
                        } else { 
                            document.documentElement.removeAttribute('data-theme'); 
                        } 
                    }
                } catch(e) {}
            """.trimIndent()
            webView.evaluateJavascript(themeJS, null)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(targetIntent: Intent?) {
        try {
            targetIntent?.let {
                val forceQuote = it.getStringExtra("forceQuote")
                if (forceQuote != null) {
                    val forceTitle = it.getStringExtra("forceTitle") ?: ""
                    val forceAuthor = it.getStringExtra("forceAuthor") ?: ""
                    val skipRoulette = it.getBooleanExtra("skipRoulette", true)

                    bridge.currentSearchKeyword = if (AppSharedState.isFiltering) AppSharedState.currentSearchText else ""

                    Handler(Looper.getMainLooper()).postDelayed({
                        applyWebViewTheme()
                        if (skipRoulette) {
                            bridge.forceDisplayQuote(forceQuote, forceTitle, forceAuthor)
                        } else {
                            bridge.spinToSpecificQuote(forceQuote, forceTitle, forceAuthor)
                        }
                    }, 300)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onInit(status: Int) {
        try {
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (utteranceId != "TTS_SILENCE") { runOnUiThread { toggleAudioIcon(true) } }
                    }
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "TTS_LAST_ID") { runOnUiThread { toggleAudioIcon(false) } }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { runOnUiThread { toggleAudioIcon(false) } }
                })
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        try {
            if (this::tts.isInitialized) { tts.stop(); tts.shutdown() }
        } catch (e: Exception) { e.printStackTrace() }
        super.onDestroy()
    }

    fun toggleAudioIcon(isPlaying: Boolean) {
        try {
            val offSVG = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polygon points=\"11 5 6 9 2 9 2 15 6 15 11 19 11 5\"></polygon><path d=\"M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07\"></path></svg>"
            val onSVG = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polygon points=\"11 5 6 9 2 9 2 15 6 15 11 19 11 5\"></polygon><path d=\"M15.54 8.46a5 5 0 0 1 0 7.07\"></path></svg>"
            val scale = if (isPlaying) "1.2" else "1.0"
            val color = if (isPlaying) "#ff9500" else ""
            val targetSVG = if (isPlaying) onSVG else offSVG

            val js = "try { var btn = document.getElementById('btn-book'); if(btn) { btn.innerHTML = '$targetSVG'; btn.style.transform = 'scale($scale)'; btn.style.color = '$color'; } } catch(e) {}"
            runOnUiThread { webView.evaluateJavascript(js, null) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    inner class AndroidBridge(private val context: Context, private val webView: WebView) {

        private val quoteHistory = mutableListOf<QuoteData>()
        private var currentHistoryIndex = -1
        private var isSpinning = false
        var currentSearchKeyword = ""

        private var translationDialog: BottomSheetDialog? = null

        private val roulettePool = listOf(
            "To be, or not to be...", "It was the best of times...", "Call me Ishmael.",
            "I am no bird; and no net ensnares me...", "All grown-ups were once children..."
        )

        private fun vibrate(durationMillis: Long) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    val vibrator = vibratorManager.defaultVibrator
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(durationMillis)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        private fun escapeJS(str: String): String {
            return str.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")
        }

        private fun stopSpeaking() {
            try {
                if (this@MainActivity::tts.isInitialized && tts.isSpeaking) {
                    tts.stop()
                    runOnUiThread { toggleAudioIcon(false) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        fun forceDisplayQuote(quote: String, title: String, author: String) {
            runOnUiThread {
                try {
                    stopSpeaking()
                    val quoteData = QuoteData(quote, title, author)
                    QuoteStorage.addHistory(context, quoteData)
                    quoteHistory.add(quoteData)
                    currentHistoryIndex = quoteHistory.size - 1

                    vibrate(30L)
                    val escapedQuote = escapeJS(quote)
                    val escapedTitle = escapeJS(title)
                    val escapedAuthor = escapeJS(author)
                    val escapedKeyword = escapeJS(currentSearchKeyword)

                    val js = """
                        try {
                            if(window.setSearchKeyword) { window.setSearchKeyword('$escapedKeyword'); }
                            var display = document.getElementById('quote-text');
                            var sourceArea = document.getElementById('source-area');
                            if (display) { display.style.transition = ''; display.style.opacity = ''; }
                            if (sourceArea) { sourceArea.style.transition = ''; sourceArea.style.opacity = ''; }
                            if(window.displayQuoteWithFade) { window.displayQuoteWithFade('$escapedQuote', '$escapedTitle', '$escapedAuthor'); }
                        } catch(e) {}
                    """.trimIndent()
                    webView.evaluateJavascript(js, null)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        fun spinToSpecificQuote(quote: String, title: String, author: String) {
            runOnUiThread {
                try {
                    stopSpeaking()
                    val quoteData = QuoteData(quote, title, author)
                    QuoteStorage.addHistory(context, quoteData)
                    quoteHistory.add(quoteData)
                    currentHistoryIndex = quoteHistory.size - 1
                    startRoulette(quoteData)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        @JavascriptInterface fun triggerHaptic(payload: String?) { vibrate(30L) }

        @JavascriptInterface
        fun requestNextQuote(payload: String?) {
            runOnUiThread {
                try {
                    if (isSpinning) return@runOnUiThread
                    stopSpeaking()

                    if (currentHistoryIndex < quoteHistory.size - 1) {
                        currentHistoryIndex++
                        val nextQuote = quoteHistory[currentHistoryIndex]
                        QuoteStorage.addHistory(context, nextQuote)
                        startRoulette(nextQuote)
                        return@runOnUiThread
                    }

                    isSpinning = true
                    Thread {
                        try {
                            var quote = ""
                            var title = ""
                            var author = ""
                            var fetchedKeyword = ""

                            val isF = AppSharedState.isFiltering
                            val filteredList = AppSharedState.filteredFortunes
                            val isA = AppSharedState.isAtmosphereMode
                            val atmosList = AppSharedState.atmosphereFortunes

                            if (isF && filteredList.isNotEmpty()) {
                                val randomItem = filteredList.random()
                                quote = randomItem["quote"] ?: ""
                                title = randomItem["title"] ?: ""
                                author = randomItem["author"] ?: ""
                                fetchedKeyword = AppSharedState.currentSearchText
                            } else if (isA && atmosList.isNotEmpty()) {
                                val randomItem = atmosList.random()
                                quote = randomItem["quote"] ?: ""
                                title = randomItem["title"] ?: ""
                                author = randomItem["author"] ?: ""
                                fetchedKeyword = ""
                            } else {
                                fetchedKeyword = ""
                                val prefs = context.getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
                                val mode = if (prefs.getInt("quoteLengthMode", 0) == 1) "long" else "short"
                                val url = URL("https://lagado.jp/fragments/api.php?action=random&mode=$mode")

                                val connection = url.openConnection() as HttpURLConnection
                                connection.requestMethod = "GET"
                                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                                    val responseText = BufferedReader(InputStreamReader(connection.inputStream)).readText()
                                    if (responseText.trim().startsWith("[")) {
                                        val arr = JSONArray(responseText)
                                        if (arr.length() > 0) {
                                            val obj = arr.getJSONObject(0)
                                            quote = obj.optString("quote", "")
                                            title = obj.optString("title", obj.optString("book", ""))
                                            author = obj.optString("author", "")
                                        }
                                    } else if (responseText.trim().startsWith("{")) {
                                        val obj = JSONObject(responseText)
                                        quote = obj.optString("quote", "")
                                        title = obj.optString("title", obj.optString("book", ""))
                                        author = obj.optString("author", "")
                                    }
                                }
                            }

                            if (quote.isNotEmpty()) {
                                val newQuoteData = QuoteData(quote, title, author)
                                runOnUiThread {
                                    try {
                                        currentSearchKeyword = fetchedKeyword
                                        quoteHistory.add(newQuoteData)
                                        currentHistoryIndex = quoteHistory.size - 1
                                        QuoteStorage.addHistory(context, newQuoteData)
                                        startRoulette(newQuoteData)
                                    } catch (e: Exception) {
                                        isSpinning = false
                                    }
                                }
                            } else {
                                runOnUiThread { isSpinning = false }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            runOnUiThread { isSpinning = false }
                        }
                    }.start()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        @JavascriptInterface
        fun requestPreviousQuote(payload: String?) {
            runOnUiThread {
                try {
                    if (isSpinning) return@runOnUiThread
                    stopSpeaking()

                    if (currentHistoryIndex > 0) {
                        currentHistoryIndex--
                        val prevQuote = quoteHistory[currentHistoryIndex]
                        QuoteStorage.addHistory(context, prevQuote)

                        vibrate(30L)

                        val escapedQuote = escapeJS(prevQuote.text)
                        val escapedTitle = escapeJS(prevQuote.title)
                        val escapedAuthor = escapeJS(prevQuote.author)
                        val escapedKeyword = escapeJS(currentSearchKeyword)

                        val js = """
                            try {
                                if(window.setSearchKeyword) { window.setSearchKeyword('$escapedKeyword'); }
                                var display = document.getElementById('quote-text');
                                var sourceArea = document.getElementById('source-area');
                                if (display) { display.style.transition = ''; display.style.opacity = ''; }
                                if (sourceArea) { sourceArea.style.transition = ''; sourceArea.style.opacity = ''; }
                                if(window.displayQuoteWithFade) { window.displayQuoteWithFade('$escapedQuote', '$escapedTitle', '$escapedAuthor'); }
                            } catch(e) {}
                        """.trimIndent()
                        webView.evaluateJavascript(js, null)
                    } else {
                        vibrate(50L)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        private fun startRoulette(quoteData: QuoteData) {
            isSpinning = true
            val maxTicks = 15
            var currentTick = 0
            val handler = Handler(Looper.getMainLooper())
            val spinTexts = roulettePool.shuffled()

            val initJs = """
                try { 
                    var display = document.getElementById('quote-text'); 
                    var sourceArea = document.getElementById('source-area'); 
                    if(display) { 
                        display.style.transition = 'none'; 
                        display.style.opacity = '1'; 
                        display.classList.remove('fade-in'); 
                    } 
                    if(sourceArea) { sourceArea.style.opacity = '0'; } 
                } catch(e) {}
            """.trimIndent()
            webView.evaluateJavascript(initJs, null)

            val runnable = object : Runnable {
                override fun run() {
                    try {
                        if (this@MainActivity.isDestroyed || this@MainActivity.isFinishing) return

                        if (currentTick >= maxTicks) {
                            vibrate(40L)
                            val escapedQuote = escapeJS(quoteData.text)
                            val escapedTitle = escapeJS(quoteData.title)
                            val escapedAuthor = escapeJS(quoteData.author)
                            val escapedKeyword = escapeJS(currentSearchKeyword)

                            val js = """
                                try {
                                    if(window.setSearchKeyword) { window.setSearchKeyword('$escapedKeyword'); }
                                    var display = document.getElementById('quote-text');
                                    var sourceArea = document.getElementById('source-area');
                                    if (display) { display.style.transition = ''; display.style.opacity = ''; }
                                    if (sourceArea) { sourceArea.style.transition = ''; sourceArea.style.opacity = ''; }
                                    if(window.displayQuoteWithFade) { window.displayQuoteWithFade('$escapedQuote', '$escapedTitle', '$escapedAuthor'); }
                                } catch(e) {}
                            """.trimIndent()
                            webView.evaluateJavascript(js, null)
                            isSpinning = false
                        } else {
                            val text = spinTexts[currentTick % spinTexts.size]
                            val escapedText = escapeJS(text)
                            val tickJs = "try { var d = document.getElementById('quote-text'); if(d) { d.innerHTML = '$escapedText'; } } catch(e) {}"
                            webView.evaluateJavascript(tickJs, null)

                            if (currentTick % 2 == 0) vibrate(15L)
                            currentTick++
                            handler.postDelayed(this, 80)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isSpinning = false
                    }
                }
            }
            handler.post(runnable)
        }

        @JavascriptInterface
        fun speakText(payload: String?) {
            try {
                if (payload != null) {
                    if (tts.isSpeaking) { stopSpeaking(); return }
                    val parts = payload.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
                    if (parts.isEmpty()) return

                    val lastIndex = parts.size - 1
                    for (i in parts.indices) {
                        val part = parts[i]
                        val utteranceId = if (i == lastIndex) "TTS_LAST_ID" else "TTS_PART_$i"

                        if (i == 0) {
                            tts.speak(part, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                        } else {
                            tts.playSilentUtterance(1000, TextToSpeech.QUEUE_ADD, "TTS_SILENCE")
                            tts.speak(part, TextToSpeech.QUEUE_ADD, null, utteranceId)
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        @JavascriptInterface
        fun stockQuote(payload: String?) {
            if (payload != null) {
                runOnUiThread {
                    try {
                        val found = QuoteStorage.getHistory(context).firstOrNull { it.text == payload }
                        QuoteStorage.addFavorite(context, QuoteData(payload, found?.title ?: "", found?.author ?: ""))
                        android.widget.Toast.makeText(context, "🌟 保存しました", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {}
                }
            }
        }

        @JavascriptInterface
        fun unstockQuote(payload: String?) {
            if (payload != null) {
                runOnUiThread {
                    try {
                        QuoteStorage.removeFavorite(context, payload)
                        android.widget.Toast.makeText(context, "解除しました", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {}
                }
            }
        }

        @JavascriptInterface
        fun showSettings(payload: String?) {
            runOnUiThread {
                try { context.startActivity(Intent(context, SettingsActivity::class.java)) } catch (e: Exception) {}
            }
        }

        @JavascriptInterface
        fun showFavorites(payload: String?) {
            runOnUiThread {
                try { context.startActivity(Intent(context, FavoritesActivity::class.java)) } catch (e: Exception) {}
            }
        }

        @JavascriptInterface
        fun explainQuote(payload: String?) {
            if (payload != null) {
                runOnUiThread {
                    try {
                        val intent = Intent(context, ExplanationActivity::class.java)
                        intent.putExtra("quote", payload)
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                }
            }
        }

        @JavascriptInterface
        fun searchBook(payload: String?) {
            if (payload != null) {
                runOnUiThread {
                    try {
                        val dict = JSONObject(payload)
                        val title = dict.optString("title", "")
                        val author = dict.optString("author", "")
                        val query = java.net.URLEncoder.encode("$title $author", "UTF-8")
                        val url = "https://www.google.com/search?q=$query"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }

        @JavascriptInterface
        fun showNativeTranslation(payload: String?) {
            if (payload.isNullOrBlank()) return
            runOnUiThread {
                try {
                    if (translationDialog?.isShowing == true) {
                        return@runOnUiThread
                    }

                    vibrate(30L)

                    translationDialog = BottomSheetDialog(this@MainActivity).apply {
                        val miniWebView = WebView(this@MainActivity).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                (resources.displayMetrics.heightPixels * 0.7).toInt()
                            )
                            val query = java.net.URLEncoder.encode(payload, "UTF-8")
                            loadUrl("https://translate.google.com/m?sl=auto&tl=ja&q=$query")
                        }

                        setContentView(miniWebView)
                        setOnDismissListener { translationDialog = null }
                    }

                    translationDialog?.show()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

object AppSharedState {
    @Volatile var isFiltering = false
    @Volatile var currentSearchText = ""
    @Volatile var filteredFortunes: List<Map<String, String>> = emptyList()

    @Volatile var isAtmosphereMode = false
    @Volatile var currentAtmosphereKeywords = ""
    @Volatile var atmosphereFortunes: List<Map<String, String>> = emptyList()

    fun clear() {
        isFiltering = false
        currentSearchText = ""
        filteredFortunes = emptyList()
        isAtmosphereMode = false
        currentAtmosphereKeywords = ""
        atmosphereFortunes = emptyList()
    }
}