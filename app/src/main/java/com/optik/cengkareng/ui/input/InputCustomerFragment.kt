package com.optik.cengkareng.ui.input

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.request.CustomerRequest
import com.optik.cengkareng.databinding.FragmentInputCustomerBinding
import com.optik.cengkareng.features.customer.presentation.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InputCustomerFragment : Fragment() {

    private var _binding: FragmentInputCustomerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CustomerViewModel by viewModels()

    // 1. Menangkap Argumen ID (Nilai defaultnya 0 jika datang dari FAB Tambah)
    private val args: InputCustomerFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = args.customerId

        // 2. DYNAMIC UI ROUTING (Ala Frappe / Monica)
        if (customerId == 0) {
            // --- MODE TAMBAH BARU ---
            binding.tvTitle.text = "Input Pelanggan Baru"
            binding.btnSimpan.text = "SIMPAN DATA"
        } else {
            // --- MODE EDIT DATA ---
            binding.tvTitle.text = "Edit Data Pelanggan"
            binding.btnSimpan.text = "PERBARUI DATA"

            // Mengambil data lama dari database lokal untuk mengisi (Pre-fill) kotak input
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getCustomerDetail(customerId).collectLatest { customer ->
                    if (customer != null) {
                        binding.etNama.setText(customer.nama)
                        binding.etNomorHp.setText(customer.noHp)
                    }
                }
            }
        }

        // 3. HANDLE KLIK TOMBOL (Satu tombol, dua aksi berbeda)
        binding.btnSimpan.setOnClickListener {
            val nama = binding.etNama.text.toString().trim()
            val hp = binding.etNomorHp.text.toString().trim()

            if (nama.isEmpty() || hp.isEmpty()) {
                Toast.makeText(context, "Nama & HP wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (customerId == 0) {
                // Eksekusi API Tambah
                viewModel.addCustomer(nama, hp)
            } else {
                // Eksekusi API Update
                // Mengemas data JSON menggunakan DTO CustomerRequest
                val request = CustomerRequest(
                    name = nama,
                    phone = hp,
                    address = null // Sesuaikan jika ada kotak input alamat di masa depan
                )
                viewModel.updateCustomer(customerId, request)
            }
        }

        // 4. OBSERVASI STATUS API
        observeViewModelStates()
    }

    private fun observeViewModelStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observasi Pipa Tambah Data
                launch {
                    viewModel.addCustomerEvent.collectLatest { event ->
                        handleApiState(event, "✅ Data Berhasil Disimpan!")
                    }
                }

                // Observasi Pipa Update Data
                launch {
                    viewModel.updateCustomerEvent.collectLatest { event ->
                        // Karena update me-return String message, kita ubah sedikit penanganannya
                        when (event) {
                            is Resource.Loading -> showLoading(true)
                            is Resource.Success -> {
                                showLoading(false)
                                Toast.makeText(context, event.data, Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is Resource.Error -> {
                                showLoading(false)
                                Toast.makeText(context, "⚠️ ${event.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    // Fungsi Pembantu Handle API Tambah Data
    private fun handleApiState(event: Resource<Boolean>, successMessage: String) {
        when (event) {
            is Resource.Loading -> showLoading(true)
            is Resource.Success -> {
                showLoading(false)
                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            is Resource.Error -> {
                showLoading(false)
                Toast.makeText(context, "⚠️ ${event.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnSimpan.isEnabled = !isLoading
        binding.btnSimpan.text = if (isLoading) "" else {
            if (args.customerId == 0) "SIMPAN DATA" else "PERBARUI DATA"
        }
        binding.progressBar.isVisible = isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}