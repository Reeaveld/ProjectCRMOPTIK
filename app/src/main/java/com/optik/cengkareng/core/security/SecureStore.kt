package com.optik.cengkareng.core.security

/**
 * Penyimpanan rahasia terenkripsi. Implementasi produksi: [EncryptedSecureStore].
 * Antarmuka ini ada agar [TokenManager] dapat dites dengan fake dan agar TIDAK ada jalur
 * yang menulis token tanpa enkripsi: bila penyimpanan aman tidak tersedia, semua operasi tulis gagal.
 */
interface SecureStore {
    /** false bila penyimpanan aman tidak dapat dibuat di perangkat ini. */
    val isAvailable: Boolean

    /** @return true bila tersimpan; false bila tidak tersedia atau gagal. Tidak pernah fallback ke plaintext. */
    fun putString(key: String, value: String): Boolean

    fun getString(key: String): String?

    /** @return true bila bersih; false bila tidak tersedia. */
    fun clear(): Boolean
}
