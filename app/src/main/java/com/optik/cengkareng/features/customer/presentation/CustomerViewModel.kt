package com.optik.cengkareng.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.data.repository.CustomerRepository
import com.optik.cengkareng.data.remote.response.TransactionItem
import com.optik.cengkareng.data.remote.request.CustomerRequest
import com.optik.cengkareng.data.remote.request.TransactionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    // --- LIST DATA PELANGGAN ---
    val customers: StateFlow<List<CustomerEntity>> = repository.getAllCustomers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- TAMBAH PELANGGAN ---
    private val _addCustomerEvent = Channel<Resource<Boolean>>()
    val addCustomerEvent = _addCustomerEvent.receiveAsFlow()

    fun addCustomer(nama: String, hp: String) {
        viewModelScope.launch {
            _addCustomerEvent.send(Resource.Loading)
            val dataBaru = CustomerEntity(
                nama = nama,
                noHp = hp.ifBlank { null }
            )
            val result = repository.addCustomer(dataBaru)
            _addCustomerEvent.send(result)
        }
    }

    // --- DETAIL PELANGGAN & TRANSAKSI ---
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCustomerDetail(id: Int): StateFlow<CustomerEntity?> {
        return repository.getCustomerById(id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    private val _transactionsState = MutableStateFlow<Resource<List<TransactionItem>>>(Resource.Loading)
    val transactionsState: StateFlow<Resource<List<TransactionItem>>> = _transactionsState.asStateFlow()

    fun getCustomerTransactions(customerId: Int) {
        viewModelScope.launch {
            repository.getCustomerTransactions(customerId).collect { result ->
                _transactionsState.value = result
            }
        }
    }

    // --- EDIT PELANGGAN ---
    private val _updateCustomerEvent = Channel<Resource<String>>()
    val updateCustomerEvent = _updateCustomerEvent.receiveAsFlow()

    fun updateCustomer(id: Int, request: CustomerRequest) {
        viewModelScope.launch {
            _updateCustomerEvent.send(Resource.Loading)
            repository.updateCustomer(id, request).collect { result ->
                _updateCustomerEvent.send(result)
            }
        }
    }

    // --- LENGKAPI PROFIL PELANGGAN ---
    private val _completeProfileEvent = Channel<Resource<String>>()
    val completeProfileEvent = _completeProfileEvent.receiveAsFlow()

    fun completeProfile(id: Int, noHp: String) {
        viewModelScope.launch {
            _completeProfileEvent.send(Resource.Loading)
            repository.completeProfile(id, noHp).collect { result ->
                _completeProfileEvent.send(result)
            }
        }
    }

    // --- TAMBAH TRANSAKSI BARU ---
    private val _createTransactionEvent = Channel<Resource<String>>()
    val createTransactionEvent = _createTransactionEvent.receiveAsFlow()

    fun createTransaction(request: TransactionRequest) {
        viewModelScope.launch {
            _createTransactionEvent.send(Resource.Loading)
            repository.createTransaction(request).collect { result ->
                _createTransactionEvent.send(result)
            }
        }
    }
}