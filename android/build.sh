#!/usr/bin/env bash
# Compila el APK de Compás sin Android Studio ni Gradle.
# Necesita: JDK, y los paquetes de Ubuntu/Debian
#   android-sdk-platform-23 aapt dalvik-exchange zipalign apksigner
# Uso: KEYSTORE=/ruta/compas.jks KEYPASS=... ./android/build.sh
set -euo pipefail
cd "$(dirname "$0")"
SDK_JAR=${SDK_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
KEYSTORE=${KEYSTORE:?Indica el almacén de claves con KEYSTORE=}
KEYPASS=${KEYPASS:?Indica la contraseña con KEYPASS=}
OUT=build
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/assets/www"

# 1. La app web, tal cual, dentro de assets/www
cp ../index.html ../manifest.webmanifest ../icon-*.png "$OUT/assets/www/"
cp -r ../sonidos "$OUT/assets/www/"

# 2. Código Java → bytecode Java 8 → dex
javac --release 8 -nowarn -classpath "$SDK_JAR" -d "$OUT/classes" $(find src -name '*.java')
dalvik-exchange --dex --min-sdk-version=23 --output="$OUT/classes.dex" "$OUT/classes"

# 3. Empaquetar manifiesto, recursos, assets y dex
aapt package -f -M AndroidManifest.xml -S res -A "$OUT/assets" -I "$SDK_JAR" -F "$OUT/sin-firmar.apk"
(cd "$OUT" && aapt add -f sin-firmar.apk classes.dex >/dev/null)

# 4. Alinear y firmar
zipalign -f -p 4 "$OUT/sin-firmar.apk" "$OUT/alineado.apk"
apksigner sign --ks "$KEYSTORE" --ks-pass "pass:$KEYPASS" --key-pass "pass:$KEYPASS" --out "$OUT/compas.apk" "$OUT/alineado.apk"
apksigner verify "$OUT/compas.apk"
echo "Listo: android/$OUT/compas.apk"
