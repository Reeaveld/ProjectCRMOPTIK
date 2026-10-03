package com.optik.cengkareng.core.network

import android.content.Context
import com.optik.cengkareng.core.security.TokenManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private val tokenManager: TokenManager = mock()
    private val mockContext: Context = mock()
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Mock packageName agar sendBroadcast berjalan tanpa crash
        whenever(mockContext.packageName).thenReturn("com.optik.cengkareng")

        val authInterceptor = AuthInterceptor(tokenManager, mockContext)
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `intercept menyertakan header Authorization Bearer ketika token tersimpan`() {
        whenever(tokenManager.getToken()).thenReturn("test-sanctum-token-xyz")

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/customers"))
            .build()

        okHttpClient.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer test-sanctum-token-xyz", recordedRequest.getHeader("Authorization"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `intercept tidak menyertakan header Authorization ketika token kosong`() {
        whenever(tokenManager.getToken()).thenReturn(null)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/login"))
            .build()

        okHttpClient.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `intercept membersihkan token dan broadcast ketika server mengembalikan 401`() {
        whenever(tokenManager.getToken()).thenReturn("expired-token")

        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("{\"message\":\"Unauthenticated.\"}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/customers"))
            .build()

        val response = okHttpClient.newCall(request).execute()

        assertEquals(401, response.code)
        verify(tokenManager).clear()
        response.close()
    }
}
