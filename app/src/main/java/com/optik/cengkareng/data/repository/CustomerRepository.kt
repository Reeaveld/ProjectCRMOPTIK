package com.optik.cengkareng.data.repository

import android.util.Log
import com.optik.cengkareng.core.utils.Constants
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.local.dao.CustomerDao
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.response.TransactionItem
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

    // 1. Ambil data (Tidak berubah)
    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Int): Flow<CustomerEntity> = customerDao.getCustomerById(id)

    suspend fun getCustomerTransactions(id: Int): Flow<Resource<List<TransactionItem>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerTransactions(id)
            if (response.isSuccessful) {
                // Mengambil list transaksi dari dalam bungkus "data" JSON Laravel
                val transactions = response.body()?.data ?: emptyList()
                emit(Resource.Success(transactions))
            } else {
                emit(Resource.Error("Gagal memuat riwayat: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan"))
        }
    }

    // 2. Fungsi Simpan (UPDATE: Mengembalikan 'Resource' agar UI tahu statusnya)
    suspend fun addCustomer(customer: CustomerEntity): Resource<Boolean> {

        // Kita pakai NonCancellable (Fitur bagus dari kodingan lama kamu)
        return withContext(Dispatchers.IO + NonCancellable) {

            // A. Simpan ke Database Lokal (Selalu dilakukan)
            val localId = customerDao.insertCustomer(customer)
            Log.d("CRM_DEBUG", "Data tersimpan di Lokal ID: $localId")

            // B. Coba Kirim ke Server
            try {
                Log.d("CRM_DEBUG", "Mulai Upload ke: ${Constants.BASE_URL}")

                val response = apiService.addCustomer(customer)

                if (response.isSuccessful) {
                    Log.d("CRM_DEBUG", "✅ SUKSES UPLOAD: ${response.body()}")
                    // BEDANYA DISINI: Kita lapor 'Sukses' ke ViewModel
                    Resource.Success(true)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                    Log.e("CRM_DEBUG", "❌ GAGAL SERVER: Kode ${response.code()} - $errorMsg")
                    // BEDANYA DISINI: Kita lapor 'Error' ke ViewModel
                    Resource.Error("Gagal Server: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e("CRM_DEBUG", "❌ ERROR JARINGAN: ${e.message}")
                e.printStackTrace()
                // BEDANYA DISINI: Kita lapor bahwa ini tersimpan Offline
                Resource.Error("Tersimpan Offline (Jaringan Error)")
            }
        }
    }
}