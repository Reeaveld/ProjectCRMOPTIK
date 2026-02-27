package com.optik.cengkareng.data.remote.request

import com.google.gson.annotations.SerializedName

// Ini adalah format JSON yang akan dikirim ke Laravel saat Edit / Tambah
data class CustomerRequest(
    val name: String,
    val phone: String,
    val address: String?,
    @SerializedName("jenis_lensa") // Sesuaikan jika ada kolom jenis lensa di backend Anda
    val jenisLensa: String? = null
)