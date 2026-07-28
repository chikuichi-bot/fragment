#!/bin/bash
# exports/icon-fu-1024.png を iOS AppIcon に反映（あれば）
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$DIR/exports/icon-fu-1024.png"
DEST="$DIR/../PocketFortune/Media.xcassets/AppIcon.appiconset/icon-3.png"

if [ ! -f "$SRC" ]; then
  echo "先にスタジオで PNG をダウンロードし、次へ置いてください:"
  echo "  $SRC"
  echo "（ダウンロード後にファイルを exports/ へ移動／改名）"
  exit 1
fi

cp "$SRC" "$DEST"
echo "反映しました → $DEST"
echo "Xcode で Clean Build して実機確認してください。"
echo "Android は Studio の Image Asset で同 PNG を ic_launcher に。"
