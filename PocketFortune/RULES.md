# プロジェクト概要：LiteraryFragments (おみくじ文庫)

本プロジェクトは、SwiftUIのネイティブアプリ内に `WKWebView` を組み込み、HTML/JS/CSSと連携して動作するハイブリッド型のiOSアプリである。

## 1. UIパーツとHTML要素のID規則
画面の見た目は `index.html` と `style.css` で管理されている。JavaScriptやSwiftから操作するため、以下のIDが厳密に定義されている。
* **名言表示エリア:** `quote-container` (全体), `quote-text` (名言本文), `source-area` (作品・著者エリア), `swipe-guide` (スワイプ案内)。
* **下部ナビゲーションボタン:** `btn-settings` (設定), `btn-bag` (お気に入り), `btn-help` (AI解説), `btn-star` (ストック), `btn-book` (音声読み上げ)。

## 2. JavaScriptからSwiftへの通信 (JS -> Swift)
JSからSwiftへ命令を送る際は、共通関数 `callSwift(handlerName, body)` を使用し、`window.webkit.messageHandlers[handlerName].postMessage` を経由する。
Swift側の `ContentView.swift` では、以下の11個のハンドラ名を受信できるように登録されている。
* `speakText`, `triggerHaptic`, `showSettings`, `showFavorites`, `explainQuote`, `stockQuote`, `unstockQuote`, `requestNextQuote`, `requestPreviousQuote`, `searchBook`, `showNativeTranslation`。

## 3. SwiftからJavaScriptへの通信 (Swift -> JS)
Swift側でAPIから取得した名言データやテーマの変更は、`evaluateJavaScript` を用いて直接JSを実行することでHTML側に反映させる。
* **名言の描画:** JS側のグローバル関数 `window.displayQuoteWithFade(text, title, author)` および `window.setSearchKeyword(keyword)` を呼び出して画面を更新する。
* **お気に入りアイコンの更新:** Swift側から `btn-star` 内のSVG要素に対し、直接CSSクラス (`stocked`) や色 (`#ff9500`) を付与・削除するJSコードを注入する。
* **音声読み上げアイコンの更新:** 再生状態に応じて、`btn-book` の `innerHTML` を `onSVG` または `offSVG` に書き換え、スケールと色を直接変更するJSを注入する。

## 4. 外観（テーマ）の連携仕様
アプリのライト/ダークモードはSwiftUIの `@AppStorage("themePreference")` (0:システム, 1:ライト, 2:ダーク) で管理される。
* Swift側でテーマが変更されると、`document.documentElement.setAttribute('data-theme', 'dark')` などのJavaScriptコードがWebViewに送付される。
* `style.css` 側では `:root` と `html[data-theme="dark"]` を用いて `--text-main` や `--glass-bg` などのCSS変数を切り替え、アプリ全体の色調を統一している。

## 5. バックエンド・データ管理仕様
Swift側で各種データを一元管理している。
* **名言の取得:** `QuoteDatabase` クラスが `https://lagado.jp/fragments/api.php` と通信し、通常モード（ランダム）、検索モード、気配（Atmosphere）モードのデータを管理する。
* **お気に入り・履歴:** `QuoteStorage` クラスが `UserDefaults` を用いて端末内に保存する。
* **多言語対応とAI解説:** `LanguageManager` や `ExplanationView` では、サーバー経由で Gemini API (`gemini-3.1-flash-lite`) を呼び出して翻訳やAI解説を生成している。