package com.optik.cengkareng.core.sync

/**
 * Abstraksi penjadwal pengiriman outbox (WorkManager di produksi, fake di test).
 */
interface SyncScheduler {
    /** Jadwalkan satu kali pengiriman begitu jaringan tersedia (backoff eksponensial). Idempoten (KEEP). */
    fun schedulePush()

    /** Pastikan pengiriman berkala terdaftar (idempoten). */
    fun ensurePeriodic()
}
