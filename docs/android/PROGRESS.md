# Literary Fragments Android — 進捗ボード

> **完了 80% · 残り 20%**  
> 次: **F** QA・ストア提出（F1 もう1台 · F2 残り · F3 本番）  
> 更新: 2026-07-30  
> 前提: iOS は Xcode **1.0.6 / Build 7**（IAP 100/500/1000）· Android は Kotlin **1.0.4 / code 5** · API は `lagado.jp/fragments/` 共通 · 課金は StoreKit / Play Billing 8.3.0（Debug は開発付与）

```
Fragments Android · 完了 80% · 残り 20%
いま: A/B/C/D 完了 · 次: F 提出
```

### Play Console（枠）

- [x] アプリ作成 **Literary Fragments** · `jp.lagado.literaryfragments`
- [x] 課金商品 D1（ticket100 / 500 / 1000 · 余裕多め ¥300 / ¥900 / ¥1,500）· Alpha **2 (1.0.1)** クローズド公開済 · 本番公開は **F** · ASC `ticket500` 作成済（2026-07-30）· 価格設定・Play 登録が残作業

### ビルドメモ

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd FragmentsApp/android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
# 実機: adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### iOS との対応

| | iOS | Android |
|--|-----|---------|
| Bundle / ID | `jp.lagado.pocketfortune`（表示 Fragments） | `jp.lagado.literaryfragments` |
| 版 | **1.0.6** / Build **7**（Xcode · 2026-07-30） | **1.0.4** / versionCode **5** |
| チケット IAP | StoreKit · `jp.lagado.literaryfragments.ticket*` | Billing 8.3.0 · 同 ID（D1–D4 済） |
| 気配・場所 | `PlaceLiteraryLexicon` + GPS | 同型（コード済） |
| 左右スワイプ | どちらも新規ランダム（履歴めくりなし） | 同型 |
| API | `api.php` / `gemini.php` | 同じ URL |
| プライバシー | `…/privacy.php` | `…/privacy-android.php` |

---

## 重み（合計 100）

| フェーズ | 重み | 取得 | 状態 |
|----------|------|------|------|
| A 殻・起動 | 12 | 12 | ビルド＋実機＋コア1周 済 |
| B コア体験 | 28 | 28 | コード完了 · 実機確認済 |
| C 仕上げ・気配 | 15 | 15 | C1–C5 済 · DISPLAY.md |
| D Play Billing | 25 | 25 | D1–D4 済 · クローズド実機課金 OK |
| F QA·提出 | 20 | 0 | 素材・プライバシー着手 · 本番は未 |
| **合計** | **100** | **80** | |

---

## A · 殻・起動（12）

- [x] プロジェクト `FragmentsApp/android/`（Activities 揃い）（3）· Main / Settings / Favorites / Explanation / TicketStore
- [x] **A1** JDK（Studio JBR）+ `./gradlew assembleDebug` 成功（3）· 2026-07-24 · `local.properties` 要（gitignore）
- [x] **A2** 実機 install（SH-03K）· 起動〜言葉表示（3）· 2026-07-24 · 断片＋著者タイトル表示確認
- [x] **A3** コア 1 周（引く → 検索 → 気配 → 解説）（3）· 2026-07-24 · 実機OK

> 取得: **12 / 12**

## B · コア体験（28）

- [x] **B1** ランダム「言葉を引く」（5）· `MainActivity` → `api.php?action=random`
- [x] **B2** 検索（scope 付き）（4）· `SettingsActivity`
- [x] **B3** お気に入り・履歴（4）· `FavoritesActivity` / `DataManager`
- [x] **B4** AI 解説・チケット消費（5）· `ExplanationActivity` → `gemini.php`
- [x] **B5** チケット UI・日次無料枠（4）· `TicketStoreActivity`
- [x] **B6** 気配・場所優先キーワード（6）· `PlaceLiteraryLexicon` + GPS / 京都フォールバック · 2026-07-14

> 取得: **28 / 28**（コード）· 実機確認は A2–A3

## C · 仕上げ・気配（15）

- [x] **C1** 位置情報権限 + 実 GPS（気配）（5）· 京都固定を解除済み · 2026-07-14
- [x] **C2** 場所レキシコン（国 ISO → 文学語）（3）· iOS 同型
- [x] **C3** 戻るキー / スワイプ閉じの端末差確認（3）· 2026-07-26 · SH-03K · BACK: TicketStore→Settings→Main · 4画面に上端スワイプ閉じ実装済
- [x] **C4** テーマ・ノッチ・文言の iOS parity メモ 1 枚（2）· [`DISPLAY.md`](DISPLAY.md) · 2026-07-26
- [x] **C5** サーバ `api.php` 気配（場所スコア）本番反映確認（2）· 2026-07-26 · `action=atmosphere&keywords=Kyoto` → 21/21 ヒット（title/author 含む）

> 取得: **15 / 15**

## D · Play Billing（25）

- [x] **D1** Play Console アプリ枠 + 製品（ticket100 / 500 / 1000 · iOS ID に合わせる）（5）· 2026-07-26 · Alpha **2 (1.0.1)** 公開 · 2026-07-29 構成を 100/500/1000・余裕多め価格に更新（`ticket500` 新規）
- [x] **D2** Billing Library（8.3.0）+ 購入フロー（シミュ撤去）（10）· `TicketLicenseManager` · 2026-07-24
- [x] **D3** 台帳冪等（purchaseToken）· 無料日次と有料の分離（5）· 2026-07-24
- [x] **D4** クローズドテスト実機課金（5）· 2026-07-26 · SH-03K · Play 版で購入成功

> 取得: **25 / 25**  
> 詳細: [`BILLING.md`](BILLING.md)

## F · QA・提出（20）

- [ ] **F1** 端末ざっくり（SH-03K + もう1台）（5）
- [ ] **F2** ストア素材・データセーフティ・プライバシー URL（7）· 正本 `deploy/privacy-android.php` · URL `https://lagado.jp/fragments/privacy-android.php`（反映済想定 · Console 項目は一部済）
- [ ] **F3** 内部／クローズド → 本番公開（8）· Alpha クローズドは済 · 本番トラックは未

> 取得: **0 / 20**

---

## 毎回の更新手順

1. 完了した `- [ ]` を `- [x]` に
2. フェーズの「取得」と冒頭の **完了 % / 残り %** を再計算
3. Obsidian `メモリ/プロジェクト/Literary Fragments.md` に 1 行

## 関連

- [HANDOFF.md](HANDOFF.md)
- [BILLING.md](BILLING.md)
- [DISPLAY.md](DISPLAY.md)
- [RELEASE_VERIFICATION.md](../RELEASE_VERIFICATION.md)
- `FragmentsApp/android/`
- `FragmentsApp/deploy/api.php`（ロリポップ反映）
- Abomon 型: `OmikujiBunko_Master/AbomonGame/docs/android/PROGRESS.md`
- プレイブック: `OmikujiBunko_Master/docs/playbooks/Abomon-V1-次のアプリへ.md`
