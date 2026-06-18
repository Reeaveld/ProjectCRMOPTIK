package com.optik.cengkareng.data.remote.request

import com.google.gson.annotations.SerializedName

/**
 * Format JSON yang dikirim ke Laravel saat Update / Tambah Pelanggan.
 *
 * Penting: nama field di JSON harus IDENTIK dengan kolom tabel `customers`
 * di Laravel (nama, no_hp, jenis_lensa, ukuran_kiri, ukuran_kanan).
 * Field di Kotlin tetap pakai camelCase, mapping dilakukan oleh @SerializedName.
 */
data class CustomerRequest(
    @SerializedName("nama")
    val nama: String,

    @SerializedName("no_hp")
    val noHp: String,

    @SerializedName("jenis_lensa")
    val jenisLensa: String? = null,

    @SerializedName("ukuran_kiri")
    val ukuranKiri: String? = null,

    @SerializedName("ukuran_kanan")
    val ukuranKanan: String? = null
)
