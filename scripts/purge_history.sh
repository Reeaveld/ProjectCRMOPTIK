#!/usr/bin/env bash
# ==============================================================================
# Skrip Pembersihan Riwayat Sensitif ProjectCRMOPTIK menggunakan git-filter-repo
# Hanya boleh dijalankan oleh Pemilik Repositori setelah membaca docs/HISTORY_PURGE.md
# ==============================================================================

set -euo pipefail

echo ">>> Memeriksa ketersediaan git-filter-repo..."
if ! command -v git-filter-repo &> /dev/null; then
    echo "ERROR: git-filter-repo belum terinstall. Install dengan: pip install git-filter-repo"
    exit 1
fi

echo ">>> PERINGATAN: Skrip ini akan menulis ulang riwayat commit!"
read -p "Apakah Anda yakin ingin melanjutkan? (y/N): " confirm
if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Dibatalkan."
    exit 0
fi

echo ">>> Menjalankan filter-repo..."
git filter-repo --path "Tugas Akhir" --path "histori_chat_AI" --invert-paths

echo ">>> Selesai! Verifikasi log histori..."
git log --all --full-history -- "Tugas Akhir/*"
git log --all --full-history -- "histori_chat_AI/*"
echo ">>> Jika kedua baris di atas kosong, riwayat berhasil dibersihkan."
