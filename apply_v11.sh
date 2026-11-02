#!/bin/sh
# Добавляет разрешение INTERNET (для переводчика). Безопасно запускать повторно.
M=app/src/main/AndroidManifest.xml
if grep -q 'android.permission.INTERNET' "$M"; then
  echo "INTERNET уже есть"
else
  sed -i 's#<application#<uses-permission android:name="android.permission.INTERNET" />\n    <application#' "$M"
  echo "INTERNET добавлен"
fi
