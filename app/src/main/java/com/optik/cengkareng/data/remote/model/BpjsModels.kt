package com.optik.cengkareng.data.remote.model

import com.google.gson.annotations.SerializedName

data class BpjsParseResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("metadata") val metadata: BpjsMetadata?,
    @SerializedName("items") val items: List<BpjsItemPreview>?
)

data class BpjsMetadata(
    @SerializedName("nomor_fpk") val nomorFpk: String?,
    @SerializedName("provider") val provider: String?,
    @SerializedName("bulan_pelayanan") val bulanPelayanan: String?,
    @SerializedName("total_data_header") val totalDataHeader: Int?,
    @SerializedName("total_tagihan_header") val totalTagihanHeader: Double?,
    @SerializedName("total_parsed") val totalParsed: Int?,
    @SerializedName("total_amount_parsed") val totalAmountParsed: Double?,
    @SerializedName("duplicate_count") val duplicateCount: Int?,
    @SerializedName("is_verified") val isVerified: Boolean?
)

data class BpjsItemPreview(
    @SerializedName("temp_id") val tempId: Int,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("transaction_date") val transactionDate: String,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("claim_type") val claimType: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("phone") var phone: String?,
    @SerializedName("suggested_phone") val suggestedPhone: String?,
    @SerializedName("is_duplicate") val isDuplicate: Boolean,
    @SerializedName("is_existing_customer") val isExistingCustomer: Boolean,
    @SerializedName("existing_customer_id") val existingCustomerId: Int?,
    @SerializedName("selected") var selected: Boolean
)

data class BpjsCommitRequest(
    @SerializedName("nomor_fpk") val nomorFpk: String?,
    @SerializedName("items") val items: List<BpjsCommitItem>
)

data class BpjsCommitItem(
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("transaction_date") val transactionDate: String,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("phone") val phone: String?
)

data class BpjsCommitResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("imported_count") val importedCount: Int?,
    @SerializedName("customers_created") val customersCreated: Int?,
    @SerializedName("customers_updated") val customersUpdated: Int?,
    @SerializedName("schedules_pending") val schedulesPending: Int?,
    @SerializedName("schedules_blocked") val schedulesBlocked: Int?
)
