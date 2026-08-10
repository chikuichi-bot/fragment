# App Store 提出チェックリスト — Fragments iOS

> 版: **1.0.8** / Build **9**  
> 更新: 2026-08-06  
> 注: **1.0.7 は承認済み／train 閉鎖**。新規は 1.0.8 必須。

## A. Xcode（淡島様）

1. 未保存の変更を保存（日本語デフォルトUI・スクショ用起動など）
2. スキーム `LiteraryFragments` · Destination **Any iOS Device**
3. **Product → Archive** → Distribute App → App Store Connect → Upload
4. 暗号化: **Exempt**（`ITSAppUsesNonExemptEncryption = NO`）

## B. App Store Connect

### 日本語ローカライズ

- [ ] 言語 **日本語** を追加
- [ ] 名前 / サブタイトル / キーワード / 概要（下の正本を貼る）
- [ ] スクショ 6.7インチ: `docs/ios/store-screenshots-ja-sim/`（01→07 · **1290×2796**）
- [ ] 「英語を使用」ではなく日本語用メディアにする

### 英語（既存）も概要が古ければ更新

### 共通

- [ ] ASC で **新しいバージョン 1.0.8** を作成してからビルドを紐づけ
- [ ] プライバシー URL: `https://lagado.jp/fragments/privacy.php`
- [ ] EULA: Apple Standard で可
- [ ] IAP: `ticket100` / `ticket500` / `ticket1000` を **審査用に追加**（価格・スクショ済）
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
日本語表示を強化しました。チケットは100 / 500 / 1000枚に整理し、価格を見直しています。左右スワイプはどちらも新しい言葉を引きます。プライバシーとサポート連絡先も更新しました。
```

### What’s New（English）
```
Improved Japanese UI. Ticket packs are now 100 / 500 / 1000 with updated pricing. Swiping either direction draws a new quote. Privacy policy and support contact details were updated.
```

---

## Review Notes（Apple Staff）

```
App: Fragments (jp.lagado.pocketfortune)
Version: 1.0.8 (Build 9)

No login required. Sandbox IAP is available.

Consumable ticket IAPs (AI explanations):
- jp.lagado.literaryfragments.ticket100 (100 tickets)
- jp.lagado.literaryfragments.ticket500 (500 tickets) — new
- jp.lagado.literaryfragments.ticket1000 (1000 tickets)

How to test: Open Settings → Ticket Store → purchase a pack → return and open AI Explanation on a quote (uses 1 ticket). Daily free tickets may apply.

Japanese App Store localization and screenshots included. In-app UI defaults to Japanese on ja devices.

Privacy / Support: https://lagado.jp/fragments/privacy.php
Contact: info@lagado.jp

Location is optional (atmosphere). Declining still allows normal draw/stock/AI.
ITSAppUsesNonExemptEncryption = NO
```
