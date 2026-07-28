# Fragments アイコンスタジオ（「ふ」崩し）

ローカル専用のアイコン編集ツールです。ひらがな **「ふ」** を傾き・波打ち・欠け・にじみなどで崩し、ホーム画面用 **1024×1024 PNG** を書き出せます。

## 起動

Finder で **`起動.command`** をダブルクリック  
→ ブラウザが `http://127.0.0.1:8765/` を開きます。

## 使い方

1. プリセット（崩しA/B/C）かスライダーで調整
2. **icon-fu-1024.png をダウンロード**
3. ファイルを `exports/icon-fu-1024.png` に置く（名前を合わせる）
4. `./apply_to_ios.sh` で iOS `AppIcon` にコピー  
   または手動で  
   `PocketFortune/Media.xcassets/AppIcon.appiconset/icon-3.png` を差し替え
5. Xcode Clean Build · Android は Image Asset で同 PNG を適用

## 旧スタジオ

英語 F 用の旧 HTML は `PocketFortune/iconstudio.html` に残してあります。  
Fragments の本線は本フォルダの **「ふ」** スタジオです。

## 注意

- オフライン・ローカルのみ（外部送信なし）
- 書体は Mac のヒラギノ等に依存。見た目は実機フォントとほぼ同じ系統
