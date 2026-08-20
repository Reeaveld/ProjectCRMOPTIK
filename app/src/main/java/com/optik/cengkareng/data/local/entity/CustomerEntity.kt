package com.optik.cengkareng.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val remoteId: Int? = null,

    val nama: String,

    @SerializedName("no_hp")
    val noHp: String? = null,

    @SerializedName("jenis_lensa")
    val jenisLensa: String = "General",

    @SerializedName("ukuran_kiri")
    val ukuranKiri: String = "0.00",

    @SerializedName("ukuran_kanan")
    val ukuranKanan: String = "0.00",

    @SerializedName("last_follow_up")
    val lastFollowUp: Long = 0,

    @SerializedName("is_profile_complete")
    val isProfileCompleteBackend: Boolean? = null,

    val isSynced: Boolean = false
) {
    /**
     * Evaluasi kelengkapan profil di client.
     * Mengembalikan true jika nomor HP terisi dan memenuhi format Indonesia.
     */
    fun isProfileComplete(): Boolean {
        if (isProfileCompleteBackend != null) {
            return isProfileCompleteBackend
        }
        val phone = noHp
        if (phone.isNullOrBlank()) return false
        // Defensive guard: dipertahankan untuk backward-compat dengan data lama di database
        if (phone.startsWith("BPJS-")) return false
        val cleaned = phone.replace(Regex("[^0-9+]"), "")
        return Regex("^(\\+62|62|0)8[0-9]{7,12}$").matches(cleaned)
    }
}