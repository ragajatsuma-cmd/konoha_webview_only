# Konoha WebView Only

Aplikasi Android native super ringan (WebView murni) untuk **input token PWA** game Konoha.

## Fitur

- Hanya satu layar: input token `konoha_pwa_token`.
- Token disimpan lokal (`SharedPreferences` + `localStorage`).
- Membuka `https://konohagame.com/` di WebView.
- Tanpa automation, tanpa userscript, tanpa multi-profil.
- Izin hanya `INTERNET` + `ACCESS_NETWORK_STATE`, cleartext diblokir (HTTPS saja).

## Struktur

```
app/
  AndroidManifest.xml
  src/com/konoha/tokenonly/MainActivity.java
  res/...
build.ps1          # build tanpa Gradle (aapt2 -> javac -> d8 -> zipalign -> apksigner)
```

## Build

Prasyarat: Android SDK (`build-tools;35.0.0`, `platforms;android-35`) dan JDK 17+.

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Output: `KonohaTokenOnly.apk` (di root repo).

## Signing

Keystore dibuat otomatis saat build pertama. Untuk rilis, sesuaikan `release.keystore` dan kredensial di `build.ps1`.
