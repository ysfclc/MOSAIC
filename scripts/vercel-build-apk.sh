#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

CACHE_DIR="${HOME}/.cache/mosaic-android"
ANDROID_HOME="$CACHE_DIR/android-sdk"
JAVA_HOME="$CACHE_DIR/jdk17"
CMDLINE_TOOLS="$ANDROID_HOME/cmdline-tools/latest"
mkdir -p "$CACHE_DIR" "$ANDROID_HOME/cmdline-tools"

export CI=true
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "Installing pinned Java 17 toolchain..."
  JDK_ARCHIVE="$CACHE_DIR/temurin-17.tar.gz"
  JDK_STAGING="$CACHE_DIR/jdk17-staging"
  curl --fail --location --silent --show-error \
    "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" \
    --output "$JDK_ARCHIVE"
  rm -rf "$JDK_STAGING"
  mkdir -p "$JDK_STAGING"
  tar -xzf "$JDK_ARCHIVE" --strip-components=1 -C "$JDK_STAGING"
  rm -rf "$JAVA_HOME"
  mv "$JDK_STAGING" "$JAVA_HOME"
fi

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
java -version

if [[ ! -x "$CMDLINE_TOOLS/bin/sdkmanager" ]]; then
  echo "Installing Android command-line tools..."
  TOOLS_ARCHIVE="$CACHE_DIR/android-commandline-tools.zip"
  TOOLS_STAGING="$CACHE_DIR/android-commandline-tools-staging"
  curl --fail --location --silent --show-error \
    "https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip" \
    --output "$TOOLS_ARCHIVE"
  rm -rf "$TOOLS_STAGING"
  mkdir -p "$TOOLS_STAGING" "$CMDLINE_TOOLS"
  unzip -q "$TOOLS_ARCHIVE" -d "$TOOLS_STAGING"
  cp -a "$TOOLS_STAGING/cmdline-tools/." "$CMDLINE_TOOLS/"
fi

SDKMANAGER="$CMDLINE_TOOLS/bin/sdkmanager"
yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" --install \
  "platform-tools" \
  "platforms;android-37.2" \
  "build-tools;36.0.0"

echo "Checking MOSAIC source contracts..."
python3 tools/verify_source.py
python3 -m unittest discover -s tools/tests -v

KEYSTORE="$CACHE_DIR/mosaic-debug.keystore"
if [[ ! -s "$ROOT/tools/mosaic-debug-signing-key.b64" ]]; then
  echo "The persistent MOSAIC development signing identity is missing." >&2
  exit 1
fi
umask 077
base64 --decode "$ROOT/tools/mosaic-debug-signing-key.b64" > "$KEYSTORE"
keytool -list -keystore "$KEYSTORE" -storepass android -alias androiddebugkey >/dev/null
export MOSAIC_DEBUG_KEYSTORE="$KEYSTORE"

chmod +x ./gradlew
BUILD_VERSION_CODE="$(date -u +%s)"
echo "Running Android unit tests, lint and APK assembly (versionCode=$BUILD_VERSION_CODE)..."
./gradlew --no-daemon --stacktrace \
  -Dorg.gradle.parallel=false \
  -Dorg.gradle.workers.max=2 \
  -PmosaicVersionCode="$BUILD_VERSION_CODE" \
  :core:media:testDebugUnitTest \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
APKSIGNER="$ANDROID_HOME/build-tools/36.0.0/apksigner"
if [[ ! -s "$APK" || ! -x "$APKSIGNER" ]]; then
  echo "Gradle completed without producing a verifiable debug APK." >&2
  exit 1
fi

CERT_SHA="$("$APKSIGNER" verify --print-certs "$APK" | awk -F': ' '/Signer #1 certificate SHA-256 digest/ {gsub(/:/, "", $2); print tolower($2)}')"
EXPECTED_CERT_SHA="700b857678b343aed93f7a7280e7d777df73c3f5f5e5024d94c119a4578079d0"
if [[ "$CERT_SHA" != "$EXPECTED_CERT_SHA" ]]; then
  echo "APK signing certificate does not match the persistent MOSAIC development identity." >&2
  exit 1
fi

mkdir -p "$ROOT/dist"
cp "$APK" "$ROOT/dist/MOSAIC-debug.apk"
(
  cd "$ROOT/dist"
  sha256sum MOSAIC-debug.apk > MOSAIC-debug.apk.sha256.txt
)
BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
COMMIT="${VERCEL_GIT_COMMIT_SHA:-local}"
cat > "$ROOT/dist/build-info.txt" <<INFO
MOSAIC Android debug APK
Built: $BUILD_DATE
Commit: $COMMIT
Version code: $BUILD_VERSION_CODE
Signing certificate SHA-256: $CERT_SHA
INFO

cat > "$ROOT/dist/index.html" <<'HTML'
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#080b12">
  <title>MOSAIC · Android build</title>
  <style>
    :root{color-scheme:dark;font-family:Inter,ui-sans-serif,system-ui,sans-serif;background:#080b12;color:#edf2ff}
    *{box-sizing:border-box}
    body{margin:0;min-height:100vh;display:grid;place-items:center;padding:24px;background:radial-gradient(ellipse at 50% 0%,#18304b 0,transparent 55%),#080b12}
    main{width:min(100%,560px);padding:32px;border:1px solid #26384c;border-radius:24px;background:#0d1420;box-shadow:0 24px 80px #0008}
    .eyebrow{color:#74d8cf;letter-spacing:.18em;text-transform:uppercase;font-size:12px;font-weight:700}
    h1{font-size:clamp(36px,8vw,56px);letter-spacing:-.06em;margin:12px 0}
    p{color:#aab8cb;line-height:1.6}
    a.download{display:flex;justify-content:center;align-items:center;margin:28px 0 18px;padding:16px 20px;border-radius:14px;background:#8af0df;color:#071411;text-decoration:none;font-weight:800}
    a.secondary{color:#8af0df;text-decoration:none}
    small{display:block;color:#7f91a8;line-height:1.6;overflow-wrap:anywhere}
  </style>
</head>
<body>
  <main>
    <div class="eyebrow">MOSAIC · Android build</div>
    <h1>Your media.<br>One place.</h1>
    <p>This is an automatically built development APK from the linked MOSAIC source revision. Install it over an existing MOSAIC development build signed with the same key.</p>
    <a class="download" href="/MOSAIC-debug.apk">Download MOSAIC APK</a>
    <p><a class="secondary" href="/MOSAIC-debug.apk.sha256.txt">SHA-256 checksum</a> · <a class="secondary" href="/build-info.txt">Build information</a></p>
    <small>Development build, not a Play Store release. Android may ask you to allow installation from this browser. Do not uninstall an existing app unless Android reports a signing conflict.</small>
  </main>
</body>
</html>
HTML

echo "Vercel output ready:"
ls -lh "$ROOT/dist"
cat "$ROOT/dist/MOSAIC-debug.apk.sha256.txt"
