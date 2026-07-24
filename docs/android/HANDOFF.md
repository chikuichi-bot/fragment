# Literary Fragments（FragmentsApp）Android — 新スレッド引き継ぎ仕様

> **誰向け:** 新 Cursor チャットで Android 版を進める AI / 淡島様  
> **更新:** 2026-07-24  
> **制作:** Lagado Research Institute  
> **進捗正本:** 同フォルダの [`PROGRESS.md`](PROGRESS.md)（Abomon 同型の％ボード）

---

## 0. 新スレッドの最初に貼る一文（コピペ用）

```
Literary Fragments（FragmentsApp）の Android 版を続ける。
ゼロから作らない。正本は FragmentsApp/docs/android/PROGRESS.md（いま完了42%）。
次は A1 assembleDebug → A2 実機 → D Play Billing。
引き継ぎ仕様: FragmentsApp/docs/android/HANDOFF.md を先に読む。
Abomon の Android 課金は型の参考のみ（別アプリ・別 ID）。
```

---

## 1. これは何のアプリか（混同禁止）

| | Literary Fragments | Abomon Game | おみくじ文庫 |
|--|-------------------|-------------|--------------|
| ローカル | `…/FragmentsApp/` | `OmikujiBunko_Master/AbomonGame/` | `OmikujiBunko_Master/server` + `app/` |
| GitHub | `chikuichi-bot/fragment` | `chikuichi-bot/OmikujiBunko`（モノレポ内） | 同上 |
| 体験 | 文学断片をランダム／検索／「今の気配」で引く · AI 解説チケット | 占いゲーム · シール · UFO | Web／Capacitor |
| Android | **Kotlin ネイティブ**（WebView 殻ではない） | WebView + Kotlin ブリッジ | Capacitor |

**禁止:** Abomon の `sync_www_to_android.sh` や www 正本運用を Fragments に持ち込むこと。  
**可:** Abomon の **Play Billing 台帳・冪等・クローズドテスト手順**を型として移植すること。

---

## 2. いまの到達点（事実）

```
Fragments Android · 完了 66% · 残り 34%
いま: A 完了 · D2/D3 済 · 次: D1 Console → D4
```

| フェーズ | 重み | 取得 | 意味 |
|----------|------|------|------|
| A 殻・起動 | 12 | 12 | ビルド＋SH-03K＋コア1周 済 |
| B コア体験 | 28 | 28 | 引く／検索／お気に入り／解説／気配の**コード済** |
| C 仕上げ・気配 | 15 | 11 | 場所優先 GPS＋レキシコン済 · 細部残り |
| D Play Billing | 25 | 15 | Billing 8.3.0 + 台帳済 · **Console / クローズド未** |
| F QA·提出 | 20 | 0 | Play 未公開想定 |

詳細チェックリストは必ず `PROGRESS.md` を更新しながら進める。

---

## 3. パス・識別子（正本）

### ローカル

```
/Users/awaashima/Desktop/デスクトップ - awashimaのMacBook - 1/FragmentsApp/
├── PocketFortune/                 # iOS Swift 正本（UI・StoreKit）
├── PocketFortune.xcodeproj/
├── android/                       # Android Kotlin 正本
│   └── app/src/main/java/jp/lagado/literaryfragments/
│       MainActivity.kt
│       SettingsActivity.kt        # 検索・気配・設定
│       PlaceLiteraryLexicon.kt    # 国→文学キーワード
│       FavoritesActivity.kt
│       ExplanationActivity.kt
│       TicketStoreActivity.kt     # ★いまシミュレート購入
│       DataManager.kt
├── deploy/
│   ├── api.php                    # 気配・場所スコア版（ロリポップへ上げる）
│   ├── gemini.php                 # キー無しテンプレ可
│   └── gemini_secrets.php*        # 本番キー · コミット禁止
└── docs/android/
    ├── PROGRESS.md                # ％ボード
    └── HANDOFF.md                 # 本ファイル
```

### アプリ ID

