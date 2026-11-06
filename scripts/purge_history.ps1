# ==============================================================================
# Pembersihan riwayat sensitif ProjectCRMOPTIK dengan git-filter-repo (PowerShell).
# HANYA oleh Pemilik Repositori, setelah membaca docs/HISTORY_PURGE.md.
#
# Pemakaian (di dalam KLON SCRATCH, bukan folder kerja):
#   git config --local crmoptik.purge-scratch true
#   .\scripts\purge_history.ps1 -BackupMirror C:\path\ProjectCRMOPTIK_backup_mirror.git
#
# Menolak berjalan bila penanda klon scratch belum diset, nama folder tidak mengandung "purge",
# working tree tidak bersih, atau backup mirror tidak ada / bukan repo bare / tidak memuat semua
# ujung branch lokal. Skrip ini TIDAK melakukan push apa pun.
# ==============================================================================
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupMirror
)

$ErrorActionPreference = 'Stop'

function Fail([string]$message) {
    Write-Host "ERROR: $message" -ForegroundColor Red
    exit 1
}

$top = (git rev-parse --show-toplevel 2>$null)
if (-not $top) { Fail "Bukan di dalam repositori git." }

$marker = (git config --local --get crmoptik.purge-scratch 2>$null)
if ($marker -ne 'true') {
    Fail "Ini bukan klon scratch. Buat klon baru (langkah 4) lalu jalankan: git config --local crmoptik.purge-scratch true"
}
$folder = Split-Path -Leaf $top
if ($folder -notlike '*purge*') {
    Fail "Nama folder klon scratch harus mengandung 'purge' (mis. ProjectCRMOPTIK_purge). Sekarang: $folder"
}

if (git status --porcelain) { Fail "Working tree tidak bersih. Komit/buang perubahan dulu." }

if (-not (Test-Path $BackupMirror)) { Fail "Backup mirror tidak ditemukan: $BackupMirror" }
$isBare = (git --git-dir="$BackupMirror" rev-parse --is-bare-repository 2>$null)
if ($isBare -ne 'true') { Fail "Backup mirror harus repo bare hasil 'git clone --mirror': $BackupMirror" }

Write-Host ">>> Memverifikasi backup memuat semua ujung branch lokal..." -ForegroundColor Cyan
foreach ($line in (git for-each-ref --format='%(objectname) %(refname)' refs/heads)) {
    $sha, $ref = $line -split ' ', 2
    git --git-dir="$BackupMirror" cat-file -e "$sha^{commit}" 2>$null
    if ($LASTEXITCODE -ne 0) { Fail "Backup TIDAK memuat $ref ($sha). Perbarui mirror (git remote update) lalu ulangi." }
}

Write-Host ">>> Memeriksa ketersediaan git-filter-repo..." -ForegroundColor Cyan
if (-not (Get-Command git-filter-repo -ErrorAction SilentlyContinue)) {
    Fail "git-filter-repo belum terinstall: pip install git-filter-repo"
}

Write-Host ""
Write-Host "=============================== PERINGATAN ===============================" -ForegroundColor Yellow
Write-Host " Skrip ini MENULIS ULANG riwayat commit dan mengubah SEMUA hash commit." -ForegroundColor Yellow
Write-Host " Setelah ini, remote hanya dapat diperbarui dengan force-push (--force)." -ForegroundColor Yellow
Write-Host " Force-push HANYA dilakukan sendiri oleh pemilik; skrip ini tidak melakukannya." -ForegroundColor Yellow
Write-Host " Backup mirror: $BackupMirror" -ForegroundColor Yellow
Write-Host "==========================================================================" -ForegroundColor Yellow
$confirmation = Read-Host "Ketik persis 'HAPUS RIWAYAT' untuk melanjutkan"
if ($confirmation -cne 'HAPUS RIWAYAT') {
    Write-Host "Dibatalkan." -ForegroundColor Yellow
    exit 0
}

Write-Host ">>> Menjalankan filter-repo..." -ForegroundColor Cyan
git filter-repo --path "Tugas Akhir" --path "histori_chat_AI" --invert-paths

Write-Host ">>> Selesai! Verifikasi log histori..." -ForegroundColor Green
git log --all --full-history -- "Tugas Akhir/*"
git log --all --full-history -- "histori_chat_AI/*"
Write-Host ">>> Jika kedua baris di atas kosong, riwayat berhasil dibersihkan." -ForegroundColor Green
Write-Host ""
Write-Host "!!! INGAT: remote belum diubah. Force-push hanya oleh pemilik, setelah verifikasi manual (docs/HISTORY_PURGE.md langkah 6-8)." -ForegroundColor Yellow
