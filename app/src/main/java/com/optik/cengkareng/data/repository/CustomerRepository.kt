package com.optik.cengkareng.data.repository

import android.util.Log
import com.optik.cengkareng.core.utils.Constants
import com.optik.cengkareng.data.local.dao.CustomerDao
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val apiService: ApiService
) {

    // 1. Ambil data dari Database HP (Agar UI cepat & bisa offline)
    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    // 2. Fungsi Simpan (Hybrid: Lokal + Cloud)
    suspend fun addCustomer(customer: CustomerEntity) {

        // PENTING: withContext(NonCancellable) mencegah "JobCancellationException"
        // Ini membuat blok kode di dalamnya kebal terhadap pembatalan UI.
        withContext(Dispatchers.IO + NonCancellable) {

            // A. Simpan ke Database Lokal dulu (Prioritas UI)
            val localId = customerDao.insertCustomer(customer)
            Log.d("CRM_DEBUG", "Data tersimpan di Lokal ID: $localId")

            // B. Coba Kirim ke Server Laravel
            try {
                Log.d("CRM_DEBUG", "Mulai Upload ke: ${Constants.BASE_URL}")

                val response = apiService.addCustomer(customer)

                if (response.isSuccessful) {
                    // Sukses 200/201
                    Log.d("CRM_DEBUG", "✅ SUKSES UPLOAD ke Server! Response: ${response.body()}")
                } else {
                    // Gagal Server (misal 404, 500, 422)
                    val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                    Log.e("CRM_DEBUG", "❌ GAGAL SERVER: Kode ${response.code()} - $errorMsg")
                }
            } catch (e: Exception) {
                // Gagal Koneksi (Internet mati / Server mati)
                Log.e("CRM_DEBUG", "❌ ERROR JARINGAN: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}