package com.optik.cengkareng.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.optik.cengkareng.data.repository.CustomerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Mengirim baris outbox pelanggan (remoteId == null, belum ditolak server) lalu menarik
 * data terbaru. Hasil:
 *  - success : semua terkirim, atau tersisa hanya yang ditolak server (syncError, perlu tindakan pengguna)
 *  - retry   : ada kegagalan sementara (jaringan/5xx); WorkManager mengulang dengan backoff eksponensial
 */
@HiltWorker
class CustomerSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: CustomerRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val summary = repository.pushPendingCustomers()
        // Tarik data terbaru (menyatukan baris yang respons POST-nya sempat hilang). Kegagalan tarik
        // tidak membatalkan hasil push.
        repository.syncCustomers()
        return if (summary.transientFailures > 0) Result.retry() else Result.success()
    }
}
