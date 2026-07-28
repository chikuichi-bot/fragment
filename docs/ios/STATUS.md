# Literary Fragments iOS — Xcode 正本ステータス

> **更新:** 2026-07-28  
> **読み取り元:** `PocketFortune.xcodeproj` + `xcodebuild -showBuildSettings`（`generic/platform=iOS` · Release）  
> **Xcode:** 26.6（Build 17F113）· SDK `iphoneos26.5`

## いまの版

| 項目 | 値 |
|------|-----|
| 表示名 | Fragments |
| ターゲット | `PocketFortune` |
| スキーム | `LiteraryFragments` |
| Bundle ID（iphoneos） | `jp.lagado.pocketfortune` |
| Marketing | **1.0.5** |
| Build | **6**（`CURRENT_PROJECT_VERSION`） |
| Deployment | **iOS 26.4** |
| Team | `Q82QB32AZ8` |
| 署名 | Automatic |
| 暗号化申告 | `ITSAppUsesNonExemptEncryption = NO` |
| App Store | ID `6760742804`（Pocket Fortune: Literary） |
| Swift | 5.0 |
| 製品 | `PocketFortune.app` |
| 端末 | iPhone / iPad（`TARGETED_DEVICE_FAMILY = 1,2,7`） |

## 構成（正本パス）

```
FragmentsApp/
├── PocketFortune.xcodeproj/          # このファイルの版情報の正本
├── PocketFortune/
│   ├── LiteraryFragmentsApp.swift
│   ├── ContentView.swift             # ネイティブ殻 · StoreKit · WebView 連携
│   ├── index.html / style.css / main.js
│   ├── RULES.md                      # JS↔Swift 契約
│   └── Media.xcassets/AppIcon        # icon-3.png（1024）
└── docs/ios/STATUS.md                # 本ファイル
```

## 最近のコード上の確定事項（2026-07-28）

- 左右スワイプは **どちらも新規ランダム**（履歴めくりなし）· `ContentView.swift` + `main.js`
- 設定のプライバシーリンクは `https://lagado.jp/fragments/privacy.php`
- チケット購入導線の絵文字ラベルを整理（`buyButton ›`）

## 関連

- [`../RELEASE_VERIFICATION.md`](../RELEASE_VERIFICATION.md)
- [`../android/HANDOFF.md`](../android/HANDOFF.md) · [`../android/PROGRESS.md`](../android/PROGRESS.md)
- `PocketFortune/RULES.md`
