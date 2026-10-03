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

    @SerializedName("delivery_rate")
    val deliveryRate: Double = 0.0,

    @SerializedName("complete_profiles_count")
    val completeProfilesCount: Int = 0,

    @SerializedName("incomplete_profiles_count")
    val incompleteProfilesCount: Int = 0,

    @SerializedName("reachability_rate")
    val reachabilityRate: Double = 0.0
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
