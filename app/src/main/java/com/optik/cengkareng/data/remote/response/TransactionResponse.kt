package com.optik.cengkareng.data.remote.response

import com.google.gson.annotations.SerializedName

// 1. Wrapper utama karena Laravel me-return { "data": [ ... ] }
data class TransactionListResponse(
    @SerializedName("data")
    val data: List<TransactionItem>
)

// 2. Format JSON dari TransactionResource.php
data class TransactionItem(
    val id: Int,
    val invoice: String,

    @SerializedName("date_formatted")
    val dateFormatted: String,

    @SerializedName("amount_raw")
    val amountRaw: Double,

    @SerializedName("amount_formatted")
    val amountFormatted: String,

    @SerializedName("status_label")
    val statusLabel: String,

    @SerializedName("status_color")
    val statusColor: String,

    // Relasi ke resep lensa
    val prescriptions: List<PrescriptionItem>
)

// 3. Format JSON dari PrescriptionResource.php
data class PrescriptionItem(
    val id: Int,
    @SerializedName("side_code") val sideCode: String,
    @SerializedName("side_label") val sideLabel: String,
    val sph: String,
    val cyl: String,
    val axis: String,
    val add: String?, // Bisa null (misal jika bukan lensa progressive)
    val pd: String,
    @SerializedName("lens_type") val lensType: String?
)