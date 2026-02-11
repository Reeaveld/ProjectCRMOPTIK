package com.optik.cengkareng.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.optik.cengkareng.databinding.FragmentCustomerHistoryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerHistoryFragment : Fragment() {

    private var _binding: FragmentCustomerHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView dengan Adapter Dummy
        val adapter = HistoryAdapter()
        binding.rvHistory.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}