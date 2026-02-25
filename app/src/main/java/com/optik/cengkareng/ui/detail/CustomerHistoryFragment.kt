package com.optik.cengkareng.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.databinding.FragmentCustomerHistoryBinding
import com.optik.cengkareng.features.customer.presentation.CustomerViewModel
import kotlinx.coroutines.launch

class CustomerHistoryFragment : Fragment() {
    private var _binding: FragmentCustomerHistoryBinding? = null
    private val binding get() = _binding!!

    // KUNCI ARSITEKTUR: Menggunakan activityViewModels()
    // agar Fragment ini menggunakan ViewModel yang SAMA PERSIS dengan CustomerDetailFragment
    private val viewModel: CustomerViewModel by activityViewModels()

    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Di sinilah tempat paling aman untuk Observe data!
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun observeData() {
        // Memastikan observer hanya berjalan saat Fragment terlihat (mencegah memory leak)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Mulai mengamati pipa data transactionsState dari ViewModel
                viewModel.transactionsState.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // Tampilkan ProgressBar, sembunyikan RecyclerView
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvHistory.visibility = View.GONE
                        }
                        is Resource.Success -> {
                            // Sembunyikan ProgressBar, tampilkan RecyclerView
                            binding.progressBar.visibility = View.GONE
                            binding.rvHistory.visibility = View.VISIBLE

                            // Lempar data JSON ke Adapter!
                            historyAdapter.submitList(resource.data)
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}