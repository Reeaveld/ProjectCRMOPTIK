package com.optik.cengkareng.data.repository

import android.util.Log
import com.optik.cengkareng.data.local.dao.CustomerDao
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.api.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val apiService: ApiService
) {
    // Ambil data untuk ditampilkan di Home (dari Lokal biar cepat)
    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    // Fungsi Simpan (Hybrid: Lokal + Cloud)
    suspend fun addCustomer(customer: CustomerEntity) {
        // 1. Simpan ke Lokal dulu (agar user merasa cepat/sat-set)
        customerDao.insertCustomer(customer)

        // 2. Kirim ke Server Laravel (Background)
        try {
            val response = apiService.addCustomer(customer)
            if (response.isSuccessful) {
                Log.d("CRM_DEBUG", "Sukses kirim ke Server: ${response.body()}")
                // Opsional: Tandai data lokal sebagai 'Synced'
            } else {
                Log.e("CRM_DEBUG", "Gagal kirim: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("CRM_DEBUG", "Error Jaringan: ${e.message}")
            // Data tetap aman di HP, nanti bisa di-sync ulang (fitur lanjut)
        }
    }
}