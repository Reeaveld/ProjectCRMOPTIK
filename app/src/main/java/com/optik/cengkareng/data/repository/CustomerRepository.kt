package com.optik.cengkareng.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.google.gson.JsonParseException
import com.optik.cengkareng.core.network.ApiErrorParser
import com.optik.cengkareng.core.sync.SyncScheduler
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.local.AppDatabase
import com.optik.cengkareng.data.local.dao.CustomerDao
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.api.ApiService
import com.optik.cengkareng.data.remote.request.CompleteProfileRequest
import com.optik.cengkareng.data.remote.request.CustomerRequest
import com.optik.cengkareng.data.remote.request.TransactionRequest
import com.optik.cengkareng.data.remote.response.CustomerResponse
import com.optik.cengkareng.data.remote.response.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hasil satu kali kirim baris outbox.
 */
sealed class PushOutcome {
    /** Server menerima; baris lokal sudah terikat ke [remoteId]. */
    data class Success(val remoteId: Int) : PushOutcome()

    /** Server menolak (4xx, mis. 422). Disimpan di syncError dan TIDAK dikirim ulang otomatis. */
    data class Rejected(val message: String) : PushOutcome()

    /** Gagal sementara (jaringan, 5xx, 401/408/429). Akan dicoba lagi oleh worker. */
    data class Transient(val message: String) : PushOutcome()
}

data class PushSummary(val pushed: Int, val rejected: Int, val transientFailures: Int)

/**
 * Sumber kebenaran data pelanggan: Room (offline-first) + Laravel.
 *
 * Aturan keras:
 *  - `id` Room TIDAK PERNAH dikirim ke server. Semua panggilan ke server memakai remoteId;
 *    bila remoteId == null aksi diblokir dengan pesan jelas.
 *  - Satu [Mutex] menyerialkan sinkronisasi (tarik) dan pengiriman outbox (dorong) sehingga
 *    snapshot server tidak pernah basi terhadap baris yang baru terikat.
 *  - POST /customers membawa `client_uuid` sehingga pengulangan setelah respons hilang idempoten.
 */
