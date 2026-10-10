package com.optik.cengkareng.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * DTO Model untuk deserialisasi respons JSON pelanggan dari server Laravel.
 * Memisahkan kontrak API (remote ID) dari primary key lokal Room SQLite.
 */
data class CustomerResponse(
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

    @SerializedName("last_follow_up")
    val lastFollowUp: Long? = null,

    @SerializedName("is_profile_complete")
    val isProfileComplete: Boolean? = null
)
