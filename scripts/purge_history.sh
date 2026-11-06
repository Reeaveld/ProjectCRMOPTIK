#!/usr/bin/env bash
# ==============================================================================
# Pembersihan riwayat sensitif ProjectCRMOPTIK dengan git-filter-repo.
# HANYA oleh Pemilik Repositori, setelah membaca docs/HISTORY_PURGE.md.
#
# Pemakaian (di dalam KLON SCRATCH, bukan folder kerja):
#   git config --local crmoptik.purge-scratch true      # penanda eksplisit klon scratch
#   scripts/purge_history.sh /path/ke/ProjectCRMOPTIK_backup_mirror.git
#
# Skrip ini menolak berjalan bila:
#   - penanda klon scratch belum diset, atau nama folder tidak mengandung "purge"
#   - working tree tidak bersih
#   - path backup mirror tidak diberikan / bukan repo bare / tidak memuat semua ujung branch lokal
# Skrip ini TIDAK melakukan push apa pun.
# ==============================================================================

set -euo pipefail

die() { echo "ERROR: $*" >&2; exit 1; }

BACKUP_MIRROR="${1:-}"
[[ -n "$BACKUP_MIRROR" ]] || die "Argumen wajib: path backup mirror (git clone --mirror ...). Lihat docs/HISTORY_PURGE.md langkah 2."

TOPLEVEL="$(git rev-parse --show-toplevel 2>/dev/null)" || die "Bukan di dalam repositori git."
[[ "$(git config --local --get crmoptik.purge-scratch || true)" == "true" ]] \
    || die "Ini bukan klon scratch. Buat klon baru (langkah 4) lalu jalankan: git config --local crmoptik.purge-scratch true"
case "$(basename "$TOPLEVEL")" in
    *purge*) ;;
    *) die "Nama folder klon scratch harus mengandung 'purge' (mis. ProjectCRMOPTIK_purge). Sekarang: $(basename "$TOPLEVEL")" ;;
esac

[[ -z "$(git status --porcelain)" ]] || die "Working tree tidak bersih. Komit/buang perubahan dulu."

[[ -d "$BACKUP_MIRROR" ]] || die "Backup mirror tidak ditemukan: $BACKUP_MIRROR"
[[ "$(git --git-dir="$BACKUP_MIRROR" rev-parse --is-bare-repository 2>/dev/null || true)" == "true" ]] \
    || die "Backup mirror harus repo bare hasil 'git clone --mirror': $BACKUP_MIRROR"

echo ">>> Memverifikasi backup memuat semua ujung branch lokal..."
while read -r sha ref; do
    git --git-dir="$BACKUP_MIRROR" cat-file -e "${sha}^{commit}" 2>/dev/null \
        || die "Backup TIDAK memuat $ref ($sha). Perbarui mirror (git remote update) lalu ulangi."
done < <(git for-each-ref --format='%(objectname) %(refname)' refs/heads)

echo ">>> Memeriksa ketersediaan git-filter-repo..."
command -v git-filter-repo >/dev/null 2>&1 || die "git-filter-repo belum terinstall: pip install git-filter-repo"

echo
echo "=============================== PERINGATAN ==============================="
echo " Skrip ini MENULIS ULANG riwayat commit dan mengubah SEMUA hash commit."
echo " Setelah ini, remote hanya dapat diperbarui dengan force-push (--force)."
echo " Force-push HANYA dilakukan sendiri oleh pemilik; skrip ini tidak melakukannya."
echo " Backup mirror: $BACKUP_MIRROR"
echo "=========================================================================="
read -r -p "Ketik persis 'HAPUS RIWAYAT' untuk melanjutkan: " confirm
[[ "$confirm" == "HAPUS RIWAYAT" ]] || { echo "Dibatalkan."; exit 0; }

echo ">>> Menjalankan filter-repo..."
git filter-repo --path "Tugas Akhir" --path "histori_chat_AI" --invert-paths

echo ">>> Selesai! Verifikasi log histori..."
git log --all --full-history -- "Tugas Akhir/*"
git log --all --full-history -- "histori_chat_AI/*"
echo ">>> Jika kedua baris di atas kosong, riwayat berhasil dibersihkan."
echo
echo "!!! INGAT: remote belum diubah. Langkah berikutnya (force-push) hanya oleh pemilik,"
echo "!!! setelah 'git remote add origin ...' dan verifikasi manual (docs/HISTORY_PURGE.md langkah 6-8)."
