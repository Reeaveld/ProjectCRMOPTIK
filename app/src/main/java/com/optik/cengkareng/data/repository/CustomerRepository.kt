package com.optik.cengkareng.data.repository

import android.util.Log
import com.optik.cengkareng.data.local.dao.CustomerDao
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.api.ApiService
import kotlinx.coroutines.Dispatchers
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
    // POIN 3 YANG SEBELUMNYA KURANG: Penambahan logika 'apiService.addCustomer'
    suspend fun addCustomer(customer: CustomerEntity) {
        withContext(Dispatchers.IO) {
            // A. Simpan ke Database Lokal dulu (Prioritas UI)
            customerDao.insertCustomer(customer)

            // B. Coba Kirim ke Server Laravel
            try {
                Log.d("CRM_DEBUG", "Sedang mengirim ke: ${com.optik.cengkareng.core.utils.Constants.BASE_URL}")

                val response = apiService.addCustomer(customer)

                if (response.isSuccessful) {
                    Log.d("CRM_DEBUG", "BERHASIL Upload ke Server! Response: ${response.body()}")
                    // (Opsional) Nanti bisa update status 'isSynced = true' di lokal
                } else {
                    Log.e("CRM_DEBUG", "GAGAL Upload: Kode ${response.code()} - Pesan: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                // Jika internet mati atau server error, data tetap aman di HP
                Log.e("CRM_DEBUG", "ERROR JARINGAN: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}