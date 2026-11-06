package com.optik.cengkareng.ui.dashboard

import com.optik.cengkareng.data.remote.model.BeforeAfterRetentionItem
import com.optik.cengkareng.data.remote.model.KpiStats
import com.optik.cengkareng.data.remote.model.SegmentRetentionItem
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

    fun segmentRepeatPurchaseRate(item: SegmentRetentionItem?): String =
        item?.repeatPurchaseRate?.let(::percent) ?: "—"

    fun segmentRepeatPurchaseDetail(item: SegmentRetentionItem?): String =
        if (item?.repeatPurchaseRate == null) NO_DATA
        else "${item.repeatCustomers} dari ${item.activeCustomers} pelanggan ${item.segment} aktif"

    fun beforeAfterImprovement(item: BeforeAfterRetentionItem?): String {
        val delta = item?.improvementDelta ?: return "Belum ada data evaluasi"
        val sign = if (delta >= 0) "+" else ""
        return "$sign${percent(delta)} dibanding sebelum sistem"
    }

    private fun percent(value: Double): String = String.format(Locale.forLanguageTag("id-ID"), "%.1f%%", value)
}
