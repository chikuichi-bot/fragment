package jp.lagado.literaryfragments

import android.content.Context
import androidx.core.content.edit
import java.util.Locale

/**
 * iOS `LanguageManager.preferredDefaultLanguage` / `defaultJapanese` と同型。
 * 端末が日本語なら初回だけ `nativeLanguage` を「日本語」にする（未設定時のみ）。
 */
object LanguagePrefs {
    private const val PREFS_NAME = "PocketFortunePrefs"
    private const val KEY_NATIVE_LANGUAGE = "nativeLanguage"

    const val JAPANESE = "日本語"
    const val ENGLISH = "English"

    fun preferredDefaultLanguage(): String {
        val locales = Locale.getDefault()
        if (locales.language.startsWith("ja", ignoreCase = true)) {
            return JAPANESE
        }
        // API 24+ の preferred 一覧も見る
        try {
            val configLocales = android.content.res.Resources.getSystem().configuration.locales
            for (i in 0 until configLocales.size()) {
                if (configLocales[i].language.startsWith("ja", ignoreCase = true)) {
                    return JAPANESE
                }
            }
        } catch (_: Exception) {
        }
        return ENGLISH
    }

    /** 起動時に呼ぶ。既存ユーザーの設定は上書きしない。 */
    fun ensureDefaultLanguage(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_NATIVE_LANGUAGE)) {
            prefs.edit { putString(KEY_NATIVE_LANGUAGE, preferredDefaultLanguage()) }
        }
    }

    fun getNativeLanguage(context: Context): String {
        ensureDefaultLanguage(context)
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NATIVE_LANGUAGE, ENGLISH) ?: ENGLISH
    }

    fun isJapanese(context: Context): Boolean {
        val lang = getNativeLanguage(context)
        return lang == JAPANESE || lang.contains("Japanese", ignoreCase = true)
    }

    /** iOS `UIStrings.defaultJapanese` の AI レベル名（設定スピナー用） */
    val aiLevelLabelsJa = arrayOf("中学", "高校", "大学", "ビジネス")

    val aiLevelDescsJa = arrayOf(
        "難しい文法用語を避け、基本的な構文をやさしく解説します。初心者向けです。",
        "受験で重要な文法を指摘し、文の構造を論理的に解説します。",
        "文学的な比喩やニュアンス、文化的背景まで掘り下げます。",
        "フォーマル度や実務での使い方に焦点を当てて解説します。",
    )
}
