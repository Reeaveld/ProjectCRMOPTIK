package com.optik.cengkareng.data.repository

import android.util.Log
import com.optik.cengkareng.core.utils.Constants
import com.optik.cengkareng.core.utils.Resource
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
    private val customerDao: CustomerDao,
    private val apiService: ApiService
) {

    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Int): Flow<CustomerEntity> = customerDao.getCustomerById(id)

    suspend fun addCustomer(customer: CustomerEntity): Resource<Boolean> {
        return withContext(Dispatchers.IO + NonCancellable) {
            val localId = customerDao.insertCustomer(customer)
            Log.d("CRM_DEBUG", "Data tersimpan di Lokal ID: $localId")

            try {
                val response = apiService.addCustomer(customer)
                if (response.isSuccessful) {
                    Resource.Success(true)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                    Resource.Error("Gagal Server: $errorMsg")
                }
            } catch (e: Exception) {
                Resource.Error("Tersimpan Offline (Jaringan Error)")
            }
        }
    }

    fun getCustomerTransactions(id: Int): Flow<Resource<List<TransactionItem>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerTransactions(id)
            if (response.isSuccessful) {
                val transactions = response.body()?.data ?: emptyList()
                emit(Resource.Success(transactions))
            } else {
                emit(Resource.Error("Gagal memuat riwayat: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan"))
        }
    }

    fun updateCustomer(id: Int, request: CustomerRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateCustomer(id, request)
            if (response.isSuccessful) {
                emit(Resource.Success("Data pelanggan berhasil diperbarui!"))
            } else {
                if (response.code() == 422) {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        val jsonObject = JSONObject(errorBody!!)
                        val errors = jsonObject.getJSONObject("errors")
                        when {
                            errors.has("no_hp") -> errors.getJSONArray("no_hp").getString(0)
                            errors.has("nama")  -> errors.getJSONArray("nama").getString(0)
                            else                -> jsonObject.optString("message", "Validasi gagal.")
                        }
                    } catch (e: Exception) {
                        "Validasi Gagal: Silakan periksa kembali data Anda."
                    }
                    emit(Resource.Error(errorMsg))
                } else {
                    emit(Resource.Error("Gagal memperbarui data: HTTP ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan"))
        }
    }

    /**
     * Melengkapi nomor HP customer (Profil Belum Lengkap)
     */
    fun completeProfile(id: Int, noHp: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.completeProfile(id, CompleteProfileRequest(noHp))
            if (response.isSuccessful) {
                emit(Resource.Success("Profil berhasil dilengkapi dan jadwal follow-up diaktifkan!"))
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Gagal melengkapi profil")
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Kesalahan jaringan"))
        }
    }

    /**
     * Memposting transaksi baru dengan resep kacamata (OD/OS)
     */
    fun createTransaction(request: TransactionRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createTransaction(request)
            if (response.isSuccessful) {
                emit(Resource.Success("Transaksi berhasil disimpan! Jadwal follow-up otomatis dibuat."))
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Gagal menyimpan transaksi")
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Kesalahan jaringan"))
        }
    }

    private fun parseErrorMessage(errorBody: String?, fallback: String): String {
        if (errorBody.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(errorBody)
            if (json.has("errors")) {
                val errors = json.getJSONObject("errors")
                val firstKey = errors.keys().next()
                errors.getJSONArray(firstKey).getString(0)
            } else {
                json.optString("message", fallback)
            }
        } catch (e: Exception) {
            fallback
        }
    }
}