package com.optik.cengkareng.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.local.dao.CustomerDao

// ============================================================
// SCHEMA VERSION HISTORY
// Wajib: naikkan 'version' setiap kali ada perubahan field di
// entity manapun (tambah/hapus/ubah tipe/ubah nullability).
// Lakukan di commit yang SAMA dengan perubahan entitasnya.
// ============================================================
// v1 (31 Jan 2026): initial schema — customers(id, remoteId, nama,
//                   noHp NOT NULL, alamat, jenisLensa, ukuranKiri,
//                   ukuranKanan, lastFollowUp, isSynced)
// v2 (24 Agu 2026): [1] hapus kolom 'alamat' (commit 79990c5, 5 Feb)
//                   [2] noHp jadi nullable (commit 25aba8f, 20 Agu)
//                   [3] tambah kolom 'isProfileCompleteBackend' Boolean?
//                   NB: fallbackToDestructiveMigration() aktif di AppModule —
//                   naik version memicu drop+recreate tabel otomatis.
//                   User WAJIB uninstall app dari device SEKALI sebelum
//                   first-run v2 jika masih ada DB v1 (identity hash lama).
// ============================================================
@Database(
    entities = [CustomerEntity::class],
    version = 2,
    exportSchema = true   // schema JSON di-export ke app/schemas/ agar ada jejak audit
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
}