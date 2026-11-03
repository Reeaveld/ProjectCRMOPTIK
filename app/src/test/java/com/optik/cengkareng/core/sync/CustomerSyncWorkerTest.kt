package com.optik.cengkareng.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.optik.cengkareng.FakeSyncScheduler
import com.optik.cengkareng.data.local.AppDatabase
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.api.ApiService
import com.optik.cengkareng.data.repository.CustomerRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CustomerSyncWorkerTest {

    private lateinit var server: MockWebServer
    private lateinit var db: AppDatabase
    private lateinit var repo: CustomerRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        repo = CustomerRepository(db, db.customerDao(), api, FakeSyncScheduler())
    }

    @After
    fun tearDown() {
        db.close()
        server.shutdown()
    }

    private fun worker() = TestListenableWorkerBuilder<CustomerSyncWorker>(context)
        .setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker = CustomerSyncWorker(appContext, workerParameters, repo)
        })
        .build()

    private fun customerJson(id: Int, uuid: String) =
        """{"id":$id,"nama":"Anon","no_hp":"081200000001","jenis_lensa":"General","ukuran_kiri":"0.00",""" +
            """"ukuran_kanan":"0.00","last_follow_up":null,"is_profile_complete":true,"client_uuid":"$uuid"}"""

    private suspend fun seedPending(uuid: String) = db.customerDao().insertCustomer(
        CustomerEntity(clientUuid = uuid, nama = "Anon", noHp = "081200000001")
    )

    @Test
    fun doWork_pushesPendingRowThenPulls_andSucceeds() = runBlocking {
        val uuid = "44444444-4444-4444-8444-444444444444"
        seedPending(uuid)
        server.enqueue(MockResponse().setResponseCode(201).setBody(customerJson(31, uuid)))
        server.enqueue(MockResponse().setBody("[${customerJson(31, uuid)}]"))

        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        val row = db.customerDao().getAllCustomersOnce().single()
        assertEquals(31, row.remoteId)
        assertEquals("POST", server.takeRequest().method)
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun doWork_retriesWhenNetworkFails_andKeepsRowPending() = runBlocking {
        seedPending("55555555-5555-4555-8555-555555555555")
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        val row = db.customerDao().getAllCustomersOnce().single()
        assertNull(row.remoteId)
        assertNull(row.syncError)
    }

    @Test
    fun doWork_retriesOnServer500() = runBlocking {
        seedPending("66666666-6666-4666-8666-666666666666")
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        assertEquals(ListenableWorker.Result.retry(), worker().doWork())
    }

    @Test
    fun doWork_succeedsAndStopsRetryingWhenServerRejects422() = runBlocking {
        seedPending("77777777-7777-4777-8777-777777777777")
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """{"message":"Validasi Gagal","errors":{"no_hp":["Nomor HP sudah terdaftar pada pelanggan lain!"]}}"""
            )
        )
        server.enqueue(MockResponse().setBody("[]"))

        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(
            "Nomor HP sudah terdaftar pada pelanggan lain!",
            db.customerDao().getAllCustomersOnce().single().syncError
        )
    }

    @Test
    fun doWork_withNothingPending_justPullsAndSucceeds() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(1, server.requestCount)
    }
}
