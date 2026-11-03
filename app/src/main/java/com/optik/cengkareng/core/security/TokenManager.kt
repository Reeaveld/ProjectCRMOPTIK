package com.optik.cengkareng.core.security

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val store: SecureStore
) {
    /** true bila penyimpanan aman tersedia di perangkat ini. */
    val isSecureStorageAvailable: Boolean get() = store.isAvailable

    /**
     * @return true hanya bila token benar-benar tersimpan terenkripsi. Pemanggil WAJIB memeriksa
     * dan menampilkan galat; jangan menganggap login berhasil bila false.
     */
    fun saveToken(token: String, email: String? = null, name: String? = null): Boolean {
        if (!store.putString(KEY_TOKEN, token)) return false
        email?.let { store.putString(KEY_ADMIN_EMAIL, it) }
        name?.let { store.putString(KEY_ADMIN_NAME, it) }
        return true
    }

    fun getToken(): String? = store.getString(KEY_TOKEN)

    fun getAdminEmail(): String? = store.getString(KEY_ADMIN_EMAIL)

    fun getAdminName(): String? = store.getString(KEY_ADMIN_NAME)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun clear() {
        store.clear()
    }

    companion object {
        private const val KEY_TOKEN = "key_auth_token"
        private const val KEY_ADMIN_EMAIL = "key_admin_email"
        private const val KEY_ADMIN_NAME = "key_admin_name"
    }
}
