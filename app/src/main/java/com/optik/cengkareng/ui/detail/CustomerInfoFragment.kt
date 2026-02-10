package com.optik.cengkareng.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.optik.cengkareng.data.local.entity.CustomerEntity
import com.optik.cengkareng.databinding.FragmentCustomerInfoBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerInfoFragment : Fragment() {

    private var _binding: FragmentCustomerInfoBinding? = null
    private val binding get() = _binding!!

    // Fungsi statis untuk membuat fragment dan mengirim data
    companion object {
        fun newInstance(customer: CustomerEntity): CustomerInfoFragment {
            val fragment = CustomerInfoFragment()
            val args = Bundle()
            // Di sini kita bisa kirim data (misal JSON string atau Parcelable)
            // Untuk sementara kita simpan di memory ViewModel induk saja
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Fungsi public untuk update UI dari Parent Fragment
    fun updateUI(customer: CustomerEntity) {
        if (_binding == null) return

        binding.tvUkuranKanan.text = customer.ukuranKanan
        binding.tvUkuranKiri.text = customer.ukuranKiri
        binding.tvJenisLensaDetail.text = "Jenis: ${customer.jenisLensa}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}