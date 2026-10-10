package com.optik.cengkareng.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.optik.cengkareng.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    // Mengambil semua data pelanggan, diurutkan ID terbaru (DESC)
    // Return Flow agar UI otomatis update jika ada data baru
    @Query("SELECT * FROM customers ORDER BY id DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    // Insert satu pelanggan. Mengembalikan local row ID yang digenerate Room
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    // Perbarui entitas pelanggan yang sudah ada
    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    // Insert banyak pelanggan sekaligus (Batch Sync)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    // Contoh query tambahan: Cari berdasarkan nama
    @Query("SELECT * FROM customers WHERE nama LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun getCustomerById(customerId: Int): Flow<CustomerEntity>

    // Temukan data lokal berdasarkan remoteId (Server ID)
    @Query("SELECT * FROM customers WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getCustomerByRemoteId(remoteId: Int): CustomerEntity?

    // Update remoteId dan flag isSynced pada data lokal setelah berhasil POST ke server
    @Query("UPDATE customers SET remoteId = :remoteId, isSynced = 1 WHERE id = :localId")
    suspend fun updateRemoteId(localId: Int, remoteId: Int)

    // Hapus data lokal yang sudah tersinkron tetapi sudah dihapus di server
    @Query("DELETE FROM customers WHERE isSynced = 1 AND remoteId NOT IN (:serverRemoteIds)")
    suspend fun deleteRemovedServerCustomers(serverRemoteIds: List<Int>)
}