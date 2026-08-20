package com.optik.cengkareng.features.customer.presentation

import app.cash.turbine.test
import com.optik.cengkareng.MainDispatcherRule
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.request.CustomerRequest
import com.optik.cengkareng.data.remote.request.PrescriptionRequest
import com.optik.cengkareng.data.remote.request.TransactionRequest
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
        whenever(repository.getAllCustomers()).thenReturn(flowOf(emptyList()))
    }

    @Test
    fun `customers state memancarkan list dari repository`() = runTest {
        whenever(repository.getAllCustomers()).thenReturn(flowOf(listOf(sampleCustomer)))
        viewModel = CustomerViewModel(repository)

        viewModel.customers.test {
            var seen: List<CustomerEntity> = awaitItem()
            if (seen.isEmpty()) seen = awaitItem()
            assertEquals(listOf(sampleCustomer), seen)
            cancelAndIgnoreRemainingEvents()
        }
    }

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

        verify(repository).addCustomer(any<CustomerEntity>())
    }

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

    @Test
    fun `getCustomerDetail memancarkan entity dari repository`() = runTest {
        whenever(repository.getCustomerById(1)).thenReturn(flowOf(sampleCustomer))
        viewModel = CustomerViewModel(repository)

        viewModel.getCustomerDetail(1).test {
            val first = awaitItem()
            val second = if (first == null) awaitItem() else first
            assertEquals(sampleCustomer, second)
            cancelAndIgnoreRemainingEvents()
        }
    }

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

    @Test
    fun `updateCustomer mengirim Loading lalu Success ketika repository sukses`() = runTest {
        val req = CustomerRequest(nama = "Budi", noHp = "081234567890")
        whenever(repository.updateCustomer(eq(1), eq(req))).thenReturn(
            flowOf(Resource.Loading, Resource.Success("Data pelanggan berhasil diperbarui!"))
        )
        viewModel = CustomerViewModel(repository)

        viewModel.updateCustomerEvent.test {
            viewModel.updateCustomer(1, req)

            assertTrue(awaitItem() is Resource.Loading)
            assertTrue(awaitItem() is Resource.Loading)
            val result = awaitItem()
            assertTrue(result is Resource.Success)
            assertEquals("Data pelanggan berhasil diperbarui!", (result as Resource.Success).data)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── COMPLETE PROFILE TEST ──────────────────────────────────────

    @Test
    fun `completeProfile mengirim Loading lalu Success ketika noHp valid`() = runTest {
        whenever(repository.completeProfile(eq(1), eq("081234567890"))).thenReturn(
            flowOf(Resource.Loading, Resource.Success("Profil berhasil dilengkapi dan jadwal follow-up diaktifkan!"))
        )
        viewModel = CustomerViewModel(repository)

        viewModel.completeProfileEvent.test {
            viewModel.completeProfile(1, "081234567890")

            assertTrue(awaitItem() is Resource.Loading)
            assertTrue(awaitItem() is Resource.Loading)
            val result = awaitItem()
            assertTrue(result is Resource.Success)
            assertEquals("Profil berhasil dilengkapi dan jadwal follow-up diaktifkan!", (result as Resource.Success).data)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── CREATE TRANSACTION TEST ───────────────────────────────────

    @Test
    fun `createTransaction mengirim Loading lalu Success ketika request valid`() = runTest {
        val req = TransactionRequest(
            customerId = 1,
            amount = 350000.0,
            transactionDate = "2026-08-19",
            prescriptions = listOf(
                PrescriptionRequest(eyeSide = "OD", sphere = -1.50, cylinder = -0.50, axis = 90, lensType = "Single Vision")
            )
        )

        whenever(repository.createTransaction(eq(req))).thenReturn(
            flowOf(Resource.Loading, Resource.Success("Transaksi berhasil disimpan! Jadwal follow-up otomatis dibuat."))
        )
        viewModel = CustomerViewModel(repository)

        viewModel.createTransactionEvent.test {
            viewModel.createTransaction(req)

            assertTrue(awaitItem() is Resource.Loading)
            assertTrue(awaitItem() is Resource.Loading)
            val result = awaitItem()
            assertTrue(result is Resource.Success)
            assertEquals("Transaksi berhasil disimpan! Jadwal follow-up otomatis dibuat.", (result as Resource.Success).data)

            cancelAndIgnoreRemainingEvents()
        }
    }
}

private inline fun <reified T : Any> any(): T = org.mockito.kotlin.any()
private fun <T> eq(value: T): T = org.mockito.kotlin.eq(value)
