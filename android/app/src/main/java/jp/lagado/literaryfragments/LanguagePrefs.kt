package jp.lagado.literaryfragments

import android.content.Context
import androidx.core.content.edit
import java.util.Locale

/**
 * iOS `LanguageManager` / `UIStrings`（Language / 言語）と同型。
 * 端末が日本語なら初回だけ `nativeLanguage` を「日本語」にする（未設定時のみ）。
 */
object LanguagePrefs {
    private const val PREFS_NAME = "PocketFortunePrefs"
    private const val KEY_NATIVE_LANGUAGE = "nativeLanguage"

    const val JAPANESE = "日本語"
    const val ENGLISH = "English"

    /** iOS LanguageManager.allLanguages と同型の主要言語コード */
    val MAJOR_CODES = listOf(
        "en", "ja", "zh", "es", "fr", "de", "ko", "it", "pt", "ru",
        "ar", "hi", "id", "tr", "vi", "th", "nl", "pl", "sv", "fi",
        "da", "no", "cs", "el", "hu", "ro", "uk", "ms", "he", "fa",
    )

    /** code → 自称名（English / 日本語 / français …） */
    val codeToDisplayName: Map<String, String> by lazy {
        MAJOR_CODES.mapNotNull { code ->
            val loc = Locale.forLanguageTag(code)
            val name = when (code) {
                "ja" -> JAPANESE
                "en" -> ENGLISH
                else -> loc.getDisplayLanguage(loc).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(loc) else it.toString()
                }
            }
            name.takeIf { it.isNotBlank() }?.let { code to it }
        }.toMap()
    }

    /** 各言語の自称名（スピナー用） */
    val allLanguages: List<String> by lazy {
        MAJOR_CODES.mapNotNull { codeToDisplayName[it] }.distinct()
    }

    /** 表示名 → 言語コード（assets/ui_strings/{code}.json 用） */
    fun languageCodeFor(displayName: String): String {
        if (isJapaneseLanguage(displayName)) return "ja"
        if (displayName.contains("English", ignoreCase = true)) return "en"
        codeToDisplayName.entries.firstOrNull { it.value.equals(displayName, ignoreCase = true) }?.let {
            return it.key
        }
        // フォールバック: 部分一致
        codeToDisplayName.entries.firstOrNull {
            displayName.contains(it.value, ignoreCase = true) || it.value.contains(displayName, ignoreCase = true)
        }?.let { return it.key }
        return "en"
    }

    fun preferredDefaultLanguage(): String {
        val locales = Locale.getDefault()
        if (locales.language.startsWith("ja", ignoreCase = true)) {
            return JAPANESE
        }
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

    fun setNativeLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_NATIVE_LANGUAGE, language) }
    }

    fun isJapanese(context: Context): Boolean = isJapaneseLanguage(getNativeLanguage(context))

    fun isJapaneseLanguage(lang: String): Boolean {
        return lang == JAPANESE || lang.contains("Japanese", ignoreCase = true)
    }

    /** iOS Settings: 常に英語 "Language" を残す */
    fun languageSectionTitle(): String = "Language / 言語"

    fun languageSectionCaption(): String = "中文 · Español · English"

    fun languageSectionFooter(): String =
        "If the UI is in the wrong language, tap here and choose yours."

    /** iOS `UIStrings.defaultJapanese` の AI レベル名 */
    val aiLevelLabelsJa = arrayOf("中学", "高校", "大学", "ビジネス")

    val aiLevelDescsJa = arrayOf(
        "難しい文法用語を避け、基本的な構文をやさしく解説します。初心者向けです。",
        "受験で重要な文法を指摘し、文の構造を論理的に解説します。",
        "文学的な比喩やニュアンス、文化的背景まで掘り下げます。",
        "フォーマル度や実務での使い方に焦点を当てて解説します。",
    )

    val aiLevelLabelsEn = arrayOf("Middle School", "High School", "College", "Business")

    val aiLevelDescsEn = arrayOf(
        "Avoids difficult grammar terms and explains basic sentence structures gently. Great for beginners.",
        "Points out important grammar for exams and explains logical sentence structures.",
        "Explores literary metaphors, nuances, and cultural backgrounds for advanced learners.",
        "Focuses on formality and how to use expressions in practical professional situations.",
    )

    fun aiLevelLabels(isJapanese: Boolean): Array<String> =
        if (isJapanese) aiLevelLabelsJa else aiLevelLabelsEn

    fun aiLevelDescs(isJapanese: Boolean): Array<String> =
        if (isJapanese) aiLevelDescsJa else aiLevelDescsEn

    fun storeBadge500(isJapanese: Boolean): String =
        if (isJapanese) "人気" else "MOST POPULAR"

    fun storeBadge1000(isJapanese: Boolean): String =
        if (isJapanese) "お得" else "BEST VALUE"
}
