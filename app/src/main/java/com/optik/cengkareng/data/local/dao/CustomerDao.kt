package com.optik.cengkareng.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.optik.cengkareng.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    // Mengambil semua data pelanggan, diurutkan ID terbaru (DESC)
    // Return Flow agar UI otomatis update jika ada data baru
    @Query("SELECT * FROM customers ORDER BY id DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    // Insert satu pelanggan. Jika ID sama, timpa (REPLACE)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    // Contoh query tambahan: Cari berdasarkan nama (untuk fitur Search nanti)
    @Query("SELECT * FROM customers WHERE nama LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>
}