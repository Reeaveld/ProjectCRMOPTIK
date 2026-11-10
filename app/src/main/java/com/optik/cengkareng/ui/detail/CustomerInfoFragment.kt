package com.optik.cengkareng.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.databinding.FragmentCustomerInfoBinding
import com.optik.cengkareng.features.customer.presentation.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomerInfoFragment : Fragment() {

    private var _binding: FragmentCustomerInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomerViewModel by activityViewModels()
    private var currentCustomer: CustomerEntity? = null

    companion object {
        fun newInstance(customer: CustomerEntity): CustomerInfoFragment {
            return CustomerInfoFragment().apply {
                // Fragment instance
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind data jika sudah ada data awal dari parent
        currentCustomer?.let { bindCustomer(it) }

        // Observasi data pelanggan secara real-time dari ViewModel induk
        val parentFragment = parentFragment as? CustomerDetailFragment
        if (parentFragment != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    val args = parentFragment.requireArguments()
                    val customerId = args.getInt("customerId")
                    if (customerId != 0) {
                        viewModel.getCustomerDetail(customerId).collect { customer ->
                            if (customer != null) {
                                currentCustomer = customer
                                bindCustomer(customer)
                            }
                        }
                    }
                }
            }
        }
    }

    // Fungsi public untuk update UI dari Parent Fragment
    fun updateUI(customer: CustomerEntity) {
        currentCustomer = customer
        if (_binding != null) {
            bindCustomer(customer)
        }
    }

    private fun bindCustomer(customer: CustomerEntity) {
        if (_binding == null) return

        binding.tvUkuranKanan.text = if (customer.ukuranKanan.isNotBlank()) customer.ukuranKanan else "0.00"
        binding.tvUkuranKiri.text = if (customer.ukuranKiri.isNotBlank()) customer.ukuranKiri else "0.00"
        binding.tvJenisLensaDetail.text = "Jenis: ${if (customer.jenisLensa.isNotBlank()) customer.jenisLensa else "-"}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
