package com.optik.cengkareng.features.auth.presentation

import app.cash.turbine.test
import com.optik.cengkareng.MainDispatcherRule
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.response.LoginResponse
import com.optik.cengkareng.data.remote.response.UserItem
import com.optik.cengkareng.data.repository.AuthRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mock()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        viewModel = AuthViewModel(authRepository)
    }

    @Test
    fun `login sukses mengirim Loading lalu Success dengan token`() = runTest {
        val dummyResponse = LoginResponse(
            success = true,
            message = "Login berhasil",
            token = "dummy-sanctum-token-123",
            user = UserItem(id = 1, name = "Admin Optik", email = "admin@optikcrm.com")
        )

        whenever(authRepository.login("admin@optikcrm.com", "password123"))
            .thenReturn(flowOf(Resource.Loading, Resource.Success(dummyResponse)))

        viewModel.loginEvent.test {
            viewModel.login("admin@optikcrm.com", "password123")

            assertTrue(awaitItem() is Resource.Loading)
            val successItem = awaitItem()
            assertTrue(successItem is Resource.Success)
            assertEquals("dummy-sanctum-token-123", (successItem as Resource.Success).data.token)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login gagal mengirim Loading lalu Error`() = runTest {
        whenever(authRepository.login("admin@optikcrm.com", "wrongpassword"))
            .thenReturn(flowOf(Resource.Loading, Resource.Error("Email atau password salah.")))

        viewModel.loginEvent.test {
            viewModel.login("admin@optikcrm.com", "wrongpassword")

            assertTrue(awaitItem() is Resource.Loading)
            val errorItem = awaitItem()
            assertTrue(errorItem is Resource.Error)
            assertEquals("Email atau password salah.", (errorItem as Resource.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
