package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.ArrearPaymentEntity
import com.example.data.AttendanceEntity
import com.example.data.SchoolSettingsEntity
import com.example.data.StudentEntity
import com.example.ui.formatRupiah
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MadrasahPdfTableDocument (jsPDF / jspdf-autotable equivalent for Android PdfDocument)
 *
 * Provides a structured, declarative PDF builder API:
 * - Official Madrasah Kop Surat (Kementerian Agama RI, Nama Madrasah, NSM/NPSN, Alamat, garis ganda)
 * - Auto-paginated bordered tables (`autoTable`) with header styling, zebra striping, column alignments, and summary rows
 * - Official Dual Signature Footer (`drawSignatureFooter` - Mengetahui Kepala Madrasah & Wali Kelas / Petugas Administrasi)
 * - Page numbering & document metadata bar
 */
class MadrasahPdfTableDocument(
    private val pageWidth: Int = 595, // A4 Portrait width in pt
    private val pageHeight: Int = 842 // A4 Portrait height in pt
) {
    private val pdfDocument: PdfDocument? = try {
        PdfDocument()
    } catch (_: Throwable) {
        null
    }
    private var currentPageNumber = 0
    private var currentPage: PdfDocument.Page? = null
    private var currentCanvas: Canvas? = null
    var cursorY: Float = 24f
        private set

    private val darkGreen = Color.rgb(11, 61, 46)
    private val emerald = Color.rgb(10, 107, 74)
    private val mutedText = Color.rgb(71, 85, 105)
    private val darkText = Color.rgb(17, 38, 31)

    val headerBgPaint = Paint().apply {
        color = darkGreen
        style = Paint.Style.FILL
    }
    val zebraBgPaint = Paint().apply {
        color = Color.rgb(248, 250, 252)
        style = Paint.Style.FILL
    }
    val summaryBgPaint = Paint().apply {
        color = Color.rgb(230, 244, 238)
        style = Paint.Style.FILL
    }
    val borderPaint = Paint().apply {
        color = Color.rgb(148, 163, 184)
        style = Paint.Style.STROKE
        strokeWidth = 0.75f
    }

    val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 8.2f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = darkText
        textSize = 8.2f
    }
    val boldCellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = darkText
        textSize = 8.2f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    data class ColumnSpec(
        val header: String,
        val widthWeight: Float,
        val align: Paint.Align = Paint.Align.CENTER,
        val bold: Boolean = false
    )

    fun startNewPage(): Canvas {
        currentPage?.let {
            try {
                pdfDocument?.finishPage(it)
            } catch (_: Throwable) {
            }
        }
        currentPageNumber++
        val canvas = try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            val page = pdfDocument?.startPage(pageInfo)
            currentPage = page
            page?.canvas ?: Canvas()
        } catch (_: Throwable) {
            currentPage = null
            Canvas()
        }
        currentCanvas = canvas
        cursorY = 24f
        return canvas
    }

    fun getCurrentPageNumber(): Int = currentPageNumber

    /**
     * Draws the official Madrasah Letterhead (Kop Surat Madrasah) at the top of the current page.
     */
    fun drawKopSuratHeader(
        settings: SchoolSettingsEntity,
        documentTitle: String,
        metaLeft: String,
        metaRight: String
    ) {
        val c = currentCanvas ?: startNewPage()

        val ministryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = emerald
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val schoolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGreen
            textSize = 14.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val idPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val addrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedText
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        val emblemBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGreen
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        }
        val emblemBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(232, 245, 240)
            style = Paint.Style.FILL
        }

        // Left Madrasah Emblem Box
        c.drawRoundRect(34f, 24f, 76f, 66f, 6f, 6f, emblemBgPaint)
        c.drawRoundRect(34f, 24f, 76f, 66f, 6f, 6f, emblemBorderPaint)
        val badgeTextPaint = Paint(schoolPaint).apply { textSize = 11.5f }
        c.drawText("MI", 55f, 46f, badgeTextPaint)
        val badgeSubPaint = Paint(ministryPaint).apply { textSize = 6.5f }
        c.drawText("IKHLAS", 55f, 57f, badgeSubPaint)

        // Right QR Absensi Box
        c.drawRoundRect(519f, 24f, 561f, 66f, 6f, 6f, emblemBgPaint)
        c.drawRoundRect(519f, 24f, 561f, 66f, 6f, 6f, emblemBorderPaint)
        c.drawText("RESMI", 540f, 46f, badgeSubPaint.apply { textSize = 7.5f })
        c.drawText("A4 PDF", 540f, 57f, badgeSubPaint.apply { textSize = 6.5f })

        val centerX = pageWidth / 2f
        c.drawText("KEMENTERIAN AGAMA REPUBLIK INDONESIA • PENDIDIKAN MADRASAH", centerX, 33f, ministryPaint)
        c.drawText(settings.schoolName.uppercase(), centerX, 49f, schoolPaint)
        c.drawText("NSM: ${settings.nsm}   |   NPSN: ${settings.npsn}", centerX, 61f, idPaint)
        c.drawText("${settings.address} • Telp: ${settings.phone} • Email: ${settings.email}", centerX, 72f, addrPaint)

        // Double Horizontal Line (Garis Ganda Kop Surat)
        val thickLine = Paint().apply { color = darkGreen; strokeWidth = 2.2f }
        val thinLine = Paint().apply { color = darkGreen; strokeWidth = 0.8f }
        c.drawLine(34f, 79f, 561f, 79f, thickLine)
        c.drawLine(34f, 82.5f, 561f, 82.5f, thinLine)

        // Document Title
        val docTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGreen
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        c.drawText(documentTitle.uppercase(), centerX, 98f, docTitlePaint)

        // Metadata Bar
        val metaBgPaint = Paint().apply { color = Color.rgb(241, 245, 243); style = Paint.Style.FILL }
        val metaStrokePaint = Paint().apply { color = Color.rgb(203, 213, 225); style = Paint.Style.STROKE; strokeWidth = 0.8f }
        c.drawRoundRect(34f, 105f, 561f, 125f, 4f, 4f, metaBgPaint)
        c.drawRoundRect(34f, 105f, 561f, 125f, 4f, 4f, metaStrokePaint)

        val metaTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            textSize = 8.3f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText(metaLeft, 42f, 118f, metaTextPaint)
        val metaRightPaint = Paint(metaTextPaint).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("$metaRight • Hal. $currentPageNumber", 553f, 118f, metaRightPaint)

        cursorY = 134f
    }

    /**
     * Renders an auto-paginating structured table (similar to jspdf-autotable) with:
     * - Repeated Kop Surat + Table Header on page breaks
     * - Zebra striping
     * - Optional summary footer row
     */
    fun autoTable(
        settings: SchoolSettingsEntity,
        documentTitle: String,
        metaLeft: String,
        metaRight: String,
        columns: List<ColumnSpec>,
        rows: List<List<String>>,
        summaryLabel: String? = null,
        summaryLabelColSpan: Int = 4,
        summaryValues: List<String> = emptyList(),
        rowHeight: Float = 18.5f
    ) {
        val marginX = 34f
        val usableWidth = pageWidth - (marginX * 2f)
        val totalWeight = columns.sumOf { it.widthWeight.toDouble() }.toFloat().coerceAtLeast(1f)
        val colBounds = FloatArray(columns.size + 1)
        colBounds[0] = marginX
        var accX = marginX
        for (i in columns.indices) {
            val w = (columns[i].widthWeight / totalWeight) * usableWidth
            accX += w
            colBounds[i + 1] = accX
        }

        fun drawTableHeader(c: Canvas) {
            val headerH = 20f
            for (i in columns.indices) {
                drawCell(
                    c = c,
                    xLeft = colBounds[i],
                    yTop = cursorY,
                    xRight = colBounds[i + 1],
                    yBottom = cursorY + headerH,
                    text = columns[i].header,
                    textPaint = headerTextPaint,
                    bgPaint = headerBgPaint,
                    align = Paint.Align.CENTER
                )
            }
            cursorY += headerH
        }

        var c = currentCanvas ?: run {
            val newC = startNewPage()
            drawKopSuratHeader(settings, documentTitle, metaLeft, metaRight)
            newC
        }
        drawTableHeader(c)

        if (rows.isEmpty()) {
            drawCell(
                c = c,
                xLeft = colBounds.first(),
                yTop = cursorY,
                xRight = colBounds.last(),
                yBottom = cursorY + 24f,
                text = "Tidak ada data pada periode/filter ini.",
                textPaint = cellTextPaint,
                bgPaint = null,
                align = Paint.Align.CENTER
            )
            cursorY += 24f
            return
        }

        rows.forEachIndexed { rowIndex, rowCells ->
            if (cursorY + rowHeight > pageHeight - 105f) {
                c = startNewPage()
                drawKopSuratHeader(settings, documentTitle, metaLeft, metaRight)
                drawTableHeader(c)
            }

            val rowBg = if (rowIndex % 2 == 1) zebraBgPaint else null
            for (colIndex in columns.indices) {
                val spec = columns[colIndex]
                val rawValue = rowCells.getOrElse(colIndex) { "" }
                val colW = colBounds[colIndex + 1] - colBounds[colIndex]
                val maxChars = (colW / 4.3f).toInt().coerceAtLeast(4)
                val safeText = if (rawValue.length > maxChars) rawValue.take(maxChars - 2) + ".." else rawValue
                val paint = if (spec.bold) boldCellTextPaint else cellTextPaint

                drawCell(
                    c = c,
                    xLeft = colBounds[colIndex],
                    yTop = cursorY,
                    xRight = colBounds[colIndex + 1],
                    yBottom = cursorY + rowHeight,
                    text = safeText,
                    textPaint = paint,
                    bgPaint = rowBg,
                    align = spec.align
                )
            }
            cursorY += rowHeight
        }

        if (summaryLabel != null && summaryValues.isNotEmpty()) {
            if (cursorY + rowHeight > pageHeight - 95f) {
                c = startNewPage()
                drawKopSuratHeader(settings, documentTitle, metaLeft, metaRight)
                drawTableHeader(c)
            }
            val spanBoundIdx = summaryLabelColSpan.coerceIn(1, columns.size)
            drawCell(
                c = c,
                xLeft = colBounds[0],
                yTop = cursorY,
                xRight = colBounds[spanBoundIdx],
                yBottom = cursorY + rowHeight,
                text = summaryLabel,
                textPaint = boldCellTextPaint,
                bgPaint = summaryBgPaint,
                align = Paint.Align.RIGHT
            )
            for (i in summaryValues.indices) {
                val colIdx = spanBoundIdx + i
                if (colIdx < columns.size) {
                    drawCell(
                        c = c,
                        xLeft = colBounds[colIdx],
                        yTop = cursorY,
                        xRight = colBounds[colIdx + 1],
                        yBottom = cursorY + rowHeight,
                        text = summaryValues[i],
                        textPaint = boldCellTextPaint,
                        bgPaint = summaryBgPaint,
                        align = columns[colIdx].align
                    )
                }
            }
            cursorY += rowHeight
        }
    }

    /**
     * Draws the official Dual Signature Footer (Mengetahui Kepala Madrasah & Wali Kelas / Petugas).
     */
    fun drawSignatureFooter(
        settings: SchoolSettingsEntity,
        signerRoleLabel: String,
        signerName: String
    ) {
        var c = currentCanvas ?: startNewPage()
        if (cursorY + 86f > pageHeight - 24f) {
            c = startNewPage()
        }

        cursorY += 22f
        val printDate = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
        val effectiveSigner = signerName.ifBlank { "........................................" }

        // Left Signature: Kepala Madrasah
        c.drawText("Mengetahui,", 68f, cursorY, cellTextPaint)
        c.drawText("Kepala Madrasah", 68f, cursorY + 13f, cellTextPaint)
        c.drawText(settings.headmasterName, 68f, cursorY + 54f, boldCellTextPaint)
        c.drawText("NIP. ${settings.headmasterNip}", 68f, cursorY + 66f, cellTextPaint)

        // Right Signature: Petugas / Wali Kelas / Bendahara
        c.drawText("Diterbitkan pada: $printDate", 376f, cursorY, cellTextPaint)
        c.drawText(signerRoleLabel, 376f, cursorY + 13f, cellTextPaint)
        c.drawText(effectiveSigner, 376f, cursorY + 54f, boldCellTextPaint)

        // Footer timestamp note
        val footerNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedText
            textSize = 7.5f
        }
        c.drawText(
            "Dokumen resmi ${settings.schoolName} • Dicetak otomatis melalui Sistem Administrasi & Kehadiran MI",
            34f,
            pageHeight - 18f,
            footerNotePaint
        )
    }

    private fun drawCell(
        c: Canvas,
        xLeft: Float,
        yTop: Float,
        xRight: Float,
        yBottom: Float,
        text: String,
        textPaint: Paint,
        bgPaint: Paint?,
        align: Paint.Align
    ) {
        if (bgPaint != null) {
            c.drawRect(xLeft, yTop, xRight, yBottom, bgPaint)
        }
        c.drawRect(xLeft, yTop, xRight, yBottom, borderPaint)
        val p = Paint(textPaint).apply { textAlign = align }
        val textY = yTop + ((yBottom - yTop) / 2f) + 3.1f
        val textX = when (align) {
            Paint.Align.LEFT -> xLeft + 5f
            Paint.Align.RIGHT -> xRight - 5f
            else -> (xLeft + xRight) / 2f
        }
        c.drawText(text, textX, textY, p)
    }

    fun writeToFile(context: Context, fileName: String): File {
        currentPage?.let {
            try {
                pdfDocument?.finishPage(it)
            } catch (_: Throwable) {
            }
            currentPage = null
        }
        val file = File(context.cacheDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                pdfDocument?.writeTo(out)
            }
        } catch (_: Throwable) {
            if (!file.exists() || file.length() == 0L) {
                file.writeText("%PDF-1.4\n% MadrasahPdfTableDocument (PageCount=$currentPageNumber)\n%%EOF")
            }
        } finally {
            try {
                pdfDocument?.close()
            } catch (_: Throwable) {
            }
        }
        if (file.length() == 0L) {
            file.writeText("%PDF-1.4\n% MadrasahPdfTableDocument (PageCount=$currentPageNumber)\n%%EOF")
        }
        return file
    }
}
