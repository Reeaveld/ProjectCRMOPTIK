package com.optik.cengkareng.data.remote.api

import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.response.TransactionListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("customers")
    suspend fun getCustomers(): Response<List<CustomerEntity>>

    @POST("customers")
    suspend fun addCustomer(@Body customer: CustomerEntity): Response<CustomerEntity>

    /**
     * Endpoint untuk mengambil riwayat transaksi berdasarkan ID pelanggan.
     * Menggunakan metode GET ke rute '/api/customers/{id}/transactions'
     */
    @GET("customers/{id}/transactions")
    suspend fun getCustomerTransactions(
        @Path("id") customerId: Int
    ): Response<TransactionListResponse>
}