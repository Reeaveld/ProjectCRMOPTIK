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

    // PERBAIKAN DI SINI:
    // Gunakan @SerializedName agar dikirim sebagai "no_hp" ke server
    @SerializedName("no_hp")
    val noHp: String,

    @SerializedName("jenis_lensa")
    val jenisLensa: String = "General",

    @SerializedName("ukuran_kiri")
    val ukuranKiri: String = "0.00",

    @SerializedName("ukuran_kanan")
    val ukuranKanan: String = "0.00",

    @SerializedName("last_follow_up")
    val lastFollowUp: Long = 0,

    val isSynced: Boolean = false
)