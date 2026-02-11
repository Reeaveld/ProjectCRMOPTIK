package com.optik.cengkareng.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.optik.cengkareng.databinding.ItemHistoryBinding

// Model data sederhana untuk UI Mockup (ATM dari Krayin Order)
data class HistoryModel(
    val tanggal: String,
    val resep: String,
    val harga: String,
    val status: String,
    val invoice: String
)

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    // Dummy Data (Hardcode dulu untuk visualisasi)
    private val listHistory = listOf(
        HistoryModel("10 Feb 2024", "R: -2.50 | L: -2.50 (Bluechromic)", "Rp 850.000", "Proses", "#INV-24002"),
        HistoryModel("12 Des 2022", "R: -2.00 | L: -1.75 (Progressive)", "Rp 1.500.000", "Selesai", "#INV-22105"),
        HistoryModel("05 Jan 2020", "R: -1.00 | L: -1.00 (Standard)", "Rp 400.000", "Selesai", "#INV-20001")
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(listHistory[position])
    }

    override fun getItemCount(): Int = listHistory.size

    class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryModel) {
            binding.tvTanggal.text = item.tanggal
            binding.tvResepRingkas.text = item.resep
            binding.tvHarga.text = item.harga
            binding.tvInvoiceId.text = item.invoice
            binding.tvStatus.text = item.status

            // Logic warna status ala Frappe (Hijau jika selesai, Kuning jika proses)
            if (item.status == "Proses") {
                // Kuning
                binding.tvStatus.background.setTint(android.graphics.Color.parseColor("#FEF3C7"))
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#D97706"))
            } else {
                // Hijau
                binding.tvStatus.background.setTint(android.graphics.Color.parseColor("#DCFCE7"))
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#166534"))
            }
        }
    }
}