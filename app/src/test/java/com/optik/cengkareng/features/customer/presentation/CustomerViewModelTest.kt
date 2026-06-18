package com.optik.cengkareng.features.customer.presentation

import app.cash.turbine.test
import com.optik.cengkareng.MainDispatcherRule
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.request.CustomerRequest
import com.optik.cengkareng.data.remote.response.TransactionItem
import com.optik.cengkareng.data.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit test untuk CustomerViewModel.
 *
 * Yang diuji:
 *  - StateFlow `customers` (BAGIAN 1)
 *  - Channel `addCustomerEvent` Loading→Success dan Loading→Error (BAGIAN 2)
 *  - `getCustomerDetail` (BAGIAN 3)
 *  - StateFlow `transactionsState` Loading→Success dan Loading→Error
 *  - Channel `updateCustomerEvent` Loading→Success dan Loading→Error (BAGIAN 4)
 *
 * Mocking: pakai mockito-kotlin agar sintaks Kotlin idiomatik.
 * Dispatcher: pakai MainDispatcherRule (UnconfinedTestDispatcher) agar
 *             viewModelScope berjalan synchronously di test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: CustomerRepository
    private lateinit var viewModel: CustomerViewModel

    private val sampleCustomer = CustomerEntity(
        id = 1,
        remoteId = null,
        nama = "Budi",
        noHp = "081234567890"
    )

    private val sampleTransactions = listOf(
        TransactionItem(
            id = 10,
            invoice = "INV-001",
            dateFormatted = "08 Jun 2026",
            amountRaw = 250_000.0,
            amountFormatted = "Rp 250.000",
            statusLabel = "Selesai",
            statusColor = "#4CAF50",
            prescriptions = emptyList()
        )
    )

    @Before
    fun setUp() {
        repository = mock()
        // Default agar konstruktor ViewModel (yang langsung mengakses getAllCustomers())
        // tidak crash karena stub null.
        whenever(repository.getAllCustomers()).thenReturn(flowOf(emptyList()))
    }

    // ---------------------------------------------------------------------
    // 1. customers StateFlow
    // ---------------------------------------------------------------------
    @Test
    fun `customers state memancarkan list dari repository`() = runTest {
        whenever(repository.getAllCustomers()).thenReturn(flowOf(listOf(sampleCustomer)))
        viewModel = CustomerViewModel(repository)

        viewModel.customers.test {
            // Karena pakai stateIn(WhileSubscribed) — emisi pertama adalah initialValue (emptyList)
            // lalu nilai dari upstream. Pakai loop kecil agar tetap aman meski urutan emisi
            // tidak deterministik di environment test.
            var seen: List<CustomerEntity> = awaitItem()
            if (seen.isEmpty()) seen = awaitItem()
            assertEquals(listOf(sampleCustomer), seen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------------------------------------------------------------------
    // 2. addCustomer — sukses
    // ---------------------------------------------------------------------
    @Test
    fun `addCustomer mengirim Loading lalu Success ketika repository berhasil`() = runTest {
        whenever(repository.addCustomer(any<CustomerEntity>())).thenReturn(Resource.Success(true))
        viewModel = CustomerViewModel(repository)

        viewModel.addCustomerEvent.test {
            viewModel.addCustomer("Budi", "081234567890")

            assertTrue(awaitItem() is Resource.Loading)
            val result = awaitItem()
            assertTrue(result is Resource.Success)
            assertEquals(true, (result as Resource.Success).data)

            cancelAndIgnoreRemainingEvents()
        }

        // Pastikan ViewModel memang memanggil repository
        verify(repository).addCustomer(any<CustomerEntity>())
    }

    // ---------------------------------------------------------------------
    // 3. addCustomer — error
    // ---------------------------------------------------------------------
    @Test
    fun `addCustomer mengirim Loading lalu Error ketika repository gagal`() = runTest {
        whenever(repository.addCustomer(any<CustomerEntity>()))
            .thenReturn(Resource.Error("Tersimpan Offline (Jaringan Error)"))
        viewModel = CustomerViewModel(repository)

        viewModel.addCustomerEvent.test {
            viewModel.addCustomer("Budi", "081234567890")

            assertTrue(awaitItem() is Resource.Loading)
            val result = awaitItem()
            assertTrue(result is Resource.Error)
            assertEquals("Tersimpan Offline (Jaringan Error)", (result as Resource.Error).message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------------------------------------------------------------------
    // 4. getCustomerDetail
    // ---------------------------------------------------------------------
    @Test
    fun `getCustomerDetail memancarkan entity dari repository`() = runTest {
        whenever(repository.getCustomerById(1)).thenReturn(flowOf(sampleCustomer))
        viewModel = CustomerViewModel(repository)

        viewModel.getCustomerDetail(1).test {
            val first = awaitItem()           // initialValue (null) dari stateIn
            val second = if (first == null) awaitItem() else first
            assertEquals(sampleCustomer, second)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------------------------------------------------------------------
    // 5. transactionsState — sukses
    // ---------------------------------------------------------------------
    @Test
    fun `getCustomerTransactions mengubah transactionsState menjadi Success`() = runTest {
        whenever(repository.getCustomerTransactions(1)).thenReturn(
            flowOf(Resource.Loading, Resource.Success(sampleTransactions))
        )
        viewModel = CustomerViewModel(repository)

        viewModel.getCustomerTransactions(1)
        advanceUntilIdle()

        val state = viewModel.transactionsState.value
        assertTrue("Expected Success, got $state", state is Resource.Success)
        assertEquals(sampleTransactions, (state as Resource.Success).data)
    }

    // ---------------------------------------------------------------------
    // 6. transactionsState — error
    // ---------------------------------------------------------------------
    @Test
    fun `getCustomerTransactions mengubah transactionsState menjadi Error`() = runTest {
        whenever(repository.getCustomerTransactions(1)).thenReturn(
            flowOf(Resource.Loading, Resource.Error("HTTP 500"))
        )
        viewModel = CustomerViewModel(repository)

        viewModel.getCustomerTransactions(1)
        advanceUntilIdle()

        val state = viewModel.transactionsState.value
        assertTrue("Expected Error, got $state", state is Resource.Error)
        assertEquals("HTTP 500", (state as Resource.Error).message)
    }

    // ---------------------------------------------------------------------
    // 7. updateCustomer — sukses
    // ---------------------------------------------------------------------
    @Test
    fun `updateCustomer mengirim Loading lalu Success ketika repository sukses`() = runTest {
        val req = CustomerRequest(nama = "Budi", noHp = "081234567890")
        whenever(repository.updateCustomer(eq(1), eq(req))).thenReturn(
            flowOf(Resource.Loading, Resource.Success("Data pelanggan berhasil diperbarui!"))
        )
        viewModel = CustomerViewModel(repository)

        viewModel.updateCustomerEvent.test {
            viewModel.updateCustomer(1, req)

            // VM mengirim Loading sendiri sebelum mengumpulkan flow Repo
            assertTrue(awaitItem() is Resource.Loading)
            // Lalu Repo emit Loading -> VM forward
            assertTrue(awaitItem() is Resource.Loading)
            // Lalu Success
            val result = awaitItem()
            assertTrue(result is Resource.Success)
            assertEquals(
                "Data pelanggan berhasil diperbarui!",
                (result as Resource.Success).data
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------------------------------------------------------------------
    // 8. updateCustomer — error 422
    // ---------------------------------------------------------------------
    @Test
    fun `updateCustomer mengirim Loading lalu Error ketika validasi 422`() = runTest {
        val req = CustomerRequest(nama = "Budi", noHp = "081234567890")
        whenever(repository.updateCustomer(eq(1), eq(req))).thenReturn(
            flowOf(
                Resource.Loading,
                Resource.Error("Nomor HP sudah terdaftar pada pelanggan lain!")
            )
        )
        viewModel = CustomerViewModel(repository)

        viewModel.updateCustomerEvent.test {
            viewModel.updateCustomer(1, req)

            assertTrue(awaitItem() is Resource.Loading) // dari VM
            assertTrue(awaitItem() is Resource.Loading) // dari Repo
            val result = awaitItem()
            assertTrue(result is Resource.Error)
            assertEquals(
                "Nomor HP sudah terdaftar pada pelanggan lain!",
                (result as Resource.Error).message
            )

            cancelAndIgnoreRemainingEvents()
        }
    }
}

// Helper agar mockito-kotlin `any()` / `eq()` ringkas — re-export saja.
private inline fun <reified T : Any> any(): T = org.mockito.kotlin.any()
private fun <T> eq(value: T): T = org.mockito.kotlin.eq(value)
