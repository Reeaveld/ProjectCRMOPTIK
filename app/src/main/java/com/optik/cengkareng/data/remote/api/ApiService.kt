package com.optik.cengkareng.data.remote.api

import com.optik.cengkareng.data.local.entity.CustomerEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("customers")
    suspend fun getCustomers(): Response<List<CustomerEntity>>

    @POST("customers")
    suspend fun addCustomer(@Body customer: CustomerEntity): Response<CustomerEntity>
}