package com.optik.cengkareng.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.optik.cengkareng.R
import com.optik.cengkareng.core.utils.ExportHelper
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.model.CustomerSearchItem
import com.optik.cengkareng.data.remote.model.DashboardStatsResponse
import com.optik.cengkareng.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var customerAdapter: DashboardCustomerAdapter

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    private var currentSearchCustomers: List<CustomerSearchItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        customerAdapter = DashboardCustomerAdapter { customer ->
            val action = DashboardFragmentDirections.actionDashboardFragmentToCustomerDetailFragment(customer.id)
            findNavController().navigate(action)
        }
        binding.rvSearchCustomers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = customerAdapter
        }
    }

    private fun setupListeners() {
        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadStatsWithCurrentRange()
            viewModel.searchWithCurrentRange()
        }

        // Header Date Range Card click
        binding.cardDateRange.setOnClickListener {
            showDateRangePickerForStats()
        }

        // Quick filter chips
        binding.chipToday.setOnClickListener { viewModel.selectFilterToday() }
        binding.chip7Days.setOnClickListener { viewModel.selectFilter7Days() }
        binding.chipThisMonth.setOnClickListener { viewModel.selectFilterThisMonth() }
        binding.chipThisYear.setOnClickListener { viewModel.selectFilterThisYear() }
        binding.chipCustom.setOnClickListener { showDateRangePickerForStats() }

        // Search date selector button
        binding.btnSearchDateSelector.setOnClickListener {
            showDateRangePickerForSearch()
        }

        binding.btnDoSearch.setOnClickListener {
            viewModel.searchWithCurrentRange()
        }

        // Export Actions
        binding.btnExportCsv.setOnClickListener {
            exportData(isPdf = false)
        }

        binding.btnExportPdf.setOnClickListener {
            exportData(isPdf = true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe date range label
                launch {
                    viewModel.dateRangeLabel.collect { label ->
                        binding.tvDateRangeText.text = label
                    }
                }

                // Observe search date label
                launch {
                    viewModel.searchDateLabel.collect { label ->
                        binding.btnSearchDateSelector.text = label
                    }
                }

                // Observe stats state
                launch {
                    viewModel.statsState.collect { state ->
                        when (state) {
                            is Resource.Loading -> {
                                binding.swipeRefresh.isRefreshing = true
                            }
                            is Resource.Success -> {
                                binding.swipeRefresh.isRefreshing = false
                                renderStats(state.data)
                            }
                            is Resource.Error -> {
                                binding.swipeRefresh.isRefreshing = false
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // Observe search state
                launch {
                    viewModel.searchState.collect { state ->
                        when (state) {
                            is Resource.Loading -> {
                                binding.progressSearchLoading.visibility = View.VISIBLE
                            }
                            is Resource.Success -> {
                                binding.progressSearchLoading.visibility = View.GONE
                                currentSearchCustomers = state.data.customers
                                customerAdapter.submitList(currentSearchCustomers)

                                val total = state.data.total
                                binding.tvSearchResultSummary.text = "Hasil: $total transaksi pelanggan ditemukan"

                                if (total == 0) {
                                    binding.layoutEmptySearch.visibility = View.VISIBLE
                                    binding.rvSearchCustomers.visibility = View.GONE
                                    binding.layoutExportActions.visibility = View.GONE
                                } else {
                                    binding.layoutEmptySearch.visibility = View.GONE
                                    binding.rvSearchCustomers.visibility = View.VISIBLE
                                    binding.layoutExportActions.visibility = View.VISIBLE
                                }
                            }
                            is Resource.Error -> {
                                binding.progressSearchLoading.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderStats(data: DashboardStatsResponse) {
        val kpi = data.kpi

        if (kpi != null) {
            // KPI 1: Total Pelanggan
            binding.tvKpiTotalCustomers.text = kpi.totalCustomers.toString()
            binding.tvKpiNewCustomersDiff.text = "+${kpi.newCustomersPeriod} baru periode ini"

            // KPI 2: Transaksi
            binding.tvKpiTransactions.text = kpi.transactionsCount.toString()
            val trxDiff = kpi.transactionsCount - kpi.transactionsCountPrev
            binding.tvKpiTransactionsDiff.text = if (trxDiff >= 0) "+$trxDiff vs periode lalu" else "$trxDiff vs periode lalu"

            // KPI 3: Revenue
            binding.tvKpiRevenue.text = rupiahFormat.format(kpi.revenue)
            val revDiff = if (kpi.revenuePrev > 0) {
                val pct = ((kpi.revenue - kpi.revenuePrev) / kpi.revenuePrev) * 100
                String.format(Locale.US, "%+.1f%% vs lalu", pct)
            } else {
                "+100% vs lalu"
            }
            binding.tvKpiRevenueDiff.text = revDiff

            // KPI 4: Follow-Up
            binding.tvKpiFollowUpSent.text = "${kpi.followupSent} / ${kpi.followupTotal}"
            binding.tvKpiFollowUpRate.text = "Rate: ${kpi.deliveryRate}% Terkirim"

            // Reachability Card
            binding.progressReachability.progress = kpi.reachabilityRate.toInt()
            binding.tvReachabilityPercentage.text = "${kpi.reachabilityRate}%"
            binding.tvReachabilityDetail.text =
                "${kpi.completeProfilesCount} dari ${kpi.totalCustomers} pelanggan memiliki nomor WhatsApp valid (siap follow-up otomatis)."
        }

        // Render Charts
        renderRevenueTrendChart(data)
        renderFollowUpDonutChart(data)
        renderCustomerGrowthChart(data)
        renderOpticalInsights(data)
    }

    private fun renderRevenueTrendChart(data: DashboardStatsResponse) {
        val chart = binding.chartRevenue
        val trend = data.revenueTrend

        if (trend.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Belum ada data pendapatan pada periode ini")
            chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            chart.invalidate()
            return
        }

        val entriesBpjs = ArrayList<Entry>()
        val entriesNonBpjs = ArrayList<Entry>()
        val xLabels = ArrayList<String>()

        for (i in trend.indices) {
            val item = trend[i]
            entriesBpjs.add(Entry(i.toFloat(), item.bpjs.toFloat()))
            entriesNonBpjs.add(Entry(i.toFloat(), item.nonBpjs.toFloat()))

            // Format label X-Axis (misal "2026-09" jadi "Sep 26" atau "13 Sep")
            val label = formatPeriodLabel(item.period)
            xLabels.add(label)
        }

        val dsBpjs = LineDataSet(entriesBpjs, "BPJS").apply {
            color = ContextCompat.getColor(requireContext(), R.color.chart_bpjs)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.chart_bpjs))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(false)
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.accent_green_light)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
        }

        val dsNonBpjs = LineDataSet(entriesNonBpjs, "Reguler").apply {
            color = ContextCompat.getColor(requireContext(), R.color.chart_non_bpjs)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.chart_non_bpjs))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(false)
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.accent_purple_light)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
        }

        val lineData = LineData(dsBpjs, dsNonBpjs)
        chart.data = lineData

        // Chart styling
        chart.description.isEnabled = false
        chart.legend.isEnabled = false // Custom legend used in layout
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.axisRight.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        xAxis.textSize = 10f
        xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        xAxis.granularity = 1f

        val yAxis = chart.axisLeft
        yAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        yAxis.textSize = 10f
        yAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.border_color)
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value >= 1_000_000) {
                    String.format(Locale.US, "%.1fjt", value / 1_000_000)
                } else if (value >= 1_000) {
                    String.format(Locale.US, "%.0frb", value / 1_000)
                } else {
                    value.toInt().toString()
                }
            }
        }

        chart.animateX(700)
        chart.invalidate()
    }

    private fun renderFollowUpDonutChart(data: DashboardStatsResponse) {
        val chart = binding.chartFollowUp
        val dist = data.followupDistribution

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        var totalFollowUps = 0

        for (item in dist) {
            if (item.count > 0) {
                entries.add(PieEntry(item.count.toFloat(), item.label))
                val color = when (item.status) {
                    "sent" -> ContextCompat.getColor(requireContext(), R.color.accent_green)
                    "pending" -> ContextCompat.getColor(requireContext(), R.color.accent_amber)
                    "blocked_incomplete_profile" -> ContextCompat.getColor(requireContext(), R.color.text_muted)
                    else -> ContextCompat.getColor(requireContext(), R.color.accent_red)
                }
                colors.add(color)
            }
            totalFollowUps += item.count
        }

        if (entries.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Belum ada jadwal follow-up pada periode ini")
            chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            chart.invalidate()
            renderFollowUpLegend(dist, 0)
            return
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(chart)
        }

        val pieData = PieData(dataSet)
        chart.data = pieData
        chart.setUsePercentValues(true)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.isDrawHoleEnabled = true
        chart.setHoleColor(Color.TRANSPARENT)
        chart.holeRadius = 60f
        chart.transparentCircleRadius = 65f
        chart.setDrawCenterText(true)
        chart.centerText = "Total\n$totalFollowUps"
        chart.setCenterTextSize(14f)
        chart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))

        chart.animateY(800)
        chart.invalidate()

        renderFollowUpLegend(dist, totalFollowUps)
    }

    private fun renderFollowUpLegend(dist: List<com.optik.cengkareng.data.remote.model.FollowUpDistributionItem>, total: Int) {
        val container = binding.layoutFollowUpLegend
        container.removeAllViews()

        for (item in dist) {
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_dashboard_customer, container, false)
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
            }

            val colorBox = View(requireContext()).apply {
                val size = (resources.displayMetrics.density * 10).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(0, (resources.displayMetrics.density * 4).toInt(), (resources.displayMetrics.density * 8).toInt(), 0)
                }
                val c = when (item.status) {
                    "sent" -> ContextCompat.getColor(requireContext(), R.color.accent_green)
                    "pending" -> ContextCompat.getColor(requireContext(), R.color.accent_amber)
                    "blocked_incomplete_profile" -> ContextCompat.getColor(requireContext(), R.color.text_muted)
                    else -> ContextCompat.getColor(requireContext(), R.color.accent_red)
                }
                setBackgroundColor(c)
            }

            val label = TextView(requireContext()).apply {
                text = item.label
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val pct = if (total > 0) String.format(Locale.US, " (%.1f%%)", (item.count.toDouble() / total) * 100) else ""
            val count = TextView(requireContext()).apply {
                text = "${item.count} pesan$pct"
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }

            row.addView(colorBox)
            row.addView(label)
            row.addView(count)
            container.addView(row)
        }
    }

    private fun renderCustomerGrowthChart(data: DashboardStatsResponse) {
        val chart = binding.chartCustomerGrowth
        val growth = data.customerGrowth

        if (growth.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Belum ada data pendaftaran pelanggan")
            chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            chart.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        val xLabels = ArrayList<String>()

        for (i in growth.indices) {
            val item = growth[i]
            entries.add(BarEntry(i.toFloat(), item.count.toFloat()))
            xLabels.add(formatPeriodLabel(item.period))
        }

        val dataSet = BarDataSet(entries, "Pelanggan Baru").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_brand)
            valueTextSize = 10f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f
        chart.data = barData

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)
        chart.axisRight.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        xAxis.textSize = 10f
        xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        xAxis.granularity = 1f

        val yAxis = chart.axisLeft
        yAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        yAxis.textSize = 10f
        yAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.border_color)
        yAxis.granularity = 1f

        chart.animateY(700)
        chart.invalidate()
    }

    private fun renderOpticalInsights(data: DashboardStatsResponse) {
        // 1. Lens Distribution
        val lensContainer = binding.layoutLensInsights
        lensContainer.removeAllViews()

        for (item in data.lensDistribution) {
            val row = createInsightBar(item.type, item.count, item.percentage, ContextCompat.getColor(requireContext(), R.color.primary_brand))
            lensContainer.addView(row)
        }

        if (data.lensDistribution.isEmpty()) {
            val tvEmpty = TextView(requireContext()).apply {
                text = "Belum ada data lensa pada periode ini"
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            }
            lensContainer.addView(tvEmpty)
        }

        // 2. Refraction Distribution
        val refContainer = binding.layoutRefractionInsights
        refContainer.removeAllViews()

        for (item in data.refractionDistribution) {
            val row = createInsightBar(item.category, item.count, item.percentage, ContextCompat.getColor(requireContext(), R.color.accent_blue))
            refContainer.addView(row)
        }
    }

    private fun createInsightBar(label: String, count: Int, percentage: Double, barColor: Int): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 6, 0, 8)
        }

        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val tvLabel = TextView(requireContext()).apply {
            text = label
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvValue = TextView(requireContext()).apply {
            text = "$count (${percentage}%)"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        header.addView(tvLabel)
        header.addView(tvValue)

        val progress = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            this.progress = percentage.toInt()
            max = 100
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (resources.displayMetrics.density * 6).toInt()).apply {
                topMargin = (resources.displayMetrics.density * 4).toInt()
            }
            progressDrawable.setTint(barColor)
        }

        layout.addView(header)
        layout.addView(progress)
        return layout
    }

    private fun showDateRangePickerForStats() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih Rentang Analisis")
            .build()

        picker.addOnPositiveButtonClickListener { range ->
            if (range.first != null && range.second != null) {
                val fromDate = Date(range.first)
                val toDate = Date(range.second)

                // Adjust for UTC offset in DatePicker
                val calFrom = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = fromDate }
                val calTo = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = toDate }

                val fromStr = apiDateFormat.format(calFrom.time)
                val toStr = apiDateFormat.format(calTo.time)
                val label = "${displayDateFormat.format(calFrom.time)} — ${displayDateFormat.format(calTo.time)}"

                viewModel.selectCustomRange(fromStr, toStr, label)
            }
        }

        picker.show(parentFragmentManager, "STATS_DATE_PICKER")
    }

    private fun showDateRangePickerForSearch() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih Rentang Tanggal Transaksi")
            .build()

        picker.addOnPositiveButtonClickListener { range ->
            if (range.first != null && range.second != null) {
                val fromDate = Date(range.first)
                val toDate = Date(range.second)

                val calFrom = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = fromDate }
                val calTo = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = toDate }

                val fromStr = apiDateFormat.format(calFrom.time)
                val toStr = apiDateFormat.format(calTo.time)
                val label = "${displayDateFormat.format(calFrom.time)} — ${displayDateFormat.format(calTo.time)}"

                viewModel.searchCustomers(fromStr, toStr, label)
            }
        }

        picker.show(parentFragmentManager, "SEARCH_DATE_PICKER")
    }

    private fun exportData(isPdf: Boolean) {
        if (currentSearchCustomers.isEmpty()) {
            Toast.makeText(requireContext(), "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        val dateLabel = viewModel.searchDateLabel.value
        if (isPdf) {
            val file = ExportHelper.exportToPdf(requireContext(), currentSearchCustomers, dateLabel)
            if (file != null) {
                Toast.makeText(requireContext(), "PDF berhasil dibuat", Toast.LENGTH_SHORT).show()
                ExportHelper.shareFile(requireContext(), file, "application/pdf", "Laporan PDF Optik CRM")
            } else {
                Toast.makeText(requireContext(), "Gagal membuat PDF", Toast.LENGTH_SHORT).show()
            }
        } else {
            val file = ExportHelper.exportToCsv(requireContext(), currentSearchCustomers, dateLabel)
            if (file != null) {
                Toast.makeText(requireContext(), "CSV berhasil dibuat", Toast.LENGTH_SHORT).show()
                ExportHelper.shareFile(requireContext(), file, "text/csv", "Laporan CSV Optik CRM")
            } else {
                Toast.makeText(requireContext(), "Gagal membuat CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatPeriodLabel(period: String): String {
        return try {
            if (period.length == 7) { // "2026-09"
                val date = SimpleDateFormat("yyyy-MM", Locale.US).parse(period)
                if (date != null) SimpleDateFormat("MMM yy", Locale("id", "ID")).format(date) else period
            } else if (period.length == 10) { // "2026-09-13"
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(period)
                if (date != null) SimpleDateFormat("dd MMM", Locale("id", "ID")).format(date) else period
            } else {
                period
            }
        } catch (e: Exception) {
            period
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
