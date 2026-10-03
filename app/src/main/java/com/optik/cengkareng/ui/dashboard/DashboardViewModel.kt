package com.optik.cengkareng.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.model.CustomerSearchResultResponse
import com.optik.cengkareng.data.remote.model.DashboardStatsResponse
import com.optik.cengkareng.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    private val _statsState = MutableStateFlow<Resource<DashboardStatsResponse>>(Resource.Loading)
    val statsState: StateFlow<Resource<DashboardStatsResponse>> = _statsState.asStateFlow()

    private val _searchState = MutableStateFlow<Resource<CustomerSearchResultResponse>>(Resource.Success(CustomerSearchResultResponse()))
    val searchState: StateFlow<Resource<CustomerSearchResultResponse>> = _searchState.asStateFlow()

    // Current Date Range for Stats
    private val _selectedDateRange = MutableStateFlow(getDefaultThisMonthRange())
    val selectedDateRange: StateFlow<Pair<String, String>> = _selectedDateRange.asStateFlow()

    private val _dateRangeLabel = MutableStateFlow(getDefaultThisMonthLabel())
    val dateRangeLabel: StateFlow<String> = _dateRangeLabel.asStateFlow()

    // Current Date Range for Search
    private val _searchDateRange = MutableStateFlow(getTodayRange())
    val searchDateRange: StateFlow<Pair<String, String>> = _searchDateRange.asStateFlow()

    private val _searchDateLabel = MutableStateFlow("Hari Ini (${displayDateFormat.format(Date())})")
    val searchDateLabel: StateFlow<String> = _searchDateLabel.asStateFlow()

    init {
        loadStatsWithCurrentRange()
        searchWithCurrentRange()
    }

    fun loadStats(from: String, to: String, label: String? = null) {
        _selectedDateRange.value = Pair(from, to)
        if (label != null) {
            _dateRangeLabel.value = label
        } else {
            try {
                val dFrom = apiDateFormat.parse(from)
                val dTo = apiDateFormat.parse(to)
                if (dFrom != null && dTo != null) {
                    _dateRangeLabel.value = "${displayDateFormat.format(dFrom)} — ${displayDateFormat.format(dTo)}"
                }
            } catch (e: Exception) {
                _dateRangeLabel.value = "$from — $to"
            }
        }

        viewModelScope.launch {
            _statsState.value = Resource.Loading
            val result = repository.getDashboardStats(from, to)
            _statsState.value = result
        }
    }

    fun loadStatsWithCurrentRange() {
        val (from, to) = _selectedDateRange.value
        loadStats(from, to, _dateRangeLabel.value)
    }

    fun selectFilterToday() {
        val today = apiDateFormat.format(Date())
        val label = "Hari Ini (${displayDateFormat.format(Date())})"
        loadStats(today, today, label)
    }

    fun selectFilter7Days() {
        val cal = Calendar.getInstance()
        val to = apiDateFormat.format(cal.time)
        val dTo = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val from = apiDateFormat.format(cal.time)
        val dFrom = cal.time
        val label = "7 Hari: ${displayDateFormat.format(dFrom)} — ${displayDateFormat.format(dTo)}"
        loadStats(from, to, label)
    }

    fun selectFilterThisMonth() {
        val (from, to) = getDefaultThisMonthRange()
        loadStats(from, to, getDefaultThisMonthLabel())
    }

    fun selectFilterThisYear() {
        val cal = Calendar.getInstance()
        val to = apiDateFormat.format(cal.time)
        cal.set(Calendar.DAY_OF_YEAR, 1)
        val from = apiDateFormat.format(cal.time)
        val label = "Tahun ${cal.get(Calendar.YEAR)} (${displayDateFormat.format(cal.time)} — ${displayDateFormat.format(Date())})"
        loadStats(from, to, label)
    }

    fun selectCustomRange(from: String, to: String, label: String) {
        loadStats(from, to, label)
    }

    fun searchCustomers(from: String, to: String, label: String? = null) {
        _searchDateRange.value = Pair(from, to)
        if (label != null) {
            _searchDateLabel.value = label
        }

        viewModelScope.launch {
            _searchState.value = Resource.Loading
            val result = repository.searchCustomersByDate(from, to)
            _searchState.value = result
        }
    }

    fun searchWithCurrentRange() {
        val (from, to) = _searchDateRange.value
        searchCustomers(from, to, _searchDateLabel.value)
    }

    private fun getDefaultThisMonthRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val to = apiDateFormat.format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val from = apiDateFormat.format(cal.time)
        return Pair(from, to)
    }

    private fun getDefaultThisMonthLabel(): String {
        val cal = Calendar.getInstance()
        val dTo = cal.time
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val dFrom = cal.time
        return "${displayDateFormat.format(dFrom)} — ${displayDateFormat.format(dTo)}"
    }

    private fun getTodayRange(): Pair<String, String> {
        val today = apiDateFormat.format(Date())
        return Pair(today, today)
    }
}
