package com.optik.cengkareng.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.optik.cengkareng.R
import com.optik.cengkareng.databinding.FragmentHomeBinding
import com.optik.cengkareng.features.customer.presentation.CustomerViewModel
import com.optik.cengkareng.features.import_bpjs.presentation.BpjsImportActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomerViewModel by viewModels()
    private lateinit var customerAdapter: CustomerAdapter

    // Launcher untuk memilih file PDF dari storage HP
    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handlePdfSelected(uri)
        }
    }

    // Launcher untuk menerima hasil setelah BpjsImportActivity selesai
    private val importBpjsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Sinkronisasi data pelanggan dari server agar langsung tampil di list Room
            viewModel.syncCustomers()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObserver()

        // Sinkronisasi data saat pertama kali dibuka
        viewModel.syncCustomers()

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_input)
        }

        binding.btnImportBpjs.setOnClickListener {
            pdfPickerLauncher.launch("application/pdf")
        }
    }

    private fun handlePdfSelected(uri: Uri) {
        try {
            val cacheFile = File(requireContext().cacheDir, "bpjs_claim_${System.currentTimeMillis()}.pdf")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val intent = Intent(requireContext(), BpjsImportActivity::class.java).apply {
                putExtra(BpjsImportActivity.EXTRA_PDF_PATH, cacheFile.absolutePath)
            }
            importBpjsLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membaca file PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerAdapter { customer ->
            if (!customer.isProfileComplete()) {
                // Alur Kelengkapan Data: Tampilkan dialog ajakan melengkapi No. HP
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Profil Belum Lengkap")
                    .setMessage("Nomor HP pelanggan '${customer.nama}' belum terisi/valid. " +
                            "Lengkapi nomor HP agar jadwal follow-up otomatis WhatsApp dapat diaktifkan.")
                    .setPositiveButton("Lengkapi Data") { _, _ ->
                        val action = HomeFragmentDirections.actionHomeToInput(customerId = customer.id)
                        findNavController().navigate(action)
                    }
                    .setNegativeButton("Lihat Detail") { _, _ ->
                        val action = HomeFragmentDirections.actionHomeFragmentToCustomerDetailFragment(customer.id)
                        findNavController().navigate(action)
                    }
                    .show()
            } else {
                val action = HomeFragmentDirections.actionHomeFragmentToCustomerDetailFragment(customer.id)
                findNavController().navigate(action)
            }
        }

        binding.rvCustomers.apply {
            adapter = customerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.customers.collectLatest { daftarPelanggan ->
                customerAdapter.submitList(daftarPelanggan)
                binding.tvCustomerCount.text = "${daftarPelanggan.size} Pelanggan Terdaftar"
                binding.tvEmpty.isVisible = daftarPelanggan.isEmpty()
                binding.rvCustomers.isVisible = daftarPelanggan.isNotEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}