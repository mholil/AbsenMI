package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.util.ReportExportHelper

@Composable
fun KopSuratHeaderCard(
    settings: SchoolSettingsEntity,
    reportTitle: String,
    subtitleRight: String,
    modifier: Modifier = Modifier
) {
    MiCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MiMintSoft,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, MiDarkGreen),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = "Logo Kop Surat",
                            tint = MiDarkGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KEMENTERIAN AGAMA RI • PENDIDIKAN MADRASAH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiEmerald,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = settings.schoolName.uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MiDarkGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "NSM: ${settings.nsm}  |  NPSN: ${settings.npsn}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${settings.address} • Telp: ${settings.phone}",
                        fontSize = 10.sp,
                        color = MiTextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Double rule Kop Surat line
            HorizontalDivider(thickness = 2.dp, color = MiDarkGreen)
            Spacer(modifier = Modifier.height(1.5.dp))
            HorizontalDivider(thickness = 0.8.dp, color = MiDarkGreen)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reportTitle.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiDarkGreen,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = subtitleRight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiEmerald
                )
            }
        }
    }
}

@Composable
fun ReportsSection(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val user = state.currentUser ?: return
    val context = LocalContext.current
    val isWali = user.role == RoleType.WALI_KELAS

    val reportModes = remember(user.role) {
        when (user.role) {
            RoleType.WALI_KELAS -> listOf("Harian", "Mingguan", "Bulanan", "Per Siswa", "Per Kelas")
            RoleType.GURU_PIKET -> listOf("Harian", "Per Kelas", "Keterlambatan")
            else -> listOf("Harian", "Mingguan", "Bulanan", "Per Siswa", "Per Kelas", "Guru/Staf")
        }
    }

    val rows = remember(
        state.students,
        state.attendance,
        state.selectedDateFilter,
        state.selectedClassIdFilter,
        state.reportPeriodType,
        state.searchQuery
    ) {
        val baseRows = viewModel.buildReportRows(state)
        if (state.reportPeriodType == "Keterlambatan") {
            baseRows.filter { it.terlambat > 0 }
        } else {
            baseRows
        }
    }

    val reportTitle = "Laporan Absensi ${state.reportPeriodType}"
    val periodLabel = remember(state.reportPeriodType, state.selectedDateFilter) {
        viewModel.getReportPeriodSubtitle(state)
    }

    val scopedStudents = remember(state.students, state.selectedClassIdFilter, state.searchQuery, user) {
        viewModel.getScopedStudents(state)
    }
    var selectedStudentIdForReport by remember(scopedStudents) {
        mutableStateOf(scopedStudents.firstOrNull()?.id)
    }
    val selectedStudentObj = remember(scopedStudents, selectedStudentIdForReport) {
        scopedStudents.find { it.id == selectedStudentIdForReport } ?: scopedStudents.firstOrNull()
    }

    val last7Dates = remember(state.selectedDateFilter) {
        val base = state.selectedDateFilter.ifBlank { viewModel.repository.currentDateString() }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val parsed = try { sdf.parse(base) } catch (_: Exception) { null }
        if (parsed != null) {
            (6 downTo 0).map { offset ->
                val cal = java.util.Calendar.getInstance()
                cal.time = parsed
                cal.add(java.util.Calendar.DAY_OF_YEAR, -offset)
                sdf.format(cal.time)
            }
        } else {
            listOf(base)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Non-print Filter & Action Controls
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reportModes) { mode ->
                    FilterChip(
                        selected = state.reportPeriodType == mode,
                        onClick = { viewModel.setReportPeriodType(mode) },
                        label = { Text(mode, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MiDarkGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Info & Date/Period Selector Banner explaining Harian vs Mingguan vs Bulanan
        item {
            MiCard(contentPadding = PaddingValues(12.dp)) {
                val modeDesc = when (state.reportPeriodType) {
                    "Harian" -> "Laporan Harian menampilkan detail kehadiran pada 1 tanggal spesifik (Jam Scan/Input, Status Hari Ini, dan Catatan)."
                    "Mingguan" -> "Laporan Mingguan mengakumulasi jumlah Hadir (H), Terlambat (T), Izin (I), Sakit (S), dan Alpa (A) selama rentang 7 hari terakhir."
                    "Bulanan" -> "Laporan Bulanan mengakumulasi total kehadiran dan persentase disiplin siswa selama 1 bulan penuh (${state.selectedDateFilter.take(7)})."
                    "Per Siswa" -> "Rekapitulasi riwayat kehadiran masing-masing siswa secara menyeluruh."
                    "Per Kelas" -> "Perbandingan tingkat kehadiran dan rekap status antar kelas."
                    else -> "Rekapitulasi data kehadiran madrasah."
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = periodLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiDarkGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = modeDesc,
                            fontSize = 11.5.sp,
                            color = MiTextSecondary
                        )
                    }
                }
            }
        }

        // Download & Print Action Card: PDF, Excel (CSV), Cetak
        item {
            MiCard(contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = "Unduh & Cetak Laporan Resmi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Format PDF A4 lengkap Kop Surat Madrasah, data Excel spreadsheet, atau pratinjau cetak.",
                    fontSize = 12.sp,
                    color = MiTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val stHistoryForPdf = if (state.reportPeriodType == "Per Siswa" && selectedStudentObj != null) {
                                state.attendance
                                    .filter { it.studentId == selectedStudentObj.id }
                                    .sortedByDescending { "${it.date}_${it.time}" }
                            } else emptyList()
                            ReportExportHelper.generateAndSharePdf(
                                context = context,
                                settings = state.settings,
                                reportTitle = "$reportTitle - ${state.settings.schoolName}",
                                periodLabel = periodLabel,
                                rows = rows,
                                onResult = { viewModel.showMessage(it) },
                                signerRoleLabel = RoleType.label(user.role),
                                signerName = user.fullName,
                                reportPeriodType = state.reportPeriodType,
                                weekDates = last7Dates,
                                selectedStudent = selectedStudentObj,
                                studentHistory = stHistoryForPdf,
                                classes = state.classes,
                                teacherAttendance = state.teacherAttendance
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MiMintBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MiMintSoft,
                            contentColor = MiDarkGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("report_pdf_button")
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("PDF", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            ReportExportHelper.generateAndShareExcelCsv(
                                context = context,
                                settings = state.settings,
                                reportTitle = "$reportTitle - ${state.settings.schoolName}",
                                periodLabel = periodLabel,
                                rows = rows,
                                onResult = { viewModel.showMessage(it) }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MiMintBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MiMintSoft,
                            contentColor = MiDarkGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("report_excel_button")
                    ) {
                        Icon(Icons.Outlined.TableView, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Excel", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            val stHistoryForPrint = if (state.reportPeriodType == "Per Siswa" && selectedStudentObj != null) {
                                state.attendance
                                    .filter { it.studentId == selectedStudentObj.id }
                                    .sortedByDescending { "${it.date}_${it.time}" }
                            } else emptyList()
                            val html = ReportExportHelper.buildAttendancePrintHtml(
                                settings = state.settings,
                                reportTitle = "LAPORAN KEHADIRAN SISWA (${state.reportPeriodType.uppercase()})",
                                periodLabel = periodLabel,
                                rows = rows,
                                signerRoleLabel = RoleType.label(user.role),
                                signerName = user.fullName,
                                reportPeriodType = state.reportPeriodType,
                                weekDates = last7Dates,
                                selectedStudent = selectedStudentObj,
                                studentHistory = stHistoryForPrint,
                                classes = state.classes,
                                teacherAttendance = state.teacherAttendance
                            )
                            viewModel.openModal(
                                ActiveModal.PrintDocumentPreviewModal(
                                    documentTitle = "Laporan Absensi ${state.reportPeriodType}",
                                    htmlContent = html,
                                    isArrearsReport = false,
                                    periodOrScopeLabel = periodLabel
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MiMintBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MiMintSoft,
                            contentColor = MiDarkGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("report_print_button")
                    ) {
                        Icon(Icons.Outlined.Print, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Cetak", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            ClassFilterRow(
                classes = state.classes,
                selectedClassId = state.selectedClassIdFilter,
                isLockedToAssignedClass = isWali,
                assignedClassName = user.assignedClassName,
                onSelectClass = { viewModel.setClassFilter(it) }
            )
        }

        // Official Kop Surat Sekolah Header Preview inside Report Tab
        item {
            KopSuratHeaderCard(
                settings = state.settings,
                reportTitle = "Rekapitulasi Kehadiran ${state.reportPeriodType}",
                subtitleRight = "TA ${state.settings.activeAcademicYear} (${state.settings.activeSemester})"
            )
        }

        if (state.reportPeriodType == "Guru/Staf") {
            items(state.teacherAttendance, key = { it.id }) { ta ->
                MiCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ta.teacherName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MiTextPrimary
                            )
                            Text(
                                text = "NIP: ${ta.nip} • ${ta.position}",
                                fontSize = 12.sp,
                                color = MiTextSecondary
                            )
                            Text(
                                text = "Tanggal: ${ta.date} • Masuk: ${ta.timeIn} • Pulang: ${ta.timeOut}",
                                fontSize = 12.sp,
                                color = MiEmerald
                            )
                        }
                        StatusBadge(status = ta.status)
                    }
                }
            }
        } else if (state.reportPeriodType == "Per Kelas") {
            val targetClasses = if (isWali && user.assignedClassId != null) {
                state.classes.filter { it.id == user.assignedClassId }
            } else {
                state.classes
            }
            items(targetClasses, key = { it.id }) { cls ->
                val classRows = rows.filter { it.className == cls.className }
                val cHadir = classRows.sumOf { it.hadir }
                val cTelat = classRows.sumOf { it.terlambat }
                val cIzin = classRows.sumOf { it.izin }
                val cSakit = classRows.sumOf { it.sakit }
                val cAlpa = classRows.sumOf { it.alpa }
                val avgPct = if (classRows.isNotEmpty()) classRows.map { it.percentage }.average().toInt() else 0

                MiCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${cls.className} (${classRows.size} Siswa)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MiTextPrimary
                            )
                            Text(
                                text = "Wali Kelas: ${cls.homeroomTeacherName}",
                                fontSize = 12.sp,
                                color = MiTextSecondary
                            )
                        }
                        StatusBadge(status = "$avgPct% Kehadiran")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ReportMiniStat("Hadir", cHadir, StatusHadirText)
                        ReportMiniStat("Terlambat", cTelat, StatusTerlambatText)
                        ReportMiniStat("Izin", cIzin, StatusIzinText)
                        ReportMiniStat("Sakit", cSakit, StatusSakitText)
                        ReportMiniStat("Alpa", cAlpa, StatusAlpaText)
                    }
                }
            }
        } else if (state.reportPeriodType == "Harian") {
            // TABEL KHUSUS LAPORAN HARIAN: Menampilkan Jam Masuk/Scan, Status Hari Ini & Keterangan
            item {
                Surface(
                    color = MiDarkGreen,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("No", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        Text("NIS & Nama Siswa", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Jam", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(52.dp))
                        Text("Status Hari Ini", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(96.dp))
                    }
                }
            }

            itemsIndexed(rows, key = { _, it -> "daily_${it.nis}" }) { idx, row ->
                val rowBg = if (idx % 2 == 1) MiSurfaceVariant.copy(alpha = 0.55f) else Color.White
                Surface(
                    color = rowBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MiBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 12.sp,
                            color = MiTextSecondary,
                            modifier = Modifier.width(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.studentName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subText = if (row.latestNote.isNotBlank()) {
                                "${row.nis} • ${row.className} • ${row.latestNote}"
                            } else {
                                "${row.nis} • ${row.className}"
                            }
                            Text(
                                text = subText,
                                fontSize = 11.sp,
                                color = MiTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = row.latestTime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiDarkGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(52.dp)
                        )
                        Box(
                            modifier = Modifier.width(96.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            StatusBadge(status = row.latestStatus)
                        }
                    }
                }
            }

            if (rows.isNotEmpty()) {
                item {
                    val totalH = rows.sumOf { it.hadir }
                    val totalT = rows.sumOf { it.terlambat }
                    val totalI = rows.sumOf { it.izin }
                    val totalS = rows.sumOf { it.sakit }
                    val totalA = rows.sumOf { it.alpa }
                    val belum = rows.count { it.latestStatus == AttendanceStatus.BELUM_HADIR }

                    Surface(
                        color = MiMintSoft,
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        border = BorderStroke(1.dp, MiDarkGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RINGKASAN HARIAN (${rows.size} Siswa)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiDarkGreen
                            )
                            Text(
                                text = "H:$totalH  T:$totalT  I:$totalI  S:$totalS  A:$totalA  Belum:$belum",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiDarkGreen
                            )
                        }
                    }
                }
            }
        } else if (state.reportPeriodType == "Mingguan") {
            // TAMPILAN KHUSUS LAPORAN MINGGUAN: Matriks 7 Hari (H-6 s/d Hari Ini) + Kode Status H/T/I/S/A per Tanggal
            item {
                MiCard(contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Matriks Kehadiran 7 Hari Terakhir",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiDarkGreen
                            )
                            Text(
                                text = "Keterangan Kode: H = Hadir • T = Terlambat • I = Izin • S = Sakit • A = Alpa • - = Belum Tercatat",
                                fontSize = 11.sp,
                                color = MiTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MiDarkGreen,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Nama Siswa", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        last7Dates.forEach { d ->
                            val dayNum = d.takeLast(2)
                            Text(
                                text = dayNum,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(26.dp)
                            )
                        }
                        Text("%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
                    }
                }
            }

            itemsIndexed(rows, key = { _, it -> "weekly_${it.nis}" }) { idx, row ->
                val studentObj = scopedStudents.find { it.nis == row.nis }
                val rowBg = if (idx % 2 == 1) MiSurfaceVariant.copy(alpha = 0.55f) else Color.White
                Surface(
                    color = rowBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MiBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.studentName,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${row.className} • H:${row.hadir} T:${row.terlambat} I:${row.izin} S:${row.sakit} A:${row.alpa}",
                                fontSize = 10.5.sp,
                                color = MiTextSecondary
                            )
                        }
                        last7Dates.forEach { dateStr ->
                            val attOnDate = state.attendance.find { it.studentId == studentObj?.id && it.date == dateStr }
                            val code = when (attOnDate?.status) {
                                AttendanceStatus.HADIR -> "H"
                                AttendanceStatus.TERLAMBAT -> "T"
                                AttendanceStatus.IZIN -> "I"
                                AttendanceStatus.SAKIT -> "S"
                                AttendanceStatus.ALPA -> "A"
                                else -> "-"
                            }
                            val badgeTint = when (code) {
                                "H" -> StatusHadirText
                                "T" -> StatusTerlambatText
                                "I" -> StatusIzinText
                                "S" -> StatusSakitText
                                "A" -> StatusAlpaText
                                else -> MiTextMuted
                            }
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .padding(horizontal = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = if (code == "-") Color.Transparent else badgeTint.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = code,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeTint,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${row.percentage}%",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (row.percentage >= 80) StatusHadirText else StatusTerlambatText,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            }
        } else if (state.reportPeriodType == "Per Siswa") {
            // TAMPILAN KHUSUS LAPORAN PER SISWA: Pemilih Siswa + Kartu Rapor Kehadiran Individu + Riwayat Tanggal & Catatan + Kirim WA
            item {
                MiCard(contentPadding = PaddingValues(14.dp)) {
                    Text(
                        text = "Pilih Siswa untuk Melihat Rapor Kehadiran Individu:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiDarkGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scopedStudents, key = { it.id }) { st ->
                            FilterChip(
                                selected = selectedStudentObj?.id == st.id,
                                onClick = { selectedStudentIdForReport = st.id },
                                label = {
                                    Text(
                                        text = "${st.fullName} (${st.className})",
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MiEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            if (selectedStudentObj != null) {
                val stRow = rows.find { it.nis == selectedStudentObj.nis }
                val stHistory = state.attendance
                    .filter { it.studentId == selectedStudentObj.id }
                    .sortedByDescending { "${it.date}_${it.time}" }
                val stNotes = state.notes.filter { it.studentId == selectedStudentObj.id }
                val stAch = state.achievements.filter { it.studentId == selectedStudentObj.id }
                val stViol = state.violations.filter { it.studentId == selectedStudentObj.id }
                val stArrears = state.arrears.filter { it.studentId == selectedStudentObj.id && it.status != "Lunas" }

                item {
                    MiCard(contentPadding = PaddingValues(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedStudentObj.fullName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiDarkGreen
                                )
                                Text(
                                    text = "NIS: ${selectedStudentObj.nis} • ${selectedStudentObj.className} • Wali: ${selectedStudentObj.parentName.ifBlank { "-" }}",
                                    fontSize = 12.sp,
                                    color = MiTextSecondary
                                )
                                Text(
                                    text = "No. WhatsApp Wali: ${selectedStudentObj.parentPhone.ifBlank { "-" }}",
                                    fontSize = 12.sp,
                                    color = MiEmerald
                                )
                            }
                            StatusBadge(status = "${stRow?.percentage ?: 100}% Hadir")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ReportMiniStat("Hadir", stRow?.hadir ?: 0, StatusHadirText)
                            ReportMiniStat("Terlambat", stRow?.terlambat ?: 0, StatusTerlambatText)
                            ReportMiniStat("Izin", stRow?.izin ?: 0, StatusIzinText)
                            ReportMiniStat("Sakit", stRow?.sakit ?: 0, StatusSakitText)
                            ReportMiniStat("Alpa", stRow?.alpa ?: 0, StatusAlpaText)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MiBorderColor)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val msg = buildStudentComprehensiveWaMessage(
                                        student = selectedStudentObj,
                                        schoolName = state.settings.schoolName,
                                        attendance = state.attendance,
                                        notes = state.notes,
                                        achievements = state.achievements,
                                        violations = state.violations,
                                        arrears = state.arrears,
                                        senderName = user.fullName
                                    )
                                    openWhatsAppChat(
                                        context = context,
                                        rawPhone = selectedStudentObj.parentPhone,
                                        message = msg,
                                        onFeedback = { viewModel.showMessage(it) }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MiEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kirim Rekap WA Ortu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.openModal(ActiveModal.StudentDetail(selectedStudentObj.id))
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MiDarkGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Detail & Catatan (${stNotes.size + stAch.size + stViol.size})", fontSize = 12.sp, color = MiDarkGreen)
                            }
                        }

                        if (stArrears.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val sisaTotal = stArrears.sumOf { (it.nominal - it.paidAmount).coerceAtLeast(0L) }
                            Surface(
                                color = StatusTerlambatBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tunggakan Belum Lunas: ${stArrears.size} item (${formatRupiah(sisaTotal)})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StatusTerlambatText
                                    )
                                    TextButton(
                                        onClick = {
                                            ReportExportHelper.generateAndShareStudentArrearsPdfForParent(
                                                context = context,
                                                settings = state.settings,
                                                student = selectedStudentObj,
                                                arrears = stArrears,
                                                signerName = user.fullName,
                                                onResult = { viewModel.showMessage(it) }
                                            )
                                        }
                                    ) {
                                        Text("Cetak PDF Ortu", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MiDarkGreen)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Log Riwayat Kehadiran Harian ${selectedStudentObj.fullName} (${stHistory.size} Hari):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiDarkGreen
                    )
                }

                if (stHistory.isEmpty()) {
                    item {
                        MiCard(contentPadding = PaddingValues(14.dp)) {
                            Text("Belum ada log riwayat kehadiran tercatat untuk siswa ini.", fontSize = 12.5.sp, color = MiTextSecondary)
                        }
                    }
                } else {
                    items(stHistory, key = { "hist_${it.id}" }) { att ->
                        MiCard(contentPadding = PaddingValues(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tanggal: ${att.date} • Jam Masuk: ${att.time}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiTextPrimary
                                    )
                                    Text(
                                        text = "Keterangan: ${att.note.ifBlank { "-" }} • Dicatat: ${att.operatorName}",
                                        fontSize = 11.5.sp,
                                        color = MiTextSecondary
                                    )
                                }
                                StatusBadge(status = att.status)
                            }
                        }
                    }
                }
            }
        } else {
            // TAMPILAN KHUSUS LAPORAN BULANAN & KETERLAMBATAN: Pemilih Bulan + Ringkasan Eksekutif Bulanan + Tabel Rekap + Predikat Disiplin
            item {
                val activeMonthPrefix = state.selectedDateFilter.take(7).ifBlank { "2026-10" }
                val monthOptions = listOf(
                    "2026-10" to "Oktober 2026",
                    "2026-09" to "September 2026",
                    "2026-08" to "Agustus 2026",
                    "2026-07" to "Juli 2026"
                )
                MiCard(contentPadding = PaddingValues(12.dp)) {
                    Text(
                        text = "Pilih Bulan Rekapitulasi:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiDarkGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(monthOptions) { (ym, label) ->
                            FilterChip(
                                selected = activeMonthPrefix == ym,
                                onClick = { viewModel.setDateFilter("$ym-06") },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MiDarkGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MiDarkGreen,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("No", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        Text("Nama Siswa & Predikat Bulanan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("H", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text("T", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text("I", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text("S", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text("A", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text("%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(38.dp))
                    }
                }
            }

            itemsIndexed(rows, key = { _, it -> "monthly_${it.nis}" }) { idx, row ->
                val rowBg = if (idx % 2 == 1) MiSurfaceVariant.copy(alpha = 0.55f) else Color.White
                val predikat = when {
                    row.recordedDays == 0 -> "Belum Ada Data Bulan Ini"
                    row.percentage >= 95 && row.alpa == 0 -> "Sangat Disiplin"
                    row.percentage >= 80 -> "Tertib / Baik"
                    else -> "Perlu Pembinaan Wali/BK"
                }
                Surface(
                    color = rowBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MiBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 12.sp,
                            color = MiTextSecondary,
                            modifier = Modifier.width(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.studentName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${row.className} • ${row.recordedDays} hari aktif • $predikat",
                                fontSize = 11.sp,
                                color = if (row.percentage >= 80) MiEmerald else StatusTerlambatText
                            )
                        }
                        Text("${row.hadir}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusHadirText, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text("${row.terlambat}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusTerlambatText, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text("${row.izin}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusIzinText, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text("${row.sakit}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusSakitText, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text("${row.alpa}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusAlpaText, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        Text(
                            text = "${row.percentage}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (row.percentage >= 80) StatusHadirText else StatusTerlambatText,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(38.dp)
                        )
                    }
                }
            }

            if (rows.isNotEmpty()) {
                item {
                    val totalH = rows.sumOf { it.hadir }
                    val totalT = rows.sumOf { it.terlambat }
                    val totalI = rows.sumOf { it.izin }
                    val totalS = rows.sumOf { it.sakit }
                    val totalA = rows.sumOf { it.alpa }
                    val avgPct = rows.map { it.percentage }.average().toInt()

                    Surface(
                        color = MiMintSoft,
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        border = BorderStroke(1.dp, MiDarkGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL / RATA-RATA (${rows.size} Siswa)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiDarkGreen,
                                modifier = Modifier.weight(1f)
                            )
                            Text("$totalH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MiDarkGreen, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                            Text("$totalT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MiDarkGreen, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                            Text("$totalI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MiDarkGreen, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                            Text("$totalS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MiDarkGreen, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                            Text("$totalA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MiDarkGreen, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                            Text("$avgPct%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MiDarkGreen, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMiniStat(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MiTextSecondary
        )
    }
}
