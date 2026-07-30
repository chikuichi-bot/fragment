# Literary Fragments Android — 表示 parity（C4）

> iOS（`PocketFortune/ContentView.swift`）との見た目・殻の差分メモ。  
> 更新: 2026-07-30 · iOS Xcode **1.0.6 / Build 7** · Android **1.0.4 / code 5**

## テーマ

| | iOS | Android |
|--|-----|---------|
| 設定 | システム / ライト / ダーク（`themePreference` 0/1/2） | 同キー・同3択（設定画面セグメント） |
| 適用 | `overrideUserInterfaceStyle` | `AppCompatDelegate` + WebView `data-theme` |
| 背景ライト | `systemGroupedBackground` 系 | `#F8F9FA` / 設定 `#F2F2F7` |
| 背景ダーク | システムダーク | `#121212` · カード `#1E1E1E` |
| アクセント | オレンジ中心（オンボード等） | 設定「完了」は iOS 風 `#007AFF` |

**差分:** Android メインは WebView（`assets/`）、設定以降はネイティブ View。色は近いが Material の完全一致ではない。

## ノッチ / セーフエリア

| | iOS | Android |
|--|-----|---------|
| メイン | SwiftUI `safeArea` | WebView 全画面 · CSS で余白 |
| サブ画面 | sheet / safeArea | ヘッダ `padding` 固定（例: favorites 上 `64dp`） |
| edge-to-edge | 標準 | **未導入**（`enableEdgeToEdge` なし） |

**SH-03K:** ノッチ無し端末。パンチホール機ではヘッダが status bar に寄る可能性あり → 本番前に F1 でもう1台確認。

## 文言（主要な殻）

| 箇所 | iOS | Android |
|------|-----|---------|
| 設定タイトル | ローカライズ UI | 固定「設定」（日本語） |
| 気配 | 「今の気配を読み取る」系 | 同趣旨（`textSense`） |
| チケット | StoreKit 表示価格 | Play `displayPrice` |
| クレジット | Lagado Research Institute | 同方針（HANDOFF） |

**差分:** iOS は Gemini 経由の多言語 UI が厚い。Android は日本語ハードコードが残る箇所あり（設定ヘッダ等）。英語 UI 完全parityは後続でも可。

## 戻る・閉じる・スワイプ（C3 / iOS 2026-07-27）

| 操作 | 実装 |
|------|------|
| システム戻る | `OnBackPressedCallback` → `closeScreen()` / `finish` |
| 上端スワイプ閉じ | Settings / Favorites / Explanation / TicketStore · 上端 120dp 内から下へ 100dp 超 |
| リスト左スワイプ | Favorites のみ削除（`ItemTouchHelper`） |
| **メイン左右スワイプ** | **どちらも新規ランダム**（履歴めくりなし · iOS 同型 · 2026-07-27） |

## プライバシー

| | iOS | Android |
|--|-----|---------|
| 設定リンク | `…/privacy.php` | `…/privacy-android.php`（設定末尾 · 2026-07-27） |

## 意図的に揃えないこと

- Abomon の四角枠ハイライト強制は持ち込まない
- メインの WebView 殻は iOS SwiftUI と構造が違う（体験の核は共通）
