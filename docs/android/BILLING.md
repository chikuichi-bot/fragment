# Literary Fragments Android — Play Billing（D）

製品 ID は **iOS と同一**（消耗型）· 余裕多め価格帯（2026-07-29）:

| Product ID | 枚数 | 設定価格（目標） | Debug 仮価格 |
|------------|------|------------------|--------------|
| `jp.lagado.literaryfragments.ticket100` | 100 | **¥300** | ¥300 |
| `jp.lagado.literaryfragments.ticket500` | 500 | **¥900** | ¥900 |
| `jp.lagado.literaryfragments.ticket1000` | 1000 | **¥1,500** | ¥1,500 |

**廃止（コードから外した）:** `…ticket10000`（旧 10000枚）  
**ASC:** `ticket500` 作成済（2026-07-30）· 価格は Console で **¥300 / ¥900 / ¥1,500** に設定すること。  
既存 `ticket100` / `ticket1000` は **価格だけ** 更新。Play も同 ID を揃える。

本番価格は Play の `displayPrice` / StoreKit の `displayPrice` をそのまま表示（未取得時は目安 ¥300 / ¥900 / ¥1,500）。  
依存: **Play Billing Library `8.3.0`**  
コード: `TicketLicenseManager.kt` · UI: `TicketStoreActivity.kt` · iOS: `IAPProduct` / `CatalogTicketPackRow` in `ContentView.swift` · Android **1.0.4 (5)**

型の参考（別アプリ）: `OmikujiBunko_Master/AbomonGame/docs/android/BILLING.md`

---

## D1 · Play Console（手動・淡島様）

1. [Play Console](https://play.google.com/console) → アプリ作成 **Literary Fragments** · `jp.lagado.literaryfragments`
2. 初回は署名 APK/AAB を上げてから課金を有効化（Console の案内に従う）
3. **収益化 → 1回限りのアイテム** → 上記 3 ID を **消耗型** で登録 → **有効**
4. ライセンステスター追加（設定 → ライセンス試験）

製品が Ready になるまで、Debug ビルドは **製品未取得時に開発用付与**（iOS `#if DEBUG` と同型）します。  
Release / 提出ビルドでは本物の Billing のみ。

### Play Console 用プライバシー URL

- Android: `https://lagado.jp/fragments/privacy-android.php`（正本: `FragmentsApp/deploy/privacy-android.php`）
- iOS / 共通: `https://lagado.jp/fragments/privacy.php`（正本: `FragmentsApp/deploy/privacy.php`）
- 旧 URL `…/privacy/` は `privacy/index.html` から新版へリダイレクト
- **お問い合わせメール（2026-07-29 · アボモン／そばメモ同型）:** `From`/`-f` は `info@lagado.jp`、利用者は `Reply-To` のみ。控えは `fragments/_inbox/inquiries.log`。ロリポップへ両 PHP をアップロードすること。

## D2 · 実装（コード）

- [x] Billing Library 8.3.0 + `BILLING` 権限
- [x] `TicketLicenseManager`（purchaseToken 台帳 · acknowledge → consume）
- [x] 無料日次枠（`freeTickets`）と有料（`remainingTickets`）分離
- [x] Debug 時のみ開発付与（Release は本物のみ）
- [x] シミュレート即加算を撤去

## D3 · 台帳

- SharedPreferences `fragments_ticket_license` · key `purchaseLedger.v1`
- 同一 `purchaseToken` は再加算しない
- 枚数本体は `PocketFortunePrefs.remainingTickets`

## D4 · テスト

### A · Debug（開発付与）

USB `installDebug`。価格は仮。タップで枚数加算（Play シートなし）。

### B · 本物の Play 課金

1. Console で製品 Ready + 内部／クローズドに署名 AAB
2. 実機の Debug 版をアンインストール → Play テスト版を入れる
3. 購入・キャンセル・再起動後も枚数が残ること

> **D4 済** · 2026-07-26 · SH-03K · Alpha 1.0.1 でチケット購入成功

---

## ビルド

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd FragmentsApp/android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
