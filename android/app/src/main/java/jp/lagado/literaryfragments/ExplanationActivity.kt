package jp.lagado.literaryfragments

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val isUser: Boolean, val text: String)

class ExplanationActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var loadingLayout: LinearLayout
    private lateinit var editInput: EditText
    private lateinit var btnSend: Button
    private lateinit var textTargetQuote: TextView
    private lateinit var textTicketInfo: TextView
    private lateinit var rootLayout: View

    private var targetQuote: String = ""
    private var chatMessages = mutableListOf<ChatMessage>()
    private var isLoading = false
    private var isDark = false

    private var swipeStartY = 0f
    private var isAnimatingOut = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeScreen()
            }
        })

        try {
            setContentView(R.layout.activity_explanation)
            rootLayout = findViewById(android.R.id.content)
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
            return
        }

        targetQuote = intent.getStringExtra("quote") ?: ""

        textTargetQuote = findViewById(R.id.textTargetQuote)
        textTargetQuote.text = "「${targetQuote}」"

        findViewById<Button>(R.id.btnBack).setOnClickListener { closeScreen() }

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        loadingLayout = findViewById(R.id.loadingLayout)
        editInput = findViewById(R.id.editInput)
        btnSend = findViewById(R.id.btnSend)
        textTicketInfo = findViewById(R.id.textTicketInfo)

        applyDarkModeColors()
        updateTicketUI()

        chatAdapter = ChatAdapter()
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        btnSend.setOnClickListener {
            val text = editInput.text.toString().trim()
            if (text.isNotEmpty() && !isLoading) {
                try {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(editInput.windowToken, 0)
                } catch (e: Exception) { e.printStackTrace() }

                if (TicketManager.consumeTicket(this)) {
                    editInput.text.clear()
                    addMessage(ChatMessage(true, text))
                    requestGemini(isInitial = false, userMessage = text)
                    updateTicketUI()
                } else {
                    Toast.makeText(this, "チケットが不足しています", Toast.LENGTH_LONG).show()
                }
            }
        }

        loadInitialData()
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
        if (::chatAdapter.isInitialized) {
            chatAdapter.notifyDataSetChanged()
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
            val textColor = if (isDark) Color.WHITE else Color.BLACK

            window.decorView.setBackgroundColor(bgColor)
            (rootLayout as? ViewGroup)?.getChildAt(0)?.setBackgroundColor(bgColor)

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

            textTargetQuote.setTextColor(textColor)
            textTicketInfo.setTextColor(if (isDark) Color.LTGRAY else Color.GRAY)

            editInput.setTextColor(textColor)
            editInput.setHintTextColor(if (isDark) Color.GRAY else Color.LTGRAY)
            editInput.setPadding(40, 24, 40, 24)
            editInput.background = GradientDrawable().apply {
                setColor(if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E2E2E7"))
                cornerRadius = 36f
            }

            val inputParent = editInput.parent as? ViewGroup
            inputParent?.setBackgroundColor(cardColor)
            val inputGrandParent = inputParent?.parent as? ViewGroup
            inputGrandParent?.setBackgroundColor(cardColor)

            // 🌟 送信ボタンを鮮やかなブルー背景＋白文字に変更！
            btnSend.setTextColor(Color.WHITE)
            btnSend.background = GradientDrawable().apply {
                setColor(if (isDark) Color.parseColor("#0A84FF") else Color.parseColor("#007AFF"))
                cornerRadius = 36f
            }

        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateTicketUI() {
        try {
            val free = TicketManager.getFreeTickets(this)
            val paid = TicketManager.getPaidTickets(this)
            if (free > 0) {
                textTicketInfo.text = "無料枠: 残り $free 回"
                editInput.hint = "質問を入力... (無料)"
            } else {
                textTicketInfo.text = "所有チケット: $paid 枚"
                editInput.hint = "質問を入力... (チケット消費)"
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadInitialData() {
        try {
            val prefs = getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
            val historyJson = prefs.getString("chatHistory_$targetQuote", null)

            if (historyJson != null && historyJson != "[]") {
                val array = JSONArray(historyJson)
                chatMessages.clear()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    chatMessages.add(ChatMessage(obj.getBoolean("isUser"), obj.getString("text")))
                }
                chatAdapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(chatMessages.size - 1)
            } else {
                if (TicketManager.consumeTicket(this)) {
                    updateTicketUI()
                    requestGemini(isInitial = true, userMessage = "")
                } else {
                    Toast.makeText(this, "初回解説のためのチケットが不足しています", Toast.LENGTH_LONG).show()
                    closeScreen()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun saveHistory() {
        try {
            val array = JSONArray()
            chatMessages.forEach {
                val obj = JSONObject()
                obj.put("isUser", it.isUser)
                obj.put("text", it.text)
                array.put(obj)
            }
            val prefs = getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("chatHistory_$targetQuote", array.toString()).apply()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun addMessage(message: ChatMessage) {
        runOnUiThread {
            try {
                chatMessages.add(message)
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                saveHistory()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setLoading(loading: Boolean) {
        runOnUiThread {
            try {
                isLoading = loading
                loadingLayout.visibility = if (loading) View.VISIBLE else View.GONE
                btnSend.isEnabled = !loading
                editInput.isEnabled = !loading
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun requestGemini(isInitial: Boolean, userMessage: String) {
        setLoading(true)
        Thread {
            try {
                val prefs = getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
                val levelIndex = prefs.getInt("englishLevelIndex", 1)
                val jpLevels = listOf(
                    "中学英語の基礎（英検3〜4級程度）を前提に、複雑な文法用語は避け、基本的な構文（SVOなど）や基礎単語をわかりやすくやさしい言葉で解説してください。",
                    "高校英語（英検準2〜2級、大学受験レベル）を前提に、関係詞、分詞構文、仮定法などの重要文法を指摘し、文の構造を論理的に解説してください。",
                    "大学生・教養レベル（英検準1級以上）を前提に、文学的な比喩やニュアンス、文化的背景、抽象的な語彙の深掘りを含めて、よりアカデミックで高度な解説を行ってください。",
                    "ビジネスパーソンを前提に、この表現や含まれる単語が実際のビジネスシーン（メール、会議、交渉など）でどう活かせるか、フォーマル度やプロフェッショナルな言い回しに焦点を当てて解説してください。"
                )
                val specificInstruction = jpLevels[levelIndex.coerceIn(0, jpLevels.size - 1)]

                val contentsArray = JSONArray()

                if (isInitial) {
                    var initialPrompt = "あなたはプロの英語教師であり、文学コンシェルジュでもあります。客観的かつ簡潔に出力してください。\n"
                    initialPrompt += "【重要】挨拶、前置き、結びの言葉は一切不要です。いきなり「【作品と作家】」から出力を開始してください。Markdown記号は使用せず、プレーンテキストで見やすく整形してください。\n\n"
                    initialPrompt += "対象読者のレベルと解説方針：\n$specificInstruction\n\n"
                    initialPrompt += "【文章】 \"$targetQuote\"\n\n"
                    initialPrompt += "初回解説時は以下の6つの角度から出力してください。\n"
                    initialPrompt += "1. 【作品と作家】\n2. 【和訳】\n3. 【意訳】\n4. 【語彙・文法】\n5. 【ニュアンス】\n6. 【実践・応用】"

                    val initParts = JSONArray().put(JSONObject().put("text", initialPrompt))
                    contentsArray.put(JSONObject().put("role", "user").put("parts", initParts))
                } else {
                    var initialPrompt = "あなたはプロの英語教師であり、文学コンシェルジュでもあります。客観的かつ簡潔に出力してください。\n"
                    initialPrompt += "【重要】挨拶、前置き、結びの言葉は一切不要です。いきなり「【作品と作家】」から出力を開始してください。Markdown記号は使用せず、プレーンテキストで見やすく整形してください。\n\n"
                    initialPrompt += "対象読者のレベルと解説方針：\n$specificInstruction\n\n"
                    initialPrompt += "【文章】 \"$targetQuote\"\n\n"
                    initialPrompt += "初回解説時は以下の6つの角度から出力してください。\n"
                    initialPrompt += "1. 【作品と作家】\n2. 【和訳】\n3. 【意訳】\n4. 【語彙・文法】\n5. 【ニュアンス】\n6. 【実践・応用】"

                    val initParts = JSONArray().put(JSONObject().put("text", initialPrompt))
                    contentsArray.put(JSONObject().put("role", "user").put("parts", initParts))

                    for (msg in chatMessages) {
                        if (msg === chatMessages.last() && msg.isUser) continue
                        val role = if (msg.isUser) "user" else "model"
                        val parts = JSONArray().put(JSONObject().put("text", msg.text))
                        contentsArray.put(JSONObject().put("role", role).put("parts", parts))
                    }

                    val followUpPrompt = "【追加の質問】: $userMessage\n\n" +
                            "上記の質問にのみ直接答えてください。初回の全体解説（作品情報、和訳、語彙など）は絶対に繰り返さないでください。"

                    val newParts = JSONArray().put(JSONObject().put("text", followUpPrompt))
                    contentsArray.put(JSONObject().put("role", "user").put("parts", newParts))
                }

                val requestBody = JSONObject().put("contents", contentsArray)
                val url = URL("https://lagado.jp/fragments/gemini.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    val input = requestBody.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val text = json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")

                    if (!text.isNullOrEmpty()) {
                        addMessage(ChatMessage(false, text))

                        val set = prefs.getStringSet("explainedQuotesSet", mutableSetOf()) ?: mutableSetOf()
                        val newSet = mutableSetOf<String>().apply { addAll(set); add(targetQuote) }
                        prefs.edit().putStringSet("explainedQuotesSet", newSet).apply()

                        try {
                            val found = QuoteStorage.getHistory(this@ExplanationActivity).firstOrNull { it.text == targetQuote }
                            QuoteStorage.addFavorite(this@ExplanationActivity, QuoteData(targetQuote, found?.title ?: "", found?.author ?: ""))
                        } catch (e: Exception) { e.printStackTrace() }

                    } else {
                        addMessage(ChatMessage(false, "AIからの応答を解析できませんでした。"))
                        TicketManager.refundFreeTicket(this@ExplanationActivity)
                    }
                } else {
                    addMessage(ChatMessage(false, "通信エラー: ${connection.responseCode}"))
                    TicketManager.refundFreeTicket(this@ExplanationActivity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                addMessage(ChatMessage(false, "ネットワークエラーが発生しました。"))
                TicketManager.refundFreeTicket(this@ExplanationActivity)
            } finally {
                setLoading(false)
            }
        }.start()
    }

    inner class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
        inner class MessageViewHolder(val layout: LinearLayout, val textView: TextView) : RecyclerView.ViewHolder(layout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val rootLayout = LinearLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) }
            }
            val textView = TextView(parent.context).apply {
                textSize = 15f
                setPadding(36, 24, 36, 24)
                maxWidth = (parent.context.resources.displayMetrics.widthPixels * 0.8).toInt()
            }
            rootLayout.addView(textView)
            return MessageViewHolder(rootLayout, textView)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val msg = chatMessages[position]
            holder.textView.text = msg.text
            val params = holder.textView.layoutParams as LinearLayout.LayoutParams

            if (msg.isUser) {
                holder.layout.gravity = Gravity.END
                val userBgColor = if (isDark) Color.parseColor("#004080") else Color.parseColor("#E3F2FD")
                val userTextColor = if (isDark) Color.parseColor("#66B2FF") else Color.parseColor("#007AFF")
                holder.textView.setTextColor(userTextColor)
                holder.textView.background = GradientDrawable().apply { setColor(userBgColor); cornerRadius = 36f }
            } else {
                holder.layout.gravity = Gravity.START
                val aiBgColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#F0F0F0")
                val aiTextColor = if (isDark) Color.WHITE else Color.BLACK
                holder.textView.setTextColor(aiTextColor)
                holder.textView.background = GradientDrawable().apply { setColor(aiBgColor); cornerRadius = 36f }
            }
            holder.textView.layoutParams = params
        }

        override fun getItemCount() = chatMessages.size
    }
}