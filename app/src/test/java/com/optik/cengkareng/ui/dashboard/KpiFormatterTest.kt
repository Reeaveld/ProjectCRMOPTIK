package com.optik.cengkareng.ui.dashboard

import com.google.gson.Gson
import com.optik.cengkareng.data.remote.model.BeforeAfterSegment
import com.optik.cengkareng.data.remote.model.BeforeAfterWindowMetrics
import com.optik.cengkareng.data.remote.model.DashboardRetentionResponse
import com.optik.cengkareng.data.remote.model.DashboardStatsResponse
import com.optik.cengkareng.data.remote.model.KpiStats
import com.optik.cengkareng.data.remote.model.PurchaseSegmentMetrics
import com.optik.cengkareng.data.remote.model.ReturnCohortMetrics
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
    fun `respons endpoint retention parsed dan terformat sesuai skema backend`() {
        val retentionJson = """
            {
              "date_range": {"from": "2026-09-01", "to": "2026-11-08"},
              "purchase": {
                "all": {
                  "active_customers": 5,
                  "repeat_customers": 2,
                  "returning_customers": 3,
                  "repeat_purchase_rate": 40.0,
                  "returning_customer_rate": 60.0
                }
              },
              "followup_return": {
                "return_days": 30,
                "treated": {
                  "cohort_size": 10,
                  "returned": 4,
                  "return_rate": 40.0
                }
              }
            }
        """.trimIndent()

        val response = Gson().fromJson(retentionJson, DashboardRetentionResponse::class.java)
        val purchaseAll = response.purchase?.all

        assertEquals("40,0%", KpiFormatter.purchaseSegmentRepeatRate(purchaseAll))
        assertEquals("2 dari 5 pelanggan aktif beli ≥2×", KpiFormatter.purchaseSegmentRepeatDetail(purchaseAll))
        assertEquals("60,0%", KpiFormatter.purchaseSegmentReturningRate(purchaseAll))
        assertEquals("3 dari 5 pelanggan aktif pernah beli sebelumnya", KpiFormatter.purchaseSegmentReturningDetail(purchaseAll))

        assertEquals("40,0%", KpiFormatter.followupReturnRate(response.followupReturn))
        assertEquals("4 dari 10 pelanggan ter-followup kembali beli (30 hari)", KpiFormatter.followupReturnDetail(response.followupReturn))
    }

    @Test
    fun `before after evaluation terformat dengan benar`() {
        val segment = BeforeAfterSegment(
            before = BeforeAfterWindowMetrics(repeatPurchaseRate = 15.0),
            after = BeforeAfterWindowMetrics(repeatPurchaseRate = 33.3)
        )

        assertEquals("Sebelum: 15,0% → Sesudah: 33,3% (+18,3%)", KpiFormatter.beforeAfterEvaluation(segment))
    }
}
