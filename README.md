# Musikku — pemutar musik sederhana

Fitur: putar lagu dari memori internal & kartu SD, pilih lagu sendiri, buat/kelola playlist,
seekbar, prev/next, tetap jalan di background.

## Cara dapat APK (paling gampang, tanpa install apa pun)
1. Buat repo baru di github.com, upload SEMUA isi folder ini (termasuk folder .github).
2. Buka tab **Actions** -> workflow "Build APK" jalan otomatis (+/- 3-5 menit).
3. Klik run yang selesai -> bagian **Artifacts** -> download **Musikku-APK** (zip berisi app-debug.apk).
4. Kirim ke HP, install (izinkan "install dari sumber tidak dikenal").

## Atau pakai Android Studio
File -> Open folder ini -> Build -> Build APK(s). Hasil: app/build/outputs/apk/debug/app-debug.apk

## Pemakaian
- Tab **Lagu**: tap = putar. Tekan lama = tambahkan ke playlist.
- Tab **Playlist**: "+ Buat playlist baru", tap playlist untuk buka, "+ Tambah / pilih lagu" untuk centang lagu.
- Tombol **+ File**: pilih file audio manual (kalau ada lagu di SD card yang tidak terdeteksi).
