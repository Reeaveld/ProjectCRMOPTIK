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
// v3 (1 Nov 2026):  [1] tambah unique index pada remoteId untuk mencegah duplikasi
//                   [2] optimasi sync dengan withTransaction dan chunked batching
// v4 (3 Nov 2026): [1] tambah 'clientUuid' (unik, nullable) untuk outbox idempoten
//                   [2] tambah 'syncError' (nullable) untuk penolakan server
//                   Migrasi eksplisit: Migrations.MIGRATION_3_4 (data lokal dipertahankan;
//                   baris tanpa remoteId diberi UUID acak lewat SQL).
//                   Destructive migration HANYA dari v1/v2 pada build debug (lihat AppModule).
// ============================================================
@Database(
    entities = [CustomerEntity::class],
    version = 4,
    exportSchema = true   // schema JSON di-export ke app/schemas/ agar ada jejak audit
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
}
