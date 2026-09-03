package com.optik.cengkareng.ui.detail

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.optik.cengkareng.data.remote.response.TransactionItem
import com.optik.cengkareng.databinding.ItemHistoryBinding

class HistoryAdapter : ListAdapter<TransactionItem, HistoryAdapter.HistoryViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TransactionItem>() {
            override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val transaction = getItem(position)

        holder.binding.apply {
            tvTanggal.text = transaction.dateFormatted
            tvInvoiceId.text = transaction.invoice
            tvHarga.text = transaction.amountFormatted
            tvStatus.text = transaction.statusLabel

            // Menerapkan Server-Driven UI Color pada Badge Status
            try {
                val color = Color.parseColor(transaction.statusColor)
                tvStatus.setTextColor(color)
                tvStatus.backgroundTintList = ColorStateList.valueOf(color).withAlpha(51)
            } catch (e: Exception) {
                // Abaikan jika hex color dari server invalid (tetap gunakan default XML)
            }

            // Logika Ekstraksi Resep untuk tvResepRingkas
            if (transaction.prescriptions.isNotEmpty()) {
                val resepText = buildString {
                    val rightEye = transaction.prescriptions.find { it.sideCode == "OD" }
                    val leftEye = transaction.prescriptions.find { it.sideCode == "OS" }

                    if (rightEye != null) append("R: ${rightEye.sph} ")
                    if (rightEye != null && leftEye != null) append("| ")
                    if (leftEye != null) append("L: ${leftEye.sph}")

                    val lensType = rightEye?.lensType ?: leftEye?.lensType
                    if (!lensType.isNullOrEmpty()) {
                        append(" ($lensType)")
                    }
                }
                tvResepRingkas.text = resepText.trim()
                tvResepRingkas.visibility = View.VISIBLE
            } else {
                tvResepRingkas.text = "Penjualan Frame / Tanpa Resep"
            }
        }
    }
}