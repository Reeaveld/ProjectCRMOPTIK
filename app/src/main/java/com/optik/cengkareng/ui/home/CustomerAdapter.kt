package com.optik.cengkareng.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.databinding.ItemCustomerBinding

class CustomerAdapter : ListAdapter<CustomerEntity, CustomerAdapter.CustomerViewHolder>(DiffCallback) {

    // View Holder: Pemegang satu item tampilan
    class CustomerViewHolder(private val binding: ItemCustomerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: CustomerEntity) {
            binding.tvNama.text = customer.nama
            binding.tvNomorHp.text = customer.noHp

            // Indikator sinkronisasi sederhana
            if (customer.isSynced) {
                binding.tvSyncStatus.text = "Tersimpan"
                binding.tvSyncStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Hijau
            } else {
                binding.tvSyncStatus.text = "Belum Sync"
                binding.tvSyncStatus.setTextColor(android.graphics.Color.parseColor("#FF9800")) // Orange
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // DiffCallback: Untuk performa, hanya update item yang berubah
    companion object DiffCallback : DiffUtil.ItemCallback<CustomerEntity>() {
        override fun areItemsTheSame(oldItem: CustomerEntity, newItem: CustomerEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CustomerEntity, newItem: CustomerEntity): Boolean {
            return oldItem == newItem
        }
    }
}