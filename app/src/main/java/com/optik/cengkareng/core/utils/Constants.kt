package com.optik.cengkareng.core.utils

object Constants {
    // URL Backend Laravel (IP 10.0.2.2 Khusus untuk Emulator Android mengakses Localhost Komputer)
    // Jika pakai HP asli, ganti dengan IP Laptop (misal: "http://192.168.1.5:8000/api/")
    const val BASE_URL = "http://10.0.2.2:8000/api/"

    // Nama Database Room
    const val DATABASE_NAME = "optikcrm"

    // Timeout untuk Retrofit (dalam detik)
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
}