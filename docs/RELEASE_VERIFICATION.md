# Literary Fragments（PocketFortune）リリース検証記録

このドキュメントはリリース前検証プランに沿った **記録用テンプレート** と、CI/エージェントで実行できた **自動確認結果** をまとめたものです。手動項目は端末ごとに表を複製して記入してください。

---

## 1. 自動確認結果（ビルド）

| 項目 | 結果 | 備考 |
|------|------|------|
| Xcode スキーム `LiteraryFragments` / ターゲット `PocketFortune` | OK | `xcodebuild -list` で確認 |
| iOS **Debug** ビルド（`generic/platform=iOS`） | **BUILD SUCCEEDED** | `-derivedDataPath /tmp/PocketFortuneDerivedData`、`CODE_SIGNING_ALLOWED=NO` でコンパイル検証（2026-05-04） |
| iOS **Release** ビルド | **BUILD SUCCEEDED** | 同上 `/tmp/PocketFortuneDerivedDataRelease`（2026-05-04） |
| Android `./gradlew assembleRelease` | **未実行** | 検証環境に Java Runtime が無く Gradle が起動不可。JDK 17+ 導入後に `android/` で実行すること。 |

### 1.1 プロジェクトに基づく署名・識別子（Xcode 突合用）

| キー | 値（`project.pbxproj` / `xcodebuild` · 2026-07-28 突合） |
|------|------------------------------|
| `CODE_SIGN_STYLE` | Automatic |
| `DEVELOPMENT_TEAM` | `Q82QB32AZ8` |
| `PRODUCT_BUNDLE_IDENTIFIER`（iphoneos） | `jp.lagado.pocketfortune` |
| `MARKETING_VERSION` | **1.0.8** |
| `CURRENT_PROJECT_VERSION` | **9** |
| `IPHONEOS_DEPLOYMENT_TARGET` | **26.4**（実機・TestFlight はこの OS 以上が必要） |
| `ITSAppUsesNonExemptEncryption` | **NO** |
| SDK | `iphoneos26.5`（Xcode 26.6 / 17F113） |
| 表示名 | `Fragments`（`INFOPLIST_KEY_CFBundleDisplayName`） |
| App Store | ID `6760742804`（Pocket Fortune: Literary） |
| 正本メモ | [`ios/STATUS.md`](ios/STATUS.md) |

実機 / TestFlight では Xcode で **チーム** と **Signing Certificate** が有効か、App Store Connect の Bundle ID と一致するかを必ず確認してください。

---

## 2. 手動チェックリスト（権限・OS）

| # | シナリオ | 期待 | Pass/Fail | 備考 |
|---|----------|------|-------------|------|
| 2.1 | 位置情報 **初回許可** | 雰囲気／天気連動が利用可能 | | |
| 2.2 | 位置情報 **拒否** | クラッシュなし。モードに応じたフォールバックが分かる | | |
| 2.3 | 許可後、設定アプリで位置 **オフ** | 同上 | | |
| 2.4 | **Translation**（単語タップ→システム翻訳） | シート表示、閉じた後 Web が正常 | | 最低 OS はデプロイメントターゲットと合わせて確認 |
| 2.5 | **TTS**（読み上げ） | サイレント ON/OFF、他アプリ再生中、Bluetooth で破綻しない | | |

---

## 3. 手動チェックリスト（ネットワーク・API）

| # | シナリオ | 期待 | Pass/Fail | 備考 |
|---|----------|------|-------------|------|
| 3.1 | **機内モード**で起動〜次の言葉 | クラッシュ・無限ローディングなし | | |
| 3.2 | 機中で検索 | エラー表示または安全な空結果 | | |
| 3.3 | 機中で AI 解説 | チケット消費と失敗の整合（失敗のみで消費済みにならないか） | | `gemini.php` POST |
| 3.4 | オンライン復帰後の再試行 | 復帰できる | | |

依存 URL（実装参照）: `https://lagado.jp/fragments/api.php`, `https://lagado.jp/fragments/gemini.php`, 天気 `https://api.open-meteo.com/`, 書籍検索で Google を開く。

---

## 4. 手動チェックリスト（チケット・日次リセット）

| # | シナリオ | 期待 | Pass/Fail | 備考 |
|---|----------|------|-------------|------|
| 4.1 | 新規相当（UserDefaults 初期） | 無料チケット 3（`freeTickets`） | | |
| 4.2 | 解説でチケット消費 | 無料優先、その後有料（`consumeTicket`） | | |
| 4.3 | チケット 0 で解説 | `outOfTickets` 系の案内 | | |
| 4.4 | 既にキャッシュされた解説 | 確認なしで解説画面へ（`isAlreadyExplained`） | | |
| 4.5 | **日付変更**（翌日） | 無料 3 にリセット（`lastFreeAITicketDate`） | | 実機で日跨ぎ or 日付偽装 |

---

## 5. App 内課金（StoreKit 2）— Sandbox 手順と方針

