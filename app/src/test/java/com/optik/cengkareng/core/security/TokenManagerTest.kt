package com.optik.cengkareng.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenManagerTest {

    /** Fake dalam memori. Mensimulasikan penyimpanan aman yang tersedia atau tidak. */
    class FakeSecureStore(override val isAvailable: Boolean = true) : SecureStore {
        val data = mutableMapOf<String, String>()
        override fun putString(key: String, value: String): Boolean {
            if (!isAvailable) return false
            data[key] = value
            return true
        }
        override fun getString(key: String): String? = if (isAvailable) data[key] else null
        override fun clear(): Boolean {
            if (!isAvailable) return false
            data.clear()
            return true
        }
    }

    @Test
    fun `saveToken menyimpan token dan metadata admin ketika penyimpanan aman tersedia`() {
        val store = FakeSecureStore()
        val manager = TokenManager(store)

        val saved = manager.saveToken("token-rahasia", "admin@optikcrm.com", "Admin")

        assertTrue(saved)
        assertEquals("token-rahasia", manager.getToken())
        assertEquals("admin@optikcrm.com", manager.getAdminEmail())
        assertEquals("Admin", manager.getAdminName())
        assertTrue(manager.isLoggedIn())
    }

    @Test
    fun `saveToken mengembalikan false dan tidak menulis apa pun ketika penyimpanan aman tidak tersedia`() {
        val store = FakeSecureStore(isAvailable = false)
        val manager = TokenManager(store)

        val saved = manager.saveToken("token-rahasia", "admin@optikcrm.com", "Admin")

        assertFalse("Login tidak boleh dianggap berhasil tanpa penyimpanan aman", saved)
        assertTrue("Tidak boleh ada jalur plaintext", store.data.isEmpty())
        assertNull(manager.getToken())
        assertFalse(manager.isLoggedIn())
        assertFalse(manager.isSecureStorageAvailable)
    }

    @Test
    fun `clear menghapus sesi`() {
        val manager = TokenManager(FakeSecureStore())
        manager.saveToken("t", "a@b.c", "N")

        manager.clear()

        assertFalse(manager.isLoggedIn())
        assertNull(manager.getAdminEmail())
    }

    @Test
    fun `token kosong tidak dianggap login`() {
        val manager = TokenManager(FakeSecureStore())
        manager.saveToken("  ")
        assertFalse(manager.isLoggedIn())
    }
}
