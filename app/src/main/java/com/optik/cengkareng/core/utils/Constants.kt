package com.optik.cengkareng.core.utils

import com.optik.cengkareng.BuildConfig

object Constants {
    // URL Backend Laravel — otomatis sesuai build variant (debug/release)
    // Debug: http://10.0.2.2:8000/api/ (emulator)
    // Release: https://api.optik-cengkareng.com/api/ (production)
    val BASE_URL: String = BuildConfig.BASE_URL

    // Nama Database Room
    const val DATABASE_NAME = "optikcrm"

    // Timeout untuk Retrofit (dalam detik)
    const val CONNECT_TIMEOUT = 60L
    const val READ_TIMEOUT = 60L
}