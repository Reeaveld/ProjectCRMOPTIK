# Panduan Pembersihan Riwayat Git (History Purge) untuk ProjectCRMOPTIK

Dokumen ini menjelaskan prosedur pembersihan commit riwayat (`git filter-repo`) untuk menghapus jejak file sensitif non-code (dokumen skripsi `Tugas Akhir/` dan transkrip `histori_chat_AI/`) dari riwayat commit Git.

> [!CAUTION]
> **PERINGATAN KEAMANAN & INTEGRITAS REPOSITORI**:
> - Perintah ini melakukan penulisan ulang (rewrite) riwayat commit dan mengubah seluruh SHA hash commit.
> - **JANGAN JALANKAN** di repositori kerja aktif sebelum membuat backup clone mirror.
> - **HANYA PEMILIK REPOSITORI (RIFQI)** yang berhak mengeksekusi force-push setelah verifikasi manual. Agen AI dilarang melakukan force-push.

---

## 1. Prasyarat & Urutan Langkah

### Langkah 1: Ubah Visibilitas Repositori Menjadi Private (Segera)
Buka GitHub:
1. Buka `https://github.com/Reeaveld/ProjectCRMOPTIK/settings`
2. Gulir ke bagian paling bawah **Danger Zone** -> **Change repository visibility**
3. Pilih **Make private** dan konfirmasi.
*(Hal ini mencegah pengindeksan publik lebih lanjut terhadap commit hash `d2219f4`)*

### Langkah 2: Buat Mirror Backup Terpisah
Jalankan di folder terpisah di luar folder project:
```bash
git clone --mirror https://github.com/Reeaveld/ProjectCRMOPTIK.git ProjectCRMOPTIK_backup_mirror.git
```

### Langkah 3: Install `git-filter-repo`
Pastikan Python 3 terpasang, lalu:
```bash
pip install git-filter-repo
```

### Langkah 4: Kloning Khusus untuk Pembersihan (Scratch Clone)
```bash
git clone https://github.com/Reeaveld/ProjectCRMOPTIK.git ProjectCRMOPTIK_purge
cd ProjectCRMOPTIK_purge
git config --local crmoptik.purge-scratch true   # penanda eksplisit: ini klon scratch
```

### Langkah 5: Jalankan Filter Repo
Hapus path `Tugas Akhir` dan `histori_chat_AI` dari seluruh riwayat tree dan commit:
```bash
git filter-repo --path "Tugas Akhir" --path "histori_chat_AI" --invert-paths
```

### Langkah 6: Verifikasi Hasil Pembersihan
1. Periksa apakah folder sensitif masih muncul di riwayat commit:
```bash
git log --all --full-history -- "Tugas Akhir/*"
git log --all --full-history -- "histori_chat_AI/*"
```
*(Kedua perintah di atas harus mengembalikan output KOSONG)*

2. Pastikan commit kode aplikasi Android tetap utuh dan test unit berjalan normal:
```bash
./gradlew testDebugUnitTest
```

### Langkah 7: Force-Push ke Remote (Hanya oleh Pemilik)
Hubungkan kembali remote jika terputus oleh filter-repo:
```bash
git remote add origin https://github.com/Reeaveld/ProjectCRMOPTIK.git
git push origin --force --all
git push origin --force --tags
```

### Langkah 8: Bersihkan Cache dan GitHub Ref (GitHub Support)
Karena GitHub menyimpan cache commit lama (dangling commits/forks/PR), commit hash lama (misal `d2219f4`) terkadang masih dapat diakses melalui URL langsung jika hash diketahui.
- Jika file proposal/skripsi berisi data mahasiswa lain yang sensitif, hubungi **GitHub Support** untuk meminta *garbage collection* / cache purge pada repositori setelah force-push dilakukan.
- Beritahukan pihak terkait jika ada salinan yang sempat terekspos saat repositori berstatus publik.

---

## 2. Skrip Eksekusi Otomatis (Helper)
Skrip bash dan PowerShell tersedia di:
- `scripts/purge_history.sh /path/ke/ProjectCRMOPTIK_backup_mirror.git`
- `scripts/purge_history.ps1 -BackupMirror C:\path\ProjectCRMOPTIK_backup_mirror.git`

Pengaman skrip (menolak berjalan bila tidak terpenuhi): penanda klon scratch (`crmoptik.purge-scratch=true`),
nama folder mengandung `purge`, working tree bersih, path backup mirror diberikan, berupa repo bare, dan memuat
semua ujung branch lokal; konfirmasi harus mengetik `HAPUS RIWAYAT`. Skrip tidak pernah melakukan push; force-push
(Langkah 7) tetap hanya dilakukan sendiri oleh pemilik.
