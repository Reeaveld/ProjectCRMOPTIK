package com.optik.cengkareng.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.optik.cengkareng.R
import com.optik.cengkareng.data.remote.model.CustomerSearchItem
import com.optik.cengkareng.databinding.ItemDashboardCustomerBinding
import java.text.NumberFormat
import java.util.Locale

class DashboardCustomerAdapter(
    private val onItemClick: ((CustomerSearchItem) -> Unit)? = null
) : ListAdapter<CustomerSearchItem, DashboardCustomerAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDashboardCustomerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDashboardCustomerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomerSearchItem) {
            binding.tvCustomerName.text = item.nama

            // Profile status badge
            if (item.isProfileComplete) {
                binding.tvProfileStatus.text = "Lengkap"
                binding.tvProfileStatus.setBackgroundResource(R.drawable.bg_badge_green)
                binding.tvProfileStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent_green))
            } else {
                binding.tvProfileStatus.text = "Belum Lengkap"
                binding.tvProfileStatus.setBackgroundResource(R.drawable.bg_badge_amber)
                binding.tvProfileStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent_amber))
            }

            // Phone
            binding.tvPhone.text = if (!item.noHp.isNullOrBlank()) "📱 ${item.noHp}" else "⚠ Tanpa Nomor WhatsApp"

            // Transactions info
            if (item.transactions.isNotEmpty()) {
                val latestTrx = item.transactions.first()
                binding.tvInvoice.text = "Inv: ${latestTrx.invoiceNumber} (${latestTrx.transactionDate})"
                binding.tvAmount.text = rupiahFormat.format(latestTrx.amount)

                val od = latestTrx.prescriptions.firstOrNull { it.eyeSide.equals("OD", ignoreCase = true) }
                val os = latestTrx.prescriptions.firstOrNull { it.eyeSide.equals("OS", ignoreCase = true) }

                val odStr = if (od != null) "OD: ${od.sphere}" else ""
                val osStr = if (os != null) "OS: ${os.sphere}" else ""
                val resepText = when {
                    odStr.isNotEmpty() && osStr.isNotEmpty() -> "$odStr | $osStr"
                    odStr.isNotEmpty() -> odStr
                    osStr.isNotEmpty() -> osStr
                    else -> "Resep Umum"
                }
                binding.tvPrescription.text = resepText

                val lens = od?.lensType ?: os?.lensType ?: item.jenisLensa ?: "General"
                binding.tvLensType.text = lens
                binding.tvLensType.visibility = View.VISIBLE
            } else {
                binding.tvInvoice.text = "Belum ada transaksi"
                binding.tvAmount.text = "Rp 0"
                binding.tvPrescription.text = "Ukuran: ${item.ukuranKiri ?: "0.00"} / ${item.ukuranKanan ?: "0.00"}"
                binding.tvLensType.text = item.jenisLensa ?: "General"
                binding.tvLensType.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CustomerSearchItem>() {
            override fun areItemsTheSame(oldItem: CustomerSearchItem, newItem: CustomerSearchItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CustomerSearchItem, newItem: CustomerSearchItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
