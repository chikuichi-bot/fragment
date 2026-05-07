package jp.lagado.literaryfragments

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesActivity : AppCompatActivity() {

    private var isFavoritesTab = true
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: QuoteAdapter
    private var isDark = false

    private lateinit var tabFavorites: TextView
    private lateinit var tabHistory: TextView

    private var swipeStartY = 0f
    private var isAnimatingOut = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeScreen()
            }
        })

        tabFavorites = findViewById(R.id.tabFavorites)
        tabHistory = findViewById(R.id.tabHistory)
        recyclerView = findViewById(R.id.recyclerView)
        emptyText = findViewById(R.id.emptyText)

        findViewById<Button>(R.id.btnBack).setOnClickListener { closeScreen() }

        applyDarkModeColors()

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = QuoteAdapter()
        recyclerView.adapter = adapter

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val quote = adapter.items[position]
                if (isFavoritesTab) {
                    QuoteStorage.removeFavorite(this@FavoritesActivity, quote.text)
                    adapter.items.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    if (adapter.items.isEmpty()) emptyText.visibility = View.VISIBLE
                }
            }
            override fun getSwipeDirs(r: RecyclerView, v: RecyclerView.ViewHolder): Int {
                if (!isFavoritesTab) return 0
                return super.getSwipeDirs(r, v)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)

        tabFavorites.setOnClickListener {
            if (!isFavoritesTab) {
                isFavoritesTab = true
                updateTabStyles()
                loadItems()
            }
        }

        tabHistory.setOnClickListener {
            if (isFavoritesTab) {
                isFavoritesTab = false
                updateTabStyles()
                loadItems()
            }
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
        loadItems()
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
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

            val bgColor = if (isDark) Color.BLACK else Color.parseColor("#F2F2F7")
            val cardColor = if (isDark) Color.parseColor("#1C1C1E") else Color.WHITE
            val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")

            window.decorView.setBackgroundColor(bgColor)
            findViewById<ViewGroup>(android.R.id.content).getChildAt(0)?.setBackgroundColor(bgColor)

            val btnBack = findViewById<Button>(R.id.btnBack)
            btnBack.setTextColor(if (isDark) Color.WHITE else Color.parseColor("#007AFF"))
            val header = btnBack.parent as? ViewGroup
            header?.setBackgroundColor(cardColor)
            for (i in 0 until (header?.childCount ?: 0)) {
                val view = header?.getChildAt(i)
                if (view is TextView && view.id != R.id.btnBack) {
                    view.setTextColor(textColor)
                }
            }

            emptyText.setTextColor(if (isDark) Color.LTGRAY else Color.GRAY)
            updateTabStyles()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateTabStyles() {
        val activeBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.WHITE
        val inactiveBgColor = Color.TRANSPARENT
        val activeTextColorFavorites = if (isDark) Color.WHITE else Color.parseColor("#FF9500")
        val activeTextColorHistory = if (isDark) Color.WHITE else Color.DKGRAY
        val inactiveTextColor = if (isDark) Color.GRAY else Color.parseColor("#888888")

        if (isFavoritesTab) {
            tabFavorites.setBackgroundColor(activeBgColor)
            tabFavorites.setTextColor(activeTextColorFavorites)
            tabFavorites.setTypeface(null, Typeface.BOLD)
            tabFavorites.elevation = 4f

            tabHistory.setBackgroundColor(inactiveBgColor)
            tabHistory.setTextColor(inactiveTextColor)
            tabHistory.setTypeface(null, Typeface.NORMAL)
            tabHistory.elevation = 0f
        } else {
            tabHistory.setBackgroundColor(activeBgColor)
            tabHistory.setTextColor(activeTextColorHistory)
            tabHistory.setTypeface(null, Typeface.BOLD)
            tabHistory.elevation = 4f

            tabFavorites.setBackgroundColor(inactiveBgColor)
            tabFavorites.setTextColor(inactiveTextColor)
            tabFavorites.setTypeface(null, Typeface.NORMAL)
            tabFavorites.elevation = 0f
        }
    }

    private fun loadItems() {
        val items = if (isFavoritesTab) QuoteStorage.getFavorites(this) else QuoteStorage.getHistory(this)
        if (items.isEmpty()) {
            emptyText.text = if (isFavoritesTab) "まだ保存された言葉はありません。" else "閲覧履歴はありません。"
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.items = items.toMutableList()
            adapter.notifyDataSetChanged()
        }
    }

    inner class QuoteAdapter : RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder>() {
        var items: MutableList<QuoteData> = mutableListOf()

        inner class QuoteViewHolder(val layout: LinearLayout, val textBody: TextView, val metaInfo: TextView, val btnAi: TextView) : RecyclerView.ViewHolder(layout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
            val cardBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.WHITE
            val textColor = if (isDark) Color.WHITE else Color.DKGRAY
            val metaColor = if (isDark) Color.LTGRAY else Color.GRAY

            val cardLayout = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)

                // 🌟 ここを四角形から角丸（36f）に変更！
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(cardBgColor)
                    cornerRadius = 36f
                }

                elevation = if (isDark) 0f else 4f
                val params = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 24)
                layoutParams = params
            }

            val textBody = TextView(parent.context).apply { textSize = 16f; setTextColor(textColor) }
            cardLayout.addView(textBody)

            val metaInfo = TextView(parent.context).apply { textSize = 12f; setTextColor(metaColor); setPadding(0, 16, 0, 0) }
            cardLayout.addView(metaInfo)

            val btnAiBgColor = if (isDark) Color.parseColor("#003366") else Color.parseColor("#E3F2FD")
            val btnAiTextColor = if (isDark) Color.parseColor("#66B2FF") else Color.parseColor("#007AFF")

            val btnAi = TextView(parent.context).apply {
                text = "✨ View Explanation"
                textSize = 12f
                setTextColor(btnAiTextColor)
                setPadding(24, 12, 24, 12)
                gravity = Gravity.CENTER
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 24, 0, 0)
                layoutParams = params
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(btnAiBgColor)
                    cornerRadius = 16f
                }
                visibility = View.GONE
            }
            cardLayout.addView(btnAi)

            return QuoteViewHolder(cardLayout, textBody, metaInfo, btnAi)
        }

        override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
            val quote = items[position]
            holder.textBody.text = quote.text.replace("<br>", "\n")

            if (quote.title.isNotEmpty() || quote.author.isNotEmpty()) {
                holder.metaInfo.text = "${quote.title} / ${quote.author}"
                holder.metaInfo.visibility = View.VISIBLE
                holder.metaInfo.setOnClickListener {
                    try {
                        val query = java.net.URLEncoder.encode("${quote.title} ${quote.author}", "UTF-8")
                        val url = "https://www.google.com/search?q=$query"
                        holder.layout.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } else {
                holder.metaInfo.visibility = View.GONE
                holder.metaInfo.setOnClickListener(null)
            }

            val prefs = holder.layout.context.getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
            val hasHistory = prefs.contains("chatHistory_${quote.text}")
            val explainedSet = prefs.getStringSet("explainedQuotesSet", emptySet()) ?: emptySet()

            if (hasHistory || explainedSet.contains(quote.text)) {
                holder.btnAi.visibility = View.VISIBLE
                holder.btnAi.setOnClickListener {
                    val intent = Intent(holder.layout.context, ExplanationActivity::class.java)
                    intent.putExtra("quote", quote.text)
                    holder.layout.context.startActivity(intent)
                }
            } else {
                holder.btnAi.visibility = View.GONE
                holder.btnAi.setOnClickListener(null)
            }

            holder.layout.setOnClickListener {
                val intent = Intent(holder.layout.context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                intent.putExtra("forceQuote", quote.text)
                intent.putExtra("forceTitle", quote.title)
                intent.putExtra("forceAuthor", quote.author)
                holder.layout.context.startActivity(intent)
            }
        }

        override fun getItemCount() = items.size
    }
}