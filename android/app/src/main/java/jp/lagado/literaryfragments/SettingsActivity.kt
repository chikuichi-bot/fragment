package jp.lagado.literaryfragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class SettingsActivity : AppCompatActivity() {

    private val apiBaseURL = "https://lagado.jp/fragments/api.php"
    private var selectedScope = "all"
    private var atmosphereKeywords = ""
    private var atmospherePlaceKeys: List<String> = emptyList()
    private var isInitialAi = true
    private var pendingSenseAfterPermission = false

    private var swipeStartY = 0f
    private var isAnimatingOut = false

    companion object {
        private const val REQ_LOCATION = 4101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeScreen()
            }
        })

        findViewById<Button>(R.id.btnDone).setOnClickListener { closeScreen() }

        applyDarkModeColors()

        val prefs = getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)

        findViewById<LinearLayout>(R.id.cardTicket).setOnClickListener {
            startActivity(Intent(this, TicketStoreActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.cardPrivacy).setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://lagado.jp/fragments/privacy-android.php")
                )
            )
        }

        findViewById<LinearLayout>(R.id.btnDrawRandom).setOnClickListener {
            fetchAndReturnToMain("$apiBaseURL?action=random&mode=${getModeStr()}", skipRoulette = false)
        }

        val btnSenseMoment = findViewById<LinearLayout>(R.id.btnSenseMoment)
        val textSense = findViewById<TextView>(R.id.textSense)
        val progressSense = findViewById<ProgressBar>(R.id.progressSense)
        val overlayAtmosphere = findViewById<FrameLayout>(R.id.overlayAtmosphere)
        val textAtmosphereResult = findViewById<TextView>(R.id.textAtmosphereResult)

        btnSenseMoment.setOnClickListener {
            textSense.text = "読み取り中..."
            textSense.setTextColor(Color.GRAY)
            progressSense.visibility = View.VISIBLE
            btnSenseMoment.isEnabled = false
            beginSenseMoment()
        }

        findViewById<Button>(R.id.btnCancelAtmosphere).setOnClickListener { overlayAtmosphere.visibility = View.GONE }

        val btnDrawAtmosphere = findViewById<Button>(R.id.btnDrawAtmosphere)
        val progressDrawAtmosphere = findViewById<ProgressBar>(R.id.progressDrawAtmosphere)

        btnDrawAtmosphere.setOnClickListener {
            btnDrawAtmosphere.text = ""
            progressDrawAtmosphere.visibility = View.VISIBLE
            AppSharedState.atmospherePlaceKeywords = atmospherePlaceKeys
            val encoded = java.net.URLEncoder.encode(atmosphereKeywords, "UTF-8")
            fetchAndReturnToMain("$apiBaseURL?action=atmosphere&keywords=$encoded&mode=${getModeStr()}", skipRoulette = false, searchKeyword = atmosphereKeywords, isAtmosphere = true)
        }

        val tabScopeAll = findViewById<TextView>(R.id.tabScopeAll)
        val tabScopeQuote = findViewById<TextView>(R.id.tabScopeQuote)
        val tabScopeAuthor = findViewById<TextView>(R.id.tabScopeAuthor)
        val tabScopeTitle = findViewById<TextView>(R.id.tabScopeTitle)

        fun updateScopeUI(selected: TextView) {
            try {
                val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val activeBg = GradientDrawable().apply { setColor(if(isDark) Color.parseColor("#3A3A3C") else Color.WHITE); cornerRadius = 12f }
                listOf(tabScopeAll, tabScopeQuote, tabScopeAuthor, tabScopeTitle).forEach {
                    it.background = if (it == selected) activeBg else null
                    it.setTextColor(if (it == selected) (if(isDark) Color.WHITE else Color.BLACK) else Color.GRAY)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        updateScopeUI(tabScopeAll)

        tabScopeAll.setOnClickListener { selectedScope = "all"; updateScopeUI(tabScopeAll) }
        tabScopeQuote.setOnClickListener { selectedScope = "quote"; updateScopeUI(tabScopeQuote) }
        tabScopeAuthor.setOnClickListener { selectedScope = "author"; updateScopeUI(tabScopeAuthor) }
        tabScopeTitle.setOnClickListener { selectedScope = "title"; updateScopeUI(tabScopeTitle) }

        val editSearch = findViewById<EditText>(R.id.editSearch)
        val progressSearch = findViewById<ProgressBar>(R.id.progressSearch)
        val btnSearch = findViewById<Button>(R.id.btnSearch)

        if (AppSharedState.isFiltering) editSearch.setText(AppSharedState.currentSearchText)

        fun executeSearch() {
            val keyword = editSearch.text.toString().trim()
            if (keyword.isEmpty()) return

            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editSearch.windowToken, 0)
            } catch (e: Exception) { e.printStackTrace() }

            progressSearch.visibility = View.VISIBLE
            btnSearch.visibility = View.INVISIBLE
            val encoded = java.net.URLEncoder.encode(keyword, "UTF-8")

            fetchAndReturnToMain("$apiBaseURL?action=search&keyword=$encoded&scope=$selectedScope&mode=${getModeStr()}", isSearch = true, skipRoulette = false, searchKeyword = keyword)
        }

        btnSearch.setOnClickListener { executeSearch() }

        editSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                executeSearch()
                true
            } else {
                false
            }
        }

        val tabThemeSystem = findViewById<TextView>(R.id.tabThemeSystem)
        val tabThemeLight = findViewById<TextView>(R.id.tabThemeLight)
        val tabThemeDark = findViewById<TextView>(R.id.tabThemeDark)

        fun updateThemeUI(position: Int) {
            try {
                val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val activeBg = GradientDrawable().apply { setColor(if(isDark) Color.parseColor("#3A3A3C") else Color.WHITE); cornerRadius = 12f }
                listOf(tabThemeSystem, tabThemeLight, tabThemeDark).forEachIndexed { i, tab ->
                    tab.background = if (i == position) activeBg else null
                    tab.setTextColor(if (i == position) (if(isDark) Color.WHITE else Color.BLACK) else Color.GRAY)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        val currentTheme = prefs.getInt("themePreference", 0)
        updateThemeUI(currentTheme)

        val themeClickListener = View.OnClickListener { v ->
            val pos = when (v.id) { R.id.tabThemeLight -> 1; R.id.tabThemeDark -> 2; else -> 0 }
            if (pos != prefs.getInt("themePreference", 0)) {
                prefs.edit().putInt("themePreference", pos).apply()
                updateThemeUI(pos)
                when (pos) {
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                applyDarkModeColors()
            }
        }
        tabThemeSystem.setOnClickListener(themeClickListener)
        tabThemeLight.setOnClickListener(themeClickListener)
        tabThemeDark.setOnClickListener(themeClickListener)

        val tabLenShort = findViewById<TextView>(R.id.tabLenShort)
        val tabLenLong = findViewById<TextView>(R.id.tabLenLong)

        fun updateLengthUI(isLong: Boolean) {
            try {
                val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val activeBg = GradientDrawable().apply { setColor(if(isDark) Color.parseColor("#3A3A3C") else Color.WHITE); cornerRadius = 12f }
                tabLenLong.background = if (isLong) activeBg else null
                tabLenLong.setTextColor(if (isLong) (if(isDark) Color.WHITE else Color.BLACK) else Color.GRAY)
                tabLenShort.background = if (!isLong) activeBg else null
                tabLenShort.setTextColor(if (!isLong) (if(isDark) Color.WHITE else Color.BLACK) else Color.GRAY)
            } catch (e: Exception) { e.printStackTrace() }
        }
        updateLengthUI(prefs.getInt("quoteLengthMode", 0) == 1)

        tabLenShort.setOnClickListener {
            val currentMode = prefs.getInt("quoteLengthMode", 0)
            if (currentMode == 0) {
                fetchAndReturnToMain("$apiBaseURL?action=random&mode=short", skipRoulette = false)
            } else {
                prefs.edit().putInt("quoteLengthMode", 0).apply()
                updateLengthUI(false)
            }
        }

        tabLenLong.setOnClickListener {
            val currentMode = prefs.getInt("quoteLengthMode", 0)
            if (currentMode == 1) {
                fetchAndReturnToMain("$apiBaseURL?action=random&mode=long", skipRoulette = false)
            } else {
                prefs.edit().putInt("quoteLengthMode", 1).apply()
                updateLengthUI(true)
            }
        }

        val spinnerAiLevel = findViewById<Spinner>(R.id.spinnerAiLevel)
        val aiLevels = arrayOf("中学レベル (Beginner)", "高校レベル (Intermediate)", "大学レベル (Advanced)", "ビジネス (Business)")
        val aiDescs = arrayOf(
            "複雑な文法用語を避け、基本的な構文や基礎単語をやさしい言葉で解説します。",
            "関係詞や仮定法など、重要な文法を指摘し論理的に解説します。",
            "文学的な比喩やニュアンス、文化的背景などアカデミックに深掘りします。",
            "フォーマル度や、実際のビジネスシーンでどう活かせるかに焦点を当てます。"
        )
        spinnerAiLevel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, aiLevels)

        val savedAiLevel = prefs.getInt("englishLevelIndex", 1)
        spinnerAiLevel.setSelection(savedAiLevel)

        val textAiLevelDesc = findViewById<TextView>(R.id.textAiLevelDesc)
        spinnerAiLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                if (isInitialAi) { isInitialAi = false; textAiLevelDesc.text = aiDescs[savedAiLevel]; return }
                prefs.edit().putInt("englishLevelIndex", pos).apply()
                textAiLevelDesc.text = aiDescs[pos]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun beginSenseMoment() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            pendingSenseAfterPermission = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        runSenseOnBackgroundThread()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION && pendingSenseAfterPermission) {
            pendingSenseAfterPermission = false
            runSenseOnBackgroundThread()
        }
    }

    private fun runSenseOnBackgroundThread() {
        Thread {
            val coords = resolveDeviceLocationOrKyoto()
            val weatherStr = fetchWeatherAndGenerateKeywords(
                lat = coords.first,
                lon = coords.second,
                iso = coords.third.iso,
                country = coords.third.country,
                admin = coords.third.admin,
                city = coords.third.city
            )
            runOnUiThread {
                try {
                    val textSense = findViewById<TextView>(R.id.textSense)
                    val progressSense = findViewById<ProgressBar>(R.id.progressSense)
                    val btnSenseMoment = findViewById<LinearLayout>(R.id.btnSenseMoment)
                    val overlayAtmosphere = findViewById<FrameLayout>(R.id.overlayAtmosphere)
                    val textAtmosphereResult = findViewById<TextView>(R.id.textAtmosphereResult)

                    textSense.text = "今の気配を読み取る"

                    val currentPrefs = getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
                    val theme = currentPrefs.getInt("themePreference", 0)
                    val isDarkApp = when (theme) {
                        1 -> false
                        2 -> true
                        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    }

                    textSense.setTextColor(if (isDarkApp) Color.WHITE else Color.parseColor("#1D1D1D"))
                    progressSense.visibility = View.GONE
                    btnSenseMoment.isEnabled = true

                    textAtmosphereResult.text = weatherStr
                    textAtmosphereResult.setTextColor(if (isDarkApp) Color.WHITE else Color.parseColor("#1D1D1D"))

                    overlayAtmosphere.visibility = View.VISIBLE
                } catch (e: Exception) { e.printStackTrace() }
            }
        }.start()
    }

    private data class PlaceBits(val iso: String?, val country: String?, val admin: String?, val city: String)

    private fun resolveDeviceLocationOrKyoto(): Triple<Double, Double, PlaceBits> {
        val fallback = Triple(35.0116, 135.7681, PlaceBits("JP", "日本", "京都府", "京都市"))
        try {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!fine && !coarse) return fallback

            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            var best: Location? = null
            for (p in providers) {
                try {
                    val loc = lm.getLastKnownLocation(p) ?: continue
                    if (best == null || loc.time > best!!.time) best = loc
                } catch (_: SecurityException) { }
            }
            val loc = best ?: return fallback
            val bits = reverseGeocode(loc.latitude, loc.longitude) ?: PlaceBits(null, null, null, "")
            return Triple(loc.latitude, loc.longitude, bits)
        } catch (e: Exception) {
            e.printStackTrace()
            return fallback
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lon: Double): PlaceBits? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(this, Locale.JAPAN)
            val list = geocoder.getFromLocation(lat, lon, 1)
            val a = list?.firstOrNull() ?: return null
            PlaceBits(
                iso = a.countryCode,
                country = a.countryName,
                admin = a.adminArea,
                city = a.locality ?: a.subAdminArea ?: ""
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun closeScreen() {
        if (isAnimatingOut) return
        isAnimatingOut = true
        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        if (rootView != null) {
            rootView.animate()
                .translationY(rootView.height.toFloat())
                .alpha(0f)
                .setDuration(250)
                .withEndAction {
                    finish()
                    overridePendingTransition(0, 0)
                }
                .start()
        } else {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isAnimatingOut) return true
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                swipeStartY = ev.rawY
            }
            MotionEvent.ACTION_UP -> {
                val deltaY = ev.rawY - swipeStartY
                val density = resources.displayMetrics.density
                if (deltaY > 100 * density && swipeStartY < 120 * density) {
                    closeScreen()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun applyDarkModeColors() {
        try {
            val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            // 💎 上質なマットカラーを適用
            val bgColor = if (isDark) Color.parseColor("#121212") else Color.parseColor("#F8F9FA")
            val cardColor = if (isDark) Color.parseColor("#1E1E1E") else Color.WHITE
            val textColor = if (isDark) Color.parseColor("#EAEAEA") else Color.parseColor("#1D1D1D")
            val subTextColor = if (isDark) Color.parseColor("#9E9E9E") else Color.parseColor("#757575")
            val segmentBg = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E9ECEF")

            window.decorView.setBackgroundColor(bgColor)
            findViewById<LinearLayout>(R.id.mainBackground)?.setBackgroundColor(bgColor)
            findViewById<LinearLayout>(R.id.headerBackground)?.setBackgroundColor(cardColor)

            val cards = listOf(R.id.cardDraw, R.id.cardSearch, R.id.cardAi, R.id.cardTicket, R.id.cardPrivacy)
            for (id in cards) {
                findViewById<LinearLayout>(id)?.apply {
                    background = GradientDrawable().apply { setColor(cardColor); cornerRadius = 28f } // 洗練された角丸
                    elevation = if (isDark) 0f else 6f // 💎 ライトモードのみ美しい影を落とす
                }
            }

            findViewById<LinearLayout>(R.id.alertContainer)?.background = GradientDrawable().apply { setColor(cardColor); cornerRadius = 36f }
            findViewById<LinearLayout>(R.id.groupScope)?.background = GradientDrawable().apply { setColor(segmentBg); cornerRadius = 16f }
            findViewById<LinearLayout>(R.id.groupTheme)?.background = GradientDrawable().apply { setColor(segmentBg); cornerRadius = 16f }
            findViewById<LinearLayout>(R.id.groupLength)?.background = GradientDrawable().apply { setColor(segmentBg); cornerRadius = 16f }

            fun updateColors(view: View) {
                if (view is TextView && view.id != R.id.btnDone && view.id != R.id.btnDrawAtmosphere && view.id != R.id.textFreeTickets && view.id != R.id.btnSearch) {
                    val current = view.currentTextColor
                    if (current == Color.parseColor("#333333") || current == Color.BLACK || current == Color.WHITE) {
                        view.setTextColor(textColor)
                    } else if (current == Color.parseColor("#666666") || current == Color.parseColor("#888888") || current == Color.parseColor("#AAAAAA")) {
                        view.setTextColor(subTextColor)
                    }
                } else if (view is ViewGroup) {
                    for (i in 0 until view.childCount) updateColors(view.getChildAt(i))
                }
            }
            findViewById<LinearLayout>(R.id.mainBackground)?.let { updateColors(it) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.textFreeTickets).text = "${TicketManager.getFreeTickets(this)} 回"
        findViewById<TextView>(R.id.textPaidTickets).text = "${TicketManager.getPaidTickets(this)} 枚"
    }

    private fun getModeStr() = if (getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE).getInt("quoteLengthMode", 0) == 1) "long" else "short"

    private fun fetchWeatherAndGenerateKeywords(
        lat: Double,
        lon: Double,
        iso: String?,
        country: String?,
        admin: String?,
        city: String
    ): String {
        var windSpeed = 0.0; var temp = 20.0; var weatherCode = 0
        try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val json = JSONObject(BufferedReader(InputStreamReader(connection.inputStream)).readText())
                val current = json.getJSONObject("current_weather")
                windSpeed = current.getDouble("windspeed")
                temp = current.getDouble("temperature")
                weatherCode = current.getInt("weathercode")
            }
        } catch (e: Exception) { e.printStackTrace() }

        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val season = when (month) { in 3..5 -> "Spring"; in 6..8 -> "Summer"; in 9..11 -> "Autumn"; else -> "Winter" }
        val sunStr = when (hour) { in 4..10 -> "Morning"; in 11..15 -> "Daytime"; in 16..18 -> "Evening"; else -> "Night" }
        val windStr = if (windSpeed < 5) "Calm" else if (windSpeed > 20) "Windy" else "Breezy"
        val condition = when (weatherCode) { in 1..3 -> "Cloudy"; 45, 48 -> "Fog"; in 51..67, in 80..82 -> "Rain"; in 71..77, in 85..86 -> "Snow"; in 95..99 -> "Storm"; else -> "Clear" }

        val place = PlaceLiteraryLexicon.build(
            isoCountryCode = iso,
            countryName = country,
            adminArea = admin,
            city = city,
            preferJapaneseDisplay = true
        )
        val search = PlaceLiteraryLexicon.uniquePreserve(
            place.search + listOf(season, sunStr, condition)
        )
        atmosphereKeywords = search.joinToString(",")
        atmospherePlaceKeys = place.placeKeysForRank

        val sMap = mapOf("Spring" to "春", "Summer" to "夏", "Autumn" to "秋", "Winter" to "冬")
        val sunMap = mapOf("Morning" to "朝", "Daytime" to "昼", "Evening" to "夕暮れ", "Night" to "夜")
        val wMap = mapOf("Calm" to "穏やか", "Breezy" to "そよ風", "Windy" to "風")
        val cMap = mapOf("Clear" to "晴れ", "Cloudy" to "曇り", "Rain" to "雨", "Snow" to "雪", "Fog" to "霧", "Storm" to "嵐")

        val placePrefix = if (place.displayPlace.isNotEmpty()) "${place.displayPlace}\n" else ""
        return "$placePrefix${sMap[season]}、${sunMap[sunStr]}\n${cMap[condition]} / ${wMap[windStr]}\n(${temp.roundToInt()}°C)"
    }

    private fun preferPlaceMatches(
        results: List<Map<String, String>>,
        placeKeys: List<String>
    ): List<Map<String, String>> {
        if (placeKeys.isEmpty() || results.isEmpty()) return results
        val keys = placeKeys.map { it.lowercase() }
        val scored = results.map { row ->
            val hay = listOf(row["quote"], row["title"], row["author"]).joinToString(" ").lowercase()
            val score = keys.sumOf { key -> if (hay.contains(key)) 3 else 0 }
            score to row
        }.sortedByDescending { it.first }
        val strong = scored.filter { it.first > 0 }.map { it.second }
        if (strong.isEmpty()) return results
        val weak = scored.filter { it.first == 0 }.map { it.second }
        return strong + weak
    }

    private fun fetchAndReturnToMain(urlString: String, isSearch: Boolean = false, skipRoulette: Boolean = true, searchKeyword: String = "", isAtmosphere: Boolean = false) {
        Thread {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
                    var quote = ""
                    var title = ""
                    var author = ""

                    val preservedPlaceKeys = if (isAtmosphere) atmospherePlaceKeys else emptyList()
                    AppSharedState.clear()

                    if (response.trim().startsWith("[")) {
                        val array = JSONArray(response)
                        val tempList = mutableListOf<Map<String, String>>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            tempList.add(mapOf(
                                "quote" to obj.optString("quote", ""),
                                "title" to obj.optString("title", obj.optString("book", "")),
                                "author" to obj.optString("author", "")
                            ))
                        }

                        if (tempList.isNotEmpty()) {
                            if (isSearch) {
                                AppSharedState.isFiltering = true
                                AppSharedState.currentSearchText = searchKeyword
                                AppSharedState.filteredFortunes = tempList.toList()
                                val randomItem = tempList.random()
                                quote = randomItem["quote"] ?: ""
                                title = randomItem["title"] ?: ""
                                author = randomItem["author"] ?: ""
                            } else if (isAtmosphere) {
                                val ranked = preferPlaceMatches(tempList, preservedPlaceKeys)
                                AppSharedState.isAtmosphereMode = true
                                AppSharedState.currentAtmosphereKeywords = searchKeyword
                                AppSharedState.atmospherePlaceKeywords = preservedPlaceKeys
                                AppSharedState.atmosphereFortunes = ranked
                                val boost = minOf(ranked.size, maxOf(8, ranked.size / 3))
                                val randomItem = ranked.take(boost).randomOrNull() ?: ranked.random()
                                quote = randomItem["quote"] ?: ""
                                title = randomItem["title"] ?: ""
                                author = randomItem["author"] ?: ""
                            } else {
                                val randomItem = tempList.random()
                                quote = randomItem["quote"] ?: ""
                                title = randomItem["title"] ?: ""
                                author = randomItem["author"] ?: ""
                            }
                        }
                    } else if (response.trim().startsWith("{")) {
                        val obj = JSONObject(response)
                        quote = obj.optString("quote", "")
                        title = obj.optString("title", obj.optString("book", ""))
                        author = obj.optString("author", "")
                    }

                    if (quote.isNotEmpty()) {
                        runOnUiThread {
                            try {
                                val intent = Intent(this@SettingsActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                intent.putExtra("forceQuote", quote)
                                intent.putExtra("forceTitle", title)
                                intent.putExtra("forceAuthor", author)
                                intent.putExtra("skipRoulette", skipRoulette)
                                startActivity(intent)
                                finish()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    } else {
                        runOnUiThread {
                            if (isSearch) Toast.makeText(applicationContext, "見つかりませんでした。別のキーワードをお試しください。", Toast.LENGTH_SHORT).show()
                            else Toast.makeText(applicationContext, "言葉が見つかりませんでした。", Toast.LENGTH_SHORT).show()
                            resetSearchUI()
                        }
                    }
                } else {
                    runOnUiThread { Toast.makeText(applicationContext, "通信エラーが発生しました。", Toast.LENGTH_SHORT).show(); resetSearchUI() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { Toast.makeText(applicationContext, "ネットワークエラーが発生しました。", Toast.LENGTH_SHORT).show(); resetSearchUI() }
            }
        }.start()
    }

    private fun resetSearchUI() {
        try {
            findViewById<ProgressBar>(R.id.progressSearch).visibility = View.GONE
            findViewById<Button>(R.id.btnSearch).visibility = View.VISIBLE

            val btnDrawAtmosphere = findViewById<Button>(R.id.btnDrawAtmosphere)
            val progressDrawAtmosphere = findViewById<ProgressBar>(R.id.progressDrawAtmosphere)
            btnDrawAtmosphere.text = "言葉を引く"
            progressDrawAtmosphere.visibility = View.GONE
            findViewById<FrameLayout>(R.id.overlayAtmosphere).visibility = View.GONE
        } catch (e: Exception) { e.printStackTrace() }
    }
}