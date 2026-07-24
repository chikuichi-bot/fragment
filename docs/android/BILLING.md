# Literary Fragments Android — Play Billing（D）

製品 ID は **iOS と同一**（消耗型）:

| Product ID | 枚数 | Debug 仮価格 |
|------------|------|--------------|
| `jp.lagado.literaryfragments.ticket100` | 100 | ¥150 |
| `jp.lagado.literaryfragments.ticket1000` | 1000 | ¥900 |
| `jp.lagado.literaryfragments.ticket10000` | 10000 | ¥4,500 |

本番価格は Play の `displayPrice` をそのまま表示（Abomon 同方針）。  
依存: **Play Billing Library `8.3.0`**  
コード: `TicketLicenseManager.kt` · UI: `TicketStoreActivity.kt`

型の参考（別アプリ）: `OmikujiBunko_Master/AbomonGame/docs/android/BILLING.md`

---

## D1 · Play Console（手動・淡島様）

1. [Play Console](https://play.google.com/console) → アプリ作成 **Literary Fragments** · `jp.lagado.literaryfragments`
2. 初回は署名 APK/AAB を上げてから課金を有効化（Console の案内に従う）
3. **収益化 → 1回限りのアイテム** → 上記 3 ID を **消耗型** で登録 → **有効**
4. ライセンステスター追加（設定 → ライセンス試験）

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

---

## ビルド

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd FragmentsApp/android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
