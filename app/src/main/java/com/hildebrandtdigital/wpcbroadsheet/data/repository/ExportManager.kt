package com.hildebrandtdigital.wpcbroadsheet.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.hildebrandtdigital.wpcbroadsheet.data.model.SiteMonthlyReport
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

object ExportManager {

    private const val DOWNLOADS_SUBFOLDER = "WPC Broadsheet App"

    private fun monthName(month: Int): String =
        SimpleDateFormat("MMMM", Locale.getDefault())
            .format(GregorianCalendar(2000, month - 1, 1).time)

    private fun reportFileName(report: SiteMonthlyReport, ext: String): String {
        val site = report.site.name.replace(" ", "_")
        val month = monthName(report.month)
        return "WPC_${site}_${month}_${report.year}.$ext"
    }

    // ─────────────────────────────────────────────────────────────────────
    // CSV EXPORT
    // ─────────────────────────────────────────────────────────────────────
    fun exportCsv(context: Context, report: SiteMonthlyReport): Uri {
        val sb = StringBuilder()

        sb.appendLine("WPC Broadsheet — ${report.site.name}")
        sb.appendLine("${monthName(report.month)} ${report.year}")
        sb.appendLine()
        sb.appendLine("BILLING SUMMARY")
        sb.appendLine("Total Meals Served,${report.grandTotalMeals}")
        sb.appendLine("Revenue (excl. VAT),${BillingCalculator.formatRand(report.grandSubtotal)}")
        sb.appendLine("VAT (15%),${BillingCalculator.formatRand(report.grandVat)}")
        sb.appendLine("T/A Bakkies,${BillingCalculator.formatRand(report.grandTaBakkies)}")
        val totalDeduction = report.residentBillings.sumOf { it.compulsoryDeduction }
        sb.appendLine("Less Compulsory Meals,${BillingCalculator.formatRand(-totalDeduction)}")
        sb.appendLine("TOTAL BILLED,${BillingCalculator.formatRand(report.grandTotal)}")
        sb.appendLine()
        sb.appendLine("PER-RESIDENT BILLING")
        sb.appendLine("Unit,Resident,Total Meals,Subtotal (excl VAT),VAT (15%),T/A Bakkies,Deduction,TOTAL BILLED")

        report.residentBillings.sortedBy { it.resident.unitNumber }.forEach { b ->
            sb.appendLine(
                "${b.resident.unitNumber}," +
                        "\"${b.resident.clientName}\"," +
                        "${b.totalMeals}," +
                        "${BillingCalculator.formatRand(b.subtotalExclVat)}," +
                        "${BillingCalculator.formatRand(b.vat)}," +
                        "${BillingCalculator.formatRand(b.taBakkiesTotal)}," +
                        "${BillingCalculator.formatRand(-b.compulsoryDeduction)}," +
                        "${BillingCalculator.formatRand(b.finalTotal)}"
            )
        }

        sb.appendLine(
            "TOTALS,," +
                    "${report.grandTotalMeals}," +
                    "${BillingCalculator.formatRand(report.grandSubtotal)}," +
                    "${BillingCalculator.formatRand(report.grandVat)}," +
                    "${BillingCalculator.formatRand(report.grandTaBakkies)}," +
                    "${BillingCalculator.formatRand(-totalDeduction)}," +
                    "${BillingCalculator.formatRand(report.grandTotal)}"
        )

        val fileName = reportFileName(report, "csv")
        val bytes = sb.toString().toByteArray(Charsets.UTF_8)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeToDownloadsQPlus(
                context = context,
                displayName = fileName,
                mimeType = "text/csv",
                bytesProvider = { bytes }
            )
        } else {
            // API 26–28: legacy public Downloads (requires WRITE_EXTERNAL_STORAGE runtime permission)
            val file = writeToDownloadsLegacy(fileName, bytes)
            fileProviderUri(context, file)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PDF EXPORT
    // ─────────────────────────────────────────────────────────────────────
    fun exportPdf(context: Context, report: SiteMonthlyReport): Uri {
        val pageWidth = 595
        val pageHeight = 842

        val document = PdfDocument()
        var pageNum = 1

        var page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        )
        var canvas = page.canvas
        var y = 60f

        fun newPageIfNeeded() {
            if (y > pageHeight - 80f) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                )
                canvas = page.canvas
                y = 40f
            }
        }

        // Header bar
        canvas.drawRect(
            0f, 0f, pageWidth.toFloat(), 50f,
            Paint().apply { color = Color.parseColor("#0B0F1A") }
        )
        canvas.drawText(
            "WPC Broadsheet", 20f, 32f,
            Paint().apply { color = Color.WHITE; textSize = 18f; isFakeBoldText = true }
        )
        canvas.drawText(
            "${report.site.name}  ·  ${monthName(report.month)} ${report.year}",
            200f, 32f, Paint().apply { color = Color.LTGRAY; textSize = 11f }
        )

        y = 70f

        // Summary box
        val totalDeduction = report.residentBillings.sumOf { it.compulsoryDeduction }
        canvas.drawRect(
            20f, y, pageWidth - 20f, y + 138f,
            Paint().apply { color = Color.parseColor("#1A1F2E") }
        )
        y += 20f
        canvas.drawText(
            "BILLING SUMMARY", 36f, y,
            Paint().apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true }
        )
        y += 18f

        listOf(
            "Total Meals Served" to "${report.grandTotalMeals}",
            "Revenue (excl. VAT)" to BillingCalculator.formatRand(report.grandSubtotal),
            "VAT (15%)" to BillingCalculator.formatRand(report.grandVat),
            "T/A Bakkies" to BillingCalculator.formatRand(report.grandTaBakkies),
            "Less: Compulsory" to "−${BillingCalculator.formatRand(totalDeduction)}",
        ).forEach { (label, value) ->
            canvas.drawText(label, 36f, y, Paint().apply { color = Color.LTGRAY; textSize = 9f })
            canvas.drawText(
                value, pageWidth - 36f, y,
                Paint().apply { color = Color.WHITE; textSize = 9f; textAlign = Paint.Align.RIGHT }
            )
            y += 16f
        }

        canvas.drawLine(36f, y, pageWidth - 36f, y, Paint().apply { color = Color.DKGRAY })
        y += 14f

        canvas.drawText(
            "TOTAL BILLED", 36f, y,
            Paint().apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true }
        )
        canvas.drawText(
            BillingCalculator.formatRand(report.grandTotal),
            pageWidth - 36f, y,
            Paint().apply {
                color = Color.parseColor("#4FF7C8")
                textSize = 11f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }
        )
        y += 30f

        // Table header
        val cols = listOf(20f, 60f, 270f, 355f, 430f, pageWidth.toFloat() - 20f)
        canvas.drawRect(
            20f, y, pageWidth - 20f, y + 22f,
            Paint().apply { color = Color.parseColor("#2A3050") }
        )
        y += 15f

        listOf("Unit", "Resident", "Meals", "Subtotal", "Total").forEachIndexed { i, h ->
            canvas.drawText(
                h, cols[i] + 4f, y,
                Paint().apply { color = Color.WHITE; textSize = 9f; isFakeBoldText = true }
            )
        }
        y += 12f

        val rowLight = Paint().apply { color = Color.parseColor("#F5F5F5") }
        val rowWhite = Paint().apply { color = Color.WHITE }

        report.residentBillings.sortedBy { it.resident.unitNumber }.forEachIndexed { idx, b ->
            newPageIfNeeded()

            canvas.drawRect(
                20f, y - 10f, pageWidth - 20f, y + 8f,
                if (idx % 2 == 0) rowLight else rowWhite
            )

            canvas.drawText(
                b.resident.unitNumber, cols[0] + 4f, y,
                Paint().apply { color = Color.DKGRAY; textSize = 9f }
            )

            val name =
                if (b.resident.clientName.length > 26) b.resident.clientName.take(24) + "…"
                else b.resident.clientName

            canvas.drawText(
                name, cols[1] + 4f, y,
                Paint().apply { color = Color.BLACK; textSize = 9f }
            )

            canvas.drawText(
                "${b.totalMeals}", cols[2] + 4f, y,
                Paint().apply { color = Color.parseColor("#0055AA"); textSize = 9f }
            )

            canvas.drawText(
                BillingCalculator.formatRand(b.subtotalExclVat), cols[3] + 4f, y,
                Paint().apply { color = Color.BLACK; textSize = 9f }
            )

            canvas.drawText(
                BillingCalculator.formatRand(b.finalTotal), cols[4] + 4f, y,
                Paint().apply {
                    color = if (b.isCredit) Color.RED else Color.parseColor("#007A5E")
                    textSize = 9f
                    isFakeBoldText = true
                }
            )

            y += 18f
        }

        document.finishPage(page)

        val fileName = reportFileName(report, "pdf")

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeToDownloadsQPlus(
                    context = context,
                    displayName = fileName,
                    mimeType = "application/pdf",
                    pdfProvider = { document }
                )
            } else {
                // API 26–28: legacy public Downloads (requires WRITE_EXTERNAL_STORAGE runtime permission)
                val dir = downloadsDirLegacy()
                val file = File(dir, fileName)
                FileOutputStream(file).use { document.writeTo(it) }
                fileProviderUri(context, file)
            }
        } finally {
            document.close()
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONSOLIDATED (ALL SITES) EXPORTS
    // ─────────────────────────────────────────────────────────────────────

    fun exportConsolidatedCsv(context: Context, reports: List<SiteMonthlyReport>): Uri {
        val first = reports.first()
        val sb = StringBuilder()
        sb.appendLine("WPC Broadsheet — All Sites Consolidated")
        sb.appendLine("${monthName(first.month)} ${first.year}")
        sb.appendLine()

        // Grand totals
        val grandMeals    = reports.sumOf { it.grandTotalMeals }
        val grandSubtotal = reports.sumOf { it.grandSubtotal }
        val grandVat      = reports.sumOf { it.grandVat }
        val grandBakkies  = reports.sumOf { it.grandTaBakkies }
        val grandTotal    = reports.sumOf { it.grandTotal }
        val grandDeduct   = reports.sumOf { r -> r.residentBillings.sumOf { it.compulsoryDeduction } }

        sb.appendLine("CONSOLIDATED SUMMARY")
        sb.appendLine("Total Meals Served,${grandMeals}")
        sb.appendLine("Revenue (excl. VAT),${BillingCalculator.formatRand(grandSubtotal)}")
        sb.appendLine("VAT (15%),${BillingCalculator.formatRand(grandVat)}")
        sb.appendLine("T/A Bakkies,${BillingCalculator.formatRand(grandBakkies)}")
        sb.appendLine("Less Compulsory Meals,${BillingCalculator.formatRand(-grandDeduct)}")
        sb.appendLine("TOTAL BILLED,${BillingCalculator.formatRand(grandTotal)}")
        sb.appendLine()

        // Per-site breakdown
        reports.forEach { report ->
            sb.appendLine("─── ${report.site.name} ───")
            sb.appendLine("Unit,Resident,Total Meals,Subtotal (excl VAT),VAT (15%),T/A Bakkies,Deduction,TOTAL BILLED")
            report.residentBillings.sortedBy { it.resident.unitNumber }.forEach { b ->
                sb.appendLine(
                    "${b.resident.unitNumber}," +
                    "\"${b.resident.clientName}\"," +
                    "${b.totalMeals}," +
                    "${BillingCalculator.formatRand(b.subtotalExclVat)}," +
                    "${BillingCalculator.formatRand(b.vat)}," +
                    "${BillingCalculator.formatRand(b.taBakkiesTotal)}," +
                    "${BillingCalculator.formatRand(-b.compulsoryDeduction)}," +
                    "${BillingCalculator.formatRand(b.finalTotal)}"
                )
            }
            val siteDeduct = report.residentBillings.sumOf { it.compulsoryDeduction }
            sb.appendLine(
                "SITE TOTAL,,${report.grandTotalMeals}," +
                "${BillingCalculator.formatRand(report.grandSubtotal)}," +
                "${BillingCalculator.formatRand(report.grandVat)}," +
                "${BillingCalculator.formatRand(report.grandTaBakkies)}," +
                "${BillingCalculator.formatRand(-siteDeduct)}," +
                "${BillingCalculator.formatRand(report.grandTotal)}"
            )
            sb.appendLine()
        }

        val fileName = "WPC_All_Sites_${monthName(first.month)}_${first.year}.csv"
        val bytes    = sb.toString().toByteArray(Charsets.UTF_8)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            writeToDownloadsQPlus(context, fileName, "text/csv", bytesProvider = { bytes })
        } else {
            val file = writeToDownloadsLegacy(fileName, bytes)
            fileProviderUri(context, file)
        }
    }

    fun exportConsolidatedPdf(context: Context, reports: List<SiteMonthlyReport>): Uri {
        val first  = reports.first()
        val doc    = PdfDocument()
        val paint  = Paint().apply { isAntiAlias = true }
        val pageW  = 595; val pageH = 842   // A4 points
        val margin = 40f
        var y      = 0f

        fun newPage(): PdfDocument.Page {
            val pi   = PdfDocument.PageInfo.Builder(pageW, pageH, doc.pages.size + 1).create()
            val page = doc.startPage(pi)
            y = margin + 20f
            return page
        }

        fun drawHeader(canvas: android.graphics.Canvas) {
            paint.color = android.graphics.Color.parseColor("#0D1428")
            canvas.drawRect(0f, 0f, pageW.toFloat(), 60f, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 16f; paint.isFakeBoldText = true
            canvas.drawText("WPC Broadsheet — All Sites Consolidated", margin, 28f, paint)
            paint.textSize = 10f; paint.isFakeBoldText = false
            canvas.drawText("${monthName(first.month)} ${first.year}", margin, 46f, paint)
            y = 80f
        }

        fun PdfDocument.Page.line(canvas: android.graphics.Canvas) {
            paint.color = android.graphics.Color.parseColor("#1E2A3A")
            paint.strokeWidth = 0.5f
            canvas.drawLine(margin, y, (pageW - margin), y, paint)
            y += 8f
        }

        fun PdfDocument.Page.row(canvas: android.graphics.Canvas, label: String, value: String, bold: Boolean = false, valueColor: Int = android.graphics.Color.parseColor("#A0B4CC")) {
            paint.color = android.graphics.Color.parseColor("#C8D8E8")
            paint.textSize = 9f; paint.isFakeBoldText = bold
            canvas.drawText(label, margin, y, paint)
            paint.color = valueColor
            val vw = paint.measureText(value)
            canvas.drawText(value, (pageW - margin) - vw, y, paint)
            paint.isFakeBoldText = false
            y += 14f
        }

        var page = newPage()
        var canvas = page.canvas
        drawHeader(canvas)

        // Grand totals block
        val grandMeals    = reports.sumOf { it.grandTotalMeals }
        val grandTotal    = reports.sumOf { it.grandTotal }
        val grandSubtotal = reports.sumOf { it.grandSubtotal }
        val grandVat      = reports.sumOf { it.grandVat }
        val grandBakkies  = reports.sumOf { it.grandTaBakkies }
        val grandDeduct   = reports.sumOf { r -> r.residentBillings.sumOf { it.compulsoryDeduction } }

        paint.color = android.graphics.Color.parseColor("#111E2E"); paint.textSize = 11f; paint.isFakeBoldText = true
        canvas.drawText("CONSOLIDATED SUMMARY", margin, y, paint); y += 18f
        paint.isFakeBoldText = false

        page.row(canvas, "Total Meals Served", "$grandMeals")
        page.row(canvas, "Revenue (excl. VAT)", BillingCalculator.formatRand(grandSubtotal))
        page.row(canvas, "VAT (15%)", BillingCalculator.formatRand(grandVat))
        page.row(canvas, "T/A Bakkies", BillingCalculator.formatRand(grandBakkies))
        page.row(canvas, "Less Compulsory Meals", "−${BillingCalculator.formatRand(grandDeduct)}", valueColor = android.graphics.Color.parseColor("#E05555"))
        page.line(canvas)
        page.row(canvas, "TOTAL BILLED", BillingCalculator.formatRand(grandTotal), bold = true, valueColor = android.graphics.Color.parseColor("#4ECFA8"))
        y += 12f

        // Per-site tables
        reports.forEach { report ->
            if (y > pageH - 120) {
                doc.finishPage(page)
                page = newPage()
                canvas = page.canvas
            }

            paint.color = android.graphics.Color.parseColor("#111E2E"); paint.textSize = 10f; paint.isFakeBoldText = true
            canvas.drawText(report.site.name, margin, y, paint); y += 16f
            paint.isFakeBoldText = false

            // Table header
            paint.color = android.graphics.Color.parseColor("#1A2840")
            canvas.drawRect(margin, y - 10f, (pageW - margin), y + 4f, paint)
            paint.color = android.graphics.Color.parseColor("#8899AA"); paint.textSize = 7f
            canvas.drawText("UNIT", margin + 2f, y, paint)
            canvas.drawText("RESIDENT", margin + 36f, y, paint)
            canvas.drawText("MEALS", margin + 220f, y, paint)
            canvas.drawText("AMOUNT", (pageW - margin) - 60f, y, paint)
            y += 14f

            report.residentBillings.sortedBy { it.resident.unitNumber }.forEach { b ->
                if (y > pageH - 60) {
                    doc.finishPage(page); page = newPage(); canvas = page.canvas
                }
                paint.color = android.graphics.Color.parseColor("#C8D8E8"); paint.textSize = 8f
                canvas.drawText(b.resident.unitNumber, margin + 2f, y, paint)
                canvas.drawText(b.resident.clientName.take(28), margin + 36f, y, paint)
                canvas.drawText("${b.totalMeals}", margin + 220f, y, paint)
                val amtColor = if (b.isCredit) android.graphics.Color.parseColor("#E05555")
                               else android.graphics.Color.parseColor("#4ECFA8")
                paint.color = amtColor
                val amtText = BillingCalculator.formatRand(b.finalTotal)
                val amtW    = paint.measureText(amtText)
                canvas.drawText(amtText, (pageW - margin) - amtW, y, paint)
                y += 12f
            }
            y += 8f
        }

        doc.finishPage(page)

        val fileName = "WPC_All_Sites_${monthName(first.month)}_${first.year}.pdf"
        val pdfBytes = java.io.ByteArrayOutputStream().also { doc.writeTo(it); doc.close() }.toByteArray()
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            writeToDownloadsQPlus(context, fileName, "application/pdf", bytesProvider = { pdfBytes })
        } else {
            val file = writeToDownloadsLegacy(fileName, pdfBytes)
            fileProviderUri(context, file)
        }
    }

    fun shareConsolidatedViaEmail(context: Context, reports: List<SiteMonthlyReport>, csvUri: Uri, pdfUri: Uri) {
        val first   = reports.first()
        val month   = monthName(first.month)
        val subject = "WPC Broadsheet — All Sites — $month ${first.year}"
        val grandTotal = reports.sumOf { it.grandTotal }
        val body = buildString {
            appendLine("Dear Management,")
            appendLine()
            appendLine("Please find attached the consolidated billing report for all sites — $month ${first.year}.")
            appendLine()
            appendLine("SITE BREAKDOWN")
            appendLine("═══════════════════════════════")
            reports.forEach { r ->
                appendLine("${r.site.name.padEnd(25)} ${BillingCalculator.formatRand(r.grandTotal)}")
            }
            appendLine("───────────────────────────────")
            appendLine("TOTAL BILLED       : ${BillingCalculator.formatRand(grandTotal)}")
            appendLine()
            appendLine("Attachments: CSV spreadsheet (opens in Excel) and PDF report.")
            appendLine()
            appendLine("Regards,")
            appendLine("WPC Broadsheet Manager")
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(csvUri, pdfUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Send Report Via…"))
    }

    // ─────────────────────────────────────────────────────────────────────
    // EMAIL INTENT
    // ─────────────────────────────────────────────────────────────────────
    fun shareViaEmail(context: Context, report: SiteMonthlyReport, csvUri: Uri, pdfUri: Uri) {
        val month = monthName(report.month)
        val subject = "WPC Broadsheet — ${report.site.name} — $month ${report.year}"
        val body = buildString {
            appendLine("Dear ${report.site.unitManagerName},")
            appendLine()
            appendLine("Please find attached the billing report for ${report.site.name} — $month ${report.year}.")
            appendLine()
            appendLine("SUMMARY")
            appendLine("═══════════════════════════════")
            appendLine("Total Meals Served : ${report.grandTotalMeals}")
            appendLine("Revenue (excl VAT) : ${BillingCalculator.formatRand(report.grandSubtotal)}")
            appendLine("VAT (15%)          : ${BillingCalculator.formatRand(report.grandVat)}")
            appendLine("T/A Bakkies        : ${BillingCalculator.formatRand(report.grandTaBakkies)}")
            appendLine("───────────────────────────────")
            appendLine("TOTAL BILLED       : ${BillingCalculator.formatRand(report.grandTotal)}")
            appendLine()
            appendLine("Attachments: CSV spreadsheet (opens in Excel) and PDF report.")
            appendLine()
            appendLine("Regards,")
            appendLine("WPC Broadsheet Manager")
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(report.site.unitManagerEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(csvUri, pdfUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Send Report Via…"))
    }

    // ─────────────────────────────────────────────────────────────────────
    // Android 10+ (API 29+) - MediaStore Downloads
    // ─────────────────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadsRelativePathQPlus(): String =
        Environment.DIRECTORY_DOWNLOADS + File.separator + DOWNLOADS_SUBFOLDER + File.separator

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadsCollectionQPlus(): Uri =
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteIfExistsQPlus(context: Context, displayName: String) {
        val resolver = context.contentResolver
        val collection = downloadsCollectionQPlus()
        val projection = arrayOf(MediaStore.MediaColumns._ID)

        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val args = arrayOf(displayName, downloadsRelativePathQPlus())

        resolver.query(collection, projection, selection, args, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val itemUri = ContentUris.withAppendedId(collection, id)
                resolver.delete(itemUri, null, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertDownloadsItemQPlus(
        context: Context,
        displayName: String,
        mimeType: String
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, downloadsRelativePathQPlus())
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        return resolver.insert(downloadsCollectionQPlus(), values)
            ?: throw IOException("MediaStore insert failed for $displayName")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun finalizePendingQPlus(context: Context, uri: Uri) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        context.contentResolver.update(uri, values, null, null)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToDownloadsQPlus(
        context: Context,
        displayName: String,
        mimeType: String,
        bytesProvider: (() -> ByteArray)? = null,
        pdfProvider: (() -> PdfDocument)? = null
    ): Uri {
        // Avoid duplicates like "file (1).pdf" by deleting existing first
        deleteIfExistsQPlus(context, displayName)

        val uri = insertDownloadsItemQPlus(context, displayName, mimeType)
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { os ->
                when {
                    bytesProvider != null -> os.write(bytesProvider())
                    pdfProvider != null -> pdfProvider().writeTo(os)
                    else -> throw IllegalArgumentException("No content provider supplied")
                }
            } ?: throw IOException("Failed to open output stream for $uri")

            finalizePendingQPlus(context, uri)
            return uri
        } catch (t: Throwable) {
            // Don't leave orphaned records
            context.contentResolver.delete(uri, null, null)
            throw t
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Legacy (API 26–28): public Downloads folder
    // ─────────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun downloadsDirLegacy(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDir = File(downloads, DOWNLOADS_SUBFOLDER)
        if (!appDir.exists()) appDir.mkdirs()
        return appDir
    }

    private fun writeToDownloadsLegacy(displayName: String, bytes: ByteArray): File {
        val dir = downloadsDirLegacy()
        val file = File(dir, displayName)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    private fun fileProviderUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
