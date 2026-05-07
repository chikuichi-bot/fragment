package jp.lagado.literaryfragments

import android.content.Context
import androidx.core.content.edit // 🌟 KTX（Kotlinの便利機能）をインポート
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 🌟 言葉のデータをまとめる箱
data class QuoteData(
    val text: String,
    val title: String,
    val author: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 🌟 お気に入りや履歴をスマホに保存するシステム
object QuoteStorage {
    private const val PREFS_NAME = "PocketFortunePrefs"
    private const val KEY_HISTORY = "displayHistory"
    private const val KEY_FAVORITES = "stockedQuotes"

    fun addHistory(context: Context, quote: QuoteData) {
        try {
            val history = getHistory(context).toMutableList()

            // 空の言葉は保存しない
            if (quote.text.isBlank()) return

            // 同じ言葉がすでに履歴にあれば一度消す
            history.removeAll { it.text.trim() == quote.text.trim() }

            // 先頭に追加
            history.add(0, quote)

            // 安全な削除処理
            if (history.size > 100) {
                history.removeAt(history.size - 1)
            }

            saveHistory(context, history)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getHistory(context: Context): List<QuoteData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return parseJsonList(jsonString)
    }

    // 🌟 修正：使われていなかった removeHistory 関数を削除してコードを軽量化しました

    private fun saveHistory(context: Context, history: List<QuoteData>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 🌟 修正：提案通り、KTXのスッキリした書き方 (edit { }) に変更
        prefs.edit { putString(KEY_HISTORY, toJsonString(history)) }
    }

    fun addFavorite(context: Context, quote: QuoteData) {
        try {
            val favorites = getFavorites(context).toMutableList()
            if (favorites.none { it.text.trim() == quote.text.trim() }) {
                favorites.add(0, quote)
                saveFavorites(context, favorites)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeFavorite(context: Context, text: String) {
        try {
            val favorites = getFavorites(context).toMutableList()
            favorites.removeAll { it.text.trim() == text.trim() }
            saveFavorites(context, favorites)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFavorites(context: Context): List<QuoteData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_FAVORITES, "[]") ?: "[]"
        return parseJsonList(jsonString)
    }

    private fun saveFavorites(context: Context, favorites: List<QuoteData>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 🌟 修正：KTXのスッキリした書き方に変更
        prefs.edit { putString(KEY_FAVORITES, toJsonString(favorites)) }
    }

    // JSONテキストをリストに変換する安全な処理
    private fun parseJsonList(jsonString: String): List<QuoteData> {
        val list = mutableListOf<QuoteData>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    QuoteData(
                        text = obj.optString("text", ""),
                        title = obj.optString("title", ""),
                        author = obj.optString("author", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // リストをJSONテキストに変換する安全な処理
    private fun toJsonString(list: List<QuoteData>): String {
        val array = JSONArray()
        try {
            for (item in list) {
                val obj = JSONObject()
                obj.put("text", item.text)
                obj.put("title", item.title)
                obj.put("author", item.author)
                obj.put("timestamp", item.timestamp)
                array.put(obj)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return array.toString()
    }
}

// 🌟 チケット管理システム
object TicketManager {
    private const val PREFS_NAME = "PocketFortunePrefs"

    fun checkDailyReset(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayString = formatter.format(Date())
            val lastDate = prefs.getString("lastFreeAITicketDate", "")

            if (todayString != lastDate) {
                // 🌟 修正：KTXを使って複数行の保存を綺麗にまとめました
                prefs.edit {
                    putInt("freeTickets", 3)
                    putString("lastFreeAITicketDate", todayString)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getFreeTickets(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt("freeTickets", 3)
    }

    fun getPaidTickets(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt("remainingTickets", 0)
    }

    fun consumeTicket(context: Context): Boolean {
        try {
            checkDailyReset(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val freeTickets = prefs.getInt("freeTickets", 3)
            val paidTickets = prefs.getInt("remainingTickets", 0)

            if (freeTickets > 0) {
                prefs.edit { putInt("freeTickets", freeTickets - 1) }
                return true
            } else if (paidTickets > 0) {
                prefs.edit { putInt("remainingTickets", paidTickets - 1) }
                return true
            }
        } catch (e: Exception) { e.printStackTrace() }
        return false
    }

    // エラー時にチケットを返還する機能
    fun refundFreeTicket(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentFree = prefs.getInt("freeTickets", 3)
            prefs.edit { putInt("freeTickets", currentFree + 1) }
        } catch (e: Exception) { e.printStackTrace() }
    }
}