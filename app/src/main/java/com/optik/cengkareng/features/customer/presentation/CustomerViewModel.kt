package com.optik.cengkareng.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    // --- BAGIAN 1: LIST DATA (KODE LAMA - DIPERTAHANKAN) ---
    // Menggunakan StateFlow karena UI perlu data terbaru setiap saat
    val customers: StateFlow<List<CustomerEntity>> = repository.getAllCustomers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- BAGIAN 2: INPUT EVENT (KODE BARU - UNTUK UX) ---
    // Menggunakan Channel karena ini adalah "Kejadian sesaat" (Loading -> Selesai)
    private val _addCustomerEvent = Channel<Resource<Boolean>>()
    val addCustomerEvent = _addCustomerEvent.receiveAsFlow()

    fun addCustomer(nama: String, hp: String) {
        viewModelScope.launch {
            // 1. Beritahu UI: "Sedang Loading..."
            _addCustomerEvent.send(Resource.Loading)

            // 2. Siapkan Data
            val dataBaru = CustomerEntity(
                nama = nama,
                noHp = hp,
                // Field lain menggunakan default value dari Entity
            )

            // 3. Panggil Repository & Tunggu Hasil
            val result = repository.addCustomer(dataBaru)

            // 4. Beritahu UI: Hasilnya (Sukses/Error)
            _addCustomerEvent.send(result)
        }
    }
}