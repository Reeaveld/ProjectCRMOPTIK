package com.optik.cengkareng.data.repository

import com.optik.cengkareng.core.security.TokenManager
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.api.ApiService
import com.optik.cengkareng.data.remote.request.LoginRequest
import com.optik.cengkareng.data.remote.response.LoginResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    fun login(email: String, password: String): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val token = body.token
                if (!token.isNullOrBlank()) {
                    tokenManager.saveToken(
                        token = token,
                        email = body.user?.email,
                        name = body.user?.name
                    )
                    emit(Resource.Success(body))
                } else {
                    emit(Resource.Error("Token otentikasi tidak ditemukan dalam respon server."))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    val json = JSONObject(errorBody ?: "")
                    json.optString("message", "Login gagal. Periksa kredensial Anda.")
                } catch (e: Exception) {
                    "Login gagal (HTTP ${response.code()})"
                }
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan"))
        }
    }

    suspend fun logout(): Boolean {
        return try {
            apiService.logout()
            tokenManager.clear()
            true
        } catch (e: Exception) {
            tokenManager.clear()
            true
        }
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun getAdminEmail(): String? = tokenManager.getAdminEmail()

    fun getAdminName(): String? = tokenManager.getAdminName()
}
