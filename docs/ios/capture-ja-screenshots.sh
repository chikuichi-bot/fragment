#!/bin/zsh
# Fragments — シミュレータから日本語 App Store スクショを撮る（iPhone 16 Plus）
# WebView 系（home / home_en）はロード＋引用注入に時間がかかるため待ちを長めに。
set -euo pipefail
setopt NULL_GLOB

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/docs/ios/store-screenshots-ja-sim"
PROJ="$ROOT/PocketFortune.xcodeproj"
SCHEME="LiteraryFragments"
DERIVED="/tmp/FragmentsScreenshotDerived2"
DEVICE_NAME="iPhone 16 Plus Fragments JA"
DEVICE_TYPE="com.apple.CoreSimulator.SimDeviceType.iPhone-16-Plus"
# 固定 UDID（無ければ名前で探す／作成）
PREFERRED_UDID="5AEDE026-188C-4C58-B1C2-5090EF963B18"

mkdir -p "$OUT"

RUNTIME=$(xcrun simctl list runtimes available | awk -F' - ' '/iOS/{print $NF}' | tail -1)
if [[ -z "$RUNTIME" ]]; then
  echo "No iOS simulator runtime found" >&2
  exit 1
fi
echo "Runtime: $RUNTIME"

UDID=""
if xcrun simctl list devices | grep -q "$PREFERRED_UDID"; then
  UDID="$PREFERRED_UDID"
else
  UDID=$(xcrun simctl list devices available | awk -v n="$DEVICE_NAME" '$0 ~ n { match($0, /\(([A-F0-9-]+)\)/); if (RSTART) { print substr($0, RSTART+1, RLENGTH-2); exit } }')
fi
if [[ -z "$UDID" ]]; then
  echo "Creating $DEVICE_NAME ..."
  UDID=$(xcrun simctl create "$DEVICE_NAME" "$DEVICE_TYPE" "$RUNTIME")
fi
echo "UDID: $UDID"

ensure_booted() {
  local state
  state=$(xcrun simctl list devices | grep "$UDID" || true)
  if echo "$state" | grep -q Booted; then
    return 0
  fi
  echo "Booting $UDID ..."
  # Simulator.app を先に開くと Shutdown ループすることがあるので headless boot 優先
  xcrun simctl boot "$UDID" 2>/dev/null || true
  local i
  for i in {1..60}; do
    state=$(xcrun simctl list devices | grep "$UDID" || true)
    if echo "$state" | grep -q Booted; then
      open -a Simulator --args -CurrentDeviceUDID "$UDID" 2>/dev/null || open -a Simulator
      sleep 3
      return 0
    fi
    sleep 1
  done
  echo "Simulator failed to stay Booted" >&2
  exit 1
}

echo "Shutting down other simulators..."
xcrun simctl shutdown all 2>/dev/null || true
sleep 2

ensure_booted

echo "Building..."
xcodebuild \
  -project "$PROJ" \
  -scheme "$SCHEME" \
  -configuration Debug \
  -destination "id=$UDID" \
  -derivedDataPath "$DERIVED" \
  CODE_SIGNING_ALLOWED=NO \
  build | tail -20

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
open -a Simulator --args -CurrentDeviceUDID "$UDID" 2>/dev/null || open -a Simulator

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
    if xcrun simctl io "$UDID" screenshot "$OUT/$file"; then
      sips -g pixelWidth -g pixelHeight "$OUT/$file" | tr '\n' ' '
      echo
      ls -la "$OUT/$file"
      return 0
    fi
    echo "Screenshot failed"
    ((attempt++))
    sleep 2
  done
  echo "FAILED $screen" >&2
  return 1
}

# home/home_en: WebView + 引用注入のため長め。settings/tickets: sheet 表示待ち。
capture home       "01-quote.png"      22
capture home_en    "02-quote-en.png"   18
capture language   "03-language.png"   4
capture welcome    "04-welcome.png"    4
capture howto      "05-howto.png"      6
capture settings   "06-settings.png"   12
capture tickets    "07-tickets.png"    14

echo "Done. Output: $OUT"
open "$OUT"
