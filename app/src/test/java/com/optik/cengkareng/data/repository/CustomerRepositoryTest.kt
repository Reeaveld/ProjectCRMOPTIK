package com.optik.cengkareng.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.optik.cengkareng.FakeSyncScheduler
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.local.AppDatabase
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.api.ApiService
import com.optik.cengkareng.data.remote.request.CompleteProfileRequest
import com.optik.cengkareng.data.remote.request.CustomerRequest
import com.optik.cengkareng.data.remote.request.PrescriptionRequest
import com.optik.cengkareng.data.remote.request.TransactionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Test repository dengan Room in-memory (Robolectric) + MockWebServer.
 * Seluruh data anonim.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CustomerRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var db: AppDatabase
    private lateinit var repo: CustomerRepository
    private val scheduler = FakeSyncScheduler()
    private val queries = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .setQueryCallback(
                RoomDatabase.QueryCallback { sql, _ -> queries.add(sql) },
                java.util.concurrent.Executor { it.run() }
            )
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        repo = CustomerRepository(db, db.customerDao(), api, scheduler)
    }

    @After
    fun tearDown() {
        db.close()
        server.shutdown()
    }

    // ─── Helper ─────────────────────────────────────────────────────

    private fun customerJson(id: Int, nama: String, hp: String? = "0812000000$id") =
        """{"id":$id,"nama":"$nama","no_hp":${if (hp == null) "null" else "\"$hp\""},""" +
            """"jenis_lensa":"General","ukuran_kiri":"0.00","ukuran_kanan":"0.00",""" +
            """"last_follow_up":null,"is_profile_complete":${hp != null}}"""

    private fun listJson(vararg items: String) = items.joinToString(prefix = "[", postfix = "]", separator = ",")

    private fun synced(remoteId: Int, nama: String) =
        CustomerEntity(remoteId = remoteId, nama = nama, noHp = "0812000000$remoteId", isSynced = true)

    private suspend fun allRows(): List<CustomerEntity> =
        db.customerDao().getAllCustomersOnce()

    // ─── AND-8: sinkronisasi ────────────────────────────────────────

    @Test
    fun sync_insertsNewAndUpdatesExistingRowsByRemoteId() = runBlocking {
        db.customerDao().insertCustomer(synced(10, "Lama"))
        server.enqueue(
            MockResponse().setBody(listJson(customerJson(10, "Baru"), customerJson(11, "Tambahan")))
        )

        val result = repo.syncCustomers()

        assertTrue(result is Resource.Success)
        val rows = allRows().sortedBy { it.remoteId }
        assertEquals(listOf("Baru", "Tambahan"), rows.map { it.nama })
        assertTrue(rows.all { it.isSynced })
    }

    @Test
    fun sync_idCollision_localIdEqualToOtherRowsRemoteId_isNotConfused() = runBlocking {
        // Baris lokal id=1 milik server 2 dan baris lokal id=2 milik server 1
        val dao = db.customerDao()
        dao.insertCustomer(synced(2, "A-lama").copy(id = 1))
        dao.insertCustomer(synced(1, "B-lama").copy(id = 2))
        server.enqueue(
            MockResponse().setBody(listJson(customerJson(1, "B-baru"), customerJson(2, "A-baru")))
        )

        repo.syncCustomers()

        val rows = allRows().associateBy { it.id }
        assertEquals("A-baru", rows.getValue(1).nama)
        assertEquals(2, rows.getValue(1).remoteId)
        assertEquals("B-baru", rows.getValue(2).nama)
        assertEquals(1, rows.getValue(2).remoteId)
    }

    @Test
    fun sync_removesStaleSyncedRows_butKeepsUnsyncedAndLegacyRows() = runBlocking {
        val dao = db.customerDao()
        dao.insertCustomer(synced(1, "Hilang"))
        dao.insertCustomer(synced(2, "Tetap"))
        dao.insertCustomer(CustomerEntity(nama = "Belum Terkirim", noHp = "081200000099", isSynced = false))
        // Baris dari build lama: tanpa remoteId namun sempat bertanda synced
        dao.insertCustomer(CustomerEntity(nama = "Warisan Build Lama", noHp = "081200000098", isSynced = true))
        server.enqueue(MockResponse().setBody(listJson(customerJson(2, "Tetap"))))

        repo.syncCustomers()

        assertEquals(
            setOf("Tetap", "Belum Terkirim", "Warisan Build Lama"),
            allRows().map { it.nama }.toSet()
        )
    }

    @Test
    fun sync_emptyServerList_removesOnlySyncedRows() = runBlocking {
        val dao = db.customerDao()
        dao.insertCustomer(synced(1, "Sinkron"))
        dao.insertCustomer(CustomerEntity(nama = "Antrean Lokal", noHp = "081200000097", isSynced = false))
        server.enqueue(MockResponse().setBody("[]"))

        val result = repo.syncCustomers()

        assertTrue(result is Resource.Success)
        assertEquals(listOf("Antrean Lokal"), allRows().map { it.nama })
    }

    @Test
    fun sync_withMoreThan999StaleIds_deletesAllStaleRows() = runBlocking {
        val dao = db.customerDao()
        dao.insertAll((1..1200).map { synced(it, "Pelanggan $it") })
        server.enqueue(MockResponse().setBody(listJson(customerJson(1, "Pelanggan 1"))))

        val result = repo.syncCustomers()

        assertTrue(result is Resource.Success)
        val rows = allRows()
        assertEquals(1, rows.size)
        assertEquals(1, rows.single().remoteId)
    }

    @Test
    fun sync_failureResponse_keepsLocalData() = runBlocking {
        db.customerDao().insertCustomer(synced(1, "Aman"))
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        val result = repo.syncCustomers()

        assertTrue(result is Resource.Error)
        assertEquals(listOf("Aman"), allRows().map { it.nama })
    }

    // ─── AND-7: tanpa N+1 ───────────────────────────────────────────

    @Test
    fun sync_queryCountDoesNotGrowLinearlyWithCustomerCount() = runBlocking {
        val dao = db.customerDao()
        dao.insertAll((1..60).map { synced(it, "Lama $it") })
        server.enqueue(
            MockResponse().setBody(listJson(*(1..120).map { customerJson(it, "Baru $it") }.toTypedArray()))
        )
        queries.clear()

        repo.syncCustomers()

        val perRowLookups = queries.count { it.contains("FROM customers WHERE remoteId", ignoreCase = true) }
        assertTrue("Lookup per baris terdeteksi ($perRowLookups kueri untuk 120 pelanggan)", perRowLookups <= 2)
        assertEquals(120, allRows().size)
    }

    @Test
    fun sync_doesNotRewriteUnchangedRows() = runBlocking {
        val dao = db.customerDao()
        dao.insertAll((1..30).map { synced(it, "Pelanggan $it").copy(isProfileCompleteBackend = true) })
        server.enqueue(
            MockResponse().setBody(
                listJson(*(1..30).map { customerJson(it, "Pelanggan $it") }.toTypedArray())
            )
        )
        queries.clear()

        repo.syncCustomers()

        val writes = queries.count {
            it.trimStart().startsWith("UPDATE", ignoreCase = true) ||
                it.trimStart().startsWith("INSERT", ignoreCase = true)
        }
        assertEquals("Data tidak berubah tetapi ada $writes penulisan", 0, writes)
    }

    @Test
    fun sync_emitsFewListUpdatesForBulkChange() = runBlocking {
        val sizes = CopyOnWriteArrayList<Int>()
        val job = launch(Dispatchers.IO) {
            db.customerDao().getAllCustomers().collect { sizes.add(it.size) }
        }
        delay(300)
        server.enqueue(
            MockResponse().setBody(listJson(*(1..100).map { customerJson(it, "Pelanggan $it") }.toTypedArray()))
        )

        repo.syncCustomers()
        delay(500)
        job.cancel()

        assertEquals(100, sizes.last())
        assertTrue("Emisi Flow terlalu banyak: $sizes", sizes.size <= 3)
    }

    // ─── AND-4: balapan add vs sync ─────────────────────────────────

    @Test
    fun addCustomer_andSyncRacing_leaveExactlyOneRow() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.method == "POST" && request.path == "/api/customers" ->
                    MockResponse().setResponseCode(201)
                        .setBody(customerJson(7, "Budi Anonim", "081200000007"))
                        .setHeadersDelay(500, TimeUnit.MILLISECONDS)
                request.method == "GET" && request.path == "/api/customers" ->
                    MockResponse().setBody(listJson(customerJson(7, "Budi Anonim", "081200000007")))
                else -> MockResponse().setResponseCode(404)
            }
        }

        val add = async(Dispatchers.IO) {
            repo.addCustomer(CustomerEntity(nama = "Budi Anonim", noHp = "081200000007"))
        }
        delay(150)
        val sync = async(Dispatchers.IO) { repo.syncCustomers() }
        val results = awaitAll(add, sync)

        assertTrue("addCustomer harus sukses: ${results[0]}", results[0] is Resource.Success)
        val rows = allRows()
        assertEquals("Harus tepat satu baris: $rows", 1, rows.size)
        assertEquals(7, rows.single().remoteId)
        assertTrue(rows.single().isSynced)
    }

    // ─── AND-1: pemetaan error per-field ────────────────────────────

    private fun validationBody(field: String, message: String, top: String = "Validasi Gagal") =
        """{"success":false,"message":"$top","errors":{"$field":["$message"]}}"""

    @Test
    fun updateCustomer_422_showsFieldMessageNotGenericMessage() = runBlocking {
        db.customerDao().insertCustomer(synced(1, "Anon").copy(id = 1))
        server.enqueue(
            MockResponse().setResponseCode(422)
                .setBody(validationBody("no_hp", "Nomor HP sudah terdaftar pada pelanggan lain!"))
        )

        val last = repo.updateCustomer(1, CustomerRequest(nama = "Anon", noHp = "081200000001")).last()

        assertEquals(Resource.Error("Nomor HP sudah terdaftar pada pelanggan lain!"), last)
    }

    @Test
    fun createTransaction_422_showsFirstFieldErrorFromLaravelFormat() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """{"message":"The amount field is required.","errors":{"amount":["The amount field is required."]}}"""
            )
        )
        val first = repo.createTransaction(sampleTransaction()).last()
        assertEquals(Resource.Error("The amount field is required."), first)

        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """{"message":"The given data was invalid.","errors":{"amount":["Jumlah transaksi wajib diisi."]}}"""
            )
        )
        val second = repo.createTransaction(sampleTransaction()).last()
        assertEquals(Resource.Error("Jumlah transaksi wajib diisi."), second)
    }

    @Test
    fun completeProfile_422_showsFieldMessage() = runBlocking {
        db.customerDao().insertCustomer(synced(1, "Anon").copy(id = 1))
        server.enqueue(
            MockResponse().setResponseCode(422)
                .setBody(validationBody("no_hp", "Format nomor HP tidak valid. Gunakan format 08xxx atau 628xxx."))
        )

        val last = repo.completeProfile(1, "123").last()

        assertEquals(
            Resource.Error("Format nomor HP tidak valid. Gunakan format 08xxx atau 628xxx."),
            last
        )
    }

    @Test
    fun addCustomer_422_showsFieldMessageNotRawJson() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(422)
                .setBody(validationBody("no_hp", "Nomor HP sudah terdaftar pada pelanggan lain!"))
        )

        val result = repo.addCustomer(CustomerEntity(nama = "Anon", noHp = "081200000001"))

        assertTrue(result is Resource.Error)
        val msg = (result as Resource.Error).message
        assertEquals("Nomor HP sudah terdaftar pada pelanggan lain!", msg)
        assertFalse("JSON mentah bocor ke pengguna: $msg", msg.contains("{"))
        // Pengguna masih di formulir: tidak boleh ada baris sisa yang ditolak
        assertTrue(allRows().isEmpty())
    }

    private fun sampleTransaction() = TransactionRequest(
        customerId = 1,
        amount = 100000.0,
        transactionDate = "2026-11-01",
        prescriptions = listOf(
            PrescriptionRequest(eyeSide = "OD", sphere = -1.0, cylinder = 0.0, axis = 0, lensType = "Single Vision")
        )
    )
}
