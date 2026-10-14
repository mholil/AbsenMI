package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun DashboardSection(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val user = state.currentUser ?: return
    val isWali = user.role == RoleType.WALI_KELAS
    val scopedStudents = if (isWali && user.assignedClassId != null) {
        state.students.filter { it.classId == user.assignedClassId && it.status == "Aktif" }
    } else {
        state.students.filter { it.status == "Aktif" }
    }

    val today = state.selectedDateFilter
    val todayAtt = state.attendance.filter { att ->
        att.date == today && (!isWali || user.assignedClassId == null || att.classId == user.assignedClassId)
    }

    val totalStudents = scopedStudents.size
    val hadirCount = todayAtt.count { it.status == AttendanceStatus.HADIR }
    val terlambatCount = todayAtt.count { it.status == AttendanceStatus.TERLAMBAT }
    val izinCount = todayAtt.count { it.status == AttendanceStatus.IZIN }
    val sakitCount = todayAtt.count { it.status == AttendanceStatus.SAKIT }
    val alpaCount = todayAtt.count { it.status == AttendanceStatus.ALPA }
    val belumHadirCount = (totalStudents - (hadirCount + terlambatCount + izinCount + sakitCount + alpaCount)).coerceAtLeast(0)

    val presentTotal = hadirCount + terlambatCount
    val attendancePercent = if (totalStudents > 0) ((presentTotal * 100f) / totalStudents).toInt() else 100

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome Banner & Attendance Rate Card
        item {
            MiCard(
                backgroundColor = MiDarkGreen,
                borderColor = MiEmerald,
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isWali) "DASHBOARD WALI KELAS • ${user.assignedClassName}" else "DASHBOARD ${RoleType.label(user.role).uppercase()}",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = user.fullName,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${state.settings.schoolName} • Tanggal: $today",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Proportional percentage badge (24-28sp per spec)
                    Surface(
                        color = MiEmerald,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$attendancePercent%",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Kehadiran",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { (attendancePercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF34D399),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Siswa Aktif: $totalStudents Siswa",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Belum Absen: $belumHadirCount Siswa",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // 6 Status Metric Grid (Hadir, Terlambat, Izin, Sakit, Alpa, Jumlah Siswa / Belum Hadir)
        item {
            Text(
                text = "Statistik Kehadiran Hari Ini ($today)",
                style = MaterialTheme.typography.titleMedium,
                color = MiTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        label = "Hadir",
                        valueText = "$hadirCount",
                        subtitle = "Tepat waktu",
                        accentColor = StatusHadirText,
                        bgTint = StatusHadirBg,
                        icon = Icons.Outlined.CheckCircle,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectBottomTab(if (user.role == RoleType.KEPALA_SEKOLAH) 1 else 2) }
                    )
                    StatMetricCard(
                        label = "Terlambat",
                        valueText = "$terlambatCount",
                        subtitle = "Lewat ${state.settings.lateThresholdTime}",
                        accentColor = StatusTerlambatText,
                        bgTint = StatusTerlambatBg,
                        icon = Icons.Outlined.Schedule,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (user.role == RoleType.KEPALA_SEKOLAH) viewModel.selectBottomTab(3)
                            else viewModel.selectBottomTab(2)
                        }
                    )
                    StatMetricCard(
                        label = "Izin",
                        valueText = "$izinCount",
                        subtitle = "Dengan keterangan",
                        accentColor = StatusIzinText,
                        bgTint = StatusIzinBg,
                        icon = Icons.Outlined.AssignmentInd,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        label = "Sakit",
                        valueText = "$sakitCount",
                        subtitle = "Istirahat/Rawat",
                        accentColor = StatusSakitText,
                        bgTint = StatusSakitBg,
                        icon = Icons.Outlined.MedicalServices,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "Alpa",
                        valueText = "$alpaCount",
                        subtitle = "Tanpa keterangan",
                        accentColor = StatusAlpaText,
                        bgTint = StatusAlpaBg,
                        icon = Icons.Outlined.Cancel,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "Belum Hadir",
                        valueText = "$belumHadirCount",
                        subtitle = "Dari $totalStudents siswa",
                        accentColor = StatusBelumText,
                        bgTint = StatusBelumBg,
                        icon = Icons.Outlined.HourglassEmpty,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Role-specific Quick Action Cards
        if (user.role == RoleType.ADMIN) {
            item {
                MiCard {
                    Text(
                        text = "Akses Cepat Administrasi Madrasah",
                        style = MaterialTheme.typography.titleMedium,
                        color = MiTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeumorphicTactileButton(
                            onClick = { viewModel.openModal(ActiveModal.ArrearsManagementModal) },
                            isPrimary = false,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_arrears_button")
                        ) {
                            Icon(Icons.Outlined.Payments, contentDescription = null, tint = MiEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tunggakan MI", fontSize = 13.sp, maxLines = 1, color = MiTextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        NeumorphicTactileButton(
                            onClick = { viewModel.openModal(ActiveModal.BackupRestoreModal) },
                            isPrimary = false,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_backup_button")
                        ) {
                            Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = MiEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup JSON", fontSize = 13.sp, maxLines = 1, color = MiTextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeumorphicTactileButton(
                            onClick = { viewModel.openModal(ActiveModal.ClassForm()) },
                            isPrimary = false,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_manage_class_button")
                        ) {
                            Icon(Icons.Outlined.School, contentDescription = null, tint = MiEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kelola Kelas", fontSize = 13.sp, maxLines = 1, color = MiTextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        NeumorphicTactileButton(
                            onClick = { viewModel.openModal(ActiveModal.StudentForm()) },
                            isPrimary = true,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_quick_add_student_button")
                        ) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Siswa", fontSize = 13.sp, maxLines = 1, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (isWali) {
            item {
                val myClass = state.classes.find { it.id == user.assignedClassId }
                val classArrears = state.arrears.filter { it.classId == user.assignedClassId && it.status != "Lunas" }
                MiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ringkasan & Administrasi ${user.assignedClassName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MiTextPrimary
                            )
                            Text(
                                text = "Ruang: ${myClass?.roomNumber ?: "-"} • Wali: ${user.fullName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MiTextSecondary
                            )
                        }
                        NeumorphicTactileButton(
                            onClick = { viewModel.openModal(ActiveModal.HomeroomAdminModal) },
                            isPrimary = true,
                            modifier = Modifier.testTag("homeroom_admin_button")
                        ) {
                            Icon(Icons.Outlined.FolderShared, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Administrasi Kelas", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MiBorderColor)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Catatan Kelas: ${myClass?.classNotes?.ifBlank { "Belum ada catatan kelas" } ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MiTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pengumuman: ${myClass?.classAnnouncement?.ifBlank { "Tidak ada pengumuman aktif" } ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MiEmerald,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tunggakan Siswa Kelas Ini: ${classArrears.size} Tagihan Belum Lunas",
                            fontSize = 13.sp,
                            color = if (classArrears.isNotEmpty()) StatusTerlambatText else MiTextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    NeumorphicTactileButton(
                        onClick = { viewModel.openModal(ActiveModal.ArrearsManagementModal) },
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth().testTag("wali_lihat_tunggakan_btn")
                    ) {
                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kelola Tunggakan & Kirim Tagihan", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Per-class Attendance Summary for Admin & Kepala Sekolah
        if (!isWali) {
            item {
                MiCard {
                    Text(
                        text = "Rekap Kehadiran Per Kelas Hari Ini",
                        style = MaterialTheme.typography.titleMedium,
                        color = MiTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    state.classes.forEachIndexed { idx, cls ->
                        val cStudents = state.students.filter { it.classId == cls.id && it.status == "Aktif" }
                        val cAtt = todayAtt.filter { it.classId == cls.id }
                        val cPresent = cAtt.count { it.status == AttendanceStatus.HADIR || it.status == AttendanceStatus.TERLAMBAT }
                        val cPct = if (cStudents.isNotEmpty()) ((cPresent * 100f) / cStudents.size).toInt() else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${cls.className} • ${cls.homeroomTeacherName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiTextPrimary
                                )
                                Text(
                                    text = "Hadir: $cPresent / ${cStudents.size} Siswa",
                                    fontSize = 12.sp,
                                    color = MiTextSecondary
                                )
                            }
                            StatusBadge(status = "$cPct% Hadir")
                        }
                        if (idx < state.classes.lastIndex) {
                            HorizontalDivider(color = MiBorderColor.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        // Recent Activity Logs
        item {
            Text(
                text = "Aktivitas Terbaru Sistem",
                style = MaterialTheme.typography.titleMedium,
                color = MiTextPrimary
            )
        }

        items(state.activityLogs.take(8)) { log ->
            MiCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MiMintSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MiEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.actionTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiTextPrimary
                            )
                            Text(
                                text = log.timestamp,
                                fontSize = 11.sp,
                                color = MiTextMuted
                            )
                        }
                        Text(
                            text = log.description,
                            fontSize = 13.sp,
                            color = MiTextSecondary
                        )
                        Text(
                            text = "Oleh: ${log.actorName} (${log.actorRole})",
                            fontSize = 11.sp,
                            color = MiEmerald
                        )
                    }
                }
            }
        }
    }
}
