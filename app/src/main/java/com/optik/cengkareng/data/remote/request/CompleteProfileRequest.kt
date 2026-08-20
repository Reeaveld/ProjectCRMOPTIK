package com.optik.cengkareng.data.remote.request

import com.google.gson.annotations.SerializedName

/**
 * DTO Request untuk melengkapi nomor HP customer.
 * Dikirim ke PATCH /api/customers/{id}/complete-profile
 */
data class CompleteProfileRequest(
    @SerializedName("no_hp")
    val noHp: String
)
