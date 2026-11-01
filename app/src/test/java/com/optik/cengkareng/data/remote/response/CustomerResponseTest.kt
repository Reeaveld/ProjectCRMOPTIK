package com.optik.cengkareng.data.remote.response

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerResponseTest {

    private val gson = Gson()

    @Test
    fun parseLastFollowUpEpoch_handlesIso8601String() {
        val json = """
            {
                "id": 10,
                "nama": "Pasien Test",
                "no_hp": "08123456789",
                "last_follow_up": "2026-10-10T14:30:00.000000Z"
            }
        """.trimIndent()

        val response = gson.fromJson(json, CustomerResponse::class.java)
        assertNotNull(response)
        assertEquals(10, response.id)
        assertEquals("2026-10-10T14:30:00.000000Z", response.lastFollowUp)

        val epoch = response.parseLastFollowUpEpoch()
        assertTrue("Epoch harus lebih besar dari 0", epoch > 0L)
    }

    @Test
    fun parseLastFollowUpEpoch_handlesSqlDateTimeString() {
        val response = CustomerResponse(
            id = 1,
            nama = "Test",
            lastFollowUp = "2026-10-10 14:30:00"
        )
        val epoch = response.parseLastFollowUpEpoch()
        assertTrue("Epoch harus lebih besar dari 0", epoch > 0L)
    }

    @Test
    fun parseLastFollowUpEpoch_handlesDateOnlyString() {
        val response = CustomerResponse(
            id = 1,
            nama = "Test",
            lastFollowUp = "2026-10-10"
        )
        val epoch = response.parseLastFollowUpEpoch()
        assertTrue("Epoch harus lebih besar dari 0", epoch > 0L)
    }

    @Test
    fun parseLastFollowUpEpoch_handlesNullOrBlank() {
        val responseNull = CustomerResponse(id = 1, nama = "Test", lastFollowUp = null)
        assertEquals(0L, responseNull.parseLastFollowUpEpoch())

        val responseBlank = CustomerResponse(id = 1, nama = "Test", lastFollowUp = "")
        assertEquals(0L, responseBlank.parseLastFollowUpEpoch())

        val responseInvalid = CustomerResponse(id = 1, nama = "Test", lastFollowUp = "invalid-date")
        assertEquals(0L, responseInvalid.parseLastFollowUpEpoch())
    }
}
