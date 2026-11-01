#!/bin/sh
# Добавляет разрешение на чтение фото (для скриншотов в буфер). Безопасно запускать повторно.
M=app/src/main/AndroidManifest.xml
if grep -q READ_MEDIA_IMAGES "$M"; then
  echo "Разрешения уже есть"
else
  sed -i 's#<application#<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />\n    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />\n    <application#' "$M"
  echo "Разрешения добавлены"
fi
