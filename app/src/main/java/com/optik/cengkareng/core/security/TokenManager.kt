package com.optik.cengkareng.core.security

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "optik_crm_auth_prefs"
        private const val KEY_TOKEN = "key_auth_token"
        private const val KEY_ADMIN_EMAIL = "key_admin_email"
        private const val KEY_ADMIN_NAME = "key_admin_name"
    }

    fun saveToken(token: String, email: String? = null, name: String? = null) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            email?.let { putString(KEY_ADMIN_EMAIL, it) }
            name?.let { putString(KEY_ADMIN_NAME, it) }
            apply()
        }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getAdminEmail(): String? {
        return prefs.getString(KEY_ADMIN_EMAIL, null)
    }

    fun getAdminName(): String? {
        return prefs.getString(KEY_ADMIN_NAME, null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
