package com.optik.cengkareng.features.import_bpjs.presentation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.optik.cengkareng.R
import com.optik.cengkareng.data.remote.model.BpjsItemPreview
import com.optik.cengkareng.databinding.ItemBpjsPreviewBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class BpjsPreviewAdapter(
    private val onItemChecked: (tempId: Int) -> Unit,
    private val onPhoneChanged: (tempId: Int, newPhone: String) -> Unit
) : ListAdapter<BpjsItemPreview, BpjsPreviewAdapter.BpjsPreviewViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BpjsPreviewViewHolder {
        val binding = ItemBpjsPreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BpjsPreviewViewHolder(binding, onItemChecked, onPhoneChanged)
    }

    override fun onBindViewHolder(holder: BpjsPreviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BpjsPreviewViewHolder(
        private val binding: ItemBpjsPreviewBinding,
        private val onItemChecked: (tempId: Int) -> Unit,
        private val onPhoneChanged: (tempId: Int, newPhone: String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentTempId: Int = -1

        init {
            binding.cbSelect.setOnClickListener {
                if (currentTempId != -1) {
                    onItemChecked(currentTempId)
                }
            }

            binding.etPhone.doAfterTextChanged { text ->
                if (currentTempId != -1 && binding.etPhone.hasFocus()) {
                    onPhoneChanged(currentTempId, text?.toString() ?: "")
                }
            }
        }

        fun bind(item: BpjsItemPreview) {
            currentTempId = item.tempId

            binding.tvCustomerName.text = item.customerName
            binding.tvInvoiceNumber.text = item.invoiceNumber

            // Format Tanggal
            val formattedDate = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                val date = inputFormat.parse(item.transactionDate)
                "📅 " + if (date != null) outputFormat.format(date) else item.transactionDate
            } catch (e: Exception) {
                "📅 ${item.transactionDate}"
            }
            binding.tvDate.text = formattedDate

            // Format Rupiah
            val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            rupiahFormat.maximumFractionDigits = 0
            binding.tvAmount.text = rupiahFormat.format(item.amount)

            // Atur status nomor HP
            if (binding.etPhone.text?.toString() != (item.phone ?: "")) {
                binding.etPhone.setText(item.phone ?: "")
            }

            // Penanganan Duplikasi vs Baru
            when {
                item.isDuplicate -> {
                    // Otomatis di-uncheck dan dinonaktifkan
                    binding.cbSelect.isChecked = false
                    binding.cbSelect.isEnabled = false
                    binding.tilPhone.isEnabled = false
                    binding.cardContainer.alpha = 0.55f
                    binding.cardContainer.strokeColor = Color.parseColor("#D1D5DB")
                    binding.tvStatusBadge.text = "SUDAH TERDAFTAR"
                    binding.tvStatusBadge.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_badge_gray)
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#4B5563"))
                }
                item.isExistingCustomer -> {
                    binding.cbSelect.isEnabled = true
                    binding.cbSelect.isChecked = item.selected
                    binding.tilPhone.isEnabled = true
                    binding.cardContainer.alpha = 1.0f
                    binding.cardContainer.strokeColor = Color.parseColor("#93C5FD")
                    binding.tvStatusBadge.text = "PASIEN LAMA"
                    binding.tvStatusBadge.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_badge_blue)
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#1E40AF"))
                }
                else -> {
                    binding.cbSelect.isEnabled = true
                    binding.cbSelect.isChecked = item.selected
                    binding.tilPhone.isEnabled = true
                    binding.cardContainer.alpha = 1.0f
                    binding.cardContainer.strokeColor = Color.parseColor("#E5E7EB")
                    binding.tvStatusBadge.text = "PASIEN BARU"
                    binding.tvStatusBadge.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_badge_green)
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#065F46"))
                }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<BpjsItemPreview>() {
        override fun areItemsTheSame(oldItem: BpjsItemPreview, newItem: BpjsItemPreview): Boolean {
            return oldItem.tempId == newItem.tempId
        }

        override fun areContentsTheSame(oldItem: BpjsItemPreview, newItem: BpjsItemPreview): Boolean {
            return oldItem == newItem
        }
    }
}
