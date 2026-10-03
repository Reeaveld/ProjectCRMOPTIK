package com.optik.cengkareng

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.optik.cengkareng.core.network.AuthInterceptor
import com.optik.cengkareng.core.security.TokenManager
import com.optik.cengkareng.features.auth.presentation.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    /**
     * Receiver yang mendengarkan broadcast SESSION_EXPIRED dari AuthInterceptor.
     * Ketika token expired (401), otomatis redirect ke Login.
     */
    private val sessionExpiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Toast.makeText(
                this@MainActivity,
                "Sesi Anda telah berakhir, silakan login kembali.",
                Toast.LENGTH_LONG
            ).show()
            redirectToLogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek apakah admin sudah login
        if (!tokenManager.isLoggedIn()) {
            redirectToLogin()
            return
        }

        // Mengaktifkan tampilan layar penuh (status bar transparan)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        // Mengatur padding agar konten tidak tertutup status bar/nav bar HP
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // Bottom 0 karena ada BottomNav
            insets
        }

        // Setup Navigasi Bawah
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        // Setup navigasi saat tab berpindah (Selected)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    if (navController.currentDestination?.id != R.id.homeFragment) {
                        val popped = navController.popBackStack(R.id.homeFragment, false)
                        if (!popped) {
                            navController.navigate(R.id.homeFragment)
                        }
                    }
                    true
                }
                R.id.dashboardFragment -> {
                    if (navController.currentDestination?.id != R.id.dashboardFragment) {
                        val popped = navController.popBackStack(R.id.dashboardFragment, false)
                        if (!popped) {
                            navController.navigate(R.id.dashboardFragment)
                        }
                    }
                    true
                }
                R.id.profileFragment -> {
                    if (navController.currentDestination?.id != R.id.profileFragment) {
                        val popped = navController.popBackStack(R.id.profileFragment, false)
                        if (!popped) {
                            navController.navigate(R.id.profileFragment)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        // Setup navigasi saat tab yang sedang aktif ditekan kembali (Reselected)
        // Memastikan jika user sedang di Input Pelanggan / Detail, menekan tab "Pelanggan" akan kembali ke list Home
        bottomNav.setOnItemReselectedListener { item ->
            if (navController.currentDestination?.id != item.itemId) {
                val popped = navController.popBackStack(item.itemId, false)
                if (!popped) {
                    navController.navigate(item.itemId)
                }
            }
        }

        // Sinkronisasi status checked pada BottomNav sesuai posisi layar
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> bottomNav.menu.findItem(R.id.homeFragment)?.isChecked = true
                R.id.dashboardFragment -> bottomNav.menu.findItem(R.id.dashboardFragment)?.isChecked = true
                R.id.profileFragment -> bottomNav.menu.findItem(R.id.profileFragment)?.isChecked = true
                R.id.inputCustomerFragment, R.id.customerDetailFragment, R.id.inputTransactionFragment -> {
                    // Sub-layar di bawah hierarki Pelanggan: tetap aktifkan tab Pelanggan
                    bottomNav.menu.findItem(R.id.homeFragment)?.isChecked = true
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Register receiver untuk mendengarkan session expired
        val filter = IntentFilter(AuthInterceptor.ACTION_SESSION_EXPIRED)
        ContextCompat.registerReceiver(
            this,
            sessionExpiredReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        // Unregister receiver agar tidak leak
        try {
            unregisterReceiver(sessionExpiredReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver sudah di-unregister, abaikan
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}