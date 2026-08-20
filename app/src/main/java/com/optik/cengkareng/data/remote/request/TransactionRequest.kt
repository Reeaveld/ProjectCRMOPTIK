package com.optik.cengkareng.data.remote.request

import com.google.gson.annotations.SerializedName

/**
 * Request body untuk POST /api/transactions
 * Mendukung header transaksi dan daftar resep lensa (OD & OS).
 */
data class TransactionRequest(
    @SerializedName("customer_id")
    val customerId: Int,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("transaction_date")
    val transactionDate: String,

    @SerializedName("prescriptions")
    val prescriptions: List<PrescriptionRequest>
)

data class PrescriptionRequest(
    @SerializedName("eye_side")
    val eyeSide: String, // "OD" atau "OS"

    @SerializedName("sphere")
    val sphere: Double,

    @SerializedName("cylinder")
    val cylinder: Double = 0.0,

    @SerializedName("axis")
    val axis: Int = 0,

    @SerializedName("lens_type")
    val lensType: String
)
