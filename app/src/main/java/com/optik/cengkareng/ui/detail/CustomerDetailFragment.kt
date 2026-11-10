package com.optik.cengkareng.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayoutMediator
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.databinding.FragmentCustomerDetailBinding
import com.optik.cengkareng.features.customer.presentation.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomerDetailFragment : Fragment() {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    // Mengambil ID lokal yang dikirim dari Home
    private val args: CustomerDetailFragmentArgs by navArgs()
    private val viewModel: CustomerViewModel by activityViewModels()

    private var currentCustomer: CustomerEntity? = null
    private var lastRequestedRemoteId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = args.customerId

        // Setup Toolbar Back Button
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabAddTransaction.setOnClickListener {
            val remoteId = currentCustomer?.remoteId
            if (remoteId == null) {
                Toast.makeText(
                    requireContext(),
                    "Data pelanggan belum tersinkron ke server. Mohon tunggu proses sinkronisasi.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val action = CustomerDetailFragmentDirections
                    .actionCustomerDetailFragmentToInputTransactionFragment(customerId)
                findNavController().navigate(action)
            }
        }

        // Observasi Data Pelanggan secara Lifecycle-aware
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getCustomerDetail(customerId).collect { customer ->
                    if (customer != null) {
                        currentCustomer = customer

                        // 1. Update Header
                        binding.tvNamaDetail.text = customer.nama
                        binding.tvHpDetail.text = customer.noHp ?: "Belum ada nomor telepon"
                        binding.tvAvatarDetail.text = customer.nama.firstOrNull()?.toString()?.uppercase() ?: "A"

                        // 2. Evaluasi status remoteId (Mencegah pengiriman ID lokal ke server)
                        val remoteId = customer.remoteId
                        if (remoteId != null) {
                            binding.tvSyncBanner.isVisible = false
                            binding.fabAddTransaction.isEnabled = true
                            binding.fabAddTransaction.alpha = 1.0f

                            // Panggil API transaksi hanya jika remoteId valid dan belum pernah dimuat
                            if (lastRequestedRemoteId != remoteId) {
                                lastRequestedRemoteId = remoteId
                                viewModel.loadCustomerTransactions(remoteId)
                            }
                        } else {
                            binding.tvSyncBanner.isVisible = true
                            binding.tvSyncBanner.text = "Belum tersinkron ke server, transaksi dinonaktifkan"
                            binding.fabAddTransaction.isEnabled = false
                            binding.fabAddTransaction.alpha = 0.5f

                            // Set state transaksi lokal agar info belum sinkron tampil di tab riwayat
                            viewModel.loadCustomerTransactions(null)
                        }

                        // 3. Setup ViewPager (Bagian Bawah/Tab)
                        if (binding.viewPager.adapter == null) {
                            val adapter = CustomerPagerAdapter(this@CustomerDetailFragment, customer)
                            binding.viewPager.adapter = adapter

                            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                                tab.text = when (position) {
                                    0 -> "Informasi"
                                    1 -> "Riwayat"
                                    else -> ""
                                }
                            }.attach()
                        }

                        // 4. Kirim data terbaru ke Fragment anak (Info)
                        val pagerAdapter = binding.viewPager.adapter as? CustomerPagerAdapter
                        pagerAdapter?.infoFragment?.updateUI(customer)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val remoteId = currentCustomer?.remoteId
        if (remoteId != null) {
            lastRequestedRemoteId = remoteId
            viewModel.loadCustomerTransactions(remoteId)
            viewModel.syncCustomers()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