@Singleton
class CustomerRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val customerDao: CustomerDao,
    private val apiService: ApiService,
    private val syncScheduler: SyncScheduler
) {

    private val syncMutex = Mutex()

    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Int): Flow<CustomerEntity> = customerDao.getCustomerById(id)

    fun observePendingCount(): Flow<Int> = customerDao.observePendingCount()

    /** Jadwalkan worker hanya bila memang ada baris outbox yang menunggu. */
    suspend fun schedulePendingPushIfNeeded() = withContext(Dispatchers.IO) {
        if (customerDao.getPendingPush().isNotEmpty()) syncScheduler.schedulePush()
    }

    // ─── Tarik (server -> lokal) ────────────────────────────────────

    suspend fun syncCustomers(): Resource<Boolean> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            try {
                val response = apiService.getCustomers()
                val remoteCustomers = response.body()
                if (!response.isSuccessful || remoteCustomers == null) {
                    return@withLock Resource.Error(
                        "Gagal sinkronisasi data pelanggan dari server: ${response.message()}"
                    )
                }

                appDatabase.withTransaction { applyRemoteSnapshot(remoteCustomers) }
                Resource.Success(true)
            } catch (e: IOException) {
                Resource.Error(e.localizedMessage ?: "Kesalahan jaringan saat sinkronisasi.")
            } catch (e: JsonParseException) {
                Resource.Error("Respons server tidak dapat dibaca saat sinkronisasi.")
            }
        }
    }

    /** Dipanggil di dalam transaksi Room. Tanpa N+1: satu query per potongan 500 id. */
    private suspend fun applyRemoteSnapshot(remoteCustomers: List<CustomerResponse>) {
        val remoteIds = remoteCustomers.map { it.id }
        val localByRemoteId = remoteIds.chunked(CHUNK)
            .flatMap { customerDao.getByRemoteIds(it) }
            .associateBy { it.remoteId }
        // Baris outbox yang mungkin sudah diterima server namun respons POST-nya hilang
        val unboundByUuid = customerDao.getUnboundWithClientUuid().associateBy { it.clientUuid }

        val toInsert = mutableListOf<CustomerEntity>()
        val toUpdate = mutableListOf<CustomerEntity>()

        for (remote in remoteCustomers) {
            val existing = localByRemoteId[remote.id]
            if (existing != null) {
                val updated = merge(existing, remote)
                if (updated != existing) toUpdate += updated
                continue
            }

            val pending = remote.clientUuid?.let { unboundByUuid[it] }
            if (pending != null) {
                // Rekonsiliasi: ikat baris lokal ke baris server, bukan membuat duplikat
                toUpdate += merge(pending, remote).copy(remoteId = remote.id, syncError = null)
            } else {
                toInsert += CustomerEntity(
                    remoteId = remote.id,
                    clientUuid = remote.clientUuid,
                    nama = remote.nama,
                    noHp = remote.noHp,
                    jenisLensa = remote.jenisLensa ?: "General",
                    ukuranKiri = remote.ukuranKiri ?: "0.00",
                    ukuranKanan = remote.ukuranKanan ?: "0.00",
                    lastFollowUp = remote.parseLastFollowUpEpoch(),
                    isProfileCompleteBackend = remote.isProfileComplete,
                    isSynced = true
                )
            }
        }

        if (toInsert.isNotEmpty()) customerDao.insertAllNew(toInsert)
        if (toUpdate.isNotEmpty()) customerDao.updateAll(toUpdate)

        // Bersihkan baris yang sudah tersinkron tetapi telah dihapus di server (potongan 500 id).
        val remoteIdSet = remoteIds.toHashSet()
        customerDao.getSyncedRemoteIds()
            .filterNot { it in remoteIdSet }
            .chunked(CHUNK)
            .forEach { customerDao.deleteByRemoteIds(it) }
    }

    private fun merge(existing: CustomerEntity, remote: CustomerResponse): CustomerEntity {
        val followUpEpoch = remote.parseLastFollowUpEpoch()
        return existing.copy(
            nama = remote.nama,
            noHp = remote.noHp,
            jenisLensa = remote.jenisLensa ?: existing.jenisLensa,
            ukuranKiri = remote.ukuranKiri ?: existing.ukuranKiri,
            ukuranKanan = remote.ukuranKanan ?: existing.ukuranKanan,
            lastFollowUp = if (followUpEpoch > 0L) followUpEpoch else existing.lastFollowUp,
            isProfileCompleteBackend = remote.isProfileComplete,
            isSynced = true
        )
    }

    // ─── Dorong (lokal -> server): outbox ───────────────────────────

    /**
     * Simpan lokal dahulu (tidak boleh hilang), lalu coba kirim.
     *  - Success(true)  : sudah diterima server
     *  - Success(false) : tersimpan di perangkat, menunggu pengiriman otomatis oleh worker
     *  - Error          : server menolak (mis. 422); baris lokal dibuang agar pengguna dapat memperbaiki isian
     */
    suspend fun addCustomer(customer: CustomerEntity): Resource<Boolean> {
        return withContext(Dispatchers.IO + NonCancellable) {
            val stored = customer.copy(
                id = 0,
                remoteId = null,
                clientUuid = customer.clientUuid ?: UUID.randomUUID().toString(),
                isSynced = false,
                syncError = null
            )
            val localId = customerDao.insertCustomer(stored).toInt()
            Log.d(TAG, "Data tersimpan di lokal (id=$localId), mencoba kirim ke server")

            when (val outcome = syncMutex.withLock { pushIfStillPending(localId) }) {
                is PushOutcome.Success, null -> Resource.Success(true)
                is PushOutcome.Rejected -> {
                    // Pengguna masih di formulir: buang baris yang ditolak agar koreksi tidak menyisakan duplikat
                    customerDao.deleteById(localId)
                    Resource.Error(outcome.message)
                }
                is PushOutcome.Transient -> {
                    // Tersimpan di perangkat; worker mengirim otomatis (idempoten lewat client_uuid)
                    syncScheduler.schedulePush()
                    Resource.Success(false)
                }
            }
        }
    }

    /**
     * Kirim semua baris outbox yang belum terikat dan belum ditolak. Berhenti pada kegagalan
     * sementara pertama (jaringan umumnya mati untuk semua baris).
     */
    suspend fun pushPendingCustomers(): PushSummary = withContext(Dispatchers.IO) {
        var pushed = 0
        var rejected = 0
        var transient = 0
        val attempted = mutableSetOf<Int>()

        loop@ while (true) {
            // Query ulang tiap putaran: baris yang ditambahkan saat worker berjalan ikut terkirim
            val batch = customerDao.getPendingPush().filter { it.id !in attempted }
            if (batch.isEmpty()) break

            for (row in batch) {
                attempted += row.id
                when (syncMutex.withLock { pushIfStillPending(row.id) }) {
                    is PushOutcome.Success -> pushed++
                    is PushOutcome.Rejected -> rejected++
                    is PushOutcome.Transient -> {
                        transient++
                        break@loop
                    }
                    null -> Unit // sudah diikat/ditolak oleh jalur lain
                }
            }
        }
        PushSummary(pushed, rejected, transient)
    }

    /** Harus dipanggil di dalam [syncMutex]. Membaca ulang baris agar tidak mengirim dua kali. */
    private suspend fun pushIfStillPending(localId: Int): PushOutcome? {
        val row = customerDao.getById(localId) ?: return null
        if (row.remoteId != null || row.syncError != null) return null
        return pushOne(row)
    }

    private suspend fun pushOne(row: CustomerEntity): PushOutcome {
        val phone = row.noHp?.trim()
        if (phone.isNullOrBlank()) {
            // K1 (no_hp boleh kosong?) belum diputuskan: server masih mewajibkan no_hp.
            customerDao.setSyncError(row.id, MSG_PHONE_REQUIRED)
            return PushOutcome.Rejected(MSG_PHONE_REQUIRED)
        }

        return try {
            val response = apiService.addCustomer(
                CustomerRequest(
                    nama = row.nama,
                    noHp = phone,
                    jenisLensa = row.jenisLensa,
                    ukuranKiri = row.ukuranKiri,
                    ukuranKanan = row.ukuranKanan,
                    clientUuid = row.clientUuid
                )
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                bindRemoteId(row.id, body)
                PushOutcome.Success(body.id)
            } else {
                val code = response.code()
                val message = ApiErrorParser.parse(
                    response.errorBody()?.string(),
                    "Server menolak data (HTTP $code)"
                )
                if (code in 400..499 && code !in RETRYABLE_4XX) {
                    customerDao.setSyncError(row.id, message)
                    PushOutcome.Rejected(message)
                } else {
                    PushOutcome.Transient(message)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Gagal koneksi saat mengirim outbox: ${e.message}")
            PushOutcome.Transient(e.localizedMessage ?: "Kesalahan jaringan")
        } catch (e: JsonParseException) {
            PushOutcome.Transient("Respons server tidak dapat dibaca")
        }
    }

    /**
     * Mengikat baris lokal ke id server. Bila baris dengan remoteId itu SUDAH ada (mis. hasil sync),
     * baris lokal yang dobel dibuang dan baris yang sudah terikat dipertahankan.
     */
    private suspend fun bindRemoteId(localId: Int, server: CustomerResponse) {
        appDatabase.withTransaction {
            val owner = customerDao.getCustomerByRemoteId(server.id)
            if (owner != null && owner.id != localId) {
                customerDao.deleteById(localId)
            } else {
                customerDao.updateRemoteId(localId, server.id)
            }
        }
    }

    // ─── Aksi lain (selalu memakai remoteId) ────────────────────────

    fun getCustomerTransactions(remoteId: Int): Flow<Resource<List<TransactionItem>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerTransactions(remoteId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.data))
            } else {
                emit(Resource.Error("Gagal mengambil data transaksi: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Terjadi kesalahan jaringan: ${e.localizedMessage}"))
        }
    }

    /**
     * @param localId id Room. Diterjemahkan ke remoteId di sini; id Room tidak pernah dikirim ke server.
     */
    fun updateCustomer(localId: Int, request: CustomerRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        val local = customerDao.getById(localId)
        val remoteId = local?.remoteId
        if (local == null) {
            emit(Resource.Error(MSG_LOCAL_NOT_FOUND))
            return@flow
        }
        if (remoteId == null) {
            emit(Resource.Error(MSG_NOT_SYNCED))
            return@flow
        }

        try {
            val response = apiService.updateCustomer(remoteId, request)
            if (response.isSuccessful) {
                val updated = local.copy(
                    nama = request.nama,
                    noHp = request.noHp,
                    jenisLensa = request.jenisLensa ?: local.jenisLensa,
                    ukuranKiri = request.ukuranKiri ?: local.ukuranKiri,
                    ukuranKanan = request.ukuranKanan ?: local.ukuranKanan,
                    isProfileCompleteBackend = null
                )
                customerDao.updateCustomer(
                    updated.copy(isProfileCompleteBackend = updated.isProfileComplete())
                )
                emit(Resource.Success("Data pelanggan berhasil diperbarui"))
            } else {
                emit(
                    Resource.Error(
                        ApiErrorParser.parse(
                            response.errorBody()?.string(),
                            "Gagal memperbarui data (HTTP ${response.code()})"
                        )
                    )
                )
            }
        } catch (e: IOException) {
            emit(Resource.Error("Terjadi kesalahan koneksi: ${e.localizedMessage}"))
        }
    }

    /**
     * @param localId id Room. Diterjemahkan ke remoteId di sini.
     */
    fun completeProfile(localId: Int, noHp: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        val local = customerDao.getById(localId)
        val remoteId = local?.remoteId
        if (local == null) {
            emit(Resource.Error(MSG_LOCAL_NOT_FOUND))
            return@flow
        }
        if (remoteId == null) {
            emit(Resource.Error(MSG_NOT_SYNCED))
            return@flow
        }

        try {
            val response = apiService.completeProfile(remoteId, CompleteProfileRequest(noHp = noHp))
            if (response.isSuccessful) {
                val updated = local.copy(noHp = noHp, isProfileCompleteBackend = null)
                customerDao.updateCustomer(
                    updated.copy(isProfileCompleteBackend = updated.isProfileComplete())
                )
                emit(Resource.Success("Profil berhasil dilengkapi dan jadwal follow-up diaktifkan!"))
            } else {
                emit(
                    Resource.Error(
                        ApiErrorParser.parse(
                            response.errorBody()?.string(),
                            "Gagal melengkapi profil (HTTP ${response.code()})"
                        )
                    )
                )
            }
        } catch (e: IOException) {
            emit(Resource.Error("Terjadi kesalahan jaringan: ${e.localizedMessage}"))
        }
    }

    /**
     * @param request.customerId HARUS id server (remoteId); pemanggil memvalidasi sebelum memanggil.
     */
    fun createTransaction(request: TransactionRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createTransaction(request)
            if (response.isSuccessful) {
                // Update spesifikasi resep dan jenis lensa terbaru di database lokal Room
                val od = request.prescriptions.find { it.eyeSide == "OD" }
                val os = request.prescriptions.find { it.eyeSide == "OS" }
                val lensType = od?.lensType ?: os?.lensType

                val localCustomer = customerDao.getCustomerByRemoteId(request.customerId)
                if (localCustomer != null) {
                    val updatedCustomer = localCustomer.copy(
                        ukuranKanan = od?.let { String.format(java.util.Locale.US, "%.2f", it.sphere) } ?: localCustomer.ukuranKanan,
                        ukuranKiri = os?.let { String.format(java.util.Locale.US, "%.2f", it.sphere) } ?: localCustomer.ukuranKiri,
                        jenisLensa = if (!lensType.isNullOrBlank()) lensType else localCustomer.jenisLensa
                    )
                    customerDao.updateCustomer(updatedCustomer)
                }

                emit(Resource.Success("Transaksi berhasil disimpan! Jadwal follow-up otomatis dibuat."))
            } else {
                emit(
                    Resource.Error(
                        ApiErrorParser.parse(
                            response.errorBody()?.string(),
                            "Gagal menyimpan transaksi (HTTP ${response.code()})"
                        )
                    )
                )
            }
        } catch (e: IOException) {
            emit(Resource.Error("Terjadi kesalahan koneksi: ${e.localizedMessage}"))
        }
    }

    companion object {
        private const val TAG = "CustomerRepository"
        private const val CHUNK = 500
        private val RETRYABLE_4XX = setOf(401, 408, 429)

        const val MSG_NOT_SYNCED =
            "Pelanggan belum tersinkronisasi ke server. Tunggu sinkronisasi selesai lalu coba lagi."
        const val MSG_LOCAL_NOT_FOUND = "Data pelanggan tidak ditemukan di perangkat."
        const val MSG_QUEUED_OFFLINE =
            "Tersimpan di perangkat. Akan dikirim otomatis saat jaringan tersedia."
        const val MSG_PHONE_REQUIRED = "Nomor HP wajib diisi sebelum data dapat dikirim ke server."
    }
}
