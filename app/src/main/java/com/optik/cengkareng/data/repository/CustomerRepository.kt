package com.optik.cengkareng.data.repository

import com.optik.cengkareng.data.local.dao.CustomerDao
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.api.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val apiService: ApiService
) {

    // Ambil data untuk UI (Selalu dari Database Lokal agar cepat)
    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    // Tambah Pelanggan
    suspend fun addCustomer(customer: CustomerEntity) {
        // 1. Simpan ke Local (Room)
        customerDao.insertCustomer(customer)

        // 2. Coba kirim ke Server (Background)
        try {
            // Mapping Entity ke DTO request body (Sederhana)
            // val response = apiService.createCustomer(...)
            // Jika sukses, update remoteId dan isSynced = true
        } catch (e: Exception) {
            // Jika gagal (Offline), biarkan isSynced = false
            // Nanti di-sync oleh WorkManager
        }
    }
}