package com.optik.cengkareng.ui.input

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.optik.cengkareng.core.utils.Resource
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Handle Klik Tombol
        binding.btnSimpan.setOnClickListener {
            val nama = binding.etNama.text.toString().trim()
            val hp = binding.etNomorHp.text.toString().trim()

            // Validasi Input Kosong
            if (nama.isEmpty() || hp.isEmpty()) {
                Toast.makeText(context, "Nama & HP wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Jalankan ViewModel
            viewModel.addCustomer(nama, hp)
        }

        // 2. Observasi State (Reaksi UI)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.addCustomerEvent.collectLatest { event ->
                when (event) {
                    is Resource.Loading -> {
                        // Kunci tombol, munculkan loading
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        Toast.makeText(context, "✅ Data Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                        // Tutup layar & kembali ke Home
                        findNavController().popBackStack()
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        // Tampilkan pesan error TAPI JANGAN tutup layar
                        // Agar user tahu datanya tersimpan di HP tapi gagal upload
                        val msg = event.message
                        Toast.makeText(context, "⚠️ $msg", Toast.LENGTH_LONG).show()

                        // Jika offline, kita tetap bisa kembali ke home (opsional)
                        if (msg.contains("Offline")) {
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnSimpan.isEnabled = !isLoading
        binding.btnSimpan.text = if (isLoading) "" else "SIMPAN DATA"
        binding.progressBar.isVisible = isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}