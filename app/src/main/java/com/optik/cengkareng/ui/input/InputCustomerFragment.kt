package com.optik.cengkareng.ui.input

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.optik.cengkareng.databinding.FragmentInputCustomerBinding
import com.optik.cengkareng.features.customer.presentation.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // <--- Agar ViewModel bisa di-inject
class InputCustomerFragment : Fragment() {

    private var _binding: FragmentInputCustomerBinding? = null
    private val binding get() = _binding!!

    // Panggil ViewModel yang sudah kita buat
    private val viewModel: CustomerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSimpan.setOnClickListener {
            val nama = binding.etNama.text.toString()
            val hp = binding.etNomorHp.text.toString()

            if (nama.isBlank() || hp.isBlank()) {
                Toast.makeText(requireContext(), "Nama & HP Wajib Diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Simpan ke Database (Via ViewModel)
            viewModel.addCustomer(nama, hp)

            // 2. Beri Feedback
            Toast.makeText(requireContext(), "Data Tersimpan!", Toast.LENGTH_SHORT).show()

            // 3. Kembali ke Halaman Depan
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}