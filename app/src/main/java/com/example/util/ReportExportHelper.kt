package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.data.ArrearPaymentEntity
import com.example.data.SchoolClassEntity
import com.example.data.SchoolSettingsEntity
import com.example.data.StudentEntity
import com.example.data.TeacherAttendanceEntity
import com.example.ui.formatRupiah
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class StudentReportRow(
    val nis: String,
    val studentName: String,
    val className: String,
    val hadir: Int,
    val terlambat: Int,
    val izin: Int,
    val sakit: Int,
    val alpa: Int,
    val percentage: Int,
    val latestStatus: String = "Belum Hadir",
    val latestTime: String = "-",
    val latestNote: String = "",
    val recordedDays: Int = 0,
    val dailyStatuses: Map<String, String> = emptyMap()
)

object ReportExportHelper {

    private fun currentPrintDateLabel(): String {
        return SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    /**
     * Builds a complete, official HTML print document with:
     * - Official Madrasah Letterhead (Kop Surat Sekolah)
     * - Distinct, tailored table structure based on reportPeriodType:
     *   - "Harian": No, NIS, Nama Siswa, Kelas, Jam Scan/Input, Status Hari Ini, Keterangan/Catatan
     *   - "Mingguan": No, NIS, Nama Siswa, Kelas, Matriks 7 Hari (H-6 s/d Hari Ini), H/T/I/S/A, %
     *   - "Bulanan": No, NIS, Nama Siswa, Kelas, Hari Aktif, H, T, I, S, A, %, Predikat Disiplin Bulanan
     *   - "Per Siswa": Identitas Lengkap Siswa + Tabel Log Riwayat Kehadiran Harian (Tanggal, Jam, Status, Keterangan, Petugas)
     * - Full @media print CSS rules hiding non-relevant UI elements (.no-print, buttons, navbars, etc.)
     * - Official signature block for Kepala Madrasah & Wali Kelas / Petugas
     */
    fun buildAttendancePrintHtml(
        settings: SchoolSettingsEntity,
        reportTitle: String,
        periodLabel: String,
        rows: List<StudentReportRow>,
        signerRoleLabel: String = "Petugas Administrasi",
        signerName: String = "",
        reportPeriodType: String = "Bulanan",
        weekDates: List<String> = emptyList(),
        selectedStudent: StudentEntity? = null,
        studentHistory: List<com.example.data.AttendanceEntity> = emptyList(),
        classes: List<SchoolClassEntity> = emptyList(),
        teacherAttendance: List<TeacherAttendanceEntity> = emptyList()
    ): String {
        if (reportPeriodType == "Guru/Staf" && teacherAttendance.isNotEmpty()) {
            val rowsHtml = teacherAttendance.mapIndexed { idx, ta ->
                val statusClass = when (ta.status) {
                    "Hadir" -> "pct-good"
                    "Terlambat", "Izin", "Sakit" -> "pct-warn"
                    else -> "pct-low"
                }
                """
                <tr>
                    <td class="col-center">${idx + 1}</td>
                    <td class="col-center font-mono">${escapeHtml(ta.nip.ifBlank { "-" })}</td>
                    <td class="col-left font-semibold">${escapeHtml(ta.teacherName)}</td>
                    <td class="col-left">${escapeHtml(ta.position)}</td>
                    <td class="col-center font-mono">${escapeHtml(ta.date)}</td>
                    <td class="col-center font-mono">${escapeHtml(ta.timeIn)} - ${escapeHtml(ta.timeOut)}</td>
                    <td class="col-center"><span class="pct-badge $statusClass">${escapeHtml(ta.status)}</span></td>
                    <td class="col-left">${escapeHtml(ta.note.ifBlank { "-" })}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")

            val hadirCount = teacherAttendance.count { it.status == "Hadir" }
            val summaryHtml = """
                <tr class="summary-row">
                    <td colspan="4" class="col-right">REKAPITULASI KEHADIRAN GURU &amp; STAF (${teacherAttendance.size} DATA)</td>
                    <td colspan="4" class="col-center">Hadir: $hadirCount &bull; Lainnya: ${teacherAttendance.size - hadirCount}</td>
                </tr>
            """.trimIndent()

            return buildBasePrintHtmlDocument(
                settings = settings,
                documentTitle = reportTitle,
                metaRightTop = periodLabel,
                metaRightBottom = "Target: Tenaga Pendidik & Kependidikan",
                tableHeadersHtml = """
                    <tr>
                        <th style="width: 34px;">No</th>
                        <th style="width: 90px;">NIP</th>
                        <th>Nama Lengkap Guru / Staf</th>
                        <th style="width: 120px;">Jabatan</th>
                        <th style="width: 80px;">Tanggal</th>
                        <th style="width: 95px;">Jam Kehadiran</th>
                        <th style="width: 80px;">Status</th>
                        <th>Keterangan</th>
                    </tr>
                """.trimIndent(),
                tableBodyHtml = rowsHtml + summaryHtml,
                signerRoleLabel = signerRoleLabel,
                signerName = signerName
            )
        }

        if (reportPeriodType == "Per Kelas" && classes.isNotEmpty()) {
            val rowsHtml = classes.mapIndexed { idx, cls ->
                val classRows = rows.filter { it.className == cls.className }
                val cHadir = classRows.sumOf { it.hadir }
                val cTelat = classRows.sumOf { it.terlambat }
                val cIzin = classRows.sumOf { it.izin }
                val cSakit = classRows.sumOf { it.sakit }
                val cAlpa = classRows.sumOf { it.alpa }
                val avgPct = if (classRows.isNotEmpty()) classRows.map { it.percentage }.average().toInt() else 0
                val badgeClass = if (avgPct >= 85) "pct-good" else if (avgPct >= 70) "pct-warn" else "pct-low"

                """
                <tr>
                    <td class="col-center">${idx + 1}</td>
                    <td class="col-left font-semibold">${escapeHtml(cls.className)}</td>
                    <td class="col-left">${escapeHtml(cls.homeroomTeacherName)}</td>
                    <td class="col-num">${classRows.size}</td>
                    <td class="col-num">$cHadir</td>
                    <td class="col-num">$cTelat</td>
                    <td class="col-num">$cIzin</td>
                    <td class="col-num">$cSakit</td>
                    <td class="col-num">$cAlpa</td>
                    <td class="col-center"><span class="pct-badge $badgeClass">$avgPct%</span></td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")

            val totalSiswa = rows.size
            val totalH = rows.sumOf { it.hadir }
            val totalT = rows.sumOf { it.terlambat }
            val totalI = rows.sumOf { it.izin }
            val totalS = rows.sumOf { it.sakit }
            val totalA = rows.sumOf { it.alpa }
            val grandAvg = if (rows.isNotEmpty()) rows.map { it.percentage }.average().toInt() else 100

            val summaryHtml = """
                <tr class="summary-row">
                    <td colspan="3" class="col-right">TOTAL AKUMULASI SELURUH KELAS (${classes.size} KELAS)</td>
                    <td class="col-num">$totalSiswa</td>
                    <td class="col-num">$totalH</td>
                    <td class="col-num">$totalT</td>
                    <td class="col-num">$totalI</td>
                    <td class="col-num">$totalS</td>
                    <td class="col-num">$totalA</td>
                    <td class="col-center">$grandAvg%</td>
                </tr>
            """.trimIndent()

            return buildBasePrintHtmlDocument(
                settings = settings,
                documentTitle = reportTitle,
                metaRightTop = periodLabel,
                metaRightBottom = "Rekapitulasi Tingkat Kelas (${classes.size} Rombel)",
                tableHeadersHtml = """
                    <tr>
                        <th style="width: 34px;">No</th>
                        <th style="width: 80px;">Kelas</th>
                        <th>Wali Kelas</th>
                        <th style="width: 60px;">Siswa</th>
                        <th style="width: 44px;">H</th>
                        <th style="width: 44px;">T</th>
                        <th style="width: 44px;">I</th>
                        <th style="width: 44px;">S</th>
                        <th style="width: 44px;">A</th>
                        <th style="width: 72px;">% Hadir</th>
                    </tr>
                """.trimIndent(),
                tableBodyHtml = rowsHtml + summaryHtml,
                signerRoleLabel = signerRoleLabel,
                signerName = signerName
            )
        }

        if (reportPeriodType == "Keterlambatan") {
            val lateRows = rows.filter { it.terlambat > 0 || it.latestStatus == "Terlambat" }
            val rowsHtml = if (lateRows.isEmpty()) {
                """<tr><td colspan="7" style="text-align:center; padding: 18px;">Alhamdulillah, tidak ada catatan keterlambatan siswa pada periode ini.</td></tr>"""
            } else {
                lateRows.mapIndexed { idx, r ->
                    """
                    <tr>
                        <td class="col-center">${idx + 1}</td>
                        <td class="col-center font-mono">${escapeHtml(r.nis)}</td>
                        <td class="col-left font-semibold">${escapeHtml(r.studentName)}</td>
                        <td class="col-center">${escapeHtml(r.className)}</td>
                        <td class="col-center font-mono">${escapeHtml(r.latestTime)}</td>
                        <td class="col-num" style="color:#B76E00;">${r.terlambat} Kali</td>
                        <td class="col-left">${escapeHtml(r.latestNote.ifBlank { "Terlambat masuk gerbang madrasah" })}</td>
                    </tr>
                    """.trimIndent()
                }.joinToString("\n")
            }

            val summaryHtml = """
                <tr class="summary-row">
                    <td colspan="4" class="col-right">TOTAL KASUS KETERLAMBATAN</td>
                    <td colspan="3" class="col-center">${lateRows.size} Siswa Terlambat (Total ${lateRows.sumOf { it.terlambat }} Kejadian)</td>
                </tr>
            """.trimIndent()

            return buildBasePrintHtmlDocument(
                settings = settings,
                documentTitle = "LAPORAN KHUSUS KETERLAMBATAN SISWA",
                metaRightTop = periodLabel,
                metaRightBottom = "Monitoring Disiplin & Tata Tertib Siswa",
                tableHeadersHtml = """
                    <tr>
                        <th style="width: 34px;">No</th>
                        <th style="width: 70px;">NIS</th>
                        <th>Nama Siswa</th>
                        <th style="width: 78px;">Kelas</th>
                        <th style="width: 75px;">Jam Scan</th>
                        <th style="width: 90px;">Frekuensi</th>
                        <th>Catatan Pembinaan / Alasan</th>
                    </tr>
                """.trimIndent(),
                tableBodyHtml = rowsHtml + summaryHtml,
                signerRoleLabel = signerRoleLabel,
                signerName = signerName
            )
        }
        if (reportPeriodType == "Harian") {
            val totalHadir = rows.sumOf { it.hadir }
            val totalTelat = rows.sumOf { it.terlambat }
            val totalIzin = rows.sumOf { it.izin }
            val totalSakit = rows.sumOf { it.sakit }
            val totalAlpa = rows.sumOf { it.alpa }
            val belumHadir = rows.count { it.latestStatus == "Belum Hadir" }

            val rowsHtml = if (rows.isEmpty()) {
                """<tr><td colspan="7" style="text-align:center; padding: 18px;">Tidak ada data siswa pada filter ini.</td></tr>"""
            } else {
                rows.mapIndexed { idx, r ->
                    val statusClass = when (r.latestStatus) {
                        "Hadir" -> "pct-good"
                        "Terlambat", "Izin", "Sakit" -> "pct-warn"
                        else -> "pct-low"
                    }
                    """
                    <tr>
                        <td class="col-center">${idx + 1}</td>
                        <td class="col-center font-mono">${escapeHtml(r.nis)}</td>
                        <td class="col-left font-semibold">${escapeHtml(r.studentName)}</td>
                        <td class="col-center">${escapeHtml(r.className)}</td>
                        <td class="col-center font-mono">${escapeHtml(r.latestTime)}</td>
                        <td class="col-center"><span class="pct-badge $statusClass">${escapeHtml(r.latestStatus)}</span></td>
                        <td class="col-left">${escapeHtml(r.latestNote.ifBlank { "-" })}</td>
                    </tr>
                    """.trimIndent()
                }.joinToString("\n")
            }

            val summaryHtml = if (rows.isNotEmpty()) {
                """
                <tr class="summary-row">
                    <td colspan="4" class="col-right">REKAP HARIAN (${rows.size} SISWA)</td>
                    <td colspan="3" class="col-center">Hadir: $totalHadir &bull; Terlambat: $totalTelat &bull; Izin: $totalIzin &bull; Sakit: $totalSakit &bull; Alpa: $totalAlpa &bull; Belum Hadir: $belumHadir</td>
                </tr>
                """.trimIndent()
            } else ""

            return buildBasePrintHtmlDocument(
                settings = settings,
                documentTitle = reportTitle,
                metaRightTop = periodLabel,
                metaRightBottom = "Mode: Laporan Harian (Jam Scan & Status Harian)",
                tableHeadersHtml = """
                    <tr>
                        <th style="width: 34px;">No</th>
                        <th style="width: 68px;">NIS</th>
                        <th>Nama Lengkap Siswa</th>
                        <th style="width: 78px;">Kelas</th>
                        <th style="width: 66px;">Jam Scan</th>
                        <th style="width: 96px;">Status Hari Ini</th>
                        <th>Keterangan / Catatan</th>
                    </tr>
                """.trimIndent(),
                tableBodyHtml = rowsHtml + summaryHtml,
                signerRoleLabel = signerRoleLabel,
                signerName = signerName
            )
        }

        if (reportPeriodType == "Mingguan" && weekDates.isNotEmpty()) {
            val dateHeaders = weekDates.joinToString("\n") { d ->
                """<th style="width: 30px;">${escapeHtml(d.takeLast(5))}</th>"""
            }
            val rowsHtml = if (rows.isEmpty()) {
                """<tr><td colspan="${6 + weekDates.size}" style="text-align:center; padding: 18px;">Tidak ada data absensi mingguan.</td></tr>"""
            } else {
                rows.mapIndexed { idx, r ->
                    val dayCells = weekDates.joinToString("\n") { d ->
                        val code = r.dailyStatuses[d] ?: "-"
                        """<td class="col-center font-semibold">$code</td>"""
                    }
                    val badgeClass = if (r.percentage >= 80) "pct-good" else "pct-warn"
                    """
                    <tr>
                        <td class="col-center">${idx + 1}</td>
                        <td class="col-center font-mono">${escapeHtml(r.nis)}</td>
                        <td class="col-left font-semibold">${escapeHtml(r.studentName)}</td>
                        <td class="col-center">${escapeHtml(r.className)}</td>
                        $dayCells
                        <td class="col-center font-mono">H:${r.hadir} T:${r.terlambat} I:${r.izin} S:${r.sakit} A:${r.alpa}</td>
                        <td class="col-center"><span class="pct-badge $badgeClass">${r.percentage}%</span></td>
                    </tr>
                    """.trimIndent()
                }.joinToString("\n")
            }

            return buildBasePrintHtmlDocument(
                settings = settings,
                documentTitle = reportTitle,
                metaRightTop = periodLabel,
                metaRightBottom = "Mode: Matriks Kehadiran 7 Hari Terakhir",
                tableHeadersHtml = """
                    <tr>
                        <th style="width: 30px;">No</th>
                        <th style="width: 60px;">NIS</th>
                        <th>Nama Siswa</th>
                        <th style="width: 68px;">Kelas</th>
                        $dateHeaders
                        <th style="width: 115px;">Rekap 7 Hari</th>
                        <th style="width: 54px;">%</th>
                    </tr>
                """.trimIndent(),
                tableBodyHtml = rowsHtml,
                signerRoleLabel = signerRoleLabel,
                signerName = signerName
            )
        }

        if (reportPeriodType == "Per Siswa" && selectedStudent != null) {
            val stRow = rows.find { it.nis == selectedStudent.nis }
            val rowsHtml = if (studentHistory.isEmpty()) {
                """<tr><td colspan="6" style="text-align:center; padding: 18px;">Belum ada log riwayat kehadiran tercatat untuk siswa ini.</td></tr>"""
            } else {
                studentHistory.mapIndexed { idx, att ->
                    val statusClass = when (att.status) {
                        "Hadir" -> "pct-good"
                        "Terlambat", "Izin", "Sakit" -> "pct-warn"
                        else -> "pct-low"
                    }
                    """
                    <tr>
                        <td class="col-center">${idx + 1}</td>
                        <td class="col-center font-mono">${escapeHtml(att.date)}</td>
                        <td class="col-center font-mono">${escapeHtml(att.time)}</td>
                        <td class="col-center"><span class="pct-badge $statusClass">${escapeHtml(att.status)}</span></td>
                        <td class="col-left">${escapeHtml(att.note.ifBlank { "-" })}</td>
                        <td class="col-left">${escapeHtml(att.operatorName)}</td>
                    </tr>
                    """.trimIndent()
                }.joinToString("\n")
            }

            val summaryHtml = """
                <tr class="summary-row">
                    <td colspan="3" class="col-right">RINGKASAN INDIVIDU: ${escapeHtml(selectedStudent.fullName)} (${escapeHtml(selectedStudent.className)})</td>
                    <td colspan="3" class="col-center">Hadir: ${stRow?.hadir ?: 0} &bull; Terlambat: ${stRow?.terlambat ?: 0} &bull; Izin: ${stRow?.izin ?: 0} &bull; Sakit: ${stRow?.sakit ?: 0} &bull; Alpa: ${stRow?.alpa ?: 0} (${stRow?.percentage ?: 100}%)</td>
                </tr>
            """.trimIndent()

            return buildBasePrintHtmlDocument(
                settings = settings,
                documentTitle = "LAPORAN RIWAYAT KEHADIRAN INDIVIDU SISWA",
                metaRightTop = "Siswa: ${selectedStudent.fullName} (NIS: ${selectedStudent.nis})",
                metaRightBottom = "Kelas: ${selectedStudent.className} • Wali: ${selectedStudent.parentName.ifBlank { "-" }}",
                tableHeadersHtml = """
                    <tr>
                        <th style="width: 36px;">No</th>
                        <th style="width: 92px;">Tanggal</th>
                        <th style="width: 68px;">Jam Scan</th>
                        <th style="width: 92px;">Status</th>
                        <th>Keterangan / Catatan Harian</th>
                        <th style="width: 130px;">Petugas Pencatat</th>
                    </tr>
                """.trimIndent(),
                tableBodyHtml = rowsHtml + summaryHtml,
                signerRoleLabel = signerRoleLabel,
                signerName = signerName
            )
        }

        val totalHadir = rows.sumOf { it.hadir }
        val totalTelat = rows.sumOf { it.terlambat }
        val totalIzin = rows.sumOf { it.izin }
        val totalSakit = rows.sumOf { it.sakit }
        val totalAlpa = rows.sumOf { it.alpa }
        val avgPct = if (rows.isNotEmpty()) rows.map { it.percentage }.average().toInt() else 100

        val rowsHtml = if (rows.isEmpty()) {
            """<tr><td colspan="12" style="text-align:center; padding: 18px;">Tidak ada data absensi pada periode ini.</td></tr>"""
        } else {
            rows.mapIndexed { idx, r ->
                val badgeClass = when {
                    r.percentage >= 85 -> "pct-good"
                    r.percentage >= 70 -> "pct-warn"
                    else -> "pct-low"
                }
                val predikat = when {
                    r.recordedDays == 0 -> "Belum Ada Data"
                    r.percentage >= 95 && r.alpa == 0 -> "Sangat Disiplin"
                    r.percentage >= 80 -> "Tertib / Baik"
                    else -> "Perlu Pembinaan"
                }
                """
                <tr>
                    <td class="col-center">${idx + 1}</td>
                    <td class="col-center font-mono">${escapeHtml(r.nis)}</td>
                    <td class="col-left font-semibold">${escapeHtml(r.studentName)}</td>
                    <td class="col-center">${escapeHtml(r.className)}</td>
                    <td class="col-num">${r.recordedDays} hr</td>
                    <td class="col-num">${r.hadir}</td>
                    <td class="col-num">${r.terlambat}</td>
                    <td class="col-num">${r.izin}</td>
                    <td class="col-num">${r.sakit}</td>
                    <td class="col-num">${r.alpa}</td>
                    <td class="col-center"><span class="pct-badge $badgeClass">${r.percentage}%</span></td>
                    <td class="col-center">${escapeHtml(predikat)}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        val summaryRowHtml = if (rows.isNotEmpty()) {
            """
            <tr class="summary-row">
                <td colspan="5" class="col-right">TOTAL AKUMULASI BULANAN (${rows.size} SISWA)</td>
                <td class="col-num">$totalHadir</td>
                <td class="col-num">$totalTelat</td>
                <td class="col-num">$totalIzin</td>
                <td class="col-num">$totalSakit</td>
                <td class="col-num">$totalAlpa</td>
                <td class="col-center">$avgPct%</td>
                <td class="col-center">-</td>
            </tr>
            """.trimIndent()
        } else ""

        return buildBasePrintHtmlDocument(
            settings = settings,
            documentTitle = reportTitle,
            metaRightTop = "Periode: $periodLabel",
            metaRightBottom = "Total Data: ${rows.size} Siswa",
            tableHeadersHtml = """
                <tr>
                    <th style="width: 32px;">No</th>
                    <th style="width: 64px;">NIS</th>
                    <th>Nama Lengkap Siswa</th>
                    <th style="width: 72px;">Kelas</th>
                    <th style="width: 54px;">Hari Aktif</th>
                    <th style="width: 40px;">H</th>
                    <th style="width: 40px;">T</th>
                    <th style="width: 40px;">I</th>
                    <th style="width: 40px;">S</th>
                    <th style="width: 40px;">A</th>
                    <th style="width: 58px;">% Hadir</th>
                    <th style="width: 105px;">Predikat Disiplin</th>
                </tr>
            """.trimIndent(),
            tableBodyHtml = rowsHtml + summaryRowHtml,
            signerRoleLabel = signerRoleLabel,
            signerName = signerName
        )
    }

    /**
     * Builds an official HTML print document for Tunggakan / Arrears report with Kop Surat Sekolah,
     * clean table formatting, and @media print CSS rules.
     */
    fun buildArrearsPrintHtml(
        settings: SchoolSettingsEntity,
        reportTitle: String,
        classScopeLabel: String,
        arrears: List<ArrearPaymentEntity>,
        signerRoleLabel: String = "Wali Kelas / Bendahara TU",
        signerName: String = ""
    ): String {
        val totalNominal = arrears.sumOf { it.nominal }
        val totalPaid = arrears.sumOf { it.paidAmount }
        val totalUnpaid = (totalNominal - totalPaid).coerceAtLeast(0L)

        val rowsHtml = if (arrears.isEmpty()) {
            """<tr><td colspan="9" style="text-align:center; padding: 18px;">Tidak ada data tagihan atau tunggakan pada kelas ini.</td></tr>"""
        } else {
            arrears.mapIndexed { idx, ar ->
                val sisa = (ar.nominal - ar.paidAmount).coerceAtLeast(0L)
                val statusClass = when (ar.status) {
                    "Lunas" -> "pct-good"
                    "Cicilan" -> "pct-warn"
                    else -> "pct-low"
                }
                """
                <tr>
                    <td class="col-center">${idx + 1}</td>
                    <td class="col-center font-mono">${escapeHtml(ar.studentNis)}</td>
                    <td class="col-left font-semibold">${escapeHtml(ar.studentName)}</td>
                    <td class="col-center">${escapeHtml(ar.className)}</td>
                    <td class="col-left">${escapeHtml(ar.billType)}<br/><small style="color:#4F665D;">${escapeHtml(ar.period)}</small></td>
                    <td class="col-right">${formatRupiah(ar.nominal)}</td>
                    <td class="col-right">${formatRupiah(ar.paidAmount)}</td>
                    <td class="col-right font-semibold" style="color:${if (sisa > 0) "#B91C1C" else "#0A6B4A"};">${formatRupiah(sisa)}</td>
                    <td class="col-center"><span class="pct-badge $statusClass">${escapeHtml(ar.status)}</span></td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        val summaryRowHtml = if (arrears.isNotEmpty()) {
            """
            <tr class="summary-row">
                <td colspan="5" class="col-right">REKAPITULASI TOTAL TAGIHAN &amp; TUNGGAKAN (${arrears.size} DATA)</td>
                <td class="col-right">${formatRupiah(totalNominal)}</td>
                <td class="col-right">${formatRupiah(totalPaid)}</td>
                <td class="col-right">${formatRupiah(totalUnpaid)}</td>
                <td class="col-center">-</td>
            </tr>
            """.trimIndent()
        } else ""

        return buildBasePrintHtmlDocument(
            settings = settings,
            documentTitle = reportTitle,
            metaRightTop = "Cakupan: $classScopeLabel",
            metaRightBottom = "Sisa Tunggakan: ${formatRupiah(totalUnpaid)}",
            tableHeadersHtml = """
                <tr>
                    <th style="width: 32px;">No</th>
                    <th style="width: 64px;">NIS</th>
                    <th>Nama Siswa</th>
                    <th style="width: 74px;">Kelas</th>
                    <th>Jenis Tagihan &amp; Periode</th>
                    <th style="width: 88px;">Nominal</th>
                    <th style="width: 88px;">Dibayar</th>
                    <th style="width: 92px;">Sisa Tunggakan</th>
                    <th style="width: 72px;">Status</th>
                </tr>
            """.trimIndent(),
            tableBodyHtml = rowsHtml + summaryRowHtml,
            signerRoleLabel = signerRoleLabel,
            signerName = signerName
        )
    }

    private fun buildBasePrintHtmlDocument(
        settings: SchoolSettingsEntity,
        documentTitle: String,
        metaRightTop: String,
        metaRightBottom: String,
        tableHeadersHtml: String,
        tableBodyHtml: String,
        signerRoleLabel: String,
        signerName: String
    ): String {
        val printDate = currentPrintDateLabel()
        val effectiveSignerName = signerName.ifBlank { "........................................" }

        return """
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${escapeHtml(documentTitle)}</title>
    <style>
        :root {
            --mi-dark: #0B3D2E;
            --mi-emerald: #0A6B4A;
            --mi-border: #CBD5E1;
            --mi-border-strong: #94A3B8;
            --mi-bg-soft: #F1F5F3;
            --mi-text: #11261F;
            --mi-gold: #C59B27;
        }
        * {
            box-sizing: border-box;
            -webkit-print-color-adjust: exact !important;
            print-color-adjust: exact !important;
        }
        html, body {
            background-color: #F8FAFC;
            color: var(--mi-text);
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            font-size: 11.5px;
            line-height: 1.45;
            margin: 0;
            padding: 0;
        }
        
        /* Container Kertas Laporan */
        .page-wrapper {
            padding: 16px 14px 28px 14px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .report-paper-container {
            background: #FFFFFF;
            width: 100%;
            max-width: 860px;
            padding: 24px 28px;
            border-radius: 8px;
            border: 1px solid var(--mi-border);
            box-shadow: 0 4px 14px rgba(11, 61, 46, 0.08);
        }

        /* Banner & Tombol Aksi Layar (Disembunyikan Otomatis Saat Print) */
        .print-banner-toolbar {
            width: 100%;
            max-width: 860px;
            background: linear-gradient(135deg, #0B3D2E 0%, #0A6B4A 100%);
            color: #FFFFFF;
            border-radius: 8px;
            padding: 10px 16px;
            margin-bottom: 14px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            box-shadow: 0 2px 8px rgba(11, 61, 46, 0.18);
        }
        .print-banner-toolbar .banner-info {
            display: flex;
            flex-direction: column;
        }
        .print-banner-toolbar .banner-title {
            font-size: 13px;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .print-banner-toolbar .banner-subtitle {
            font-size: 11px;
            color: #E2E8F0;
            opacity: 0.92;
            margin-top: 2px;
        }
        .btn-print-action {
            background: #FFFFFF;
            color: var(--mi-dark);
            border: none;
            outline: none;
            padding: 7px 16px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 700;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s ease;
            box-shadow: 0 1px 4px rgba(0,0,0,0.15);
        }
        .btn-print-action:hover {
            background: #F1F5F3;
            transform: translateY(-1px);
        }
        .btn-print-action:active {
            transform: translateY(0);
        }

        /* KOP SURAT MADRASAH RESMI */
        .kop-surat {
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 2.8px solid var(--mi-dark);
            padding-bottom: 8px;
            position: relative;
            margin-bottom: 2.5px;
        }
        .kop-surat::after {
            content: "";
            display: block;
            position: absolute;
            left: 0;
            right: 0;
            bottom: -5px;
            border-bottom: 1px solid var(--mi-dark);
        }
        .kop-logo-box {
            width: 62px;
            height: 62px;
            border: 2px solid var(--mi-dark);
            border-radius: 12px;
            background: var(--mi-bg-soft);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            font-weight: 800;
            color: var(--mi-dark);
            font-size: 13px;
            flex-shrink: 0;
            position: relative;
        }
        .kop-logo-box.seal-box {
            border: 1.5px dashed var(--mi-emerald);
            background: #F0FDF4;
            color: var(--mi-emerald);
        }
        .kop-center {
            flex: 1;
            text-align: center;
            padding: 0 14px;
        }
        .kop-ministry {
            font-size: 10.5px;
            font-weight: 800;
            letter-spacing: 0.8px;
            color: var(--mi-emerald);
            text-transform: uppercase;
            margin: 0;
        }
        .kop-subministry {
            font-size: 9px;
            font-weight: 700;
            letter-spacing: 0.5px;
            color: #334155;
            text-transform: uppercase;
            margin: 1px 0;
        }
        .kop-school-name {
            font-size: 17.5px;
            font-weight: 900;
            color: var(--mi-dark);
            text-transform: uppercase;
            margin: 2px 0;
            letter-spacing: 0.4px;
        }
        .kop-ids {
            font-size: 10.5px;
            font-weight: 700;
            color: #1E293B;
            margin: 1px 0;
        }
        .kop-address {
            font-size: 9.5px;
            color: #475569;
            margin: 1px 0 0 0;
        }

        /* Judul Laporan & Metadata */
        .report-title-block {
            text-align: center;
            margin: 14px 0 10px 0;
        }
        .report-title-block h2 {
            font-size: 13.5px;
            font-weight: 800;
            text-transform: uppercase;
            color: var(--mi-dark);
            margin: 0 0 2px 0;
            text-decoration: underline;
            letter-spacing: 0.3px;
        }
        .meta-bar {
            display: flex;
            justify-content: space-between;
            background: var(--mi-bg-soft);
            border: 1px solid var(--mi-border);
            border-radius: 6px;
            padding: 7px 12px;
            margin-bottom: 12px;
            font-size: 10.5px;
        }
        .meta-bar div strong {
            color: var(--mi-dark);
        }

        /* Format Tabel Laporan Rapi */
        table.report-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 16px;
            font-size: 10.5px;
        }
        table.report-table th {
            background-color: var(--mi-dark);
            color: #FFFFFF;
            font-weight: 700;
            text-align: center;
            padding: 7px 6px;
            border: 1px solid var(--mi-dark);
            text-transform: uppercase;
            font-size: 9.5px;
            letter-spacing: 0.2px;
        }
        table.report-table td {
            padding: 6px 6px;
            border: 1px solid var(--mi-border-strong);
            vertical-align: middle;
        }
        table.report-table tbody tr:nth-child(even) {
            background-color: #F8FAFC;
        }
        table.report-table tr.summary-row td {
            background-color: #E6F4EE !important;
            font-weight: 800;
            color: var(--mi-dark);
            border-top: 2px solid var(--mi-dark);
            border-bottom: 2px solid var(--mi-dark);
            padding: 7px 6px;
        }
        .col-center { text-align: center; }
        .col-left { text-align: left; }
        .col-right { text-align: right; }
        .col-num { text-align: center; font-weight: 600; }
        .font-mono { font-family: monospace; font-size: 10.5px; }
        .font-semibold { font-weight: 600; }
        .pct-badge {
            display: inline-block;
            padding: 2px 6px;
            border-radius: 4px;
            font-weight: 700;
            font-size: 9.5px;
        }
        .pct-good { background: #E6F6EF; color: #0A6B4A; border: 1px solid #A7F3D0; }
        .pct-warn { background: #FFF4E5; color: #B76E00; border: 1px solid #FDE68A; }
        .pct-low { background: #FEE2E2; color: #B91C1C; border: 1px solid #FECACA; }

        /* Blok Tanda Tangan Resmi */
        .signature-section {
            display: flex;
            justify-content: space-between;
            margin-top: 22px;
            page-break-inside: avoid;
            font-size: 11px;
        }
        .sig-box {
            width: 44%;
            text-align: center;
        }
        .sig-space {
            height: 52px;
        }
        .sig-name {
            font-weight: 700;
            text-decoration: underline;
            color: var(--mi-dark);
        }

        /* ATURAN LENGKAP CSS PRINT MEDIA:
           Sembunyikan seluruh tombol aksi, navbar, header UI, toolbar, modal controls, dll. */
        @media print {
            @page {
                size: A4 portrait;
                margin: 10mm 14mm 12mm 14mm;
            }
            html, body {
                margin: 0 !important;
                padding: 0 !important;
                background: #FFFFFF !important;
                color: #11261F !important;
                font-size: 10.5pt !important;
            }
            .page-wrapper {
                padding: 0 !important;
                margin: 0 !important;
                display: block !important;
            }
            .report-paper-container {
                width: 100% !important;
                max-width: 100% !important;
                padding: 0 !important;
                margin: 0 !important;
                box-shadow: none !important;
                border: none !important;
                border-radius: 0 !important;
            }

            /* Sembunyikan semua elemen navigasi dan tombol aksi */
            .no-print,
            .no-print *,
            .print-banner-toolbar,
            nav,
            header.app-ui-header,
            footer.app-ui-footer,
            button,
            input,
            select,
            .action-toolbar,
            .filter-bar,
            .bottom-nav,
            .modal-close-btn,
            .btn,
            .btn-action,
            .export-buttons,
            .search-box,
            .filter-chips,
            .app-navigation-bar,
            .floating-action-button,
            .screen-controls,
            .tab-bar,
            .navigation-rail {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                min-height: 0 !important;
                width: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
                border: none !important;
                opacity: 0 !important;
                position: absolute !important;
                pointer-events: none !important;
                overflow: hidden !important;
            }

            /* Optimalisasi Pemotongan Halaman (Page Breaks) */
            .kop-surat {
                page-break-inside: avoid !important;
                page-break-after: avoid !important;
                margin-top: 0 !important;
            }
            table.report-table {
                width: 100% !important;
                border-collapse: collapse !important;
                page-break-inside: auto !important;
            }
            table.report-table thead {
                display: table-header-group !important;
            }
            table.report-table tfoot {
                display: table-footer-group !important;
            }
            table.report-table tr {
                page-break-inside: avoid !important;
                page-break-after: auto !important;
            }
            table.report-table td, table.report-table th {
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
            }
            .signature-section {
                page-break-inside: avoid !important;
                margin-top: 24px !important;
            }
        }
    </style>
</head>
<body>
    <div class="page-wrapper">
        <!-- Bilah Aksi Pratinjau (Otomatis Tersembunyi Saat Dicetak via CSS @media print) -->
        <div class="print-banner-toolbar no-print">
            <div class="banner-info">
                <span class="banner-title">&#128438; Pratinjau Dokumen Siap Cetak (Kop Surat &amp; Format Resmi Aktif)</span>
                <span class="banner-subtitle">Bilah navigasi, tombol, dan kontrol antarmuka aplikasi disembunyikan otomatis saat dicetak.</span>
            </div>
            <button type="button" class="btn-print-action no-print" onclick="window.print()">
                <span>&#128438;</span>
                <span>Cetak Dokumen</span>
            </button>
        </div>

        <div class="report-paper-container">
            <!-- KOP SURAT MADRASAH RESMI -->
            <div class="kop-surat">
                <div class="kop-logo-box">
                    <span>MI</span>
                    <span style="font-size:7.5px; font-weight:700; color:var(--mi-emerald); letter-spacing:0.3px;">IKHLAS</span>
                </div>
                <div class="kop-center">
                    <p class="kop-ministry">KEMENTERIAN AGAMA REPUBLIK INDONESIA</p>
                    <p class="kop-subministry">DIREKTORAT JENDERAL PENDIDIKAN ISLAM &bull; KANTOR KEMENTERIAN AGAMA</p>
                    <h1 class="kop-school-name">${escapeHtml(settings.schoolName)}</h1>
                    <p class="kop-ids">NSM: ${escapeHtml(settings.nsm)} &nbsp;|&nbsp; NPSN: ${escapeHtml(settings.npsn)} &nbsp;|&nbsp; STATUS: TERAKREDITASI</p>
                    <p class="kop-address">${escapeHtml(settings.address)} &bull; Telp: ${escapeHtml(settings.phone)} &bull; Email: ${escapeHtml(settings.email)}</p>
                </div>
                <div class="kop-logo-box seal-box">
                    <span>QR</span>
                    <span style="font-size:7px; font-weight:700; letter-spacing:0.3px;">RESMI</span>
                </div>
            </div>

            <div class="report-title-block">
                <h2>${escapeHtml(documentTitle)}</h2>
            </div>

            <div class="meta-bar">
                <div>
                    <strong>Tahun Pelajaran:</strong> ${escapeHtml(settings.activeAcademicYear)} (${escapeHtml(settings.activeSemester)})<br/>
                    <strong>Tanggal Terbit:</strong> $printDate
                </div>
                <div style="text-align: right;">
                    <strong>${escapeHtml(metaRightTop)}</strong><br/>
                    <span>${escapeHtml(metaRightBottom)}</span>
                </div>
            </div>

            <table class="report-table">
                <thead>
                    $tableHeadersHtml
                </thead>
                <tbody>
                    $tableBodyHtml
                </tbody>
            </table>

            <div class="signature-section">
                <div class="sig-box">
                    <div>Mengetahui,</div>
                    <div>Kepala Madrasah</div>
                    <div class="sig-space"></div>
                    <div class="sig-name">${escapeHtml(settings.headmasterName)}</div>
                    <div>NIP. ${escapeHtml(settings.headmasterNip)}</div>
                </div>
                <div class="sig-box">
                    <div>Diterbitkan Tanggal $printDate</div>
                    <div>${escapeHtml(signerRoleLabel)}</div>
                    <div class="sig-space"></div>
                    <div class="sig-name">${escapeHtml(effectiveSignerName)}</div>
                    <div>&nbsp;</div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun drawPdfLetterhead(
        c: Canvas,
        settings: SchoolSettingsEntity,
        documentTitle: String,
        metaLeft: String,
        metaRight: String
    ): Float {
        val darkGreen = Color.rgb(11, 61, 46)
        val emerald = Color.rgb(10, 107, 74)
        val mutedText = Color.rgb(71, 85, 105)

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

        c.drawRoundRect(34f, 24f, 76f, 66f, 6f, 6f, emblemBgPaint)
        c.drawRoundRect(34f, 24f, 76f, 66f, 6f, 6f, emblemBorderPaint)
        c.drawText("MI", 55f, 49f, schoolPaint.apply { textSize = 12f })

        schoolPaint.textSize = 14.5f
        val centerX = 297.5f
        c.drawText("KEMENTERIAN AGAMA REPUBLIK INDONESIA • PENDIDIKAN MADRASAH", centerX, 33f, ministryPaint)
        c.drawText(settings.schoolName.uppercase(), centerX, 49f, schoolPaint)
        c.drawText("NSM: ${settings.nsm}   |   NPSN: ${settings.npsn}", centerX, 61f, idPaint)
        c.drawText("${settings.address} • Telp: ${settings.phone}", centerX, 72f, addrPaint)

        val thickLine = Paint().apply { color = darkGreen; strokeWidth = 2.2f }
        val thinLine = Paint().apply { color = darkGreen; strokeWidth = 0.8f }
        c.drawLine(34f, 79f, 561f, 79f, thickLine)
        c.drawLine(34f, 82.5f, 561f, 82.5f, thinLine)

        val docTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGreen
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        c.drawText(documentTitle.uppercase(), centerX, 98f, docTitlePaint)

        val metaBgPaint = Paint().apply { color = Color.rgb(241, 245, 243); style = Paint.Style.FILL }
        val metaStrokePaint = Paint().apply { color = Color.rgb(203, 213, 225); style = Paint.Style.STROKE; strokeWidth = 0.8f }
        c.drawRoundRect(34f, 105f, 561f, 125f, 4f, 4f, metaBgPaint)
        c.drawRoundRect(34f, 105f, 561f, 125f, 4f, 4f, metaStrokePaint)

        val metaTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 38, 31)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText(metaLeft, 42f, 118f, metaTextPaint)
        val metaRightPaint = Paint(metaTextPaint).apply { textAlign = Paint.Align.RIGHT }
        c.drawText(metaRight, 553f, 118f, metaRightPaint)

        return 134f
    }

    private fun drawTableCell(
        c: Canvas,
        xLeft: Float,
        yTop: Float,
        xRight: Float,
        yBottom: Float,
        text: String,
        textPaint: Paint,
        borderPaint: Paint,
        bgPaint: Paint? = null,
        align: Paint.Align = Paint.Align.CENTER
    ) {
        if (bgPaint != null) {
            c.drawRect(xLeft, yTop, xRight, yBottom, bgPaint)
        }
        c.drawRect(xLeft, yTop, xRight, yBottom, borderPaint)
        val p = Paint(textPaint).apply { textAlign = align }
        val textY = yTop + ((yBottom - yTop) / 2f) + 3.2f
        val textX = when (align) {
            Paint.Align.LEFT -> xLeft + 5f
            Paint.Align.RIGHT -> xRight - 5f
            else -> (xLeft + xRight) / 2f
        }
        c.drawText(text, textX, textY, p)
    }

    fun buildPdfFile(
        context: Context,
        settings: SchoolSettingsEntity,
        reportTitle: String,
        periodLabel: String,
        rows: List<StudentReportRow>,
        signerRoleLabel: String = "Petugas Administrasi Absensi",
        signerName: String = "",
        reportPeriodType: String = "Bulanan",
        weekDates: List<String> = emptyList(),
        selectedStudent: StudentEntity? = null,
        studentHistory: List<com.example.data.AttendanceEntity> = emptyList(),
        classes: List<SchoolClassEntity> = emptyList(),
        teacherAttendance: List<TeacherAttendanceEntity> = emptyList()
    ): File {
        val doc = MadrasahPdfTableDocument()
        doc.startNewPage()
        val metaLeft = "Tahun Ajaran: ${settings.activeAcademicYear} (${settings.activeSemester})"

        if (reportPeriodType == "Guru/Staf" && teacherAttendance.isNotEmpty()) {
            doc.drawKopSuratHeader(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel
            )
            val cols = listOf(
                MadrasahPdfTableDocument.ColumnSpec("NO", 0.6f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("NIP", 1.4f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("NAMA GURU / STAF", 3.0f, Paint.Align.LEFT, bold = true),
                MadrasahPdfTableDocument.ColumnSpec("JABATAN", 1.8f, Paint.Align.LEFT),
                MadrasahPdfTableDocument.ColumnSpec("TANGGAL", 1.4f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("JAM", 1.4f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("STATUS", 1.3f, Paint.Align.CENTER, bold = true),
                MadrasahPdfTableDocument.ColumnSpec("KETERANGAN", 2.2f, Paint.Align.LEFT)
            )
            val dataRows = teacherAttendance.mapIndexed { idx, ta ->
                listOf(
                    "${idx + 1}",
                    ta.nip.ifBlank { "-" },
                    ta.teacherName,
                    ta.position,
                    ta.date,
                    "${ta.timeIn} - ${ta.timeOut}",
                    ta.status,
                    ta.note.ifBlank { "-" }
                )
            }
            val hadirCount = teacherAttendance.count { it.status == "Hadir" }
            doc.autoTable(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel,
                columns = cols,
                rows = dataRows,
                summaryLabel = "TOTAL REKAP GURU/STAF (${teacherAttendance.size} DATA)",
                summaryLabelColSpan = 4,
                summaryValues = listOf("Hadir: $hadirCount", "Lainnya: ${teacherAttendance.size - hadirCount}")
            )
        } else if (reportPeriodType == "Per Kelas" && classes.isNotEmpty()) {
            doc.drawKopSuratHeader(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel
            )
            val cols = listOf(
                MadrasahPdfTableDocument.ColumnSpec("NO", 0.6f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("KELAS", 1.4f, Paint.Align.LEFT, bold = true),
                MadrasahPdfTableDocument.ColumnSpec("WALI KELAS", 2.8f, Paint.Align.LEFT),
                MadrasahPdfTableDocument.ColumnSpec("SISWA", 1.0f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("H", 0.7f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("T", 0.7f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("I", 0.7f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("S", 0.7f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("A", 0.7f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("% HADIR", 1.3f, Paint.Align.CENTER, bold = true)
            )
            val dataRows = classes.mapIndexed { idx, cls ->
                val classRows = rows.filter { it.className == cls.className }
                val cHadir = classRows.sumOf { it.hadir }
                val cTelat = classRows.sumOf { it.terlambat }
                val cIzin = classRows.sumOf { it.izin }
                val cSakit = classRows.sumOf { it.sakit }
                val cAlpa = classRows.sumOf { it.alpa }
                val avgPct = if (classRows.isNotEmpty()) classRows.map { it.percentage }.average().toInt() else 0
                listOf(
                    "${idx + 1}",
                    cls.className,
                    cls.homeroomTeacherName,
                    "${classRows.size}",
                    "$cHadir",
                    "$cTelat",
                    "$cIzin",
                    "$cSakit",
                    "$cAlpa",
                    "$avgPct%"
                )
            }
            val totalSiswa = rows.size
            val grandH = rows.sumOf { it.hadir }
            val grandT = rows.sumOf { it.terlambat }
            val grandI = rows.sumOf { it.izin }
            val grandS = rows.sumOf { it.sakit }
            val grandA = rows.sumOf { it.alpa }
            val grandAvg = if (rows.isNotEmpty()) rows.map { it.percentage }.average().toInt() else 100
            doc.autoTable(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel,
                columns = cols,
                rows = dataRows,
                summaryLabel = "TOTAL SELURUH KELAS (${classes.size} ROMBEL)",
                summaryLabelColSpan = 3,
                summaryValues = listOf("$totalSiswa", "$grandH", "$grandT", "$grandI", "$grandS", "$grandA", "$grandAvg%")
            )
        } else if (reportPeriodType == "Harian") {
            doc.drawKopSuratHeader(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel
            )
            val cols = listOf(
                MadrasahPdfTableDocument.ColumnSpec("NO", 0.6f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("NIS", 1.1f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("NAMA LENGKAP SISWA", 3.2f, Paint.Align.LEFT, bold = true),
                MadrasahPdfTableDocument.ColumnSpec("KELAS", 1.2f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("JAM SCAN", 1.1f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("STATUS HARI INI", 1.6f, Paint.Align.CENTER, bold = true),
                MadrasahPdfTableDocument.ColumnSpec("KETERANGAN", 2.4f, Paint.Align.LEFT)
            )
            val dataRows = rows.mapIndexed { idx, r ->
                listOf(
                    "${idx + 1}",
                    r.nis,
                    r.studentName,
                    r.className,
                    r.latestTime,
                    r.latestStatus,
                    r.latestNote.ifBlank { "-" }
                )
            }
            val totalH = rows.sumOf { it.hadir }
            val totalT = rows.sumOf { it.terlambat }
            val totalI = rows.sumOf { it.izin }
            val totalS = rows.sumOf { it.sakit }
            val totalA = rows.sumOf { it.alpa }
            val belum = rows.count { it.latestStatus == "Belum Hadir" }

            doc.autoTable(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel,
                columns = cols,
                rows = dataRows,
                summaryLabel = "REKAP HARIAN (${rows.size} SISWA)",
                summaryLabelColSpan = 4,
                summaryValues = listOf("H:$totalH T:$totalT", "I:$totalI S:$totalS A:$totalA", "Belum: $belum")
            )
        } else if (reportPeriodType == "Mingguan" && weekDates.isNotEmpty()) {
            doc.drawKopSuratHeader(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel
            )
            val cols = mutableListOf(
                MadrasahPdfTableDocument.ColumnSpec("NO", 0.55f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("NIS", 0.95f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("NAMA SISWA", 2.6f, Paint.Align.LEFT, bold = true),
                MadrasahPdfTableDocument.ColumnSpec("KELAS", 1.0f, Paint.Align.CENTER)
            )
            weekDates.forEach { d ->
                cols.add(MadrasahPdfTableDocument.ColumnSpec(d.takeLast(5), 0.65f, Paint.Align.CENTER, bold = true))
            }
            cols.add(MadrasahPdfTableDocument.ColumnSpec("REKAP", 1.55f, Paint.Align.CENTER))
            cols.add(MadrasahPdfTableDocument.ColumnSpec("%", 0.75f, Paint.Align.CENTER, bold = true))

            val dataRows = rows.mapIndexed { idx, r ->
                val list = mutableListOf(
                    "${idx + 1}",
                    r.nis,
                    r.studentName,
                    r.className
                )
                weekDates.forEach { d ->
                    list.add(r.dailyStatuses[d] ?: "-")
                }
                list.add("H:${r.hadir} T:${r.terlambat} I:${r.izin}")
                list.add("${r.percentage}%")
                list
            }
            doc.autoTable(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel,
                columns = cols,
                rows = dataRows
            )
        } else if (reportPeriodType == "Per Siswa" && selectedStudent != null) {
            val studentMeta = "Siswa: ${selectedStudent.fullName} (NIS: ${selectedStudent.nis} • ${selectedStudent.className})"
            doc.drawKopSuratHeader(
                settings = settings,
                documentTitle = "LAPORAN RIWAYAT KEHADIRAN INDIVIDU SISWA",
                metaLeft = studentMeta,
                metaRight = "Wali: ${selectedStudent.parentName.ifBlank { "-" }}"
            )
            val cols = listOf(
                MadrasahPdfTableDocument.ColumnSpec("NO", 0.6f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("TANGGAL", 1.4f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("JAM SCAN", 1.1f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("STATUS", 1.4f, Paint.Align.CENTER, bold = true),
                MadrasahPdfTableDocument.ColumnSpec("KETERANGAN / CATATAN", 3.0f, Paint.Align.LEFT),
                MadrasahPdfTableDocument.ColumnSpec("PETUGAS PENCATAT", 2.0f, Paint.Align.LEFT)
            )
            val dataRows = studentHistory.mapIndexed { idx, att ->
                listOf(
                    "${idx + 1}",
                    att.date,
                    att.time,
                    att.status,
                    att.note.ifBlank { "-" },
                    att.operatorName
                )
            }
            val stRow = rows.find { it.nis == selectedStudent.nis }
            doc.autoTable(
                settings = settings,
                documentTitle = "LAPORAN RIWAYAT KEHADIRAN INDIVIDU SISWA",
                metaLeft = studentMeta,
                metaRight = "Wali: ${selectedStudent.parentName.ifBlank { "-" }}",
                columns = cols,
                rows = dataRows,
                summaryLabel = "TOTAL REKAP INDIVIDU",
                summaryLabelColSpan = 3,
                summaryValues = listOf(
                    "${stRow?.percentage ?: 100}% Hadir",
                    "H:${stRow?.hadir ?: 0} T:${stRow?.terlambat ?: 0} I:${stRow?.izin ?: 0} S:${stRow?.sakit ?: 0} A:${stRow?.alpa ?: 0}",
                    "${studentHistory.size} Hari Tercatat"
                )
            )
        } else {
            doc.drawKopSuratHeader(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel
            )
            val cols = listOf(
                MadrasahPdfTableDocument.ColumnSpec("NO", 0.55f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("NIS", 0.95f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("NAMA LENGKAP SISWA", 2.8f, Paint.Align.LEFT, bold = true),
                MadrasahPdfTableDocument.ColumnSpec("KELAS", 1.1f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("AKTIF", 0.8f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("H", 0.6f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("T", 0.6f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("I", 0.6f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("S", 0.6f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("A", 0.6f, Paint.Align.CENTER),
                MadrasahPdfTableDocument.ColumnSpec("%", 0.85f, Paint.Align.CENTER, bold = true),
                MadrasahPdfTableDocument.ColumnSpec("PREDIKAT", 1.65f, Paint.Align.CENTER)
            )
            val dataRows = rows.mapIndexed { idx, r ->
                val predikat = when {
                    r.recordedDays == 0 -> "Belum Ada Data"
                    r.percentage >= 95 && r.alpa == 0 -> "Sangat Disiplin"
                    r.percentage >= 80 -> "Tertib / Baik"
                    else -> "Perlu Pembinaan"
                }
                listOf(
                    "${idx + 1}",
                    r.nis,
                    r.studentName,
                    r.className,
                    "${r.recordedDays} hr",
                    "${r.hadir}",
                    "${r.terlambat}",
                    "${r.izin}",
                    "${r.sakit}",
                    "${r.alpa}",
                    "${r.percentage}%",
                    predikat
                )
            }
            val totalH = rows.sumOf { it.hadir }
            val totalT = rows.sumOf { it.terlambat }
            val totalI = rows.sumOf { it.izin }
            val totalS = rows.sumOf { it.sakit }
            val totalA = rows.sumOf { it.alpa }
            val avgP = if (rows.isNotEmpty()) rows.map { it.percentage }.average().toInt() else 100

            doc.autoTable(
                settings = settings,
                documentTitle = reportTitle,
                metaLeft = metaLeft,
                metaRight = periodLabel,
                columns = cols,
                rows = dataRows,
                summaryLabel = "TOTAL / RATA-RATA (${rows.size} SISWA)",
                summaryLabelColSpan = 5,
                summaryValues = listOf("$totalH", "$totalT", "$totalI", "$totalS", "$totalA", "$avgP%", "-")
            )
        }

        doc.drawSignatureFooter(
            settings = settings,
            signerRoleLabel = signerRoleLabel,
            signerName = signerName
        )

        val fileName = "Laporan_Absensi_MI_${System.currentTimeMillis()}.pdf"
        return doc.writeToFile(context, fileName)
    }

    fun generateAndSharePdf(
        context: Context,
        settings: SchoolSettingsEntity,
        reportTitle: String,
        periodLabel: String,
        rows: List<StudentReportRow>,
        onResult: (String) -> Unit,
        signerRoleLabel: String = "Petugas Administrasi Absensi",
        signerName: String = "",
        reportPeriodType: String = "Bulanan",
        weekDates: List<String> = emptyList(),
        selectedStudent: StudentEntity? = null,
        studentHistory: List<com.example.data.AttendanceEntity> = emptyList(),
        classes: List<SchoolClassEntity> = emptyList(),
        teacherAttendance: List<TeacherAttendanceEntity> = emptyList()
    ) {
        try {
            val file = buildPdfFile(
                context = context,
                settings = settings,
                reportTitle = reportTitle,
                periodLabel = periodLabel,
                rows = rows,
                signerRoleLabel = signerRoleLabel,
                signerName = signerName,
                reportPeriodType = reportPeriodType,
                weekDates = weekDates,
                selectedStudent = selectedStudent,
                studentHistory = studentHistory,
                classes = classes,
                teacherAttendance = teacherAttendance
            )

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, reportTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Simpan PDF Laporan"))
            onResult("File PDF Kop Surat & Tanda Tangan berhasil dibuat: ${file.name}")
        } catch (e: Exception) {
            onResult("Gagal membuat PDF: ${e.localizedMessage}")
        }
    }

    fun buildArrearsPdfFile(
        context: Context,
        settings: SchoolSettingsEntity,
        reportTitle: String,
        classScopeLabel: String,
        arrears: List<ArrearPaymentEntity>,
        signerRoleLabel: String = "Bendahara / Wali Kelas",
        signerName: String = ""
    ): File {
        val doc = MadrasahPdfTableDocument()
        doc.startNewPage()
        val metaLeft = "Cakupan: $classScopeLabel"
        val metaRight = "TA ${settings.activeAcademicYear} (${settings.activeSemester})"

        doc.drawKopSuratHeader(
            settings = settings,
            documentTitle = reportTitle,
            metaLeft = metaLeft,
            metaRight = metaRight
        )

        val cols = listOf(
            MadrasahPdfTableDocument.ColumnSpec("NO", 0.55f, Paint.Align.CENTER),
            MadrasahPdfTableDocument.ColumnSpec("NIS", 0.95f, Paint.Align.CENTER),
            MadrasahPdfTableDocument.ColumnSpec("NAMA SISWA", 2.2f, Paint.Align.LEFT, bold = true),
            MadrasahPdfTableDocument.ColumnSpec("KELAS", 1.0f, Paint.Align.CENTER),
            MadrasahPdfTableDocument.ColumnSpec("TAGIHAN & PERIODE", 2.35f, Paint.Align.LEFT),
            MadrasahPdfTableDocument.ColumnSpec("NOMINAL", 1.3f, Paint.Align.RIGHT),
            MadrasahPdfTableDocument.ColumnSpec("DIBAYAR", 1.3f, Paint.Align.RIGHT),
            MadrasahPdfTableDocument.ColumnSpec("SISA", 1.35f, Paint.Align.RIGHT, bold = true),
            MadrasahPdfTableDocument.ColumnSpec("STATUS", 1.1f, Paint.Align.CENTER, bold = true)
        )

        val dataRows = arrears.mapIndexed { idx, ar ->
            val sisa = (ar.nominal - ar.paidAmount).coerceAtLeast(0L)
            listOf(
                "${idx + 1}",
                ar.studentNis,
                ar.studentName,
                ar.className,
                "${ar.billType} (${ar.period})",
                formatRupiah(ar.nominal),
                formatRupiah(ar.paidAmount),
                formatRupiah(sisa),
                ar.status
            )
        }

        val totalNom = arrears.sumOf { it.nominal }
        val totalPaid = arrears.sumOf { it.paidAmount }
        val totalUnpaid = (totalNom - totalPaid).coerceAtLeast(0L)

        doc.autoTable(
            settings = settings,
            documentTitle = reportTitle,
            metaLeft = metaLeft,
            metaRight = metaRight,
            columns = cols,
            rows = dataRows,
            summaryLabel = "TOTAL TUNGGAKAN (${arrears.size} TAGIHAN)",
            summaryLabelColSpan = 5,
            summaryValues = listOf(
                formatRupiah(totalNom),
                formatRupiah(totalPaid),
                formatRupiah(totalUnpaid),
                "-"
            )
        )

        doc.drawSignatureFooter(
            settings = settings,
            signerRoleLabel = signerRoleLabel,
            signerName = signerName
        )

        val fileName = "Laporan_Tunggakan_MI_${System.currentTimeMillis()}.pdf"
        return doc.writeToFile(context, fileName)
    }

    fun generateAndShareArrearsPdf(
        context: Context,
        settings: SchoolSettingsEntity,
        reportTitle: String,
        classScopeLabel: String,
        arrears: List<ArrearPaymentEntity>,
        onResult: (String) -> Unit,
        signerRoleLabel: String = "Bendahara / Wali Kelas",
        signerName: String = ""
    ) {
        try {
            val file = buildArrearsPdfFile(
                context = context,
                settings = settings,
                reportTitle = reportTitle,
                classScopeLabel = classScopeLabel,
                arrears = arrears,
                signerRoleLabel = signerRoleLabel,
                signerName = signerName
            )

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, reportTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Simpan PDF Tunggakan"))
            onResult("File PDF Laporan Tunggakan berhasil dibuat: ${file.name}")
        } catch (e: Exception) {
            onResult("Gagal membuat PDF Tunggakan: ${e.localizedMessage}")
        }
    }

    /**
     * Generates a clean, professional PDF Surat Rekap Tunggakan & Pembayaran Siswa
     * specifically formatted to be shared with parents/guardians (via WhatsApp / Print).
     */
    fun generateAndShareStudentArrearsPdfForParent(
        context: Context,
        settings: SchoolSettingsEntity,
        student: StudentEntity,
        arrears: List<ArrearPaymentEntity>,
        signerName: String,
        signerRoleLabel: String = "Wali Kelas / Bendahara Madrasah",
        onResult: (String) -> Unit
    ) {
        val reportTitle = "SURAT PEMBERITAHUAN & REKAP TUNGGAKAN SISWA"
        val scopeLabel = "${student.fullName} (NIS: ${student.nis} • ${student.className} • Wali: ${student.parentName.ifBlank { "-" }})"
        generateAndShareArrearsPdf(
            context = context,
            settings = settings,
            reportTitle = reportTitle,
            classScopeLabel = scopeLabel,
            arrears = arrears,
            onResult = onResult,
            signerRoleLabel = signerRoleLabel,
            signerName = signerName
        )
    }

    fun generateAndShareExcelCsv(
        context: Context,
        settings: SchoolSettingsEntity,
        reportTitle: String,
        periodLabel: String,
        rows: List<StudentReportRow>,
        onResult: (String) -> Unit
    ) {
        try {
            val sb = StringBuilder()
            sb.appendLine("\"${settings.schoolName}\"")
            sb.appendLine("\"NSM: ${settings.nsm} | NPSN: ${settings.npsn} | ${settings.address}\"")
            sb.appendLine("\"$reportTitle - Periode: $periodLabel\"")
            sb.appendLine("\"Tahun Ajaran: ${settings.activeAcademicYear} (${settings.activeSemester})\"")
            sb.appendLine()
            sb.appendLine("No,NIS,Nama Siswa,Kelas,Hadir,Terlambat,Izin,Sakit,Alpa,Persentase Kehadiran")
            rows.forEachIndexed { index, r ->
                val cleanName = r.studentName.replace("\"", "\"\"")
                sb.appendLine("${index + 1},\"${r.nis}\",\"$cleanName\",\"${r.className}\",${r.hadir},${r.terlambat},${r.izin},${r.sakit},${r.alpa},\"${r.percentage}%\"")
            }

            val fileName = "Rekap_Excel_MI_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText(sb.toString(), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$reportTitle (Excel CSV)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Ekspor Laporan Excel (CSV)"))
            onResult("File Excel (CSV) berhasil diekspor: $fileName")
        } catch (e: Exception) {
            onResult("Gagal mengekspor Excel: ${e.localizedMessage}")
        }
    }

    fun printHtmlDocument(
        context: Context,
        jobName: String,
        htmlContent: String,
        fallbackAction: () -> Unit,
        onResult: (String) -> Unit
    ) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                fallbackAction()
                return
            }

            val webView = WebView(context)
            webView.settings.javaScriptEnabled = false
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    try {
                        val adapter = webView.createPrintDocumentAdapter(jobName)
                        val attributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        printManager.print(jobName, adapter, attributes)
                        onResult("Membuka dialog cetak dokumen resmi (Kop Surat & CSS Print Media aktif)...")
                    } catch (e: Exception) {
                        fallbackAction()
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            fallbackAction()
        }
    }

    fun printStudentQrCardPdf(
        context: Context,
        settings: SchoolSettingsEntity,
        student: StudentEntity,
        onResult: (String) -> Unit
    ) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(350, 220, 1).create()
            val page = document.startPage(pageInfo)
            val c = page.canvas

            val headerPaint = Paint().apply {
                color = Color.rgb(11, 61, 46)
                style = Paint.Style.FILL
            }
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(17, 38, 31)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint().apply {
                color = Color.rgb(79, 102, 93)
                textSize = 8.5f
            }

            c.drawRect(0f, 0f, 350f, 46f, headerPaint)
            c.drawText("KARTU ABSENSI QR SISWA", 16f, 22f, titlePaint)
            c.drawText(settings.schoolName, 16f, 36f, titlePaint.apply { textSize = 8.5f })

            c.drawText(student.fullName, 16f, 74f, bodyPaint)
            c.drawText("NIS: ${student.nis}", 16f, 92f, subPaint)
            c.drawText("Kelas: ${student.className}", 16f, 108f, subPaint)
            c.drawText("Wali: ${student.parentName.ifBlank { "-" }}", 16f, 124f, subPaint)
            c.drawText("Kode QR: ${student.qrCodeValue.ifBlank { "MI-${student.nis}" }}", 16f, 144f, bodyPaint.apply { textSize = 9f })

            val qrPayload = student.qrCodeValue.ifBlank { "MI-${student.nis}" }
            val matrix = QrMatrixGenerator.generateMatrix(qrPayload, 21)
            val qrSize = 110f
            val cell = qrSize / 21f
            val startX = 220f
            val startY = 64f
            val blackPaint = Paint().apply { color = Color.rgb(11, 61, 46); style = Paint.Style.FILL }
            for (r in 0 until 21) {
                for (col in 0 until 21) {
                    if (matrix[r][col]) {
                        c.drawRect(
                            startX + col * cell,
                            startY + r * cell,
                            startX + (col + 1) * cell,
                            startY + (r + 1) * cell,
                            blackPaint
                        )
                    }
                }
            }

            document.finishPage(page)
            val fileName = "Kartu_QR_${student.nis}_${student.fullName.replace(" ", "_")}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Kartu QR - ${student.fullName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Cetak Kartu QR Siswa"))
            onResult("Kartu QR PDF siap dicetak: $fileName")
        } catch (e: Exception) {
            onResult("Gagal membuat Kartu QR PDF: ${e.localizedMessage}")
        }
    }

    fun shareBackupJsonFile(context: Context, jsonContent: String, onResult: (String) -> Unit) {
        try {
            val fileName = "Backup_Absensi_MI_${System.currentTimeMillis()}.json"
            val file = File(context.cacheDir, fileName)
            file.writeText(jsonContent, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Backup Data Absensi MI")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Simpan / Bagikan File Backup JSON"))
            onResult("File Backup JSON berhasil dibuat: $fileName")
        } catch (e: Exception) {
            onResult("Gagal mengekspor JSON: ${e.localizedMessage}")
        }
    }
}

object QrMatrixGenerator {
    /**
     * Generates a 21x21 QR Code Version 1 matrix with standard finder patterns,
     * timing patterns, and deterministic data modules encoded from payload.
     * Also encodes the 16-char ASCII payload in row-major unreserved cells so
     * CameraX real-time frame analysis can decode physical/screen QR cards.
     */
    fun generateMatrix(payload: String, size: Int = 21): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        fun placeFinder(rowOffset: Int, colOffset: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    matrix[rowOffset + r][colOffset + c] = isBorder || isInner
                    reserved[rowOffset + r][colOffset + c] = true
                }
            }
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = rowOffset + r
                    val cc = colOffset + c
                    if (rr in 0 until size && cc in 0 until size) {
                        reserved[rr][cc] = true
                    }
                }
            }
        }

        placeFinder(0, 0)
        placeFinder(0, size - 7)
        placeFinder(size - 7, 0)

        for (i in 8 until size - 8) {
            matrix[6][i] = i % 2 == 0
            reserved[6][i] = true
            matrix[i][6] = i % 2 == 0
            reserved[i][6] = true
        }

        val cleanPayload = payload.trim().ifEmpty { "MI-1101" }
        val bytes = cleanPayload.toByteArray(Charsets.UTF_8)
        // Header: 1 byte length + payload bytes + checksum byte
        val packet = ByteArray(bytes.size + 2)
        packet[0] = bytes.size.coerceAtMost(18).toByte()
        var checksum = packet[0].toInt()
        for (i in 0 until packet[0].toInt()) {
            packet[i + 1] = bytes[i]
            checksum = (checksum * 31 + bytes[i].toInt()) and 0xFF
        }
        packet[packet.size - 1] = checksum.toByte()

        var bitIdx = 0
        val totalPacketBits = packet.size * 8
        val hash = cleanPayload.hashCode()

        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!reserved[r][c]) {
                    if (bitIdx < totalPacketBits) {
                        val b = packet[bitIdx / 8].toInt() and 0xFF
                        val bit = (b shr (7 - (bitIdx % 8))) and 1
                        matrix[r][c] = bit == 1
                    } else {
                        val byteVal = bytes[(bitIdx / 8) % bytes.size].toInt()
                        val bit = (byteVal shr (bitIdx % 8)) and 1
                        val mix = ((r * 31 + c * 17 + hash) shr ((r + c) % 7)) and 1
                        matrix[r][c] = (bit xor mix) == 1
                    }
                    bitIdx++
                }
            }
        }
        return matrix
    }

    /**
     * Decodes a 21x21 boolean QR matrix generated by [generateMatrix] back into its QR payload string.
     */
    fun decodeMatrix(matrix: Array<BooleanArray>, size: Int = 21): String? {
        if (matrix.size != size) return null
        val reserved = Array(size) { BooleanArray(size) }

        fun markFinder(rowOffset: Int, colOffset: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = rowOffset + r
                    val cc = colOffset + c
                    if (rr in 0 until size && cc in 0 until size) {
                        reserved[rr][cc] = true
                    }
                }
            }
        }
        markFinder(0, 0)
        markFinder(0, size - 7)
        markFinder(size - 7, 0)
        for (i in 8 until size - 8) {
            reserved[6][i] = true
            reserved[i][6] = true
        }

        val bits = ArrayList<Int>(256)
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!reserved[r][c]) {
                    bits.add(if (matrix[r][c]) 1 else 0)
                }
            }
        }

        fun readByte(byteIndex: Int): Int {
            val start = byteIndex * 8
            if (start + 8 > bits.size) return -1
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[start + b]
            }
            return v
        }

        val len = readByte(0)
        if (len <= 0 || len > 18) return null
        val out = ByteArray(len)
        var checksum = len
        for (i in 0 until len) {
            val b = readByte(i + 1)
            if (b < 0) return null
            out[i] = b.toByte()
            checksum = (checksum * 31 + out[i].toInt()) and 0xFF
        }
        val expectedChecksum = readByte(len + 1)
        if (expectedChecksum != checksum) return null
        val decoded = String(out, Charsets.UTF_8)
        return decoded.takeIf { it.isNotBlank() && it.all { ch -> ch.code in 32..126 } }
    }

    /**
     * Real-time CameraX YUV_420_888 Luminance (Y-plane) QR Finder Pattern & Matrix Analyzer.
     * Detects 1:1:3:1:1 finder patterns in the center ROI and samples the 21x21 QR grid
     * against registered student QR matrices.
     */
    fun analyzeLuminanceFrame(
        yBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        knownCodes: List<String>
    ): Pair<String?, Float> {
        if (width < 64 || height < 64) return null to 0f
        yBuffer.rewind()
        val limit = yBuffer.remaining()

        fun lum(x: Int, y: Int): Int {
            val idx = y * rowStride + x
            return if (idx in 0 until limit) (yBuffer.get(idx).toInt() and 0xFF) else 128
        }

        val cropSize = (minOf(width, height) * 0.62f).toInt().coerceAtLeast(42)
        val startX = (width - cropSize) / 2
        val startY = (height - cropSize) / 2

        // Compute average luminance and contrast in the ROI
        var sum = 0L
        var minL = 255
        var maxL = 0
        val step = (cropSize / 21).coerceAtLeast(1)
        var samples = 0
        for (y in startY until (startY + cropSize) step step) {
            for (x in startX until (startX + cropSize) step step) {
                val v = lum(x, y)
                sum += v
                if (v < minL) minL = v
                if (v > maxL) maxL = v
                samples++
            }
        }
        val avgLum = if (samples > 0) (sum / samples).toFloat() else 128f
        val contrast = (maxL - minL).toFloat()
        if (contrast < 48f) {
            return null to avgLum
        }

        val threshold = ((minL + maxL) / 2)
        val sampledMatrix = Array(21) { BooleanArray(21) }
        val cellSize = cropSize / 21f
        for (r in 0 until 21) {
            for (c in 0 until 21) {
                val px = (startX + (c + 0.5f) * cellSize).toInt().coerceIn(0, width - 1)
                val py = (startY + (r + 0.5f) * cellSize).toInt().coerceIn(0, height - 1)
                sampledMatrix[r][c] = lum(px, py) < threshold
            }
        }

        // Verify top-left, top-right, bottom-left 7x7 finder corners
        fun checkFinderCorner(r0: Int, c0: Int): Boolean {
            // Center 3x3 should be dark, ring at distance 2 should be light, outer border dark
            val centerDark = sampledMatrix[r0 + 3][c0 + 3]
            val ringLight = !sampledMatrix[r0 + 1][c0 + 3] && !sampledMatrix[r0 + 3][c0 + 1]
            val borderDark = sampledMatrix[r0][c0] && sampledMatrix[r0 + 6][c0 + 6]
            return centerDark && ringLight && borderDark
        }

        if (!checkFinderCorner(0, 0) || !checkFinderCorner(0, 14) || !checkFinderCorner(14, 0)) {
            return null to avgLum
        }

        // Direct packet decode from sampled 21x21 matrix
        val directDecoded = decodeMatrix(sampledMatrix, 21)
        if (directDecoded != null) {
            return directDecoded to avgLum
        }

        // Fallback Hamming distance match against registered student QR codes
        var bestCode: String? = null
        var bestMatches = 0
        val totalCells = 21 * 21
        for (code in knownCodes) {
            val ref = generateMatrix(code, 21)
            var matches = 0
            for (r in 0 until 21) {
                for (c in 0 until 21) {
                    if (sampledMatrix[r][c] == ref[r][c]) matches++
                }
            }
            if (matches > bestMatches) {
                bestMatches = matches
                bestCode = code
            }
        }

        // Require > 86% bit match across the 21x21 matrix
        return if (bestCode != null && bestMatches >= (totalCells * 0.86).toInt()) {
            bestCode to avgLum
        } else {
            null to avgLum
        }
    }
}
