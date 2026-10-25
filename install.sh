#!/usr/bin/env bash
# Install and configure the bridge on the phone connected over USB.
#   ./install.sh            (settings come from .env; see .env.example)
set -euo pipefail
cd "$(dirname "$0")"

# Load settings from .env if present (real env vars set on the command line still work)
if [ -f .env ]; then set -a; . ./.env; set +a; fi

PKG=com.louismachin.smsbridge
: "${PORT:?set PORT in .env (see .env.example)}"
: "${SMS_SECRET:?set SMS_SECRET in .env (see .env.example)}"
adb() { command adb ${ADB_SERIAL:+-s "$ADB_SERIAL"} "$@"; }

adb install -r smsbridge.apk
adb shell pm grant "$PKG" android.permission.SEND_SMS
adb shell pm grant "$PKG" android.permission.RECEIVE_SMS

# Keep Android from putting the app to sleep (ignore failure on old versions)
adb shell dumpsys deviceidle whitelist "+$PKG" >/dev/null || true

# Phone's 127.0.0.1:$PORT -> this machine's 127.0.0.1:$PORT, over the USB cable.
# Re-run after a reboot or replug (the Sinatra app does this on startup too).
adb reverse "tcp:$PORT" "tcp:$PORT"

# Store config + wake the app out of the "stopped" state
adb shell am start -n "$PKG/.Main" \
    --es secret "$SMS_SECRET" --es webhook "http://127.0.0.1:$PORT/incoming"

echo "done. watch it with: adb logcat -s smsbridge"