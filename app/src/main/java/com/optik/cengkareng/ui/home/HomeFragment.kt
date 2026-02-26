package com.optik.cengkareng.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.optik.cengkareng.R
import com.optik.cengkareng.databinding.FragmentHomeBinding
import com.optik.cengkareng.features.customer.presentation.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomerViewModel by viewModels()
    private lateinit var customerAdapter: CustomerAdapter

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

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_input)
        }
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerAdapter { customerId ->
            val action = HomeFragmentDirections.actionHomeFragmentToCustomerDetailFragment(customerId)
            findNavController().navigate(action)
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