package com.optik.cengkareng.features.import_bpjs.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.optik.cengkareng.core.network.AuthInterceptor
import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.model.BpjsMetadata
import com.optik.cengkareng.databinding.ActivityBpjsImportBinding
import com.optik.cengkareng.features.auth.presentation.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale


@AndroidEntryPoint
class BpjsImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBpjsImportBinding
    private val viewModel: BpjsImportViewModel by viewModels()
    private lateinit var previewAdapter: BpjsPreviewAdapter

    private var isAllSelected = true

    /**
     * Receiver untuk menangani session expired (401) dari AuthInterceptor.
     */
    private val sessionExpiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Toast.makeText(
                this@BpjsImportActivity,
                "Sesi Anda telah berakhir, silakan login kembali.",
                Toast.LENGTH_LONG
            ).show()
            val loginIntent = Intent(this@BpjsImportActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(loginIntent)
            finish()
        }
    }

    companion object {
        const val EXTRA_PDF_PATH = "extra_pdf_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBpjsImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Ambil path file PDF dari Intent dan mulai proses parsing
        val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
        if (!pdfPath.isNullOrBlank()) {
            val file = File(pdfPath)
            if (file.exists()) {
                viewModel.uploadAndParse(file)
            } else {
                Toast.makeText(this, "File PDF tidak ditemukan.", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Path PDF tidak valid.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
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
        try {
            unregisterReceiver(sessionExpiredReceiver)
        } catch (_: IllegalArgumentException) { }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        previewAdapter = BpjsPreviewAdapter(
            onItemChecked = { tempId ->
                viewModel.toggleSelection(tempId)
            },
            onPhoneChanged = { tempId, newPhone ->
                viewModel.updatePhone(tempId, newPhone)
            }
        )

        binding.rvPreview.apply {
            adapter = previewAdapter
            layoutManager = LinearLayoutManager(this@BpjsImportActivity)
        }
    }

    private fun setupListeners() {
        // Search filter
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }

        // Toggle Select All
        binding.btnToggleSelectAll.setOnClickListener {
            isAllSelected = !isAllSelected
            viewModel.selectAll(isAllSelected)
            binding.btnToggleSelectAll.text = if (isAllSelected) "Batal Semua" else "Pilih Semua"
        }

        // Export CSV
        binding.btnExportCsv.setOnClickListener {
            exportCsvToFile()
        }

        // Commit Button
        binding.btnCommit.setOnClickListener {
            val selectedCount = viewModel.items.value.count { it.selected && !it.isDuplicate }
            if (selectedCount == 0) {
                Toast.makeText(this, "Pilih minimal 1 klaim untuk disimpan.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Konfirmasi Simpan")
                .setMessage("Simpan ${selectedCount} transaksi klaim BPJS ke database? Jadwal follow-up WhatsApp akan dibuat secara otomatis.")
                .setPositiveButton("Simpan Data") { _, _ ->
                    viewModel.commitSelected()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Observasi Parsing PDF
                launch {
                    viewModel.parseState.collectLatest { state ->
                        when (state) {
                            is Resource.Loading -> {
                                binding.loadingOverlay.isVisible = true
                                binding.tvLoadingMessage.text = "Memproses file PDF klaim BPJS..."
                            }
                            is Resource.Success -> {
                                binding.loadingOverlay.isVisible = false
                            }
                            is Resource.Error -> {
                                binding.loadingOverlay.isVisible = false
                                MaterialAlertDialogBuilder(this@BpjsImportActivity)
                                    .setTitle("Gagal Membaca PDF")
                                    .setMessage(state.message)
                                    .setPositiveButton("Tutup") { _, _ -> finish() }
                                    .setCancelable(false)
                                    .show()
                            }
                            null -> Unit
                        }
                    }
                }

                // 2. Observasi Metadata
                launch {
                    viewModel.metadata.collectLatest { meta ->
                        if (meta != null) {
                            updateSummaryCard(meta)
                        }
                    }
                }

                // 3. Observasi Daftar Item
                launch {
                    viewModel.filteredItems.collectLatest { list ->
                        previewAdapter.submitList(list)
                        val totalSelected = viewModel.items.value.count { it.selected && !it.isDuplicate }
                        binding.btnCommit.text = "KONFIRMASI & SIMPAN (${totalSelected} DATA)"
                        binding.btnCommit.isEnabled = totalSelected > 0
                    }
                }

                // 4. Observasi Commit
                launch {
                    viewModel.commitState.collectLatest { state ->
                        when (state) {
                            is Resource.Loading -> {
                                binding.loadingOverlay.isVisible = true
                                binding.tvLoadingMessage.text = "Menyimpan transaksi ke database..."
                            }
                            is Resource.Success -> {
                                binding.loadingOverlay.isVisible = false
                                val data = state.data
                                val msg = "✅ ${data.message}\n\n" +
                                        "• Pasien Baru: ${data.customersCreated ?: 0}\n" +
                                        "• Pasien Diperbarui: ${data.customersUpdated ?: 0}\n" +
                                        "• Jadwal WhatsApp Aktif: ${data.schedulesPending ?: 0}\n" +
                                        "• Jadwal Tertunda (Belum Lengkap): ${data.schedulesBlocked ?: 0}"

                                MaterialAlertDialogBuilder(this@BpjsImportActivity)
                                    .setTitle("Impor BPJS Berhasil!")
                                    .setMessage(msg)
                                    .setPositiveButton("Selesai") { _, _ ->
                                        setResult(RESULT_OK)
                                        finish()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                            is Resource.Error -> {
                                binding.loadingOverlay.isVisible = false
                                MaterialAlertDialogBuilder(this@BpjsImportActivity)
                                    .setTitle("Gagal Menyimpan Data")
                                    .setMessage(state.message)
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun updateSummaryCard(meta: BpjsMetadata) {
        val fpk = meta.nomorFpk ?: "L2512"
        val prov = meta.provider ?: "OPTIK RS CENGKARENG"
        binding.tvFpkInfo.text = "FPK: $fpk • $prov"
        binding.tvBulanPelayanan.text = "Bulan Pelayanan: ${meta.bulanPelayanan ?: "-"}"

        val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        rupiahFormat.maximumFractionDigits = 0
        binding.tvTotalTagihan.text = rupiahFormat.format(meta.totalAmountParsed ?: 0.0)

        val totalParsed = meta.totalParsed ?: 0
        val isVerified = meta.isVerified ?: true
        binding.tvTotalData.text = if (isVerified) {
            "$totalParsed Data (100% Cocok)"
        } else {
            "$totalParsed Data"
        }
    }

    private fun exportCsvToFile() {
        try {
            val csvData = viewModel.generateCsvContent()
            val fileName = "Klaim_BPJS_${System.currentTimeMillis()}.csv"
            val downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
            val csvFile = File(downloadsDir, fileName)

            FileOutputStream(csvFile).use { fos ->
                fos.write(csvData.toByteArray())
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Ekspor Data Klaim BPJS")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Bagikan / Buka File CSV"))
            Toast.makeText(this, "CSV disimpan: ${csvFile.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mengekspor CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
