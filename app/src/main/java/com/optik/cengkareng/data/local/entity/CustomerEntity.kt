package com.optik.cengkareng.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["clientUuid"], unique = true)
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val remoteId: Int? = null,

    /**
     * Kunci idempoten untuk POST /customers (outbox). Dibuat saat baris lokal dibuat;
     * null untuk baris yang berasal dari server. Server mengembalikan baris yang sama
     * bila client_uuid yang sama dikirim ulang, sehingga respons yang hilang tidak
     * menghasilkan duplikat.
     */
    val clientUuid: String? = null,

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

    val isSynced: Boolean = false,

    /**
     * Alasan penolakan server (mis. 422) untuk baris yang belum terkirim. Baris dengan
     * syncError tidak dikirim ulang otomatis; pengguna harus memperbaikinya.
     */
    val syncError: String? = null
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
