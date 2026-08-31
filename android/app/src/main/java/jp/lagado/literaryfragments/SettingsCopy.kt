package jp.lagado.literaryfragments

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/**
 * 設定・解説まわりの UI 文言。
 * - 日本語・英語: 内蔵
 * - その他: `assets/ui_strings/{code}.json` に同梱（オフライン即時）
 */
data class SettingsCopy(
    val settingsTitle: String,
    val done: String,
    val drawSection: String,
    val drawRandom: String,
    val senseMoment: String,
    val sensing: String,
    val searchSection: String,
    val scopeAll: String,
    val scopeQuote: String,
    val scopeAuthor: String,
    val scopeTitle: String,
    val searchHint: String,
    val search: String,
    val appearance: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val quoteLength: String,
    val lengthShort: String,
    val lengthLong: String,
    val aiLevelTitle: String,
    val level1: String,
    val level2: String,
    val level3: String,
    val level4: String,
    val level1Desc: String,
    val level2Desc: String,
    val level3Desc: String,
    val level4Desc: String,
    val ticketStore: String,
    val freeToday: String,
    val paidTickets: String,
    val goToStore: String,
    val freeUnit: String,
    val paidUnit: String,
    val privacy: String,
    val atmosphereTitle: String,
    val atmosphereAsk: String,
    val cancel: String,
    val drawQuote: String,
    val languageChanged: String,
    val aiExplanationTitle: String,
    val generatingText: String,
    val close: String,
    val send: String,
    val askHintFree: String,
    val askHintPaid: String,
    val freeLeftFmt: String,
    val paidLeftFmt: String,
    val outOfTickets: String,
    val outOfTicketsInitial: String,
    val parseError: String,
    val networkError: String,
) {
    fun aiLevelLabels(): Array<String> = arrayOf(level1, level2, level3, level4)
    fun aiLevelDescs(): Array<String> = arrayOf(level1Desc, level2Desc, level3Desc, level4Desc)

    fun toJson(): JSONObject = JSONObject().apply {
        put("settingsTitle", settingsTitle)
        put("done", done)
        put("drawSection", drawSection)
        put("drawRandom", drawRandom)
        put("senseMoment", senseMoment)
        put("sensing", sensing)
        put("searchSection", searchSection)
        put("scopeAll", scopeAll)
        put("scopeQuote", scopeQuote)
        put("scopeAuthor", scopeAuthor)
        put("scopeTitle", scopeTitle)
        put("searchHint", searchHint)
        put("search", search)
        put("appearance", appearance)
        put("themeSystem", themeSystem)
        put("themeLight", themeLight)
        put("themeDark", themeDark)
        put("quoteLength", quoteLength)
        put("lengthShort", lengthShort)
        put("lengthLong", lengthLong)
        put("aiLevelTitle", aiLevelTitle)
        put("level1", level1)
        put("level2", level2)
        put("level3", level3)
        put("level4", level4)
        put("level1Desc", level1Desc)
        put("level2Desc", level2Desc)
        put("level3Desc", level3Desc)
        put("level4Desc", level4Desc)
        put("ticketStore", ticketStore)
        put("freeToday", freeToday)
        put("paidTickets", paidTickets)
        put("goToStore", goToStore)
        put("freeUnit", freeUnit)
        put("paidUnit", paidUnit)
        put("privacy", privacy)
        put("atmosphereTitle", atmosphereTitle)
        put("atmosphereAsk", atmosphereAsk)
        put("cancel", cancel)
        put("drawQuote", drawQuote)
        put("languageChanged", languageChanged)
        put("aiExplanationTitle", aiExplanationTitle)
        put("generatingText", generatingText)
        put("close", close)
        put("send", send)
        put("askHintFree", askHintFree)
        put("askHintPaid", askHintPaid)
        put("freeLeftFmt", freeLeftFmt)
        put("paidLeftFmt", paidLeftFmt)
        put("outOfTickets", outOfTickets)
        put("outOfTicketsInitial", outOfTicketsInitial)
        put("parseError", parseError)
        put("networkError", networkError)
    }

    companion object {
        private const val CACHE_PREFIX = "ui_settings_v1_"

        fun japanese() = SettingsCopy(
            settingsTitle = "設定",
            done = "完了",
            drawSection = "言葉を引く",
            drawRandom = "無作為に引く",
            senseMoment = "今の気配を読み取る",
            sensing = "読み取り中...",
            searchSection = "言葉を探す",
            scopeAll = "すべて",
            scopeQuote = "言葉",
            scopeAuthor = "著者",
            scopeTitle = "作品名",
            searchHint = "キーワードで検索...",
            search = "検索",
            appearance = "外観 (テーマ)",
            themeSystem = "システム",
            themeLight = "ライト",
            themeDark = "ダーク",
            quoteLength = "文章の長さ",
            lengthShort = "短文",
            lengthLong = "長文",
            aiLevelTitle = "AI解説のレベル",
            level1 = "中学",
            level2 = "高校",
            level3 = "大学",
            level4 = "ビジネス",
            level1Desc = "難しい文法用語を避け、基本的な構文をやさしく解説します。初心者向けです。",
            level2Desc = "受験で重要な文法を指摘し、文の構造を論理的に解説します。",
            level3Desc = "文学的な比喩やニュアンス、文化的背景まで掘り下げます。",
            level4Desc = "フォーマル度や実務での使い方に焦点を当てて解説します。",
            ticketStore = "チケットストア",
            freeToday = "本日の無料解説",
            paidTickets = "所有チケット",
            goToStore = "ストアへ行く ›",
            freeUnit = "回",
            paidUnit = "枚",
            privacy = "プライバシーポリシー",
            atmosphereTitle = "今の気配",
            atmosphereAsk = "この気配に重なる言葉を引きますか？",
            cancel = "キャンセル",
            drawQuote = "言葉を引く",
            languageChanged = "言語を「%s」にしました",
            aiExplanationTitle = "AI解説",
            generatingText = "生成中…\n少々お待ちください⏳",
            close = "◀︎ 閉じる",
            send = "送信",
            askHintFree = "質問を入力... (無料枠: 残り%s回)",
            askHintPaid = "質問を入力... (チケット: 残り%s枚)",
            freeLeftFmt = "無料枠: 残り %s 回",
            paidLeftFmt = "所有チケット: %s 枚",
            outOfTickets = "チケットが不足しています",
            outOfTicketsInitial = "初回解説のためのチケットが不足しています",
            parseError = "AIからの応答を解析できませんでした。",
            networkError = "ネットワークエラーが発生しました。",
        )

        fun english() = SettingsCopy(
            settingsTitle = "Settings",
            done = "Done",
            drawSection = "Draw a Quote",
            drawRandom = "Draw Randomly",
            senseMoment = "Sense the Moment",
            sensing = "Sensing...",
            searchSection = "Search",
            scopeAll = "All",
            scopeQuote = "Quote",
            scopeAuthor = "Author",
            scopeTitle = "Title",
            searchHint = "Search by keyword...",
            search = "Search",
            appearance = "Appearance",
            themeSystem = "System",
            themeLight = "Light",
            themeDark = "Night",
            quoteLength = "Quote Length",
            lengthShort = "Short",
            lengthLong = "Long",
            aiLevelTitle = "AI Explanation Level",
            level1 = "Middle School",
            level2 = "High School",
            level3 = "College",
            level4 = "Business",
            level1Desc = "Avoids difficult grammar terms and explains basic sentence structures gently. Great for beginners.",
            level2Desc = "Points out important grammar for exams and explains logical sentence structures.",
            level3Desc = "Explores literary metaphors, nuances, and cultural backgrounds for advanced learners.",
            level4Desc = "Focuses on formality and how to use expressions in practical professional situations.",
            ticketStore = "Ticket Store",
            freeToday = "Free Today",
            paidTickets = "Paid Tickets",
            goToStore = "Go to Store ›",
            freeUnit = "",
            paidUnit = "",
            privacy = "Privacy Policy",
            atmosphereTitle = "Current Atmosphere",
            atmosphereAsk = "Draw a quote matching this moment?",
            cancel = "Cancel",
            drawQuote = "Draw Quote",
            languageChanged = "Language set to %s",
            aiExplanationTitle = "AI Explanation",
            generatingText = "Generating...\nPlease wait⏳",
            close = "◀︎ Close",
            send = "Send",
            askHintFree = "Ask a question... (Free: %s left)",
            askHintPaid = "Ask a question... (Tickets: %s left)",
            freeLeftFmt = "Free: %s left",
            paidLeftFmt = "Tickets: %s",
            outOfTickets = "You need tickets for AI Explanation.",
            outOfTicketsInitial = "You need tickets for AI Explanation.",
            parseError = "Failed to parse AI response.",
            networkError = "A network error occurred.",
        )

        fun fromJson(json: JSONObject, fallback: SettingsCopy = english()): SettingsCopy {
            fun s(key: String, def: String) = json.optString(key, def).ifBlank { def }
            return SettingsCopy(
                settingsTitle = s("settingsTitle", fallback.settingsTitle),
                done = s("done", fallback.done),
                drawSection = s("drawSection", fallback.drawSection),
                drawRandom = s("drawRandom", fallback.drawRandom),
                senseMoment = s("senseMoment", fallback.senseMoment),
                sensing = s("sensing", fallback.sensing),
                searchSection = s("searchSection", fallback.searchSection),
                scopeAll = s("scopeAll", fallback.scopeAll),
                scopeQuote = s("scopeQuote", fallback.scopeQuote),
                scopeAuthor = s("scopeAuthor", fallback.scopeAuthor),
                scopeTitle = s("scopeTitle", fallback.scopeTitle),
                searchHint = s("searchHint", fallback.searchHint),
                search = s("search", fallback.search),
                appearance = s("appearance", fallback.appearance),
                themeSystem = s("themeSystem", fallback.themeSystem),
                themeLight = s("themeLight", fallback.themeLight),
                themeDark = s("themeDark", fallback.themeDark),
                quoteLength = s("quoteLength", fallback.quoteLength),
                lengthShort = s("lengthShort", fallback.lengthShort),
                lengthLong = s("lengthLong", fallback.lengthLong),
                aiLevelTitle = s("aiLevelTitle", fallback.aiLevelTitle),
                level1 = s("level1", fallback.level1),
                level2 = s("level2", fallback.level2),
                level3 = s("level3", fallback.level3),
                level4 = s("level4", fallback.level4),
                level1Desc = s("level1Desc", fallback.level1Desc),
                level2Desc = s("level2Desc", fallback.level2Desc),
                level3Desc = s("level3Desc", fallback.level3Desc),
                level4Desc = s("level4Desc", fallback.level4Desc),
                ticketStore = s("ticketStore", fallback.ticketStore),
                freeToday = s("freeToday", fallback.freeToday),
                paidTickets = s("paidTickets", fallback.paidTickets),
                goToStore = s("goToStore", fallback.goToStore),
                freeUnit = s("freeUnit", fallback.freeUnit),
                paidUnit = s("paidUnit", fallback.paidUnit),
                privacy = s("privacy", fallback.privacy),
                atmosphereTitle = s("atmosphereTitle", fallback.atmosphereTitle),
                atmosphereAsk = s("atmosphereAsk", fallback.atmosphereAsk),
                cancel = s("cancel", fallback.cancel),
                drawQuote = s("drawQuote", fallback.drawQuote),
                languageChanged = s("languageChanged", fallback.languageChanged),
                aiExplanationTitle = s("aiExplanationTitle", fallback.aiExplanationTitle),
                generatingText = s("generatingText", fallback.generatingText),
                close = s("close", fallback.close),
                send = s("send", fallback.send),
                askHintFree = s("askHintFree", fallback.askHintFree),
                askHintPaid = s("askHintPaid", fallback.askHintPaid),
                freeLeftFmt = s("freeLeftFmt", fallback.freeLeftFmt),
                paidLeftFmt = s("paidLeftFmt", fallback.paidLeftFmt),
                outOfTickets = s("outOfTickets", fallback.outOfTickets),
                outOfTicketsInitial = s("outOfTicketsInitial", fallback.outOfTicketsInitial),
                parseError = s("parseError", fallback.parseError),
                networkError = s("networkError", fallback.networkError),
            )
        }

        fun loadCached(context: Context, language: String): SettingsCopy? {
            val raw = context.getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
                .getString(CACHE_PREFIX + language, null) ?: return null
            return try {
                fromJson(JSONObject(raw))
            } catch (_: Exception) {
                null
            }
        }

        fun saveCache(context: Context, language: String, copy: SettingsCopy) {
            context.getSharedPreferences("PocketFortunePrefs", Context.MODE_PRIVATE)
                .edit { putString(CACHE_PREFIX + language, copy.toJson().toString()) }
        }

        /** アプリ同梱 `assets/ui_strings/{code}.json` */
        fun loadFromAssets(context: Context, languageCode: String): SettingsCopy? {
            return try {
                context.assets.open("ui_strings/$languageCode.json").use { stream ->
                    val text = stream.bufferedReader(Charsets.UTF_8).readText()
                    fromJson(JSONObject(text))
                }
            } catch (_: Exception) {
                null
            }
        }

        /**
         * 即時に使えるコピー。
         * 日本語・英語は内蔵 → 同梱 assets →（旧）端末キャッシュ → 英語。
         * ネット翻訳は不要（主要言語はアプリに同梱済み）。
         */
        fun resolveSync(context: Context, language: String): SettingsCopy {
            if (LanguagePrefs.isJapaneseLanguage(language)) return japanese()
            val code = LanguagePrefs.languageCodeFor(language)
            if (code == "en") return english()
            loadFromAssets(context, code)?.let { return it }
            loadCached(context, language)?.let { return it }
            return english()
        }

        /**
         * 同梱に無い言語向けの予備。通常は呼ばない。
         */
        fun fetchTranslation(
            context: Context,
            targetLanguage: String,
            onReady: (SettingsCopy) -> Unit,
        ) {
            if (LanguagePrefs.isJapaneseLanguage(targetLanguage)) {
                onReady(japanese())
                return
            }
            val code = LanguagePrefs.languageCodeFor(targetLanguage)
            if (code == "en") {
                onReady(english())
                return
            }
            loadFromAssets(context, code)?.let {
                onReady(it)
                return
            }
            loadCached(context, targetLanguage)?.let {
                onReady(it)
                return
            }
            // 同梱済みが前提。失敗時は英語のまま（ネット取得はしない）
            onReady(english())
        }
    }
}
