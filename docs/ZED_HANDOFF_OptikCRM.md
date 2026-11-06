# OptikCRM — Android Handoff untuk Gemini di Android Studio

Revisi: 6 November 2026

Dokumen ini merangkum konteks penting dari Project, repo, serta chat lintas AI agar bisa dipakai di Android Studio Agent Mode. Android repo terakhir terverifikasi pada commit `a6e4094` (3 Nov 2026 15:44 UTC). Backend repo terakhir terverifikasi pada commit `d2ff4e0` (3 Nov 2026 15:57 UTC). Repo lokal mungkin punya perubahan baru yang belum dipush; Gemini WAJIB memeriksa `git status` dan riwayat lokal sebelum edit.

## 1. Konteks TA

Judul: “Sistem Customer Relationship Management Berbasis Mobile dengan Automated Follow-Up untuk Meningkatkan Customer Retention pada Bisnis Optik (Studi Kasus: Optik RS Cengkareng)”.

Stack: Android Kotlin/MVVM/Hilt/Room/Retrofit/Coroutines/Navigation; backend Laravel 12/Sanctum/MySQL; Fonnte untuk pengiriman WhatsApp.

Proposal menetapkan:
- Data BPJS berasal dari PDF daftar tagihan; pelanggan non-BPJS diinput manual.
- PDF BPJS hanya memuat nomor transaksi, biaya, nama peserta, tanggal, dan jenis klaim. Tidak ada nomor telepon atau resep. HP ditanyakan/dilengkapi manual; profil tanpa HP tidak boleh menerima follow-up.
- Follow-up bersifat satu arah dan rule-based, bukan machine learning/chatbot.
- Tujuan evaluasi meliputi repeat purchase rate dan customer engagement sebelum/sesudah.

Fakta domain: invoice BPJS dimulai `0115O006L` (karakter kelima huruf O; pada data ada kemungkinan variasi visual O/0 yang harus diverifikasi parser). Kacamata BPJS paling cepat dapat diklaim setiap dua tahun menurut Permenkes 28/2014. Jangan menjanjikan “pemeriksaan gratis” tanpa konfirmasi optik.

## 2. Status GitHub yang Terverifikasi

Android `ProjectCRMOPTIK`: HEAD `a6e4094` — feat(sync): idempotent outbox, explicit migration, no N+1 (3 Nov 2026 15:44 UTC).
Backend `Backend_CRMOPTIK`: HEAD `d2ff4e0` — pesan commit “Big Update” (3 Nov 2026 15:57 UTC).

Penting: commit backend `d2ff4e0` hanya menambahkan `storage/framework/lsp-feedca227426c432.php` (294 baris), yaitu helper enumerasi test yang dibuat tooling editor/LSP, bukan bagian runtime CRM. Hapus dari tracking dan tambahkan pola ignore yang spesifik, mis. `/storage/framework/lsp-*.php`. Jangan hapus file lain di `storage/` secara luas.

Backend sudah punya (berdasarkan commit yang dibaca): login/logout Sanctum, CRUD customer, transaksi/resep, import BPJS dua tahap parse/commit, follow-up Fonnte+webhook, soft delete, `is_bpjs`, dashboard dan endpoint retention metrics, configurable follow-up planner. Jangan mengimplementasikan ulang fitur ini tanpa bukti lokal bahwa belum ada.

Android sudah punya: login/token session, import BPJS review screen, dashboard chart, export helper, Room v3, Remote ID, atomic chunked sync, Worker outbox, explicit migration v3→v4, `SecureStore`, URL debug configurable dan URL release `https://api.crmoptik.com/api/`, cleartext debug-only.

Commit Android terakhir `a6e4094` menyebut: `ApiErrorParser`; `clientUuid`/`syncError`; `CustomerSyncWorker` (WorkManager/Hilt, connected network, exponential backoff, KEEP, periodic 15 min); backend 4xx disimpan sebagai syncError (tidak retry); `no_hp` kosong ditolak di client (K1 belum dijawab); Mutex untuk sync/push; `bindRemoteId`; sync chunk 500 dan tanpa N+1; `remoteId == null` memblokir transaksi; test repository/outbox/worker/migration. Ini ringkasan commit, BUKAN bukti semua test lulus. Verifikasi kode dan jalankan sendiri.

Jangan klaim hasil test lulus kecuali benar-benar menjalankan test pada mesin ini. Jangan percaya laporan Antigravity/Zed lama tanpa mengulang.

## 3. Keputusan Pemilik yang Belum Ditetapkan

- K1: pelanggan boleh tanpa no_hp? Default sistem saat ini menolak pelanggan manual tanpa nomor; data hasil import BPJS boleh null. Jangan ubah validasi/UX sebelum konfirmasi.
- K2: jenis/jarak follow-up resmi. Yang disetujui dalam kode saat ini hanya H+3 dan H+330; D+7/D+30/D+730 belum boleh diaktifkan tanpa konfirmasi.
- K3: repo Public/Private dan izin rewrite history. Agen dilarang force-push atau menjalankan `git filter-repo`.
- K5: go-live date dan baseline untuk evaluasi before-after.
- K6: prosedur pasien BPJS dengan nama sama saat import.

