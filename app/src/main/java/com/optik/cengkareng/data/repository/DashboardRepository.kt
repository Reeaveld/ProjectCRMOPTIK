package com.optik.cengkareng.data.repository

import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.api.ApiService
import com.optik.cengkareng.data.remote.model.CustomerSearchResultResponse
import com.optik.cengkareng.data.remote.model.DashboardStatsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getDashboardStats(from: String, to: String): Resource<DashboardStatsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDashboardStats(from, to)
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Resource.Error("Gagal memuat data statistik: $errorMsg")
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan.")
            }
        }
    }

    suspend fun searchCustomersByDate(from: String, to: String): Resource<CustomerSearchResultResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchCustomersByDate(from, to)
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    Resource.Error("Gagal mencari data pelanggan: $errorMsg")
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan.")
            }
        }
    }
}
