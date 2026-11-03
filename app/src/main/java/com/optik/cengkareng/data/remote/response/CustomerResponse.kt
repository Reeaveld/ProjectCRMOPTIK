package com.optik.cengkareng.data.remote.response

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
    val lastFollowUp: String? = null,

    @SerializedName("is_profile_complete")
    val isProfileComplete: Boolean? = null,

    /** Dikembalikan server (BE-11) agar sync dapat merekonsiliasi baris outbox yang respons POST-nya hilang. */
    @SerializedName("client_uuid")
    val clientUuid: String? = null
) {
    /**
     * Konversi timestamp string aman (ISO-8601 atau SQL format) ke epoch millisecond.
     * Mencegah kegagalan parsing Gson jika server mengirimkan string datetime.
     */
    fun parseLastFollowUpEpoch(): Long {
        if (lastFollowUp.isNullOrBlank()) return 0L

        // Jika string berupa angka mentah
        lastFollowUp.toLongOrNull()?.let { return it }

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(lastFollowUp)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return 0L
    }
}
