package com.optik.cengkareng.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayoutMediator
import com.optik.cengkareng.databinding.FragmentCustomerDetailBinding
import com.optik.cengkareng.features.customer.presentation.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomerDetailFragment : Fragment() {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    // Mengambil ID yang dikirim dari Home
    private val args: CustomerDetailFragmentArgs by navArgs()
    private val viewModel: CustomerViewModel by viewModels()

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

        // Observasi Data Pelanggan
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getCustomerDetail(customerId).collectLatest { customer ->
                if (customer != null) {
                    // 1. Update Header (Bagian Atas)
                    binding.tvNamaDetail.text = customer.nama
                    binding.tvHpDetail.text = customer.noHp
                    binding.tvAvatarDetail.text = customer.nama.firstOrNull().toString().uppercase()

                    // 2. Setup ViewPager (Bagian Bawah/Tab)
                    // Kita setup adapter hanya sekali agar tidak reset saat state berubah
                    if (binding.viewPager.adapter == null) {
                        val adapter = CustomerPagerAdapter(this@CustomerDetailFragment, customer)
                        binding.viewPager.adapter = adapter

                        // Hubungkan TabLayout dengan ViewPager2
                        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                            tab.text = when (position) {
                                0 -> "Informasi"
                                1 -> "Riwayat"
                                else -> ""
                            }
                        }.attach()
                    }

                    // 3. Kirim data terbaru ke Fragment anak (Info)
                    // Cast adapter untuk akses fragment
                    val pagerAdapter = binding.viewPager.adapter as? CustomerPagerAdapter
                    pagerAdapter?.infoFragment?.updateUI(customer)

                } else {
                    // Handle jika data null (loading atau error)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}