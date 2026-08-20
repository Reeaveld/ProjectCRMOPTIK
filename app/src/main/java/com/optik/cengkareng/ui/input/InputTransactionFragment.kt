package com.optik.cengkareng.ui.input

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.request.PrescriptionRequest
import com.optik.cengkareng.data.remote.request.TransactionRequest
import com.optik.cengkareng.databinding.FragmentInputTransactionBinding
import com.optik.cengkareng.features.customer.presentation.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class InputTransactionFragment : Fragment() {

    private var _binding: FragmentInputTransactionBinding? = null
    private val binding get() = _binding!!

    private val args: InputTransactionFragmentArgs by navArgs()
    private val viewModel: CustomerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = args.customerId

        setupObserver()

        binding.btnSaveTransaction.setOnClickListener {
            submitTransaction(customerId)
        }
    }

    private fun submitTransaction(customerId: Int) {
        val odSph = binding.etOdSph.text.toString().toDoubleOrNull() ?: 0.0
        val odCyl = binding.etOdCyl.text.toString().toDoubleOrNull() ?: 0.0
        val odAxis = binding.etOdAxis.text.toString().toIntOrNull() ?: 0

        val osSph = binding.etOsSph.text.toString().toDoubleOrNull() ?: 0.0
        val osCyl = binding.etOsCyl.text.toString().toDoubleOrNull() ?: 0.0
        val osAxis = binding.etOsAxis.text.toString().toIntOrNull() ?: 0

        val lensType = binding.etLensType.text.toString().ifBlank { "Single Vision" }
        val amount = binding.etAmount.text.toString().toDoubleOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Nominal biaya harus diisi dengan benar", Toast.LENGTH_SHORT).show()
            return
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val prescriptions = listOf(
            PrescriptionRequest(eyeSide = "OD", sphere = odSph, cylinder = odCyl, axis = odAxis, lensType = lensType),
            PrescriptionRequest(eyeSide = "OS", sphere = osSph, cylinder = osCyl, axis = osAxis, lensType = lensType)
        )

        val request = TransactionRequest(
            customerId = customerId,
            amount = amount,
            transactionDate = todayDate,
            prescriptions = prescriptions
        )

        viewModel.createTransaction(request)
    }

    private fun setupObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createTransactionEvent.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnSaveTransaction.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnSaveTransaction.isEnabled = true
                        Toast.makeText(requireContext(), resource.data, Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnSaveTransaction.isEnabled = true
                        Toast.makeText(requireContext(), "Error: ${resource.message}", Toast.LENGTH_LONG).show()
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
