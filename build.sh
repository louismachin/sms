#!/usr/bin/env bash
# Needs: a JDK (17+), zip, and an Android SDK with build-tools + one platform:
#   sdkmanager "build-tools;34.0.0" "platforms;android-34"
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
BT="${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)}"
JAR="${ANDROID_JAR:-$(ls -d "$SDK"/platforms/android-* | sort -V | tail -1)/android.jar}"
MIN_SDK=21
TARGET_SDK=30
KEYSTORE="${KEYSTORE:-$HOME/.android/smsbridge.keystore}"

echo "build-tools: $BT"
echo "android.jar: $JAR"

rm -rf build && mkdir -p build/classes build/dex

# 1. Manifest -> base APK (no resources needed)
"$BT/aapt2" link -o build/unsigned.apk -I "$JAR" \
    --manifest AndroidManifest.xml \
    --min-sdk-version "$MIN_SDK" --target-sdk-version "$TARGET_SDK" \
    --version-code 1 --version-name 1.0

# 2. Java -> .class
javac --release 8 -Xlint:-options -classpath "$JAR" -d build/classes $(find src -name '*.java')

# 3. .class -> classes.dex (d8 also desugars the lambda)
"$BT/d8" --min-api "$MIN_SDK" --lib "$JAR" --output build/dex $(find build/classes -name '*.class')

# 4. Put the dex in the APK, align, sign
(cd build/dex && zip -qj ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk

if [ ! -f "$KEYSTORE" ]; then
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android \
        -alias smsbridge -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=smsbridge" >/dev/null 2>&1
    echo "created signing key: $KEYSTORE (keep it; reinstalls need the same key)"
fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android \
    --out smsbridge.apk build/aligned.apk

echo "built: $(pwd)/smsbridge.apk"
