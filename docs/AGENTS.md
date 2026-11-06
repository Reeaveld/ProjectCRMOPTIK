# AGENTS.md — Android OptikCRM

Rujukan utama: `docs/ZED_HANDOFF_OptikCRM.md`. Jika file itu tidak tersedia, jangan mengarang; minta pemilik menyediakannya. Hanya fokus Android kecuali pemilik menyetujui perubahan backend.

## Stack
Kotlin, MVVM, Hilt, Room v3, Retrofit/Gson, Navigation, Coroutines/Flow, WorkManager, MPAndroidChart.

## Fakta penting
- API produksi: `https://api.crmoptik.com/api/`; debug default emulator `http://10.0.2.2:8000/api/`, override via `local.properties` `API_BASE_URL`.
- Room `id` adalah ID lokal; server ID hanya di `remoteId`. Jangan kirim `id` Room ke API.
- Jika `remoteId == null`, blokir operasi server (transaksi/riwayat) dan tampilkan status belum sinkron.
- BPJS PDF tidak punya no_hp/resep. Hasil impor dapat `no_hp=null`; jangan mengarang atau membuat nomor dummy.
- Follow-up yang saat ini aktif hanya H+3 dan H+330. Jangan menambah D+7/D+30/D+730 tanpa keputusan Rifqi.
- `sent` dari Fonnte berarti masuk antrean, bukan delivery.
- Jangan commit data pasien nyata, token Fonnte, `.env`, `local.properties`, atau dokumen TA.

## Aturan kerja
1. Periksa `git status`, `git log`, dan diff sebelum mengubah. Pertahankan perubahan lokal.
2. Baca file yang akan diubah secara utuh.
3. Untuk bug, buat test reproduksi yang gagal, perbaiki, lalu jalankan test.
4. Jangan mengubah toolchain/lokasi SDK/Gradle/JDK tanpa bukti dan persetujuan.
5. Jangan jalankan `git push`, `git push --force`, `git reset --hard`, `git clean`, `git filter-repo`, atau perintah destruktif.
6. Buat patch lokal yang dapat direview; jangan push. Laporkan command dan output nyata, termasuk test skipped.
7. Jika kuota/akses tidak cukup, berhenti dan nyatakan sisa pekerjaan; jangan mengklaim selesai.
