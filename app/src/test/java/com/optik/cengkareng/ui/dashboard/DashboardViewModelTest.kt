package com.optik.cengkareng.ui.dashboard

import app.cash.turbine.test
import com.optik.cengkareng.MainDispatcherRule
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.model.BeforeAfterRetentionItem
import com.optik.cengkareng.data.remote.model.CustomerSearchItem
import com.optik.cengkareng.data.remote.model.CustomerSearchResultResponse
import com.optik.cengkareng.data.remote.model.DashboardRetentionResponse
import com.optik.cengkareng.data.remote.model.DashboardStatsResponse
import com.optik.cengkareng.data.remote.model.DateRangeInfo
import com.optik.cengkareng.data.remote.model.KpiStats
import com.optik.cengkareng.data.remote.model.SegmentRetentionItem
import com.optik.cengkareng.data.repository.DashboardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var repository: DashboardRepository

    private lateinit var viewModel: DashboardViewModel

    private val mockStatsResponse = DashboardStatsResponse(
        dateRange = DateRangeInfo(from = "2026-09-01", to = "2026-09-13"),
        kpi = KpiStats(
            totalCustomers = 347,
            newCustomersPeriod = 89,
            revenue = 32340000.0,
            transactionsCount = 156,
            followupSent = 156,
            followupTotal = 178,
            queuedRate = 87.6,
            reachabilityRate = 76.2
        )
    )

    private val mockRetentionResponse = DashboardRetentionResponse(
        dateRange = DateRangeInfo(from = "2026-09-01", to = "2026-09-13"),
        kpi = KpiStats(
            activeCustomersCount = 3,
            repeatCustomersCount = 1,
            repeatPurchaseRate = 33.3,
            returningCustomersCount = 2,
            returningCustomerRate = 66.7
        ),
        bpjsRetention = SegmentRetentionItem("BPJS", 10, 5, 2, 40.0, 3, 60.0),
        nonBpjsRetention = SegmentRetentionItem("Reguler", 20, 10, 4, 40.0, 5, 50.0),
        beforeAfter = BeforeAfterRetentionItem(15.0, 33.3, 18.3)
    )

    private val mockSearchResponse = CustomerSearchResultResponse(
        total = 1,
        customers = listOf(
            CustomerSearchItem(
                id = 1,
                nama = "Siti Aminah",
                noHp = "081234567890",
                isProfileComplete = true
            )
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `loadStats updates statsState and retentionState with success data`() = runTest {
        `when`(repository.getDashboardStats(any(), any()))
            .thenReturn(Resource.Success(mockStatsResponse))
        `when`(repository.getDashboardRetention(any(), any()))
            .thenReturn(Resource.Success(mockRetentionResponse))
        `when`(repository.searchCustomersByDate(any(), any()))
            .thenReturn(Resource.Success(mockSearchResponse))

        viewModel = DashboardViewModel(repository)

        viewModel.statsState.test {
            val item = awaitItem()
            assertTrue(item is Resource.Success)
            val data = (item as Resource.Success).data
            assertEquals(347, data.kpi?.totalCustomers)
            assertEquals(32340000.0, data.kpi?.revenue ?: 0.0, 0.01)
            assertEquals(87.6, data.kpi?.queuedRate ?: 0.0, 0.01)
        }

        viewModel.retentionState.test {
            val item = awaitItem()
            assertTrue(item is Resource.Success)
            val data = (item as Resource.Success).data
            assertEquals(33.3, data.kpi?.repeatPurchaseRate ?: 0.0, 0.01)
            assertEquals("BPJS", data.bpjsRetention?.segment)
        }
    }

    @Test
    fun `searchCustomers updates searchState with result list`() = runTest {
        `when`(repository.getDashboardStats(any(), any()))
            .thenReturn(Resource.Success(mockStatsResponse))
        `when`(repository.getDashboardRetention(any(), any()))
            .thenReturn(Resource.Success(mockRetentionResponse))
        `when`(repository.searchCustomersByDate(any(), any()))
            .thenReturn(Resource.Success(mockSearchResponse))

        viewModel = DashboardViewModel(repository)

        viewModel.searchState.test {
            val item = awaitItem()
            assertTrue(item is Resource.Success)
            val data = (item as Resource.Success).data
            assertEquals(1, data.total)
            assertEquals("Siti Aminah", data.customers.first().nama)
        }
    }

    @Test
    fun `selectFilterToday sets today label and loads data`() = runTest {
        `when`(repository.getDashboardStats(any(), any()))
            .thenReturn(Resource.Success(mockStatsResponse))
        `when`(repository.getDashboardRetention(any(), any()))
            .thenReturn(Resource.Success(mockRetentionResponse))
        `when`(repository.searchCustomersByDate(any(), any()))
            .thenReturn(Resource.Success(mockSearchResponse))

        viewModel = DashboardViewModel(repository)
        viewModel.selectFilterToday()

        assertTrue(viewModel.dateRangeLabel.value.startsWith("Hari Ini"))
    }

    @Test
    fun `loadStats handles error correctly`() = runTest {
        `when`(repository.getDashboardStats(any(), any()))
            .thenReturn(Resource.Error("Network failure"))
        `when`(repository.getDashboardRetention(any(), any()))
            .thenReturn(Resource.Error("Network failure"))
        `when`(repository.searchCustomersByDate(any(), any()))
            .thenReturn(Resource.Success(mockSearchResponse))

        viewModel = DashboardViewModel(repository)

        viewModel.statsState.test {
            val item = awaitItem()
            assertTrue(item is Resource.Error)
            assertEquals("Network failure", (item as Resource.Error).message)
        }
    }
}
