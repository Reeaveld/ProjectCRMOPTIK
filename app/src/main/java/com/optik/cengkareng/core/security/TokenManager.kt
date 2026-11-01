package com.optik.cengkareng.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences? = createEncryptedSharedPreferencesWithRetry(context)

    companion object {
        private const val PREF_NAME = "optik_crm_auth_encrypted_prefs"
        private const val KEY_TOKEN = "key_auth_token"
        private const val KEY_ADMIN_EMAIL = "key_admin_email"
        private const val KEY_ADMIN_NAME = "key_admin_name"

        private fun createEncryptedSharedPreferencesWithRetry(context: Context): SharedPreferences? {
            return try {
                buildEncryptedPrefs(context)
            } catch (e: Exception) {
                Log.w("TokenManager", "Gagal inisialisasi awal EncryptedSharedPreferences: ${e.message}. Membersihkan file korup dan mencoba ulang...")
                try {
                    deletePrefsFile(context, PREF_NAME)
                    buildEncryptedPrefs(context)
                } catch (retryEx: Exception) {
                    // JANGAN simpan di SharedPreferences biasa (menolak fallback plaintext)
                    Log.e("TokenManager", "KRITIS: Inisialisasi ulang EncryptedSharedPreferences gagal. Menolak fallback plaintext.", retryEx)
                    null
                }
            }
        }

        private fun buildEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        private fun deletePrefsFile(context: Context, prefName: String) {
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            val prefsFile = File(prefsDir, "$prefName.xml")
            if (prefsFile.exists()) {
                prefsFile.delete()
            }
        }
    }

    fun saveToken(token: String, email: String? = null, name: String? = null) {
        prefs?.edit()?.apply {
            putString(KEY_TOKEN, token)
            email?.let { putString(KEY_ADMIN_EMAIL, it) }
            name?.let { putString(KEY_ADMIN_NAME, it) }
            apply()
        } ?: Log.e("TokenManager", "Gagal menyimpan token: Secure storage tidak tersedia")
    }

    fun getToken(): String? {
        return prefs?.getString(KEY_TOKEN, null)
    }

    fun getAdminEmail(): String? {
        return prefs?.getString(KEY_ADMIN_EMAIL, null)
    }

    fun getAdminName(): String? {
        return prefs?.getString(KEY_ADMIN_NAME, null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}
