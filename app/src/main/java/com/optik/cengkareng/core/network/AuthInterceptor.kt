package com.optik.cengkareng.core.network

import android.content.Context
import android.content.Intent
import com.optik.cengkareng.core.security.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : Interceptor {

    companion object {
        /**
         * Action yang di-broadcast ketika server mengembalikan 401.
         * Activity bisa mendengarkan ini via BroadcastReceiver untuk auto-redirect ke Login.
         */
        const val ACTION_SESSION_EXPIRED = "com.optik.cengkareng.SESSION_EXPIRED"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .addHeader("Accept", "application/json")

        val token = tokenManager.getToken()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // Jika server mengembalikan 401 Unauthorized pada request terautentikasi (bukan login),
        // maka token sudah expired/revoked. Bersihkan kredensial lokal dan broadcast event redirect.
        val isLoginRequest = originalRequest.url.encodedPath.endsWith("login")
        if (response.code == 401 && !isLoginRequest && !token.isNullOrBlank()) {
            tokenManager.clear()
            val intent = Intent(ACTION_SESSION_EXPIRED).apply {
                setPackage(context.packageName) // Explicit broadcast, hanya app ini yang terima
            }
            context.sendBroadcast(intent)
        }

        return response
    }
}
