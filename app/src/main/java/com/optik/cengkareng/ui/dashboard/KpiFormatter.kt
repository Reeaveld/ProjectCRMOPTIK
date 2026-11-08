package com.optik.cengkareng.ui.dashboard

import com.optik.cengkareng.data.remote.model.BeforeAfterSegment
import com.optik.cengkareng.data.remote.model.FollowupReturnMetrics
import com.optik.cengkareng.data.remote.model.KpiStats
import com.optik.cengkareng.data.remote.model.PurchaseSegmentMetrics
import java.util.Locale

/**
 * Format teks KPI untuk dashboard. Murni (tanpa Android) agar teruji.
 * Angka berasal LANGSUNG dari respons API (/api/dashboard/stats & /api/dashboard/retention); tidak dihitung ulang di klien.
 * Definisi metrik: docs/METRICS.md di repo backend.
 */
object KpiFormatter {

    private const val NO_DATA = "Belum ada data"

    /** "sent" = diterima antrean WhatsApp (Fonnte), bukan terkirim ke pelanggan. */
    fun queuedRate(kpi: KpiStats): String {
        val rate = kpi.queuedRate ?: return "Belum ada pesan jatuh tempo"
        return "${percent(rate)} masuk antrean WhatsApp (bukan jaminan terkirim)"
    }

    fun repeatPurchaseRate(kpi: KpiStats): String = kpi.repeatPurchaseRate?.let(::percent) ?: "—"

    fun repeatPurchaseDetail(kpi: KpiStats): String =
        if (kpi.repeatPurchaseRate == null) NO_DATA
        else "${kpi.repeatCustomersCount} dari ${kpi.activeCustomersCount} pelanggan aktif beli ≥2×"

    fun returningCustomerRate(kpi: KpiStats): String = kpi.returningCustomerRate?.let(::percent) ?: "—"

    fun returningCustomerDetail(kpi: KpiStats): String =
        if (kpi.returningCustomerRate == null) NO_DATA
        else "${kpi.returningCustomersCount} dari ${kpi.activeCustomersCount} pelanggan aktif pernah beli sebelumnya"

    // Formatters untuk PurchaseSegmentMetrics dari endpoint /api/dashboard/retention
    fun purchaseSegmentRepeatRate(item: PurchaseSegmentMetrics?): String =
        item?.repeatPurchaseRate?.let(::percent) ?: "—"

    fun purchaseSegmentRepeatDetail(item: PurchaseSegmentMetrics?, segmentName: String = "aktif"): String =
        if (item?.repeatPurchaseRate == null) NO_DATA
        else "${item.repeatCustomers} dari ${item.activeCustomers} pelanggan $segmentName beli ≥2×"

    fun purchaseSegmentReturningRate(item: PurchaseSegmentMetrics?): String =
        item?.returningCustomerRate?.let(::percent) ?: "—"

    fun purchaseSegmentReturningDetail(item: PurchaseSegmentMetrics?, segmentName: String = "aktif"): String =
        if (item?.returningCustomerRate == null) NO_DATA
        else "${item.returningCustomers} dari ${item.activeCustomers} pelanggan $segmentName pernah beli sebelumnya"

    fun followupReturnRate(item: FollowupReturnMetrics?): String =
        item?.treated?.returnRate?.let(::percent) ?: "—"

    fun followupReturnDetail(item: FollowupReturnMetrics?): String {
        val treated = item?.treated ?: return NO_DATA
        if (treated.returnRate == null) return NO_DATA
        return "${treated.returned} dari ${treated.cohortSize} pelanggan ter-followup kembali beli (${item.returnDays} hari)"
    }

    fun beforeAfterEvaluation(segment: BeforeAfterSegment?): String {
        if (segment == null) return "Belum ada data evaluasi"
        val beforeRate = segment.before?.repeatPurchaseRate
        val afterRate = segment.after?.repeatPurchaseRate
        if (beforeRate == null || afterRate == null) return "Baseline belum mencukupi"
        val delta = afterRate - beforeRate
        val sign = if (delta >= 0) "+" else ""
        return "Sebelum: ${percent(beforeRate)} → Sesudah: ${percent(afterRate)} ($sign${percent(delta)})"
    }

    private fun percent(value: Double): String = String.format(Locale.forLanguageTag("id-ID"), "%.1f%%", value)
}
