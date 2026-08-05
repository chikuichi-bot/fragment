# App Store 提出チェックリスト — Fragments iOS

> 版: **1.0.7** / Build **8**（未コミットの日本語UIあり → 提出前に Archive するバイナリに含める）  
> 更新: 2026-08-05  
> 注: **1.0.6 は承認済み**のため train 閉鎖。新規は 1.0.7 必須。

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

- [ ] ASC で **新しいバージョン 1.0.7** を作成してからビルドを紐づけ
- [ ] プライバシー URL: `https://lagado.jp/fragments/privacy.php`
- [ ] EULA: Apple Standard で可
- [ ] IAP: `ticket100` / `ticket500` / `ticket1000` を **審査用に追加**（価格・スクショ済）
- [ ] 旧 `ticket10000` は販売停止
- [ ] 年齢・コンテンツ権利・輸出コンプライアンス

### 提出

- [ ] ビルドを選択 → **審査へ提出**

---

## 正本：日本語・概要（審査に合わせて実装済み機能のみ）

※長い英語原稿のうち、未実装（デイリーレター・月次リフレクション・フォルダ・全文閲覧など）は入れていません。審査で弾かれやすいです。

```
たとえ1日に100回引いても、すべてを読み終えるにはおよそ1,100年かかる——。
「Fragments」は、世界の名著6万冊以上から文学の一節をランダムに引ける、手のひらの小さな文庫です。

収録する言葉の断片はおよそ4,300万。
この圧倒的な物語の海から、思いがけない言葉があなたのもとに現れます。
何気なく引いた一行が、今日の気分にそっと寄り添うこともあれば、未知の世界へ連れ去ることもあります。

おみくじを引くような気軽さで、時代も国境も超えた美しい言葉の領域を味わってください。

【主な機能】

言葉を引く：およそ4,300万のフレーズとの偶然の出会いを楽しめます。

ストック：心に触れた言葉を大切に残せます。

単語翻訳：単語をタップして意味を確認できます。

Webで調べる：著者名・作品名から本を検索できます。

AI解説：Geminiが言葉の意味や背景をやさしく解きほぐします（チケット制）。

いまの気配：天気や場所の気配に響く言葉を引きます（位置情報は任意）。

【サポート・プライバシー】
https://lagado.jp/fragments/privacy.php

【利用規約（EULA）】
https://www.apple.com/legal/internet-services/itunes/dev/stdeula/
```

### サブタイトル
`文学の断片を、いまの気配で`

### キーワード
`文学,名言,読書,英語学習,AI,断片,古典,翻訳,教養,言葉`

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
Version: 1.0.7 (Build 8)

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
