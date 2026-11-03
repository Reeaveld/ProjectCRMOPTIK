package com.optik.cengkareng.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorParserTest {

    private val fallback = "Gagal"

    @Test
    fun `memakai pesan field pertama dari errors pada format CustomerController`() {
        val body = """{"success":false,"message":"Validasi Gagal","errors":{"no_hp":["Nomor HP sudah terdaftar pada pelanggan lain!"]}}"""
        assertEquals("Nomor HP sudah terdaftar pada pelanggan lain!", ApiErrorParser.parse(body, fallback))
    }

    @Test
    fun `memakai pesan field pertama pada format standar Laravel`() {
        val body = """{"message":"The amount field is required.","errors":{"amount":["The amount field is required."]}}"""
        assertEquals("The amount field is required.", ApiErrorParser.parse(body, fallback))
    }

    @Test
    fun `memilih errors di atas message yang generik`() {
        val body = """{"message":"The given data was invalid.","errors":{"amount":["Jumlah wajib diisi."]}}"""
        assertEquals("Jumlah wajib diisi.", ApiErrorParser.parse(body, fallback))
    }

    @Test
    fun `memakai message bila tidak ada errors`() {
        assertEquals("Customer tidak ditemukan", ApiErrorParser.parse("""{"success":false,"message":"Customer tidak ditemukan"}""", fallback))
    }

    @Test
    fun `memakai fallback untuk body kosong, bukan JSON, atau bukan objek`() {
        assertEquals(fallback, ApiErrorParser.parse(null, fallback))
        assertEquals(fallback, ApiErrorParser.parse("", fallback))
        assertEquals(fallback, ApiErrorParser.parse("<html>502 Bad Gateway</html>", fallback))
        assertEquals(fallback, ApiErrorParser.parse("[1,2]", fallback))
        assertEquals(fallback, ApiErrorParser.parse("""{"message":"  "}""", fallback))
    }

    @Test
    fun `errors dengan nilai string tunggal tetap terbaca`() {
        assertEquals("Salah", ApiErrorParser.parse("""{"errors":{"x":"Salah"}}""", fallback))
    }
}
