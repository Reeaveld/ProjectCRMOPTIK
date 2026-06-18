package com.optik.cengkareng.ui.profile

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.optik.cengkareng.databinding.FragmentProfileBinding

/**
 * Halaman Profil minimal — placeholder yang sudah rapi.
 *
 * Status implementasi:
 * - Tidak ada Hilt/ViewModel di sini (memang belum dibutuhkan).
 * - Menampilkan nama aplikasi, tagline, info akun statis ("Admin Optik"),
 *   dan versi aplikasi dari PackageInfo.
 * - Slot siap dipakai saat fitur Auth (login/logout) diimplementasikan.
 *
 * Jika nanti ada Auth: tinggal inject AuthRepository / SessionManager,
 * isi tvUserName + tvUserStatus dari data sesi, tambahkan tombol Logout.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
        renderVersion()
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
