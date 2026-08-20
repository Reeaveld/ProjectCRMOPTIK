package com.optik.cengkareng.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.databinding.ItemCustomerBinding
import kotlin.math.abs

class CustomerAdapter(
    private val onCustomerClick: (CustomerEntity) -> Unit
) : ListAdapter<CustomerEntity, CustomerAdapter.CustomerViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding, onCustomerClick)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class CustomerViewHolder(
        private val binding: ItemCustomerBinding,
        private val onCustomerClick: (CustomerEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: CustomerEntity) {
            binding.tvNama.text = customer.nama

            // No HP display logic
            val hasPhone = !customer.noHp.isNullOrBlank() && !customer.noHp.startsWith("BPJS-")
            binding.tvNomorHp.text = if (hasPhone) customer.noHp else "Belum ada No. HP"

            binding.tvJenisLensa.text = if (customer.jenisLensa.isNotEmpty()) customer.jenisLensa else "General"

            // Avatar Logic
            val initial = customer.nama.firstOrNull()?.toString()?.uppercase() ?: "?"
            binding.tvAvatar.text = initial
            val color = generateColorFromName(customer.nama)
            binding.tvAvatar.backgroundTintList = ColorStateList.valueOf(color)

            binding.tvLastDate.text = "Baru"

            // Profile Completeness Indicator
            val isComplete = customer.isProfileComplete()
            if (!isComplete) {
                binding.tvIncompleteBadge.visibility = View.VISIBLE
                binding.cardCustomer.strokeColor = Color.parseColor("#F59E0B") // Amber border
                binding.cardCustomer.setCardBackgroundColor(Color.parseColor("#FFFBEB")) // Soft yellow bg
            } else {
                binding.tvIncompleteBadge.visibility = View.GONE
                binding.cardCustomer.strokeColor = Color.parseColor("#E5E7EB")
                binding.cardCustomer.setCardBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                onCustomerClick(customer)
            }
        }

        private fun generateColorFromName(name: String): Int {
            val hash = abs(name.hashCode())
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