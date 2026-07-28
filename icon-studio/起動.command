#!/bin/bash
# Fragments 「ふ」アイコンスタジオ — Finder からダブルクリックで起動
set -u

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR" || exit 1

export PATH="/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"
xattr -d com.apple.quarantine "$0" 2>/dev/null || true

PORT=8765
URL="http://127.0.0.1:${PORT}/"

# 既に起動中ならブラウザだけ開く
if curl -fsS "$URL" >/dev/null 2>&1; then
  open "$URL"
  exit 0
fi

PYTHON="$(command -v python3 || true)"
if [ -z "$PYTHON" ]; then
  osascript -e 'display alert "Fragments アイコンスタジオ" message "python3 が見つかりません" as critical' 2>/dev/null || true
  exit 1
fi

echo "Fragments アイコンスタジオを起動します…"
echo "  $URL"
echo "終了するときはこのウィンドウを閉じてください。"
echo ""

"$PYTHON" -m http.server "$PORT" --bind 127.0.0.1 >/tmp/fragments-icon-studio.log 2>&1 &
PID=$!
sleep 0.6
open "$URL"

trap 'kill $PID 2>/dev/null' EXIT INT TERM
wait $PID
