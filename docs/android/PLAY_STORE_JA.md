# Google Play — 日本語ストア掲載（貼り付け用）

> Play Console → アプリ → **ストアの掲載情報** → デフォルト言語／日本語。  
> 更新: 2026-08-05 · 版 **1.0.5**  
> iOS 正本: [`../ios/APP_STORE_JA.md`](../ios/APP_STORE_JA.md)

## アプリ名 · 短い説明

| 項目 | 文言 | 上限目安 |
|------|------|----------|
| アプリ名 | `Fragments` または `Literary Fragments` | 30 |
| 短い説明 | `文学の断片を、いまの気配で` | 80 |

## 詳しい説明

```
Fragmentsは、世界の名著から抽出した言葉の断片を、ランダムに、あるいは「いまの気配」に合わせて届けるアプリです。

・スワイプして新しい言葉を引く
・お気に入りに保存する
・単語タップで翻訳
・著者・作品名から本を調べる
・AI解説で語彙・ニュアンス・背景を深く味わう（チケット制）

位置情報は「気配」モードでのみ使い、拒否しても通常利用できます。広告やトラッキングSDKはありません。

サポート・プライバシー:
https://lagado.jp/fragments/privacy-android.php
```

## 新機能（このリリースの内容）例

```
日本語環境では初回から日本語UIになります。チケットは 100 / 500 / 1000 枚、AI解説レベル表記を整理しました。
```

## アプリ内の日本語対応（コード · 2026-08-05）

- 端末言語が日本語なら `nativeLanguage` を初回「日本語」にする（`LanguagePrefs` · iOS 同型）
- 設定の AI レベル: 中学 / 高校 / 大学 / ビジネス（英語併記なし）
- 気配の場所表示は日本語UI時に日本語表記を優先

## 課金（Play）

| Product ID | 枚数 | 価格 |
|------------|------|------|
| `jp.lagado.literaryfragments.ticket100` | 100 | ¥300 |
| `jp.lagado.literaryfragments.ticket500` | 500 | ¥900 |
| `jp.lagado.literaryfragments.ticket1000` | 1000 | ¥1,500 |

## 関連

- [`BILLING.md`](BILLING.md) · [`PROGRESS.md`](PROGRESS.md) · [`HANDOFF.md`](HANDOFF.md)
- iOS: [`../ios/APP_STORE_JA.md`](../ios/APP_STORE_JA.md) · [`../ios/STATUS.md`](../ios/STATUS.md)
