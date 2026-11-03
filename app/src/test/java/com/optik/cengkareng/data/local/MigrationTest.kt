package com.optik.cengkareng.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migrasi eksplisit v3 -> v4 pada basis data v3 yang dibuat dengan DDL persis dari app/schemas/.../3.json.
 * Room memvalidasi skema hasil migrasi terhadap 4.json saat dibuka, sehingga ketidakcocokan akan gagal.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MigrationTest {

    private val dbName = "migration-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createV3WithRows() {
        val raw = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        raw.execSQL(
            "CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`remoteId` INTEGER, `nama` TEXT NOT NULL, `noHp` TEXT, `jenisLensa` TEXT NOT NULL, " +
                "`ukuranKiri` TEXT NOT NULL, `ukuranKanan` TEXT NOT NULL, `lastFollowUp` INTEGER NOT NULL, " +
                "`isProfileCompleteBackend` INTEGER, `isSynced` INTEGER NOT NULL)"
        )
        raw.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_remoteId` ON `customers` (`remoteId`)")
        fun insert(remoteId: String, nama: String, synced: Int) = raw.execSQL(
            "INSERT INTO customers (remoteId, nama, noHp, jenisLensa, ukuranKiri, ukuranKanan, lastFollowUp, " +
                "isProfileCompleteBackend, isSynced) VALUES ($remoteId, '$nama', '081200000000', 'General', " +
                "'0.00', '0.00', 0, NULL, $synced)"
        )
        insert("5", "Dari Server", 1)
        insert("NULL", "Belum Terkirim A", 0)
        insert("NULL", "Belum Terkirim B", 0)
        insert("NULL", "Warisan Build Lama", 1)
        raw.version = 3
        raw.close()
    }

    @Test
    fun migrate3to4_keepsAllRowsAndAssignsUniqueUuidOnlyToUnboundRows() = runBlocking {
        createV3WithRows()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val rows = db.customerDao().getAllCustomersOnce()

            assertEquals("Tidak boleh ada data hilang", 4, rows.size)

            val fromServer = rows.single { it.nama == "Dari Server" }
            assertEquals(5, fromServer.remoteId)
            assertNull("Baris dari server tidak diberi uuid", fromServer.clientUuid)

            val unbound = rows.filter { it.remoteId == null }
            assertEquals(3, unbound.size)
            unbound.forEach {
                assertNotNull("Baris ${it.nama} harus punya clientUuid", it.clientUuid)
                assertTrue(
                    "Format UUID v4 tidak valid: ${it.clientUuid}",
                    Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(it.clientUuid!!)
                )
                assertNull(it.syncError)
            }
            assertEquals("UUID harus unik", 3, unbound.map { it.clientUuid }.toSet().size)

            // Baris dari build lama kini masuk antrean kirim (idempoten; no_hp unik di server mencegah duplikat)
            assertEquals(3, db.customerDao().getPendingPush().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate3to4_createsUniqueIndexOnClientUuid() = runBlocking {
        createV3WithRows()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val first = db.customerDao().getAllCustomersOnce().first { it.clientUuid != null }
            val duplicate = first.copy(id = 0, remoteId = 999)
            val failure = runCatching { db.customerDao().insertAllNew(listOf(duplicate)) }
            assertTrue("Indeks unik clientUuid harus menolak duplikat", failure.isFailure)
        } finally {
            db.close()
        }
    }
}
