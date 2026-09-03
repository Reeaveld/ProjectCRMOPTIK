package com.optik.cengkareng.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.optik.cengkareng.databinding.FragmentProfileBinding
import com.optik.cengkareng.features.auth.presentation.AuthViewModel
import com.optik.cengkareng.features.auth.presentation.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderUserInfo()
        renderVersion()
        setupListeners()
    }

    private fun renderUserInfo() {
        binding.tvUserName.text = authViewModel.getAdminName()
        binding.tvUserEmail.text = authViewModel.getAdminEmail()
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Keluar dari Aplikasi")
                .setMessage("Apakah Anda yakin ingin keluar dari sesi admin OptikCRM?")
                .setPositiveButton("Keluar") { _, _ ->
                    authViewModel.logout {
                        Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun renderVersion() {
        val ctx = context ?: return
        val pm = ctx.packageManager
        val versionText = runCatching {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(ctx.packageName, 0)
            val name = info.versionName ?: "1.0"
            "Versi $name"
        }.getOrDefault("Versi 1.0")
        binding.tvVersion.text = versionText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