### 5.1 リポジトリ方針（`.storekit`）

このプロジェクト **ルートには `.storekit` 構成ファイルがありません**。次のいずれかで検証します。

- **推奨（リリース相当）**: 実機 + **Sandbox の Apple ID**（App Store Connect のサンドボックステスター）。App Store Connect に登録済みの消費型プロダクトと **同じプロダクト ID** が取得できること。
- **任意（ローカル）**: Xcode で **File → New → File → StoreKit Configuration File** を追加し、スキームの Run → Options でそのファイルを紐付け。オフラインで購入 UI と `Product.products(for:)` を試せるが、**ASC の ID と一致させる**必要がある。

### 5.2 コード上のプロダクト ID（`IAPProduct`）

| プロダクト ID | チケット枚数 | 目標価格 |
|----------------|-------------|---------|
| `jp.lagado.literaryfragments.ticket100` | 100 | **¥300**（¥250は価格帯に無し） |
| `jp.lagado.literaryfragments.ticket500` | 500 | **¥900**（新規 ID） |
| `jp.lagado.literaryfragments.ticket1000` | 1000 | **¥1,500** |

> 旧 `ticket10000` はコードから削除。App Store Connect / Play Console で販売停止し、`ticket500` を新規作成すること。

**Bundle ID**（アプリ）は `jp.lagado.pocketfortune` だが、IAP ID は上記の `literaryfragments` プレフィックス。App Store Connect 側のプロダクト ID がコードと **完全一致**しているか必ず確認すること。

### 5.3 Sandbox 検証チェックリスト（実機）

| # | 手順 | 期待 | Pass/Fail | 備考 |
|---|------|------|-------------|------|
| S.1 | 設定で Sandbox アカウントでサインイン | | | |
| S.2 | アプリ起動 → チケットストアで商品読込 | `StoreManager.fetchProducts` 成功、価格表示 | | 失敗時は Xcode コンソールの `商品の取得に失敗` を確認 |
| S.3 | いずれか 1 商品を購入 | 購入完了後、`paidTickets` が `ticketAmount` 分増える | | `grantTickets` → `transaction.finish()` |
| S.4 | 別端末または再インストール後 | `Transaction.updates` のみでは過去購入の復元は保証されない。**復元 UI が無い場合**は「再購入不可の消費型」としての仕様を審査用に説明できるか検討 | | 必要なら `Transaction.currentEntitlements` / `AppStore.sync()` を使った復元を別タスクで設計 |
| S.5 | 購入 **Pending**（家族共有承認待ち等） | UI が固まらない（`case .pending` で break） | | |

---

## 6. 手動チェックリスト（WebView ↔ ネイティブ）

| # | 領域 | 確認ポイント | Pass/Fail |
|---|------|----------------|-----------|
| 6.1 | スワイプ次 | `requestNextQuote`、ルーレット中の連打 | |
| 6.2 | スワイプ前 | 履歴先端でハプティクのみ等 | |
| 6.3 | お気に入り | 追加・削除・再起動後の保持 | |
| 6.4 | 検索 | `searchAPI` とキーワードハイライト | |
| 6.5 | 単語翻訳 | `showNativeTranslation` | |
| 6.6 | 外部リンク | Google 検索、プライバシーページ | |
| 6.7 | テーマ | ライト/ダーク切替後の Web 表示 | |

---

## 7. Android リリースビルドとスモーク（JDK がある環境で実施）

```bash
cd android
./gradlew assembleRelease
```

出力 APK の場所は通常 `android/app/build/outputs/apk/release/`。

### 7.1 スモーク（iOS と同等フロー）

| # | 画面 / フロー | 確認 | Pass/Fail |
|---|----------------|------|-----------|
| A.1 | `MainActivity` | 言葉表示・スワイプ相当の操作 | |
| A.2 | `SettingsActivity` | テーマ等の保存 | |
| A.3 | `TicketStoreActivity` | 行タップで `remainingTickets` が増える（**実装はシミュレート購入**。本番課金は別途 Google Play Billing が要る場合あり） | |
| A.4 | `FavoritesActivity` | 一覧・整合 | |
| A.5 | `ExplanationActivity` | 解説フロー | |

---

## 8. 端末別記録テンプレート

| 端末名 | OS バージョン | ビルド種別（Debug/Release/TestFlight） | ビルド番号 / Git | 検証日 | 担当 | 総合結果 | メモ |
|--------|----------------|----------------------------------------|------------------|--------|------|----------|------|
| | | | | | | Pass / Fail | |

---

## 9. 既知の制限（検証記録にそのまま転記可）

- オフライン時の挙動は API 依存。
- iOS デプロイメントターゲット **26.4** のため、それ未満の端末ではインストール不可。
- IAP は Sandbox での検証記録を本番と区別して記載すること。
- Android Release ビルドは本記録作成時点の自動実行環境では **JDK 未導入のため未実施**。
