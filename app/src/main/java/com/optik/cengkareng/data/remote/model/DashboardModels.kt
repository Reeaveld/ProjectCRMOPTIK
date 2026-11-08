package com.optik.cengkareng.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Model data respons API Dashboard Analytics
 */
data class DashboardStatsResponse(
    @SerializedName("date_range")
    val dateRange: DateRangeInfo? = null,

    @SerializedName("kpi")
    val kpi: KpiStats? = null,

    @SerializedName("revenue_trend")
    val revenueTrend: List<RevenueTrendItem> = emptyList(),

    @SerializedName("followup_distribution")
    val followupDistribution: List<FollowUpDistributionItem> = emptyList(),

    @SerializedName("customer_growth")
    val customerGrowth: List<CustomerGrowthItem> = emptyList(),

    @SerializedName("lens_distribution")
    val lensDistribution: List<LensDistributionItem> = emptyList(),

    @SerializedName("refraction_distribution")
    val refractionDistribution: List<RefractionDistributionItem> = emptyList()
)

data class DateRangeInfo(
    @SerializedName("from")
    val from: String,

    @SerializedName("to")
    val to: String,

    @SerializedName("prev_from")
    val prevFrom: String? = null,

    @SerializedName("prev_to")
    val prevTo: String? = null
)

data class KpiStats(
    @SerializedName("total_customers")
    val totalCustomers: Int = 0,

    @SerializedName("new_customers_period")
    val newCustomersPeriod: Int = 0,

    @SerializedName("new_customers_prev")
    val newCustomersPrev: Int = 0,

    @SerializedName("revenue")
    val revenue: Double = 0.0,

    @SerializedName("revenue_prev")
    val revenuePrev: Double = 0.0,

    @SerializedName("transactions_count")
    val transactionsCount: Int = 0,

    @SerializedName("transactions_count_prev")
    val transactionsCountPrev: Int = 0,

    @SerializedName("followup_sent")
    val followupSent: Int = 0,

    @SerializedName("followup_total")
    val followupTotal: Int = 0,

    /** sent / (sent + failed) jadwal jatuh tempo. "sent" = diterima antrean WhatsApp (bukan terkirim). null = belum ada data. */
    @SerializedName("queued_rate")
    val queuedRate: Double? = null,

    @SerializedName("followup_failed")
    val followupFailed: Int = 0,

    @SerializedName("followup_pending")
    val followupPending: Int = 0,

    @SerializedName("followup_blocked")
    val followupBlocked: Int = 0,

    @SerializedName("followup_simulated")
    val followupSimulated: Int = 0,

    @SerializedName("complete_profiles_count")
    val completeProfilesCount: Int = 0,

    @SerializedName("incomplete_profiles_count")
    val incompleteProfilesCount: Int = 0,

    @SerializedName("reachability_rate")
    val reachabilityRate: Double = 0.0,

    // Rumusan Masalah 3 (rentang yang dipilih). null = tidak ada pelanggan aktif pada rentang.
    @SerializedName("active_customers_count")
    val activeCustomersCount: Int = 0,

    @SerializedName("repeat_customers_count")
    val repeatCustomersCount: Int = 0,

    @SerializedName("repeat_purchase_rate")
    val repeatPurchaseRate: Double? = null,

    @SerializedName("returning_customers_count")
    val returningCustomersCount: Int = 0,

    @SerializedName("returning_customer_rate")
    val returningCustomerRate: Double? = null
)

data class RevenueTrendItem(
    @SerializedName("period")
    val period: String,

    @SerializedName("bpjs")
    val bpjs: Double = 0.0,

    @SerializedName("non_bpjs")
    val nonBpjs: Double = 0.0,

    @SerializedName("total")
    val total: Double = 0.0
)

data class FollowUpDistributionItem(
    @SerializedName("status")
    val status: String,

    @SerializedName("label")
    val label: String,

    @SerializedName("count")
    val count: Int = 0
)

data class CustomerGrowthItem(
    @SerializedName("period")
    val period: String,

    @SerializedName("count")
    val count: Int = 0
)

data class LensDistributionItem(
    @SerializedName("type")
    val type: String,

    @SerializedName("count")
    val count: Int = 0,

    @SerializedName("percentage")
    val percentage: Double = 0.0
)

data class RefractionDistributionItem(
    @SerializedName("category")
    val category: String,

    @SerializedName("count")
    val count: Int = 0,

    @SerializedName("percentage")
    val percentage: Double = 0.0
)

/**
 * Model data respons Pencarian Pelanggan berdasarkan Tanggal
 */
data class CustomerSearchResultResponse(
    @SerializedName("date_range")
    val dateRange: DateRangeInfo? = null,

    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("customers")
    val customers: List<CustomerSearchItem> = emptyList()
)

data class CustomerSearchItem(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nama")
    val nama: String,

    @SerializedName("no_hp")
    val noHp: String? = null,

    @SerializedName("jenis_lensa")
    val jenisLensa: String? = null,

    @SerializedName("ukuran_kiri")
    val ukuranKiri: String? = null,

    @SerializedName("ukuran_kanan")
    val ukuranKanan: String? = null,

    @SerializedName("is_profile_complete")
    val isProfileComplete: Boolean = false,

    @SerializedName("transactions")
    val transactions: List<SearchTransactionItem> = emptyList()
)

