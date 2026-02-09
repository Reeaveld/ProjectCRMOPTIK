package com.optik.cengkareng.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.databinding.ItemCustomerBinding
import java.util.Locale
import kotlin.math.abs

class CustomerAdapter : ListAdapter<CustomerEntity, CustomerAdapter.CustomerViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class CustomerViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: CustomerEntity) {
            // 1. Set Nama
            binding.tvNama.text = customer.nama

            // 2. Set No HP
            binding.tvNomorHp.text = customer.noHp

            // 3. Set Badge Jenis Lensa (Frappe Style Logic)
            // Jika kosong, default ke "General"
            binding.tvJenisLensa.text = if (customer.jenisLensa.isNotEmpty()) customer.jenisLensa else "General"

            // 4. Smart Avatar Logic (Inisial & Warna)
            val initial = customer.nama.firstOrNull()?.toString()?.uppercase() ?: "?"
            binding.tvAvatar.text = initial

            // Generate warna pastel unik berdasarkan nama
            val color = generateColorFromName(customer.nama)
            binding.tvAvatar.backgroundTintList = ColorStateList.valueOf(color)

            // 5. Set Tanggal (Dummy logic untuk sekarang, nanti bisa pakai data real)
            // Di Skripsi, fitur 'Last Follow Up' ini nilai plus besar
            binding.tvLastDate.text = "Baru"

            // Klik Card
            binding.root.setOnClickListener {
                // TODO: Navigasi ke Detail
            }
        }

        // Fungsi Helper: Mengubah String nama menjadi Warna Hex yang konsisten
        private fun generateColorFromName(name: String): Int {
            val hash = abs(name.hashCode())
            // Daftar warna-warna pastel "Professional" (Frappe/Trello/Jira style)
            val colors = listOf(
                "#3B82F6", // Blue
                "#EF4444", // Red
                "#10B981", // Emerald
                "#F59E0B", // Amber
                "#8B5CF6", // Violet
                "#EC4899", // Rose
                "#6366F1"  // Indigo
            )
            val index = hash % colors.size
            return Color.parseColor(colors[index])
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CustomerEntity>() {
        override fun areItemsTheSame(oldItem: CustomerEntity, newItem: CustomerEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CustomerEntity, newItem: CustomerEntity) = oldItem == newItem
    }
}