## 4. Tugas Android Sekarang

Fokus hanya Android, sesuai permintaan pemilik. Jangan menyentuh backend kecuali perlu kontrak API, dan laporkan kebutuhan itu dulu.

1. Mulai dengan `git status`, `git log -5 --oneline`, `git diff --stat`, dan cek `git stash list`. Jangan overwrite perubahan lokal belum ter-commit.
2. Baca seluruh diff Android sejak commit `6a0744d`, cek code aktual di working tree, lalu petakan test yang ada.
3. Prioritas pertama: **kontrak dashboard Android vs backend**.
   - Backend sudah mengganti `delivery_rate` menjadi `queued_rate` (nullable), menambah follow-up failed/pending/blocked/simulated, active/repeat/returning rates (nullable), dan endpoint `/api/dashboard/retention`.
   - Pastikan Android `DashboardModels.kt`, `DashboardRepository.kt`, `DashboardViewModel.kt`, `DashboardFragment.kt`, serta tests mengonsumsi schema terbaru dengan tipe nullable yang aman.
   - Label harus jujur: `queued_rate` = diterima antrean, bukan terkirim ke pelanggan. Jika denominator nol, tampilkan “—”/“Belum ada data”, bukan 0% atau 100%.
   - Tampilkan metrik per segmen BPJS/non-BPJS, sebelum-sesudah, follow-up return rate, dan keterbatasan engagement sesuai response API. Jangan hitung ulang metrik secara berbeda di Android.
4. Periksa masalah outbox/worker dari commit `a6e4094` dan selesaikan hanya jika terkonfirmasi di kode/test:
   - Jangan menganggap semua HTTP 4xx permanen. Bedakan 401/403 (butuh autentikasi), 408/429 (retry terbatas dengan backoff), 400/409/422 (perlu koreksi; simpan syncError), 5xx/network (retry). Hindari retry tak terbatas untuk kesalahan permanen.
   - Pastikan idempotensi `client_uuid` bertahan saat server berhasil menyimpan tetapi respons hilang.
   - Pastikan WorkManager periodic constraints dan unique work policy tidak menyebabkan duplikasi atau antrean macet.
   - K1 masih belum diputuskan; jangan mengubah kebijakan no_hp kosong sendiri.
5. Perbaiki/test pesan error validasi 422, Room migration, retry worker, sinkronisasi dan session 401 bila ada regresi nyata.
6. Jangan menambah fitur atau mengubah desain besar di luar Android tanpa izin. Jangan menambah follow-up D+7/D+30/D+730.

## 5. Aturan Kerja yang Wajib

- Jangan menggampangkan. Baca file terkait penuh sebelum edit.
- Untuk bug, buat test reproduksi yang gagal dahulu, lalu perbaiki dan tunjukkan test lulus.
- Jalankan `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, dan `./gradlew lint` bila SDK/JDK/dependensi tersedia. Laporkan perintah dan ringkasan output nyata, termasuk failed/skipped.
- Jika environment/kuota tidak bisa menjalankan sesuatu, berhenti dan nyatakan tepat apa yang tidak bisa dilakukan. Jangan mengarang output, dan jangan terus menghabiskan kuota pada percobaan yang sama.
- Setiap perubahan dikelompokkan dalam patch/commit lokal dengan pesan jelas. Pemilik secara eksplisit meminta “gunakan patch seperti cara awal”: hasil akhir wajib menyediakan patch yang bisa dicek dan dipasang, bukan hanya mengedit diam-diam.
- Sebelum membuat patch, simpan diff binary dan pastikan tidak membawa `local.properties`, `.env`, token, PDF/nama pasien, atau perubahan pengguna lain.
- Jangan `git push`, `git push --force`, `git reset --hard`, `git clean`, `git filter-repo`, atau menjalankan `migrate:fresh`.
- Jangan mengubah lokasi SDK, Gradle home, JDK, atau variabel sistem. Masalah KSP/Gradle sebelumnya sudah diperbaiki dan Android Studio sudah bisa jalan; jangan mengutak-atik toolchain tanpa error aktual yang membuktikan perlu.
- Hindari manipulasi cache di luar project tanpa persetujuan. Jangan menghapus seluruh `.gradle` sebagai langkah awal.
- Jangan menaruh data pasien asli/kredensial di test, log, fixture, screenshots, atau commit.

## 6. Format Laporan Akhir

1. Apa yang dibaca dan apa yang tidak dapat dibaca.
2. Status Git dan hash lokal/remot, termasuk uncommitted changes.
3. Temuan berdasarkan bukti file dan lokasi (bukan asumsi).
4. Perubahan Android yang dibuat, file per file.
5. Test/build: lulus/gagal/skip dengan output/artefak yang nyata.
6. Patch yang dibuat, hash commit lokal, instruksi apply/rollback PowerShell.
7. Item tertunda dan keputusan yang perlu dari Rifqi.

Mulai hanya dengan langkah awal di Bagian 4 nomor 1–2, lalu laporkan rencana perbaikan sebelum mengedit.
