package jp.lagado.literaryfragments

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class TicketStoreActivity : AppCompatActivity() {

    private var isDark = false
    private var swipeStartY = 0f
    private var isAnimatingOut = false
    private var purchaseBusy = false

    private lateinit var licenseManager: TicketLicenseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_store)

        licenseManager = TicketLicenseManager(this) { status ->
            runOnUiThread {
                updateTicketDisplay()
                if (status.phase == "prices") {
                    rebuildStoreList()
                }
            }
        }
        licenseManager.attachActivity(this)
        licenseManager.start()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeScreen()
            }
        })

        findViewById<Button>(R.id.btnBack).setOnClickListener { closeScreen() }

        applyDarkModeColors()
        rebuildStoreList()
        updateTicketDisplay()
    }

    override fun onDestroy() {
        licenseManager.destroy()
        super.onDestroy()
    }

    private fun rebuildStoreList() {
        val storeList = findViewById<LinearLayout>(R.id.storeList)
        storeList.removeAllViews()
        // iOS CatalogTicketPackRow 同型: 常に 100 / 500 / 1000 を出す
        addStoreItem(
            storeList,
            TicketLicenseManager.PRODUCT_100,
            100,
            licenseManager.displayPrice(TicketLicenseManager.PRODUCT_100),
            null,
        )
        addStoreItem(
            storeList,
            TicketLicenseManager.PRODUCT_500,
            500,
            licenseManager.displayPrice(TicketLicenseManager.PRODUCT_500),
            "人気",
        )
        addStoreItem(
            storeList,
            TicketLicenseManager.PRODUCT_1000,
            1000,
            licenseManager.displayPrice(TicketLicenseManager.PRODUCT_1000),
            "お得",
        )
        val anyPlayProduct = listOf(
            TicketLicenseManager.PRODUCT_100,
            TicketLicenseManager.PRODUCT_500,
            TicketLicenseManager.PRODUCT_1000,
        ).any { licenseManager.isProductAvailable(it) && !BuildConfig.DEBUG }
        if (!anyPlayProduct && !BuildConfig.DEBUG) {
            storeList.addView(TextView(this).apply {
                text = "Play の商品がまだ反映されていません。価格は目安表示です。"
                textSize = 11f
                setTextColor(if (isDark) Color.parseColor("#9E9E9E") else Color.parseColor("#757575"))
                setPadding(16, 12, 16, 8)
            })
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

    override fun onResume() {
        super.onResume()
        applyDarkModeColors()
        updateTicketDisplay()
    }

    private fun applyDarkModeColors() {
        try {
            val prefs = getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
            val theme = prefs.getInt("themePreference", 0)
            isDark = when (theme) {
                1 -> false
                2 -> true
                else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }

            // 💎 上質なマットカラーと立体感
            val bgColor = if (isDark) Color.parseColor("#121212") else Color.parseColor("#F8F9FA")
            val cardColor = if (isDark) Color.parseColor("#1E1E1E") else Color.WHITE
            val textColor = if (isDark) Color.parseColor("#EAEAEA") else Color.parseColor("#1D1D1D")
            val subTextColor = if (isDark) Color.parseColor("#9E9E9E") else Color.parseColor("#757575")

            window.decorView.setBackgroundColor(bgColor)
            findViewById<ViewGroup>(android.R.id.content).getChildAt(0)?.setBackgroundColor(bgColor)

            val btnBack = findViewById<Button>(R.id.btnBack)
            btnBack.setTextColor(if (isDark) Color.WHITE else Color.parseColor("#0A84FF")) // 落ち着いたアクセントブルー
            val header = btnBack.parent as? ViewGroup
            header?.setBackgroundColor(cardColor)
            for (i in 0 until (header?.childCount ?: 0)) {
                val view = header?.getChildAt(i)
                if (view is TextView && view.id != R.id.btnBack) {
                    view.setTextColor(textColor)
                }
            }

            val cardBg = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = 28f // 💎 洗練された角丸
            }

            findViewById<LinearLayout>(R.id.cardTop)?.apply {
                background = cardBg
                elevation = if (isDark) 0f else 6f
            }
            findViewById<LinearLayout>(R.id.storeList)?.apply {
                background = cardBg
                elevation = if (isDark) 0f else 6f
            }

            fun updateColors(view: View) {
                if (view is TextView && view.id != R.id.btnBack) {
                    val current = view.currentTextColor
                    if (current == Color.parseColor("#333333") || current == Color.BLACK || current == Color.WHITE) {
                        view.setTextColor(textColor)
                    } else if (current == Color.parseColor("#666666") || current == Color.GRAY || current == Color.parseColor("#AAAAAA")) {
                        view.setTextColor(subTextColor)
                    }
                } else if (view is ViewGroup) {
                    for (i in 0 until view.childCount) updateColors(view.getChildAt(i))
                }
            }
            findViewById<LinearLayout>(R.id.cardTop)?.let { updateColors(it) }

        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateTicketDisplay() {
        val current = TicketManager.getPaidTickets(this)
        findViewById<TextView>(R.id.textCurrentTickets).text = current.toString()
    }

    private fun addStoreItem(
        parent: LinearLayout,
        productId: String,
        amount: Int,
        price: String,
        badge: String?,
    ) {
        val available = licenseManager.isProductAvailable(productId)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 32, 16, 32)
            // iOS: 未取得でも青ボタン表示 · 透明度だけ落とす
            alpha = if (available) 1f else 0.85f

            setOnClickListener {
                if (purchaseBusy || isAnimatingOut) return@setOnClickListener
                if (!available) {
                    Toast.makeText(
                        this@TicketStoreActivity,
                        "商品を読み込めませんでした",
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@setOnClickListener
                }
                purchaseBusy = true
                licenseManager.purchase(productId) { status ->
                    runOnUiThread {
                        purchaseBusy = false
                        updateTicketDisplay()
                        when {
                            status.ok && status.phase == "purchased" -> {
                                Toast.makeText(
                                    this@TicketStoreActivity,
                                    "$amount 枚のチケットを購入しました！",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            status.phase == "cancelled" -> {
                                // ユーザーキャンセル — 黙って戻る
                            }
                            status.phase == "product_unavailable" -> {
                                Toast.makeText(
                                    this@TicketStoreActivity,
                                    "商品を読み込めませんでした",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            else -> {
                                Toast.makeText(
                                    this@TicketStoreActivity,
                                    "購入に失敗しました",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        val leftInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        if (badge != null) {
            leftInfo.addView(TextView(this).apply {
                text = badge
                textSize = 10f
                setTextColor(Color.parseColor("#FF9500"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            })
        }

        val titleContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        titleContainer.addView(TextView(this).apply {
            text = "$amount"
            textSize = 18f
            setTextColor(if (isDark) Color.parseColor("#EAEAEA") else Color.parseColor("#1D1D1D"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        titleContainer.addView(TextView(this).apply {
            text = " 枚"
            textSize = 14f
            setTextColor(if (isDark) Color.parseColor("#9E9E9E") else Color.parseColor("#757575"))
            setPadding(8, 0, 0, 0)
        })
        leftInfo.addView(titleContainer)
        leftInfo.addView(TextView(this).apply {
            text = "AI解説を読むためのチケットです。"
            textSize = 12f
            setTextColor(if (isDark) Color.parseColor("#9E9E9E") else Color.parseColor("#757575"))
            setPadding(0, 4, 0, 0)
        })

        val priceBtn = TextView(this).apply {
            text = price
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(40, 20, 40, 20)
            // 💎 購入ボタンも洗練された色と形に
            background = GradientDrawable().apply {
                setColor(if (isDark) Color.parseColor("#0A84FF") else Color.parseColor("#007AFF"))
                cornerRadius = 28f
            }
        }

        row.addView(leftInfo)
        row.addView(priceBtn)
        parent.addView(row)

        if (amount != 1000) {
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply {
                    setMargins(16, 0, 16, 0)
                }
                setBackgroundColor(if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E9ECEF"))
            }
            parent.addView(divider)
        }
    }
}