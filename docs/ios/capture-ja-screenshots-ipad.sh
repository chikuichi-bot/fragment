#!/bin/zsh
# Fragments — シミュレータから日本語 App Store スクショを新規撮影（iPad 13インチ）
# ASC iPad 13": 2048×2732 / 2064×2752（縦）
set -euo pipefail
setopt NULL_GLOB

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_RAW="$ROOT/docs/ios/store-screenshots-ja-ipad13/_raw"
OUT="$ROOT/docs/ios/store-screenshots-ja-ipad13"
PROJ="$ROOT/PocketFortune.xcodeproj"
SCHEME="LiteraryFragments"
DERIVED="/tmp/FragmentsScreenshotDerivedIPad"
DEVICE_NAME="iPad Pro 13 Fragments JA"
DEVICE_TYPE="com.apple.CoreSimulator.SimDeviceType.iPad-Pro-13-inch-M4-8GB"
TARGET_W=2048
TARGET_H=2732

mkdir -p "$OUT_RAW" "$OUT"

RUNTIME=$(xcrun simctl list runtimes available | awk -F' - ' '/iOS/{print $NF}' | tail -1)
if [[ -z "$RUNTIME" ]]; then
  echo "No iOS simulator runtime found" >&2
  exit 1
fi
echo "Runtime: $RUNTIME"

UDID=$(xcrun simctl list devices available | awk -v n="$DEVICE_NAME" '$0 ~ n { match($0, /\(([A-F0-9-]+)\)/); if (RSTART) { print substr($0, RSTART+1, RLENGTH-2); exit } }')
if [[ -z "$UDID" ]]; then
  echo "Creating $DEVICE_NAME ..."
  UDID=$(xcrun simctl create "$DEVICE_NAME" "$DEVICE_TYPE" "$RUNTIME")
fi
echo "UDID: $UDID"

# 他シミュレータを落として Shutdown ループを避ける
echo "Shutting down other simulators..."
xcrun simctl shutdown all 2>/dev/null || true
sleep 2

ensure_booted() {
  local state
  state=$(xcrun simctl list devices | grep "$UDID" || true)
  if echo "$state" | grep -q Booted; then
    return 0
  fi
  echo "Booting $UDID (headless first)..."
  # Simulator.app を先に開くと Shutdown ループすることがあるので headless boot 優先
  xcrun simctl boot "$UDID" 2>/dev/null || true
  local i
  for i in {1..90}; do
    state=$(xcrun simctl list devices | grep "$UDID" || true)
    if echo "$state" | grep -q Booted; then
      # UI が安定してから Simulator.app を開く
      sleep 2
      open -a Simulator --args -CurrentDeviceUDID "$UDID" 2>/dev/null || open -a Simulator
      sleep 4
      return 0
    fi
    sleep 1
  done
  echo "Simulator failed to stay Booted" >&2
  exit 1
}

ensure_booted

echo "Building..."
xcodebuild \
  -project "$PROJ" \
  -scheme "$SCHEME" \
  -configuration Debug \
  -destination "id=$UDID" \
  -derivedDataPath "$DERIVED" \
  CODE_SIGNING_ALLOWED=NO \
  build | tail -40

APP=$(find "$DERIVED/Build/Products/Debug-iphonesimulator" -name "PocketFortune.app" -maxdepth 2 | head -1)
if [[ -z "$APP" ]]; then
  echo "App not found" >&2
  exit 1
fi
echo "App: $APP"
BUNDLE=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$APP/Info.plist")
echo "Bundle: $BUNDLE"

ensure_booted
xcrun simctl uninstall "$UDID" "$BUNDLE" 2>/dev/null || true
xcrun simctl install "$UDID" "$APP"
xcrun simctl ui "$UDID" appearance light 2>/dev/null || true
# Portrait
xcrun simctl status_bar "$UDID" override --time "9:41" --batteryState charged --batteryLevel 100 2>/dev/null || true

normalize() {
  local src="$1"
  local dest="$2"
  python3 - "$src" "$dest" "$TARGET_W" "$TARGET_H" <<'PY'
import sys
from PIL import Image
src, dest, w, h = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
im = Image.open(src).convert("RGB")
# Near-target: center-crop if slightly larger; else fit on white canvas
if im.width >= w and im.height >= h:
    left = (im.width - w) // 2
    top = (im.height - h) // 2
    im.crop((left, top, left + w, top + h)).save(dest, "PNG")
else:
    im.thumbnail((w, h), Image.Resampling.LANCZOS)
    canvas = Image.new("RGB", (w, h), (255, 255, 255))
    canvas.paste(im, ((w - im.width)//2, (h - im.height)//2))
    canvas.save(dest, "PNG")
print(dest, Image.open(dest).size)
PY
}

capture() {
  local screen="$1"
  local file="$2"
  local wait_s="${3:-8}"
  local attempt=1
  local max=3
  while (( attempt <= max )); do
    echo "=== Capture $screen -> $file (wait ${wait_s}s) attempt $attempt ==="
    ensure_booted
    xcrun simctl terminate "$UDID" "$BUNDLE" 2>/dev/null || true
    sleep 1
    if ! xcrun simctl launch "$UDID" "$BUNDLE" \
      -AppleLanguages "(ja)" \
      -AppleLocale "ja_JP" \
      -UIScreenshotScreen "$screen"; then
      echo "Launch failed"
      ((attempt++))
      sleep 2
      continue
    fi
    sleep "$wait_s"
    if xcrun simctl io "$UDID" screenshot "$OUT_RAW/$file"; then
      sips -g pixelWidth -g pixelHeight "$OUT_RAW/$file" | tr '\n' ' '
      echo
      local stem="${file%.png}"
      normalize "$OUT_RAW/$file" "$OUT/${stem}-2048x2732.png"
      # ASC も 2064×2752 を受け付けるため raw を別名保存
      local rw rh
      rw=$(sips -g pixelWidth "$OUT_RAW/$file" 2>/dev/null | awk '/pixelWidth/{print $2}')
      rh=$(sips -g pixelHeight "$OUT_RAW/$file" 2>/dev/null | awk '/pixelHeight/{print $2}')
      if [[ "$rw" == "2064" && "$rh" == "2752" ]]; then
        cp "$OUT_RAW/$file" "$OUT/${stem}-2064x2752.png"
      else
        # raw が別解像度なら 2064 にリサイズして残す
        sips -z 2752 2064 "$OUT_RAW/$file" --out "$OUT/${stem}-2064x2752.png" >/dev/null
      fi
      return 0
    fi
    echo "Screenshot failed"
    ((attempt++))
    sleep 2
  done
  echo "FAILED $screen" >&2
  return 1
}

# WebView 引用はロード＋注入に時間がかかる。settings/tickets は sheet 表示後も余裕を見る。
capture home       "01-quote.png"      28
capture home_en    "02-quote-en.png"   22
capture language   "03-language.png"   6
capture welcome    "04-welcome.png"    6
capture howto      "05-howto.png"      8
capture settings   "06-settings.png"   14
capture tickets    "07-tickets.png"    16

echo "Done. Output: $OUT"
open "$OUT"