| 項目 | 値 |
|------|-----|
| Android `applicationId` | `jp.lagado.literaryfragments` |
| iOS Bundle（Xcode） | `jp.lagado.pocketfortune`（表示名 Fragments） |
| 表示名 | Literary Fragments / Fragments |
| Android `versionName` / `versionCode`（要確認） | `1.0` / `1`（`android/app/build.gradle.kts`） |
| iOS 近況 | Marketing **1.0.3** · Build **4**（1.0.2 は承認済みで再提出不可だった） |

### 課金プロダクト ID（iOS と揃える・消耗型）

| Product ID | 枚数 | 備考 |
|------------|------|------|
| `jp.lagado.literaryfragments.ticket100` | 100 | iOS StoreKit 済 |
| `jp.lagado.literaryfragments.ticket1000` | 1000 | |
| `jp.lagado.literaryfragments.ticket10000` | 10000 | |

Android UI の表示価格（いまハードコード）: ¥150 / ¥900 / ¥4,500 — **本番は Play の `displayPrice` を使う**（Abomon 同方針）。

### サーバ（共通・iOS/Android）

| URL | 用途 |
|-----|------|
| `https://lagado.jp/fragments/api.php` | `random` / `search` / `atmosphere` |
| `https://lagado.jp/fragments/gemini.php` | AI 解説・翻訳 |
| `https://api.open-meteo.com/...` | 気配の天気 |

`action=atmosphere` は **quote + author + title** を検索し、キーワード先頭（場所）を高スコアにする版が `deploy/api.php` にある。  
**ロリポップ未反映だと場所ヒットが弱い。** 新スレ開始時に本番 API の挙動を一度確認すること。

---

## 4. プロダクト仕様（Android で守ること）

### 4.1 コア体験（B · 実装済み）

1. **言葉を引く** — `api.php?action=random&mode=short|long`
2. **検索** — `action=search&keyword=&scope=all|quote|author|title&mode=both`
3. **今の気配を読み取る** — GPS → 逆ジオ → Open-Meteo → キーワード → `action=atmosphere`
4. **お気に入り／履歴** — 端末内永続
5. **AI 解説** — チケット 1 枚消費 → `gemini.php`（失敗時は消費と整合を取る）
6. **日次無料枠** — 無料チケット日次リセット（iOS `TicketManager` と同趣旨）

### 4.2 気配は「場所優先・全世界」（重要・2026-07-14）

- 気候・時間・季節は従来どおり **後ろ**に足す
- **国・土地**をキーワード先頭に厚く積む（`PlaceLiteraryLexicon`）
- 表示文字列にも国・都市を出す
- 位置拒否時は京都フォールバック（日本レキシコン）
- クライアントでも `preferPlaceMatches` で再ソートして抽選

**完了の感覚:** 日本なら本文／タイトル／著者のどれかに Japan / Japanese / Tokyo / Kyoto 等が出やすい。毎回完璧な「日本文学」保証ではない（キーワード一致）。

### 4.3 UI・クレジット

- 主要アクションは断片を「引く」メタファー
- クレジット: **Lagado Research Institute**（表記ゆれ: Lagado研究所）
- 四角枠ハイライト強制などの Abomon UI 方針は **そのまま持ち込まない**（別プロダクト）

### 4.4 課金（D）

- `TicketLicenseManager` + Play Billing Library **8.3.0**（シミュ撤去済）
- Debug: 開発付与。Release／Play 署名ビルドは本物課金のみ
- 必須: `purchaseToken` 台帳で冪等 · finish 前に保存 · 無料枠と有料を分離
- **残り:** D1 Play Console 製品 · D4 クローズド実機
- 詳細: [`BILLING.md`](BILLING.md)
- 参考正本（型）:  
  `OmikujiBunko_Master/AbomonGame/docs/android/BILLING.md`  
  `AbomonGame/android/.../AbomonLicenseManager.kt`

---

## 5. 推奨作業順（新スレのロードマップ）

1. **この HANDOFF + PROGRESS を読む**（Abomon リポを開いても Fragments を触らない）
2. **A1** — Studio JBR で `./gradlew assembleDebug`（済 · 2026-07-24）
3. **A2–A3** 実機（済 · SH-03K）→ **D1** Play Console 製品 ← **いまここ**
4. **C5** — `deploy/api.php` が本番に乗っているか確認（未ならロリポップへ）
5. **D1** — Play Console にアプリ枠 + チケット 3 商品（iOS と同 ID）
6. **D2–D3** — Billing 実装（済）→ **D4** クローズド実機課金
7. **F** — データセーフティ・ストア素材・公開

