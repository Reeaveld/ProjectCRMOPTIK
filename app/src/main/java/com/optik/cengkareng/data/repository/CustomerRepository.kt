package com.optik.cengkareng.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.optik.cengkareng.core.utils.Constants
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.local.AppDatabase
import com.optik.cengkareng.data.local.dao.CustomerDao
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.response.TransactionItem
import com.optik.cengkareng.data.remote.request.CustomerRequest
import com.optik.cengkareng.data.remote.request.CompleteProfileRequest
import com.optik.cengkareng.data.remote.request.TransactionRequest
import org.json.JSONObject
import com.optik.cengkareng.data.remote.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CustomerRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val customerDao: CustomerDao,
    private val apiService: ApiService
) {

    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Int): Flow<CustomerEntity> = customerDao.getCustomerById(id)

    suspend fun syncCustomers(): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCustomers()
            if (response.isSuccessful && response.body() != null) {
                val remoteCustomers = response.body()!!
                val remoteIds = remoteCustomers.map { it.id }.toSet()

                // Eksekusi seluruh operasi batch sync di dalam satu transaksi Room atomik
                appDatabase.withTransaction {
                    for (remote in remoteCustomers) {
                        val existing = customerDao.getCustomerByRemoteId(remote.id)
                        val followUpEpoch = remote.parseLastFollowUpEpoch()

                        if (existing != null) {
                            val updated = existing.copy(
                                nama = remote.nama,
                                noHp = remote.noHp,
                                jenisLensa = remote.jenisLensa ?: existing.jenisLensa,
                                ukuranKiri = remote.ukuranKiri ?: existing.ukuranKiri,
                                ukuranKanan = remote.ukuranKanan ?: existing.ukuranKanan,
                                lastFollowUp = if (followUpEpoch > 0L) followUpEpoch else existing.lastFollowUp,
                                isProfileCompleteBackend = remote.isProfileComplete,
                                isSynced = true
                            )
                            // Hindari penulisan ulang jika data tidak mengalami perubahan
                            if (updated != existing) {
                                customerDao.updateCustomer(updated)
                            }
                        } else {
                            val newEntity = CustomerEntity(
                                id = 0,
                                remoteId = remote.id,
                                nama = remote.nama,
                                noHp = remote.noHp,
                                jenisLensa = remote.jenisLensa ?: "General",
                                ukuranKiri = remote.ukuranKiri ?: "0.00",
                                ukuranKanan = remote.ukuranKanan ?: "0.00",
                                lastFollowUp = followUpEpoch,
                                isProfileCompleteBackend = remote.isProfileComplete,
                                isSynced = true
                            )
                            customerDao.insertCustomer(newEntity)
                        }
                    }

                    // Bersihkan record lokal yang sudah tersinkron tetapi telah dihapus di server
                    // Menggunakan batching 500 ID agar aman dari batasan variabel SQLite versi lama
                    val localSyncedIds = customerDao.getSyncedRemoteIds()
                    val staleRemoteIds = localSyncedIds.filterNot { it in remoteIds }
                    if (staleRemoteIds.isNotEmpty()) {
                        staleRemoteIds.chunked(500).forEach { batch ->
                            customerDao.deleteByRemoteIds(batch)
                        }
                    }
                }

                Resource.Success(true)
            } else {
                Resource.Error("Gagal sinkronisasi data pelanggan dari server: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Kesalahan jaringan saat sinkronisasi.")
        }
    }

    suspend fun addCustomer(customer: CustomerEntity): Resource<Boolean> {
        return withContext(Dispatchers.IO + NonCancellable) {
            val localId = customerDao.insertCustomer(customer.copy(isSynced = false))
            Log.d("CRM_DEBUG", "Data tersimpan di Lokal ID: $localId")

            try {
                val request = CustomerRequest(
                    nama = customer.nama,
                    noHp = customer.noHp ?: "",
                    jenisLensa = customer.jenisLensa,
                    ukuranKiri = customer.ukuranKiri,
                    ukuranKanan = customer.ukuranKanan
                )
                val response = apiService.addCustomer(request)
                if (response.isSuccessful && response.body() != null) {
                    val serverCustomer = response.body()!!
                    customerDao.updateRemoteId(localId.toInt(), serverCustomer.id)
                    Resource.Success(true)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Log.e("CRM_DEBUG", "Server menolak: $errorMsg")
                    Resource.Error("Data tersimpan lokal, tapi gagal ke server: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e("CRM_DEBUG", "Gagal koneksi: ${e.message}")
                Resource.Error("Tersimpan secara Offline (Menunggu jaringan)")
            }
        }
    }

    fun getCustomerTransactions(customerId: Int): Flow<Resource<List<TransactionItem>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerTransactions(customerId)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                emit(Resource.Success(apiResponse.data))
            } else {
                emit(Resource.Error("Gagal mengambil data transaksi: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Terjadi kesalahan jaringan: ${e.localizedMessage}"))
        }
    }

    fun updateCustomer(customerId: Int, request: CustomerRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateCustomer(customerId, request)
            if (response.isSuccessful) {
                emit(Resource.Success("Data pelanggan berhasil diperbarui"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    JSONObject(errorBody ?: "").optString("message", "Gagal memperbarui data")
                } catch (e: Exception) {
                    "Gagal memperbarui data (HTTP ${response.code()})"
                }
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Terjadi kesalahan koneksi: ${e.localizedMessage}"))
        }
    }

    fun completeProfile(customerId: Int, noHp: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val request = CompleteProfileRequest(noHp = noHp)
            val response = apiService.completeProfile(customerId, request)
            if (response.isSuccessful) {
                emit(Resource.Success("Profil berhasil dilengkapi dan jadwal follow-up diaktifkan!"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    val json = JSONObject(errorBody ?: "")
                    val errors = json.optJSONObject("errors")
                    if (errors != null && errors.has("no_hp")) {
                        errors.getJSONArray("no_hp").getString(0)
                    } else {
                        json.optString("message", "Gagal melengkapi profil")
                    }
                } catch (e: Exception) {
                    "Gagal melengkapi profil (HTTP ${response.code()})"
                }
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Terjadi kesalahan jaringan: ${e.localizedMessage}"))
        }
    }

    fun createTransaction(request: TransactionRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createTransaction(request)
            if (response.isSuccessful) {
                emit(Resource.Success("Transaksi berhasil disimpan! Jadwal follow-up otomatis dibuat."))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    JSONObject(errorBody ?: "").optString("message", "Gagal menyimpan transaksi")
                } catch (e: Exception) {
                    "Gagal menyimpan transaksi (HTTP ${response.code()})"
                }
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Terjadi kesalahan koneksi: ${e.localizedMessage}"))
        }
    }
}
