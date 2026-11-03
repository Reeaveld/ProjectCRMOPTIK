package com.optik.cengkareng.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.KeyStore

/**
 * [SecureStore] berbasis EncryptedSharedPreferences (androidx.security-crypto 1.1.0: stabil namun
 * seluruh API-nya ditandai deprecated oleh AndroidX; belum ada pengganti resmi satu-ke-satu).
 *
 * Bila inisialisasi gagal (berkas korup / kunci Keystore tidak valid), dicoba SEKALI lagi setelah
 * menghapus berkas prefs DAN alias kunci default di Android Keystore. Bila tetap gagal, penyimpanan
 * dinyatakan tidak tersedia; tidak ada fallback ke SharedPreferences biasa.
 */
@Suppress("DEPRECATION")
class EncryptedSecureStore(private val context: Context) : SecureStore {

    private val prefs: SharedPreferences? = createWithRetry()

    override val isAvailable: Boolean get() = prefs != null

    override fun putString(key: String, value: String): Boolean {
        val p = prefs ?: return false
        return try {
            p.edit().putString(key, value).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menulis ke penyimpanan aman", e)
            false
        }
    }

    override fun getString(key: String): String? = try {
        prefs?.getString(key, null)
    } catch (e: Exception) {
        Log.e(TAG, "Gagal membaca dari penyimpanan aman", e)
        null
    }

    override fun clear(): Boolean {
        val p = prefs ?: return false
        return try {
            p.edit().clear().commit()
        } catch (e: Exception) {
            false
        }
    }

    private fun createWithRetry(): SharedPreferences? {
        return try {
            build()
        } catch (e: Exception) {
            Log.w(TAG, "Inisialisasi awal gagal: ${e.message}. Membersihkan berkas prefs dan kunci default lalu mencoba ulang.")
            try {
                deletePrefsFile()
                deleteDefaultMasterKey()
                build()
            } catch (retry: Exception) {
                Log.e(TAG, "KRITIS: penyimpanan aman tidak tersedia; menolak fallback plaintext.", retry)
                null
            }
        }
    }

    private fun build(): SharedPreferences {
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

    private fun deletePrefsFile() {
        val file = File(File(context.applicationInfo.dataDir, "shared_prefs"), "$PREF_NAME.xml")
        if (file.exists()) file.delete()
    }

    private fun deleteDefaultMasterKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
            keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
    }

    companion object {
        const val PREF_NAME = "optik_crm_auth_encrypted_prefs"
        private const val TAG = "EncryptedSecureStore"
    }
}
