package com.optik.cengkareng.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.optik.cengkareng.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppDatabaseTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        // Build in-memory database v2 to verify schema validation and room identity hash
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseCreationAndSchemaValidation_succeedsWithoutException() {
        // Assert database instance is created cleanly without schema integrity crash
        assertNotNull(database)
        val dao = database.customerDao()
        assertNotNull(dao)
    }

    @Test
    fun customerDao_insertAndRead_withNullableNoHpAndBackendProfileComplete_succeeds() = runBlocking {
        val dao = database.customerDao()

        // 1. Insert customer with null noHp (BPJS import case)
        val bpjsCustomer = CustomerEntity(
            nama = "Pasien BPJS Test",
            noHp = null,
            isProfileCompleteBackend = false
        )
        dao.insertCustomer(bpjsCustomer)

        val customers = dao.getAllCustomers().first()
        assertEquals(1, customers.size)
        val retrieved = customers[0]

        assertEquals("Pasien BPJS Test", retrieved.nama)
        assertNull(retrieved.noHp)
        assertEquals(false, retrieved.isProfileCompleteBackend)
        assertEquals(false, retrieved.isProfileComplete())
    }
}
