package com.optik.cengkareng.ui.dashboard

import com.google.gson.Gson
import com.optik.cengkareng.data.remote.model.DashboardStatsResponse
import com.optik.cengkareng.data.remote.model.KpiStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KpiFormatterTest {

    @Test
    fun `angka KPI retensi pada respons API sama dengan yang ditampilkan`() {
        // Kutipan respons /api/dashboard/stats (angka dari RetentionMetricsTest backend: 1 dari 3 aktif)
        val json = """
            {"kpi":{"total_customers":3,"queued_rate":66.7,"followup_failed":1,"followup_pending":1,
            "followup_blocked":1,"active_customers_count":3,"repeat_customers_count":1,
            "repeat_purchase_rate":33.3,"returning_customers_count":2,"returning_customer_rate":66.7}}
        """.trimIndent()

        val kpi = Gson().fromJson(json, DashboardStatsResponse::class.java).kpi!!

        assertEquals(66.7, kpi.queuedRate!!, 0.0)
        assertEquals(1, kpi.followupFailed)
        assertEquals(33.3, kpi.repeatPurchaseRate!!, 0.0)
        assertEquals(66.7, kpi.returningCustomerRate!!, 0.0)

        assertEquals("33,3%", KpiFormatter.repeatPurchaseRate(kpi))
        assertEquals("1 dari 3 pelanggan aktif beli ≥2×", KpiFormatter.repeatPurchaseDetail(kpi))
        assertEquals("66,7%", KpiFormatter.returningCustomerRate(kpi))
        assertEquals("2 dari 3 pelanggan aktif pernah beli sebelumnya", KpiFormatter.returningCustomerDetail(kpi))
        assertEquals("66,7% masuk antrean WhatsApp (bukan jaminan terkirim)", KpiFormatter.queuedRate(kpi))
    }

    @Test
    fun `null dari server berarti tidak ada data, bukan nol persen`() {
        val json = """{"kpi":{"total_customers":0,"queued_rate":null,"repeat_purchase_rate":null,"returning_customer_rate":null}}"""
        val kpi = Gson().fromJson(json, DashboardStatsResponse::class.java).kpi!!

        assertNull(kpi.queuedRate)
        assertNull(kpi.repeatPurchaseRate)
        assertEquals("—", KpiFormatter.repeatPurchaseRate(kpi))
        assertEquals("Belum ada data", KpiFormatter.repeatPurchaseDetail(kpi))
        assertEquals("—", KpiFormatter.returningCustomerRate(kpi))
        assertEquals("Belum ada pesan jatuh tempo", KpiFormatter.queuedRate(kpi))
    }

    @Test
    fun `nilai nol dari server tetap ditampilkan sebagai 0,0 persen`() {
        val kpi = KpiStats(
            repeatPurchaseRate = 0.0, activeCustomersCount = 4, repeatCustomersCount = 0,
            returningCustomerRate = 0.0, returningCustomersCount = 0
        )

        assertEquals("0,0%", KpiFormatter.repeatPurchaseRate(kpi))
        assertEquals("0 dari 4 pelanggan aktif beli ≥2×", KpiFormatter.repeatPurchaseDetail(kpi))
    }

    @Test
    fun `payload lama tanpa field baru tidak membuat parser gagal`() {
        val kpi = Gson().fromJson("""{"kpi":{"total_customers":5}}""", DashboardStatsResponse::class.java).kpi!!

        assertEquals(5, kpi.totalCustomers)
        assertNull(kpi.queuedRate)
    }

    @Test
    fun `segment retention dan before after improvement terformat dengan benar`() {
        val segmentJson = """{"segment":"BPJS","total_customers":10,"active_customers":5,"repeat_customers":2,"repeat_purchase_rate":40.0}"""
        val segmentItem = Gson().fromJson(segmentJson, com.optik.cengkareng.data.remote.model.SegmentRetentionItem::class.java)

        assertEquals("40,0%", KpiFormatter.segmentRepeatPurchaseRate(segmentItem))
        assertEquals("2 dari 5 pelanggan BPJS aktif", KpiFormatter.segmentRepeatPurchaseDetail(segmentItem))

        val beforeAfterJson = """{"before_rate":15.0,"after_rate":33.3,"improvement_delta":18.3}"""
        val beforeAfterItem = Gson().fromJson(beforeAfterJson, com.optik.cengkareng.data.remote.model.BeforeAfterRetentionItem::class.java)

        assertEquals("+18,3% dibanding sebelum sistem", KpiFormatter.beforeAfterImprovement(beforeAfterItem))
    }
}
