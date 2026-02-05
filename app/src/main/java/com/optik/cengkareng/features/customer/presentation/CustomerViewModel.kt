package com.optik.cengkareng.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    // PERBAIKAN 1: Gunakan nama fungsi yang ada di Repository (Bahasa Inggris)
    // getAllCustomers() sesuai dengan yang ada di DAO/Repo
    val customers: StateFlow<List<CustomerEntity>> = repository.getAllCustomers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // PERBAIKAN 2: Hapus parameter 'alamat' agar input lebih cepat
    fun addCustomer(nama: String, hp: String) {
        viewModelScope.launch {
            val dataBaru = CustomerEntity(
                nama = nama,
                noHp = hp, // Pastikan nama variabel ini sama dengan di CustomerEntity (noHp atau phone?)
                jenisLensa = "General", // Default value dulu
                ukuranKanan = "0.00",
                ukuranKiri = "0.00"
            )

            // Panggil fungsi insert di repository (sesuaikan namanya, misal: addCustomer)
            repository.addCustomer(dataBaru)
        }
    }
}