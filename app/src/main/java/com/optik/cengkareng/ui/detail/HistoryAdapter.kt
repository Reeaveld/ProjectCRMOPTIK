package com.optik.cengkareng.ui.detail

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.optik.cengkareng.data.remote.response.TransactionItem
import com.optik.cengkareng.databinding.ItemHistoryBinding

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val transactions = mutableListOf<TransactionItem>()

    fun submitList(newList: List<TransactionItem>) {
        transactions.clear()
        transactions.addAll(newList)
        notifyDataSetChanged()
    }

    class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.binding.apply {
            // Mapping ID sesuai dengan item_history.xml secara valid
            tvTanggal.text = transaction.dateFormatted
            tvInvoiceId.text = transaction.invoice
            tvHarga.text = transaction.amountFormatted
            tvStatus.text = transaction.statusLabel

            // Menerapkan Server-Driven UI Color pada Badge Status
            try {
                val color = Color.parseColor(transaction.statusColor)
                tvStatus.setTextColor(color)
                // Mengatur warna latar (backgroundTint) dengan alpha 20% agar transparan
                tvStatus.backgroundTintList = ColorStateList.valueOf(color).withAlpha(51)
            } catch (e: Exception) {
                // Abaikan jika hex color dari server invalid (tetap gunakan default XML)
            }

            // Logika Ekstraksi Resep untuk tvResepRingkas
            if (transaction.prescriptions.isNotEmpty()) {
                val resepText = buildString {
                    // Mencari lensa Kanan (OD) dan Kiri (OS) dari list resep
                    val rightEye = transaction.prescriptions.find { it.sideCode == "OD" }
                    val leftEye = transaction.prescriptions.find { it.sideCode == "OS" }

                    if (rightEye != null) append("R: ${rightEye.sph} ")
                    if (rightEye != null && leftEye != null) append("| ")
                    if (leftEye != null) append("L: ${leftEye.sph}")

                    // Menambahkan Tipe Lensa jika ada (mengambil dari salah satu mata)
                    val lensType = rightEye?.lensType ?: leftEye?.lensType
                    if (!lensType.isNullOrEmpty()) {
                        append(" ($lensType)")
                    }
                }
                tvResepRingkas.text = resepText.trim()
                tvResepRingkas.visibility = View.VISIBLE
            } else {
                // Skenario untuk Data Import BPJS yang tidak memiliki data lensa
                tvResepRingkas.text = "Penjualan Frame / Tanpa Resep"
                // Atau bisa juga kita hilangkan dengan: tvResepRingkas.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = transactions.size
}