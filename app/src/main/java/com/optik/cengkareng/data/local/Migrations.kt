package com.optik.cengkareng.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    /**
     * v3 -> v4: outbox idempoten.
     *  - kolom clientUuid (TEXT, nullable) + indeks unik
     *  - kolom syncError (TEXT, nullable)
     *  - baris yang belum punya remoteId diberi UUID v4 acak (dibuat di SQLite) agar dapat dikirim
     *    ulang secara idempoten oleh worker. Baris dari server dibiarkan null.
     * Tidak ada data yang dihapus.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE customers ADD COLUMN clientUuid TEXT")
            db.execSQL("ALTER TABLE customers ADD COLUMN syncError TEXT")
            db.execSQL(
                "UPDATE customers SET clientUuid = lower(" +
                    "hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || " +
                    "substr(hex(randomblob(2)), 2) || '-' || " +
                    "substr('89ab', abs(random()) % 4 + 1, 1) || substr(hex(randomblob(2)), 2) || '-' || " +
                    "hex(randomblob(6))) WHERE remoteId IS NULL"
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_clientUuid` ON `customers` (`clientUuid`)")
        }
    }

    val ALL = arrayOf(MIGRATION_3_4)
}
