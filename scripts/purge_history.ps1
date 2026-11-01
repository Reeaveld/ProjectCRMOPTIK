# ==============================================================================
# Skrip Pembersihan Riwayat Sensitif ProjectCRMOPTIK menggunakan git-filter-repo (PowerShell)
# Hanya boleh dijalankan oleh Pemilik Repositori setelah membaca docs/HISTORY_PURGE.md
# ==============================================================================

Write-Host ">>> Memeriksa ketersediaan git-filter-repo..." -ForegroundColor Cyan
if (-not (Get-Command git-filter-repo -ErrorAction SilentlyContinue)) {
    Write-Error "ERROR: git-filter-repo belum terinstall. Install dengan: pip install git-filter-repo"
    exit 1
}

$confirmation = Read-Host "PERINGATAN: Skrip ini akan menulis ulang riwayat commit! Lanjutkan? (y/N)"
if ($confirmation -ne "y" -and $confirmation -ne "Y") {
    Write-Host "Dibatalkan." -ForegroundColor Yellow
    exit 0
}

Write-Host ">>> Menjalankan filter-repo..." -ForegroundColor Cyan
git filter-repo --path "Tugas Akhir" --path "histori_chat_AI" --invert-paths

Write-Host ">>> Selesai! Verifikasi log histori..." -ForegroundColor Green
git log --all --full-history -- "Tugas Akhir/*"
git log --all --full-history -- "histori_chat_AI/*"
Write-Host ">>> Jika kedua baris di atas kosong, riwayat berhasil dibersihkan." -ForegroundColor Green
