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

    // Snapshot sekali jalan (dipakai worker/repository/test)
    @Query("SELECT * FROM customers ORDER BY id")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

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
    @Query("UPDATE customers SET remoteId = :remoteId, isSynced = 1, syncError = NULL WHERE id = :localId")
    suspend fun updateRemoteId(localId: Int, remoteId: Int)

    // Ambil baris lokal untuk sekumpulan remoteId sekaligus (hindari N+1; panggil dalam potongan <= 500)
    @Query("SELECT * FROM customers WHERE remoteId IN (:remoteIds)")
    suspend fun getByRemoteIds(remoteIds: List<Int>): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :localId LIMIT 1")
    suspend fun getById(localId: Int): CustomerEntity?

    // Insert massal baris BARU dari server. ABORT: bentrok remoteId berarti ada bug dan harus terlihat.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllNew(customers: List<CustomerEntity>)

    @Update
    suspend fun updateAll(customers: List<CustomerEntity>)

    // Outbox: baris yang belum punya remoteId, belum ditolak server, dan punya kunci idempoten
    @Query("SELECT * FROM customers WHERE remoteId IS NULL AND syncError IS NULL AND clientUuid IS NOT NULL ORDER BY id")
    suspend fun getPendingPush(): List<CustomerEntity>

    // Baris yang belum terikat ke server namun punya clientUuid (untuk rekonsiliasi saat sync)
    @Query("SELECT * FROM customers WHERE remoteId IS NULL AND clientUuid IS NOT NULL")
    suspend fun getUnboundWithClientUuid(): List<CustomerEntity>

    @Query("UPDATE customers SET syncError = :message WHERE id = :localId")
    suspend fun setSyncError(localId: Int, message: String?)

    @Query("DELETE FROM customers WHERE id = :localId")
    suspend fun deleteById(localId: Int)

    // Jumlah baris yang menunggu dikirim / ditolak (untuk indikator UI)
    @Query("SELECT COUNT(*) FROM customers WHERE remoteId IS NULL AND syncError IS NULL AND clientUuid IS NOT NULL")
    fun observePendingCount(): Flow<Int>

    // Ambil daftar seluruh remoteId lokal yang sudah tersinkron dan tidak null
    @Query("SELECT remoteId FROM customers WHERE isSynced = 1 AND remoteId IS NOT NULL")
    suspend fun getSyncedRemoteIds(): List<Int>

    // Hapus data lokal berdasarkan daftar remoteId (dieksekusi dalam batch chunked 500)
    @Query("DELETE FROM customers WHERE remoteId IN (:remoteIds)")
    suspend fun deleteByRemoteIds(remoteIds: List<Int>)
}
