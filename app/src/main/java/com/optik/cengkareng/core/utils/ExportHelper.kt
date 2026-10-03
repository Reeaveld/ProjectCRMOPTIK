package com.optik.cengkareng.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.optik.cengkareng.data.remote.model.CustomerSearchItem
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    /**
     * Export daftar pelanggan & transaksi ke format CSV dengan UTF-8 BOM
     */
    fun exportToCsv(
        context: Context,
        customers: List<CustomerSearchItem>,
        dateRangeText: String
    ): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Laporan_OptikCRM_${timeStamp}.csv"
            val file = File(context.cacheDir, fileName)

            val fos = FileOutputStream(file)
            val writer = OutputStreamWriter(fos, Charsets.UTF_8)

            // Tulis UTF-8 BOM agar Excel membaca karakter Indonesia dan separator koma dengan baik
            writer.write("\uFEFF")

            // Header informasi dokumen
            writer.write("LAPORAN DATA PELANGGAN & TRANSAKSI OPTIK CENGKARENG\n")
            writer.write("Periode: ${dateRangeText}\n")
            writer.write("Tanggal Cetak: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())}\n")
            writer.write("Total Pelanggan: ${customers.size}\n\n")

            // Header Kolom Tabel
            writer.write("No,Nama Pelanggan,No HP,Status Profil,Tanggal Transaksi,No Invoice,Total Transaksi (Rp),Status,Resep Kanan (OD),Resep Kiri (OS),Jenis Lensa,Catatan\n")

            var counter = 1
            for (customer in customers) {
                val statusProfil = if (customer.isProfileComplete) "Lengkap" else "Belum Lengkap"
                val noHp = customer.noHp ?: "-"

                if (customer.transactions.isEmpty()) {
                    val row = listOf(
                        counter.toString(),
                        escapeCsv(customer.nama),
                        escapeCsv(noHp),
                        escapeCsv(statusProfil),
                        "-",
                        "-",
                        "0",
                        "-",
                        "-",
                        "-",
                        escapeCsv(customer.jenisLensa ?: "-"),
                        "-"
                    ).joinToString(",")
                    writer.write("$row\n")
                    counter++
                } else {
                    for (trx in customer.transactions) {
                        val od = trx.prescriptions.firstOrNull { it.eyeSide.equals("OD", ignoreCase = true) }
                        val os = trx.prescriptions.firstOrNull { it.eyeSide.equals("OS", ignoreCase = true) }

                        val odStr = if (od != null) "Sph: ${od.sphere}, Cyl: ${od.cylinder ?: 0}, Ax: ${od.axis ?: 0}" else "-"
                        val osStr = if (os != null) "Sph: ${os.sphere}, Cyl: ${os.cylinder ?: 0}, Ax: ${os.axis ?: 0}" else "-"
                        val lensType = od?.lensType ?: os?.lensType ?: customer.jenisLensa ?: "-"

                        val row = listOf(
                            counter.toString(),
                            escapeCsv(customer.nama),
                            escapeCsv(noHp),
                            escapeCsv(statusProfil),
                            escapeCsv(trx.transactionDate),
                            escapeCsv(trx.invoiceNumber),
                            trx.amount.toLong().toString(),
                            escapeCsv(trx.status),
                            escapeCsv(odStr),
                            escapeCsv(osStr),
                            escapeCsv(lensType),
                            escapeCsv(trx.notes ?: "-")
                        ).joinToString(",")
                        writer.write("$row\n")
                        counter++
                    }
                }
            }

            writer.flush()
            writer.close()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Export daftar pelanggan & transaksi ke format PDF yang rapi dan profesional
     */
    fun exportToPdf(
        context: Context,
        customers: List<CustomerSearchItem>,
        dateRangeText: String
    ): File? {
        return try {
            val pageWidth = 595 // A4 standard width in points (72 dpi)
            val pageHeight = 842 // A4 standard height in points
            val doc = PdfDocument()

            val titlePaint = Paint().apply {
                color = Color.parseColor("#111827")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#4B5563")
                textSize = 9f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#F3F4F6")
                style = Paint.Style.FILL
            }

            val headerTextPaint = Paint().apply {
                color = Color.parseColor("#1F2937")
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val rowTextPaint = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 7.5f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                strokeWidth = 0.75f
                style = Paint.Style.STROKE
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = doc.startPage(pageInfo)
            var canvas = page.canvas

            val startX = 30f
            val endX = pageWidth - 30f
            var currentY = 40f

            fun drawHeader() {
                // Header Toko / Brand
                canvas.drawText("OPTIK CENGKARENG", startX, currentY, titlePaint)
                currentY += 14f
                canvas.drawText("Laporan Data Pelanggan & Transaksi — Periode: $dateRangeText", startX, currentY, subtitlePaint)
                currentY += 12f
                canvas.drawText(
                    "Dicetak pada: ${SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())} | Total: ${customers.size} Pelanggan",
                    startX,
                    currentY,
                    subtitlePaint
                )
                currentY += 15f

                // Garis pembatas header
                canvas.drawLine(startX, currentY, endX, currentY, linePaint)
                currentY += 12f

                // Table Header Row Background
                val headerHeight = 18f
                canvas.drawRect(startX, currentY, endX, currentY + headerHeight, headerBgPaint)

                // Kolom: No(20), Nama(100), No HP(80), Tanggal(60), Invoice(80), Total(70), Resep OD/OS(125)
                canvas.drawText("No", startX + 5f, currentY + 12f, headerTextPaint)
                canvas.drawText("Nama Pelanggan", startX + 25f, currentY + 12f, headerTextPaint)
                canvas.drawText("No HP", startX + 130f, currentY + 12f, headerTextPaint)
                canvas.drawText("Tanggal", startX + 215f, currentY + 12f, headerTextPaint)
                canvas.drawText("No Invoice", startX + 275f, currentY + 12f, headerTextPaint)
                canvas.drawText("Total (Rp)", startX + 365f, currentY + 12f, headerTextPaint)
                canvas.drawText("Resep Kacamata", startX + 435f, currentY + 12f, headerTextPaint)

                currentY += headerHeight + 5f
            }

            drawHeader()

            var itemNo = 1
            val rowHeight = 22f

            for (customer in customers) {
                val transactions = if (customer.transactions.isNotEmpty()) customer.transactions else listOf(null)

                for (trx in transactions) {
                    // Cek jika butuh halaman baru
                    if (currentY + rowHeight > pageHeight - 40f) {
                        // Footer nomor halaman
                        canvas.drawText("Halaman $pageNumber", endX - 50f, pageHeight - 20f, subtitlePaint)
                        doc.finishPage(page)

                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = doc.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 40f
                        drawHeader()
                    }

                    val noHp = customer.noHp ?: "-"
                    val trxDate = trx?.transactionDate ?: "-"
                    val inv = trx?.invoiceNumber ?: "-"
                    val amountStr = if (trx != null) rupiahFormat.format(trx.amount).replace("Rp", "").trim() else "0"

                    val od = trx?.prescriptions?.firstOrNull { it.eyeSide.equals("OD", ignoreCase = true) }
                    val os = trx?.prescriptions?.firstOrNull { it.eyeSide.equals("OS", ignoreCase = true) }
                    val resepStr = when {
                        od != null && os != null -> "OD: ${od.sphere} / OS: ${os.sphere}"
                        od != null -> "OD: ${od.sphere}"
                        os != null -> "OS: ${os.sphere}"
                        else -> customer.jenisLensa ?: "-"
                    }

                    // Draw Row
                    canvas.drawText(itemNo.toString(), startX + 5f, currentY + 10f, rowTextPaint)
                    canvas.drawText(truncate(customer.nama, 18), startX + 25f, currentY + 10f, rowTextPaint)
                    canvas.drawText(truncate(noHp, 14), startX + 130f, currentY + 10f, rowTextPaint)
                    canvas.drawText(trxDate, startX + 215f, currentY + 10f, rowTextPaint)
                    canvas.drawText(truncate(inv, 13), startX + 275f, currentY + 10f, rowTextPaint)
                    canvas.drawText(amountStr, startX + 365f, currentY + 10f, rowTextPaint)
                    canvas.drawText(truncate(resepStr, 22), startX + 435f, currentY + 10f, rowTextPaint)

                    currentY += rowHeight
                    canvas.drawLine(startX, currentY - 6f, endX, currentY - 6f, linePaint)
                    itemNo++
                }
            }

            // Footer halaman terakhir
            canvas.drawText("Halaman $pageNumber", endX - 50f, pageHeight - 20f, subtitlePaint)
            canvas.drawText("Dokumen ini digenerate otomatis oleh Sistem CRM Optik Cengkareng", startX, pageHeight - 20f, subtitlePaint)
            doc.finishPage(page)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Laporan_OptikCRM_${timeStamp}.pdf"
            val file = File(context.cacheDir, fileName)

            val fos = FileOutputStream(file)
            doc.writeTo(fos)
            doc.close()
            fos.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Membuka Android ShareSheet untuk mengirim file via WhatsApp, Gmail, dsb.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Bagikan Laporan CRM") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Berikut terlampir laporan dari Optik CRM: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\"", "\"\"")
        if (str.contains(",") || str.contains("\n") || str.contains("\"")) {
            str = "\"$str\""
        }
        return str
    }

    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.take(maxLength - 2) + ".."
        } else {
            text
        }
    }
}
