package com.optik.cengkareng.data.remote.api

import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.remote.request.CustomerRequest
import com.optik.cengkareng.data.remote.request.CompleteProfileRequest
import com.optik.cengkareng.data.remote.request.TransactionRequest
import com.optik.cengkareng.data.remote.response.TransactionListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

import com.optik.cengkareng.data.remote.request.LoginRequest
import com.optik.cengkareng.data.remote.response.LoginResponse

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("logout")
    suspend fun logout(): Response<Any>

    @GET("customers")
    suspend fun getCustomers(): Response<List<CustomerEntity>>

    @POST("customers")
    suspend fun addCustomer(@Body customer: CustomerEntity): Response<CustomerEntity>

    @GET("customers/{id}/transactions")
    suspend fun getCustomerTransactions(
        @Path("id") customerId: Int
    ): Response<TransactionListResponse>

    @PUT("customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: Int,
        @Body request: CustomerRequest
    ): Response<Any>

    /**
     * Endpoint untuk melengkapi nomor HP customer (profil belum lengkap)
     */
    @PATCH("customers/{id}/complete-profile")
    suspend fun completeProfile(
        @Path("id") id: Int,
        @Body request: CompleteProfileRequest
    ): Response<Any>

    /**
     * Endpoint untuk membuat transaksi baru beserta resep kacamata
     */
    @POST("transactions")
    suspend fun createTransaction(
        @Body request: TransactionRequest
    ): Response<Any>

    /**
     * Endpoint Tahap 1: Upload PDF dan ekstrak data staging untuk ditinjau
     */
    @retrofit2.http.Multipart
    @POST("import/bpjs/parse")
    suspend fun parseBpjsPdf(
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): Response<com.optik.cengkareng.data.remote.model.BpjsParseResponse>

    /**
     * Endpoint Tahap 2: Konfirmasi commit data yang telah ditinjau ke database
     */
    @POST("import/bpjs/commit")
    suspend fun commitBpjs(
        @Body request: com.optik.cengkareng.data.remote.model.BpjsCommitRequest
    ): Response<com.optik.cengkareng.data.remote.model.BpjsCommitResponse>
}