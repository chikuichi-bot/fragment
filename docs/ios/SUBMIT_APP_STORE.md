# App Store 提出チェックリスト — Fragments iOS

> 版: **1.1.4** / Build **12**  
> 更新: 2026-08-30  
> 目的: 日本語名 `Fragments（文学の断片）` · 言語設定の見つけやすさ（Language / 言語）· 審査提出  
> 注: ASC で **1.1.4** を作成 → ビルド紐づけ → 審査。プライマリ日本語は可能なときアプリ情報で変更。

## A. Xcode（淡島様）

1. 未保存の変更を保存（日本語デフォルトUI・スクショ用起動など）
2. スキーム `LiteraryFragments` · Destination **Any iOS Device**
3. **Product → Archive** → Distribute App → App Store Connect → Upload
4. 暗号化: **Exempt**（`ITSAppUsesNonExemptEncryption = NO`）

## B. App Store Connect

### 日本語ローカライズ

- [ ] 言語 **日本語** を追加
- [ ] 名前: `Fragments（文学の断片）`
- [ ] サブタイトル: `文学の断片を、いまの気配で`
- [ ] キーワード / 概要（下の正本を貼る）
- [ ] スクショ: 「英語を使用」ではなく日本語用に独自アップロード
  - iPhone: `docs/ios/store-screenshots-ja-sim/`
  - iPad 13インチ: `docs/ios/store-screenshots-ja-ipad13/提出用-iPad13-2026-08-15/`

### 共通

- [ ] ASC で **バージョン 1.1.4** にビルドを紐づけ
- [ ] **アプリ情報 → プライマリ言語 → 日本語 → 保存**（可能なとき）
- [ ] プライバシー URL: `https://lagado.jp/fragments/privacy.php`
- [ ] EULA: Apple Standard で可
- [ ] IAP: `ticket100` / `ticket500` / `ticket1000` を **審査用に追加**
- [ ] 旧 `ticket10000` は販売停止
- [ ] 年齢・コンテンツ権利・輸出コンプライアンス

### 提出

- [ ] ビルドを選択 → **審査へ提出**

---

## 正本：日本語・概要（実装済みのみ · おみくじ文庫とは別アプリ）

**Fragments** = 世界の名著から抽出した文学断片（多言語・学習寄り）  
**おみくじ文庫** = 青空文庫からの日本語おみくじ体験（別プロダクト）

貼ってはいけない（未実装／別アプリ寄り）: フォルダ・しおり・続きを読む・デイリーレター・月次リフレクション・「文庫の鍵」

```
たとえ1日に100回引いても、すべてを読み終えるにはおよそ1,100年かかる——。
「Fragments」は、世界の名著およそ6万冊から文学の一節をランダムに届ける、手のひらの小さな文庫です。

収録する言葉の断片はおよそ4,300万。
スワイプひとつで、思いがけない一行が現れます。今日の気分に寄り添うこともあれば、未知の作品へ誘うこともあります。

【主な機能】

言葉を引く：およそ4,300万のフレーズとの偶然の出会い。

ストック：心に触れた言葉を残せます。

単語翻訳：単語をタップして意味を確認できます。

Webで調べる：著者名・作品名から本を検索できます。

キーワード検索：データベースから言葉を探せます。

AI解説：Geminiが意味や背景をやさしく解きほぐします（チケット制）。

いまの気配：天気や場所の気配に響く言葉を引きます（位置情報は任意。拒否しても通常利用可）。

【サポート・プライバシー】
https://lagado.jp/fragments/privacy.php

【利用規約（EULA）】
https://www.apple.com/legal/internet-services/itunes/dev/stdeula/
```

### サブタイトル
`文学の断片を、いまの気配で`

### 日本語・名前（ASC）
`Fragments（文学の断片）`

### キーワード
`文学,名言,読書,英語学習,AI,断片,古典,翻訳,教養,言葉`

### 英語・概要（English Description）

```
Even if you drew a hundred times a day, it would take about 1,100 years to read them all—
Fragments is a pocket library that delivers a random literary passage from roughly 60,000 of the world’s great books.

It holds about 43 million fragments of language.
One swipe brings an unexpected line—sometimes it meets the mood of the day; sometimes it leads you toward a book you’ve never known.

【Features】

Draw a quote: Chance encounters with about 43 million phrases.

Stock: Keep the lines that stay with you.

Word translate: Tap a word to check its meaning.

Search the web: Look up books from the author or title.

Keyword search: Find phrases in the database.

AI explanation: Gemini gently unpacks meaning and background (ticket-based).

Sense the moment: Draw lines that resonate with weather and place (location optional; declining still allows normal use).

【Support & Privacy】
https://lagado.jp/fragments/privacy.php

【EULA】
https://www.apple.com/legal/internet-services/itunes/dev/stdeula/
```

### What’s New（日本語）
```
設定の言語項目を「Language / 言語」に揃え、どの言語でも見つけやすくしました。
```

### What’s New（English）
```
Settings language row now always shows “Language / 言語” so it is easy to find in any UI language.
```

---

## Review Notes（Apple Staff）

```
App: Fragments (jp.lagado.pocketfortune)
Version: 1.1.4 (Build 12)

No login required. Sandbox IAP is available.

Consumable ticket IAPs (AI explanations):
- jp.lagado.literaryfragments.ticket100 (100 tickets)
- jp.lagado.literaryfragments.ticket500 (500 tickets)
- jp.lagado.literaryfragments.ticket1000 (1000 tickets)

How to test: Open Settings → Ticket Store → purchase a pack → return and open AI Explanation on a quote (uses 1 ticket). Daily free tickets may apply.

Japanese App Store localization with dedicated screenshots (not shared from English). In-app UI defaults to Japanese on ja devices.
This build improves the Settings language label (always “Language / 言語” with globe icon).

Privacy / Support: https://lagado.jp/fragments/privacy.php
Contact: info@lagado.jp

Location is optional (atmosphere). Declining still allows normal draw/stock/AI.
ITSAppUsesNonExemptEncryption = NO
```
