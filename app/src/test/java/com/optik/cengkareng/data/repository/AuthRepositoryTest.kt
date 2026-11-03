package com.optik.cengkareng.data.repository

import com.optik.cengkareng.core.security.TokenManager
import com.optik.cengkareng.core.security.TokenManagerTest.FakeSecureStore
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.api.ApiService
import com.optik.cengkareng.data.remote.response.LoginResponse
import com.optik.cengkareng.data.remote.response.UserItem
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response

class AuthRepositoryTest {

    private val api: ApiService = mock()

    private fun loginResponse() = LoginResponse(
        success = true,
        message = "ok",
        token = "token-uji",
        user = UserItem(id = 1, name = "Admin Anonim", email = "admin@optikcrm.test")
    )

    @Test
    fun `login sukses menyimpan token terenkripsi lalu memancarkan Success`() = runBlocking {
        whenever(api.login(any())).thenReturn(Response.success(loginResponse()))
        val store = FakeSecureStore(isAvailable = true)
        val repo = AuthRepository(api, TokenManager(store))

        val events = repo.login("admin@optikcrm.test", "x").toList()

        assertTrue(events.last() is Resource.Success)
        assertEquals("token-uji", store.data["key_auth_token"])
        assertTrue(repo.isLoggedIn())
    }

    @Test
    fun `login gagal menyimpan token menampilkan pesan, bukan sukses diam-diam`() = runBlocking {
        whenever(api.login(any())).thenReturn(Response.success(loginResponse()))
        val store = FakeSecureStore(isAvailable = false)
        val repo = AuthRepository(api, TokenManager(store))

        val events = repo.login("admin@optikcrm.test", "x").toList()

        val last = events.last()
        assertTrue("Harus Error, bukan $last", last is Resource.Error)
        assertEquals(AuthRepository.MSG_SECURE_STORAGE_UNAVAILABLE, (last as Resource.Error).message)
        assertTrue("Tidak boleh ada penulisan plaintext", store.data.isEmpty())
        assertEquals(false, repo.isLoggedIn())
    }
}
