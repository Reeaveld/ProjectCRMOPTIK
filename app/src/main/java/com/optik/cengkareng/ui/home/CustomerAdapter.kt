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
import kotlin.math.abs

// 1. Parameter onCustomerClick ditambahkan di Konstruktor Utama
class CustomerAdapter(
    private val onCustomerClick: (Int) -> Unit
) : ListAdapter<CustomerEntity, CustomerAdapter.CustomerViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // 2. onCustomerClick diteruskan ke ViewHolder
        return CustomerViewHolder(binding, onCustomerClick)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    // 3. Konstruktor ViewHolder menerima onCustomerClick
    class CustomerViewHolder(
        private val binding: ItemCustomerBinding,
        private val onCustomerClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: CustomerEntity) {
            // Set Data
            binding.tvNama.text = customer.nama
            binding.tvNomorHp.text = customer.noHp
            binding.tvJenisLensa.text = if (customer.jenisLensa.isNotEmpty()) customer.jenisLensa else "General"

            // Avatar Logic
            val initial = customer.nama.firstOrNull()?.toString()?.uppercase() ?: "?"
            binding.tvAvatar.text = initial
            val color = generateColorFromName(customer.nama)
            binding.tvAvatar.backgroundTintList = ColorStateList.valueOf(color)

            binding.tvLastDate.text = "Baru"

            // BPJS Penanda Logic
            if (customer.noHp.startsWith("BPJS")) {
                binding.root.setBackgroundColor(Color.parseColor("#FEF08A"))
            } else {
                binding.root.setBackgroundColor(Color.WHITE)
            }

            // 4. Klik dieksekusi dengan valid
            binding.root.setOnClickListener {
                onCustomerClick(customer.id)
            }
        }

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