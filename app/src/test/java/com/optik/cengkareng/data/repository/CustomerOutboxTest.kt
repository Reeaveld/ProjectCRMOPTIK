package com.optik.cengkareng.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.optik.cengkareng.FakeSyncScheduler
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.local.AppDatabase
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.api.ApiService
import com.optik.cengkareng.data.remote.request.CustomerRequest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Outbox pelanggan (AND-2/AND-3) dan penerjemahan id lokal -> remoteId (AND-6).
 * Server palsu meniru idempotensi client_uuid dari BE-11. Data anonim.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CustomerOutboxTest {

    private class ServerCustomer(val id: Int, val nama: String, val hp: String, val uuid: String?)

    /** Server Laravel palsu: POST /customers idempoten berdasarkan client_uuid. */
    private class FakeCustomerServer : Dispatcher() {
        val customers = LinkedHashMap<String, ServerCustomer>()
        val postBodies = CopyOnWriteArrayList<com.google.gson.JsonObject>()
        val requestLog = CopyOnWriteArrayList<String>()
        var nextId = 100
        var dropNextPostResponse = false
        var nextPostOverride: MockResponse? = null

        fun toJson(c: ServerCustomer) =
            """{"id":${c.id},"nama":"${c.nama}","no_hp":"${c.hp}","jenis_lensa":"General",""" +
                """"ukuran_kiri":"0.00","ukuran_kanan":"0.00","last_follow_up":null,""" +
                """"is_profile_complete":true,"client_uuid":${c.uuid?.let { "\"$it\"" } ?: "null"}}"""

        override fun dispatch(request: RecordedRequest): MockResponse {
            requestLog += "${request.method} ${request.path}"
            return when {
                request.method == "POST" && request.path == "/api/customers" -> {
                    nextPostOverride?.let { nextPostOverride = null; return it }
                    val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
                    postBodies += body
                    val uuid = body.get("client_uuid")?.asString
                    val key = uuid ?: "anon-${nextId}"
                    val existing = customers[key]
                    val customer = existing ?: ServerCustomer(
                        id = nextId++,
                        nama = body.get("nama").asString,
                        hp = body.get("no_hp").asString,
                        uuid = uuid
                    ).also { customers[key] = it }

                    if (dropNextPostResponse) {
                        dropNextPostResponse = false
                        MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
                    } else {
                        MockResponse().setResponseCode(if (existing == null) 201 else 200).setBody(toJson(customer))
                    }
                }
                request.method == "GET" && request.path == "/api/customers" ->
                    MockResponse().setBody(customers.values.joinToString(prefix = "[", postfix = "]", separator = ",") { toJson(it) })
                request.method == "PUT" || request.method == "PATCH" ->
                    MockResponse().setResponseCode(200).setBody("""{"success":true}""")
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    private lateinit var server: MockWebServer
    private lateinit var fake: FakeCustomerServer
    private lateinit var db: AppDatabase
    private lateinit var repo: CustomerRepository
    private val scheduler = FakeSyncScheduler()

    @Before
    fun setUp() {
        fake = FakeCustomerServer()
        server = MockWebServer().apply {
            dispatcher = fake
            start()
        }
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        // Tanpa retry otomatis OkHttp agar "respons POST hilang" benar-benar terlihat oleh repository
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .client(client)
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

    private suspend fun rows() = db.customerDao().getAllCustomersOnce()

    // ─── Penyimpanan lokal + pengiriman ─────────────────────────────

    @Test
    fun addCustomer_online_bindsRemoteIdAndSendsClientUuid() = runBlocking {
        val result = repo.addCustomer(CustomerEntity(nama = "Pelanggan Anonim", noHp = "081200000001"))

        assertEquals(Resource.Success(true), result)
        val row = rows().single()
        assertEquals(100, row.remoteId)
        assertTrue(row.isSynced)
        assertNotNull(row.clientUuid)
        assertEquals(row.clientUuid, fake.postBodies.single().get("client_uuid").asString)
        assertEquals(0, scheduler.pushRequests)
    }

    @Test
    fun addCustomer_offline_keepsRowAndSchedulesWorker() = runBlocking {
        fake.nextPostOverride = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

        val result = repo.addCustomer(CustomerEntity(nama = "Pelanggan Anonim", noHp = "081200000002"))

        assertEquals(Resource.Success(false), result)
        val row = rows().single()
        assertNull(row.remoteId)
        assertNull(row.syncError)
        assertNotNull(row.clientUuid)
        assertEquals(1, scheduler.pushRequests)
    }

    @Test
    fun offlineThenOnline_workerPushSendsPendingRowAutomatically() = runBlocking {
        fake.nextPostOverride = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        repo.addCustomer(CustomerEntity(nama = "Pelanggan Anonim", noHp = "081200000003"))

        val summary = repo.pushPendingCustomers()

        assertEquals(PushSummary(pushed = 1, rejected = 0, transientFailures = 0), summary)
        val row = rows().single()
        assertEquals(100, row.remoteId)
        assertEquals(1, fake.customers.size)
    }

    // ─── Respons POST hilang: tidak boleh duplikat ──────────────────

    @Test
    fun postResponseLost_thenRetry_createsNoDuplicateOnServerOrLocally() = runBlocking {
        fake.dropNextPostResponse = true

        val first = repo.addCustomer(CustomerEntity(nama = "Pelanggan Anonim", noHp = "081200000004"))
        assertEquals(Resource.Success(false), first)
        assertEquals("Server sudah menyimpan walau respons hilang", 1, fake.customers.size)

        val summary = repo.pushPendingCustomers()

        assertEquals(1, summary.pushed)
        assertEquals("Pengulangan tidak boleh membuat pelanggan kedua di server", 1, fake.customers.size)
        assertEquals(2, fake.postBodies.size)
        assertEquals(
            fake.postBodies[0].get("client_uuid").asString,
            fake.postBodies[1].get("client_uuid").asString
        )
        val row = rows().single()
        assertEquals(100, row.remoteId)
    }

    @Test
    fun postResponseLost_thenSyncReconcilesByClientUuidWithoutDuplicate() = runBlocking {
        fake.dropNextPostResponse = true
        repo.addCustomer(CustomerEntity(nama = "Pelanggan Anonim", noHp = "081200000005"))
        val pendingUuid = rows().single().clientUuid

        val sync = repo.syncCustomers()

        assertTrue(sync is Resource.Success)
        val row = rows().single()
        assertEquals(100, row.remoteId)
        assertEquals(pendingUuid, row.clientUuid)
        assertTrue(row.isSynced)
        // Tidak ada lagi yang menunggu dikirim
        assertTrue(db.customerDao().getPendingPush().isEmpty())
    }

    @Test
    fun bindingToRemoteIdAlreadyOwnedBySyncedRow_mergesInsteadOfCrashing() = runBlocking {
        // Baris hasil sync sudah memiliki remoteId 100 (tanpa client_uuid)
        db.customerDao().insertCustomer(
            CustomerEntity(remoteId = 100, nama = "Pelanggan Anonim", noHp = "081200000006", isSynced = true)
        )

        val result = repo.addCustomer(CustomerEntity(nama = "Pelanggan Anonim", noHp = "081200000006"))

        assertEquals(Resource.Success(true), result)
        val all = rows()
        assertEquals("Harus tersisa satu baris: $all", 1, all.size)
        assertEquals(100, all.single().remoteId)
    }

    // ─── Penolakan server ───────────────────────────────────────────

    @Test
    fun addCustomer_rejected422_removesLocalRowAndReturnsFieldMessage() = runBlocking {
        fake.nextPostOverride = MockResponse().setResponseCode(422).setBody(
            """{"success":false,"message":"Validasi Gagal","errors":{"no_hp":["Nomor HP sudah terdaftar pada pelanggan lain!"]}}"""
        )

        val result = repo.addCustomer(CustomerEntity(nama = "Pelanggan Anonim", noHp = "081200000007"))

        assertEquals(Resource.Error("Nomor HP sudah terdaftar pada pelanggan lain!"), result)
        assertTrue(rows().isEmpty())
        assertEquals(0, scheduler.pushRequests)
    }

    @Test
    fun pushPending_422_storesSyncErrorAndIsNotRetried() = runBlocking {
        db.customerDao().insertCustomer(
            CustomerEntity(clientUuid = "11111111-1111-4111-8111-111111111111", nama = "Anon", noHp = "081200000008")
        )
        fake.nextPostOverride = MockResponse().setResponseCode(422).setBody(
            """{"message":"Validasi Gagal","errors":{"no_hp":["Nomor HP sudah terdaftar pada pelanggan lain!"]}}"""
        )

        val first = repo.pushPendingCustomers()
        assertEquals(PushSummary(pushed = 0, rejected = 1, transientFailures = 0), first)
        assertEquals("Nomor HP sudah terdaftar pada pelanggan lain!", rows().single().syncError)

        val before = server.requestCount
        val second = repo.pushPendingCustomers()
        assertEquals(PushSummary(0, 0, 0), second)
        assertEquals("Baris yang ditolak tidak boleh dikirim ulang otomatis", before, server.requestCount)
    }

    @Test
    fun pushPending_blankPhone_neverCallsServerAndFlagsRow() = runBlocking {
        db.customerDao().insertCustomer(
            CustomerEntity(clientUuid = "22222222-2222-4222-8222-222222222222", nama = "Tanpa HP", noHp = null)
        )

        val summary = repo.pushPendingCustomers()

        assertEquals(PushSummary(pushed = 0, rejected = 1, transientFailures = 0), summary)
        assertEquals(0, server.requestCount)
        assertEquals(CustomerRepository.MSG_PHONE_REQUIRED, rows().single().syncError)
    }

    @Test
    fun pushPending_serverError500_isTransientAndKeepsRowPending() = runBlocking {
        db.customerDao().insertCustomer(
            CustomerEntity(clientUuid = "33333333-3333-4333-8333-333333333333", nama = "Anon", noHp = "081200000009")
        )
        fake.nextPostOverride = MockResponse().setResponseCode(500).setBody("{}")

        val summary = repo.pushPendingCustomers()

        assertEquals(PushSummary(pushed = 0, rejected = 0, transientFailures = 1), summary)
        val row = rows().single()
        assertNull(row.remoteId)
        assertNull(row.syncError)
    }

    // ─── AND-6: id lokal tidak pernah dikirim ───────────────────────

    @Test
    fun updateCustomer_translatesLocalIdToRemoteId_evenWhenIdsCollide() = runBlocking {
        val dao = db.customerDao()
        dao.insertCustomer(CustomerEntity(id = 1, remoteId = 2, nama = "A", noHp = "081200000011", isSynced = true))
        dao.insertCustomer(CustomerEntity(id = 2, remoteId = 1, nama = "B", noHp = "081200000012", isSynced = true))

        val result = repo.updateCustomer(1, CustomerRequest(nama = "A baru", noHp = "081200000011")).last()

        assertTrue(result is Resource.Success)
        assertEquals("PUT /api/customers/2", fake.requestLog.single())
        assertEquals("A baru", dao.getById(1)!!.nama)
        assertEquals("B", dao.getById(2)!!.nama)
    }

    @Test
    fun updateCustomer_blockedWithoutRemoteId_doesNotCallServer() = runBlocking {
        db.customerDao().insertCustomer(CustomerEntity(id = 5, nama = "Lokal", noHp = "081200000013"))

        val result = repo.updateCustomer(5, CustomerRequest(nama = "Lokal", noHp = "081200000013")).last()

        assertEquals(Resource.Error(CustomerRepository.MSG_NOT_SYNCED), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun updateCustomer_unknownLocalId_doesNotCallServer() = runBlocking {
        val result = repo.updateCustomer(404, CustomerRequest(nama = "X", noHp = "081200000014")).last()

        assertEquals(Resource.Error(CustomerRepository.MSG_LOCAL_NOT_FOUND), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun completeProfile_usesRemoteIdAndUpdatesLocalCompleteness() = runBlocking {
        val dao = db.customerDao()
        dao.insertCustomer(
            CustomerEntity(
                id = 3, remoteId = 77, nama = "Pasien BPJS Anonim", noHp = null,
                isProfileCompleteBackend = false, isSynced = true
            )
        )

        val result = repo.completeProfile(3, "081234567890").last()

        assertTrue(result is Resource.Success)
        assertEquals("PATCH /api/customers/77/complete-profile", fake.requestLog.single())
        val row = dao.getById(3)!!
        assertEquals("081234567890", row.noHp)
        assertTrue("Kelengkapan profil lokal harus ikut diperbarui", row.isProfileComplete())
    }

    @Test
    fun completeProfile_blockedWithoutRemoteId() = runBlocking {
        db.customerDao().insertCustomer(CustomerEntity(id = 9, nama = "Lokal", noHp = null))

        val result = repo.completeProfile(9, "081234567890").last()

        assertEquals(Resource.Error(CustomerRepository.MSG_NOT_SYNCED), result)
        assertEquals(0, server.requestCount)
    }
}
