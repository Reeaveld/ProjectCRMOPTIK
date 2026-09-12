package com.optik.cengkareng.features.import_bpjs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.model.BpjsCommitItem
import com.optik.cengkareng.data.remote.model.BpjsCommitRequest
import com.optik.cengkareng.data.remote.model.BpjsCommitResponse
import com.optik.cengkareng.data.remote.model.BpjsItemPreview
import com.optik.cengkareng.data.remote.model.BpjsMetadata
import com.optik.cengkareng.data.remote.model.BpjsParseResponse
import com.optik.cengkareng.data.repository.BpjsImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BpjsImportViewModel @Inject constructor(
    private val repository: BpjsImportRepository
) : ViewModel() {

    private val _parseState = MutableStateFlow<Resource<BpjsParseResponse>?>(null)
    val parseState: StateFlow<Resource<BpjsParseResponse>?> = _parseState.asStateFlow()

    private val _commitState = MutableStateFlow<Resource<BpjsCommitResponse>?>(null)
    val commitState: StateFlow<Resource<BpjsCommitResponse>?> = _commitState.asStateFlow()

    private val _metadata = MutableStateFlow<BpjsMetadata?>(null)
    val metadata: StateFlow<BpjsMetadata?> = _metadata.asStateFlow()

    private val _items = MutableStateFlow<List<BpjsItemPreview>>(emptyList())
    val items: StateFlow<List<BpjsItemPreview>> = _items.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter list item berdasarkan query pencarian nama / nomor transaksi
    val filteredItems: StateFlow<List<BpjsItemPreview>> = combine(_items, _searchQuery) { currentItems, query ->
        if (query.isBlank()) {
            currentItems
        } else {
            val q = query.trim().lowercase()
            currentItems.filter {
                it.customerName.lowercase().contains(q) || it.invoiceNumber.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Memulai proses unggah dan parsing PDF ke backend
     */
    fun uploadAndParse(file: File) {
        viewModelScope.launch {
            repository.parsePdf(file).collect { resource ->
                _parseState.value = resource
                if (resource is Resource.Success) {
                    val body = resource.data
                    _metadata.value = body.metadata
                    _items.value = body.items ?: emptyList()
                }
            }
        }
    }

    /**
     * Memperbarui input nomor WhatsApp pada item tertentu
     */
    fun updatePhone(tempId: Int, newPhone: String) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.tempId == tempId }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(phone = newPhone.ifBlank { null })
            _items.value = current
        }
    }

    /**
     * Mengubah status checklist item
     */
    fun toggleSelection(tempId: Int) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.tempId == tempId }
        if (index != -1) {
            val item = current[index]
            // Jangan izinkan centang item duplikat
            if (!item.isDuplicate) {
                current[index] = item.copy(selected = !item.selected)
                _items.value = current
            }
        }
    }

    /**
     * Memilih / membatalkan semua item yang valid (bukan duplikat)
     */
    fun selectAll(select: Boolean) {
        val current = _items.value.map { item ->
            if (item.isDuplicate) {
                item.copy(selected = false)
            } else {
                item.copy(selected = select)
            }
        }
        _items.value = current
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Melakukan commit item yang dicentang ke backend
     */
    fun commitSelected() {
        val selectedItems = _items.value.filter { it.selected && !it.isDuplicate }
        if (selectedItems.isEmpty()) {
            _commitState.value = Resource.Error("Tidak ada item yang dipilih untuk disimpan.")
            return
        }

        val commitItems = selectedItems.map {
            BpjsCommitItem(
                invoiceNumber = it.invoiceNumber,
                transactionDate = it.transactionDate,
                customerName = it.customerName,
                amount = it.amount,
                phone = it.phone
            )
        }

        val request = BpjsCommitRequest(
            nomorFpk = _metadata.value?.nomorFpk,
            items = commitItems
        )

        viewModelScope.launch {
            repository.commitBpjs(request).collect { resource ->
                _commitState.value = resource
            }
        }
    }

    /**
     * Menghasilkan konten file CSV untuk diekspor ke memori penyimpanan perangkat
     */
    fun generateCsvContent(): String {
        val sb = StringBuilder()
        sb.append("No,No Transaksi,Tanggal,Nama Peserta,Jenis Klaim,Biaya,Nomor WhatsApp,Status\n")
        _items.value.forEachIndexed { idx, it ->
            val status = when {
                it.isDuplicate -> "Duplikat (Sudah Terdaftar)"
                it.isExistingCustomer -> "Pasien Terdaftar"
                else -> "Baru"
            }
            val escapedName = "\"${it.customerName.replace("\"", "\"\"")}\""
            val phone = it.phone ?: "-"
            sb.append("${idx + 1},${it.invoiceNumber},${it.transactionDate},$escapedName,${it.claimType},${it.amount.toLong()},$phone,$status\n")
        }
        return sb.toString()
    }
}