進捗を進めたら必ず:

1. `PROGRESS.md` の `- [x]` と **完了％** を更新  
2. Obsidian `メモリ/プロジェクト/Literary Fragments.md` に 1 行追記

---

## 6. ビルドコマンド

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd "/Users/awaashima/Desktop/デスクトップ - awashimaのMacBook - 1/FragmentsApp/android"
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Release / AAB は Play 提出時。署名キーはユーザー管理（コミットしない）。

---

## 7. 権限（Manifest）

既にある想定:

- `INTERNET`
- `VIBRATE`
- `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION`（気配のみ · When-in-use 相当の運用）

位置は第三者へ送らない。国・都市キーワード生成と天気取得にだけ使う（App Review メモ同趣旨）。

---

## 8. ドツボ・作法（淡島様ルール要約）

- 日本語で簡潔に報告
- **最小 diff** · 依頼外リファクタ禁止
- コミット／push は **明示依頼時のみ**（成功の「うまくいきました」トリガーは Fragments リポ方針に従う）
- 同一症状で失敗 2 回 → ドツボゲート（軸転換）。Fragments では仮説を `FragmentsApp/docs/` か Obsidian プロジェクトに残す
- Abomon の www ビルド ID（`APP_WWW_BUILD`）儀式は **Fragments Android には不要**
- 新機能で旧が壊れたら **足した側**を疑う

詳細プレイブック（横断）:  
`OmikujiBunko_Master/docs/playbooks/Abomon-V1-次のアプリへ.md`

---

## 9. 既知の落とし穴

| 落とし穴 | 対処 |
|----------|------|
| 「Android が無い」と思って作り直す | `FragmentsApp/android/` が既にある。読む |
| Abomon の sync www を探す | Fragments は Kotlin 直。www sync なし |
| 課金をシミュのまま提出 | Play 審査で弾かれる／ポリシー違反リスク。D 必須 |
| `api.php` を上げずに場所優先を実機判定 | サーバ旧のままだと弱い。`deploy/api.php` 反映確認 |
| Product ID を Android だけ変える | iOS と **同一 ID** を維持 |
| デスクトップパスの NFC/NFD | Finder/シェルでパス不一致が出たら実パスを `ls` で確認 |
| iOS `1.0.2` 再アップロード | 閉じた列車。Android 版とは無関係だが iOS は 1.0.3+ |

---

## 10. 関連ドキュメント

| ファイル | 内容 |
|----------|------|
| [`PROGRESS.md`](PROGRESS.md) | ％ボード · 毎回更新 |
| [`BILLING.md`](BILLING.md) | Play Billing · 製品 ID · テスト手順 |
| [`../RELEASE_VERIFICATION.md`](../RELEASE_VERIFICATION.md) | 検証テンプレ |
| `FragmentsApp/PocketFortune/RULES.md` | iOS／API 仕様メモ（気配の場所優先追記あり） |
| Obsidian `メモリ/プロジェクト/Literary Fragments.md` | チャット横断の進捗 1 行ログ |
| Abomon `AbomonGame/docs/android/PROGRESS.md` · `BILLING.md` | **型の参考のみ** |

---

## 11. 完了の定義（Android 初版）

次を満たしたら「Android 初版できた」と言える:

1. Play クローズド（または内部）で実機インストール可能  
2. 引く／検索／気配（場所表示）／解説が動く  
3. チケット 3 商品が **本物の Play Billing** で加算される（シミュなし）  
4. データセーフティ・プライバシー URL が課金モデルと一致  
5. `PROGRESS.md` の D 取得 25 + F の主要項目が埋まっている（目安 **完了 ≥ 85%**）

---

*新スレ AI へ: 実装前に本ファイルと PROGRESS を読み、フェーズを飛ばして課金だけ／UI 全面書き換えをしないこと。*