data class SearchTransactionItem(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("customer_id")
    val customerId: Int = 0,

    @SerializedName("invoice_number")
    val invoiceNumber: String = "",

    @SerializedName("amount")
    val amount: Double = 0.0,

    @SerializedName("status")
    val status: String = "",

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("transaction_date")
    val transactionDate: String = "",

    @SerializedName("prescriptions")
    val prescriptions: List<SearchPrescriptionItem> = emptyList()
)

data class SearchPrescriptionItem(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("eye_side")
    val eyeSide: String = "",

    @SerializedName("sphere")
    val sphere: Double = 0.0,

    @SerializedName("cylinder")
    val cylinder: Double? = null,

    @SerializedName("axis")
    val axis: Int? = null,

    @SerializedName("addition")
    val addition: Double? = null,

    @SerializedName("pd")
    val pd: Double? = null,

    @SerializedName("lens_type")
    val lensType: String? = null
)

/**
 * Model data respons API Dashboard Retention Analytics (/api/dashboard/retention)
 * Sesuai dengan output controller Laravel DashboardController@retention
 */
data class DashboardRetentionResponse(
    @SerializedName("date_range")
    val dateRange: DateRangeInfo? = null,

    @SerializedName("purchase")
    val purchase: PurchaseMetricsGroup? = null,

    @SerializedName("followup_queue")
    val followupQueue: FollowupQueueMetrics? = null,

    @SerializedName("followup_return")
    val followupReturn: FollowupReturnMetrics? = null,

    @SerializedName("before_after")
    val beforeAfter: Map<String, BeforeAfterSegment>? = null,

    @SerializedName("engagement")
    val engagement: EngagementMetrics? = null,

    @SerializedName("notes")
    val notes: List<String> = emptyList()
)

data class PurchaseMetricsGroup(
    @SerializedName("all")
    val all: PurchaseSegmentMetrics? = null,

    @SerializedName("bpjs")
    val bpjs: PurchaseSegmentMetrics? = null,

    @SerializedName("non_bpjs")
    val nonBpjs: PurchaseSegmentMetrics? = null
)

data class PurchaseSegmentMetrics(
    @SerializedName("active_customers")
    val activeCustomers: Int = 0,

    @SerializedName("repeat_customers")
    val repeatCustomers: Int = 0,

    @SerializedName("returning_customers")
    val returningCustomers: Int = 0,

    @SerializedName("repeat_purchase_rate")
    val repeatPurchaseRate: Double? = null,

    @SerializedName("returning_customer_rate")
    val returningCustomerRate: Double? = null
)

data class FollowupQueueMetrics(
    @SerializedName("sent")
    val sent: Int = 0,

    @SerializedName("sent_simulated")
    val sentSimulated: Int = 0,

    @SerializedName("failed")
    val failed: Int = 0,

    @SerializedName("pending")
    val pending: Int = 0,

    @SerializedName("blocked")
    val blocked: Int = 0,

    @SerializedName("cancelled")
    val cancelled: Int = 0,

    @SerializedName("queued_rate")
    val queuedRate: Double? = null,

    @SerializedName("delivered")
    val delivered: Int = 0,

    @SerializedName("delivered_rate")
    val deliveredRate: Double? = null,

    @SerializedName("delivery_tracking")
    val deliveryTracking: Boolean = false
)

data class FollowupReturnMetrics(
    @SerializedName("return_days")
    val returnDays: Int = 30,

    @SerializedName("treated")
    val treated: ReturnCohortMetrics? = null,

    @SerializedName("comparison")
    val comparison: ReturnCohortMetrics? = null,

    @SerializedName("caveat")
    val caveat: String? = null
)

data class ReturnCohortMetrics(
    @SerializedName("cohort_size")
    val cohortSize: Int = 0,

    @SerializedName("returned")
    val returned: Int = 0,

    @SerializedName("return_rate")
    val returnRate: Double? = null,

    @SerializedName("immature_excluded")
    val immatureExcluded: Int = 0
)

data class BeforeAfterSegment(
    @SerializedName("go_live_date")
    val goLiveDate: String? = null,

    @SerializedName("window_days")
    val windowDays: Int = 90,

    @SerializedName("segment")
    val segment: String = "all",

    @SerializedName("before")
    val before: BeforeAfterWindowMetrics? = null,

    @SerializedName("after")
    val after: BeforeAfterWindowMetrics? = null,

    @SerializedName("baseline_available")
    val baselineAvailable: Boolean = false,

    @SerializedName("after_window_complete")
    val afterWindowComplete: Boolean = false
)

data class BeforeAfterWindowMetrics(
    @SerializedName("from")
    val from: String = "",

    @SerializedName("to")
    val to: String = "",

    @SerializedName("transactions")
    val transactions: Int = 0,

    @SerializedName("active_customers")
    val activeCustomers: Int = 0,

    @SerializedName("repeat_customers")
    val repeatCustomers: Int = 0,

    @SerializedName("returning_customers")
    val returningCustomers: Int = 0,

    @SerializedName("repeat_purchase_rate")
    val repeatPurchaseRate: Double? = null,

    @SerializedName("returning_customer_rate")
    val returningCustomerRate: Double? = null
)

data class EngagementMetrics(
    @SerializedName("available")
    val available: Boolean = false,

    @SerializedName("reason")
    val reason: String? = null
)
