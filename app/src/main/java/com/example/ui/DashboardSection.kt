package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
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
        // Welcome Banner & Attendance Rate Card (Matching Login Gradient & Style)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color(0x333A4FA0),
                        spotColor = Color(0x333A4FA0)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4978FF), // #4978ff
                                Color(0xFF8051E9)  // #8051e9
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.settings.schoolName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tanggal: $today",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isWali) "Wali ${user.assignedClassName} • ${user.fullName}" else "${user.fullName} • ${RoleType.label(user.role)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Proportional percentage badge
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
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
                        color = Color(0xFF72C7FF),
                        trackColor = Color.White.copy(alpha = 0.25f)
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
    }

        // 6 Status Metric Grid (Hadir, Terlambat, Izin, Sakit, Alpa, Jumlah Siswa / Belum Hadir)
        item {
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
            val myClass = state.classes.find { it.id == user.assignedClassId }
            val classArrears = state.arrears.filter { it.classId == user.assignedClassId && it.status != "Lunas" }
            val totalArrearsNominal = classArrears.sumOf { (it.nominal - it.paidAmount).coerceAtLeast(0L) }

            // 1. Menu Aksi Cepat Wali Kelas (Quick Actions Grid)
            item {
                MiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HomeroomActionTile(
                            label = "Absensi Kelas",
                            subtitle = "Input & Cek",
                            icon = Icons.Outlined.FactCheck,
                            accentColor = Color(0xFF4978FF),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.selectBottomTab(2) }
                        )
                        HomeroomActionTile(
                            label = "Data Siswa",
                            subtitle = "${scopedStudents.size} Siswa",
                            icon = Icons.Outlined.Groups,
                            accentColor = Color(0xFF8051E9),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.selectBottomTab(1) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HomeroomActionTile(
                            label = "Info & Catatan",
                            subtitle = "Pengumuman",
                            icon = Icons.Outlined.Campaign,
                            accentColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.openModal(ActiveModal.HomeroomAdminModal) }
                        )
                        HomeroomActionTile(
                            label = "Tunggakan",
                            subtitle = "${classArrears.size} Tagihan",
                            icon = Icons.Outlined.Payments,
                            accentColor = Color(0xFFD97706),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.openModal(ActiveModal.ArrearsManagementModal) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HomeroomActionTile(
                            label = "Rapor Kelas",
                            subtitle = "Rekap & Cetak",
                            icon = Icons.Outlined.Assessment,
                            accentColor = Color(0xFF059669),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.selectBottomTab(3) }
                        )
                        HomeroomActionTile(
                            label = "Format WA",
                            subtitle = "Template Pesan",
                            icon = Icons.Outlined.SendToMobile,
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.openModal(ActiveModal.WaArrearTemplateModal) }
                        )
                    }
                }
            }

            // 2. Alert / Urgent Box: Siswa Belum Hadir & Alpa Hari Ini
            val unmarkedStudents = scopedStudents.filter { s -> todayAtt.none { it.studentId == s.id } }
            val alpaStudents = scopedStudents.filter { s -> todayAtt.any { it.studentId == s.id && it.status == AttendanceStatus.ALPA } }
            val attentionList = (alpaStudents + unmarkedStudents).distinctBy { it.id }

            item {
                if (attentionList.isNotEmpty()) {
                    MiCard(
                        borderColor = Color(0xFFF59E0B).copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Perhatian Khusus Hari Ini",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MiTextPrimary
                                    )
                                    Text(
                                        text = "${attentionList.size} siswa belum absen / tanpa keterangan",
                                        fontSize = 12.sp,
                                        color = MiTextSecondary
                                    )
                                }
                            }
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${attentionList.size} Siswa",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MiBorderColor)
                        Spacer(modifier = Modifier.height(8.dp))

                        attentionList.take(5).forEachIndexed { idx, st ->
                            val isAlpa = alpaStudents.any { it.id == st.id }
                            val statusLabel = if (isAlpa) "Alpa" else "Belum Hadir"
                            val statusBg = if (isAlpa) StatusAlpaBg else StatusBelumBg
                            val statusColor = if (isAlpa) StatusAlpaText else StatusBelumText

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MiMintSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = st.fullName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MiEmerald
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = st.fullName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MiTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "NIS: ${st.nis} • Wali: ${st.parentName.ifBlank { "Ortu" }}",
                                            fontSize = 11.sp,
                                            color = MiTextMuted
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = statusBg,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    // WhatsApp Button
                                    IconButton(
                                        onClick = {
                                            val waMsg = """
                                                Assalamu'alaikum Wr. Wb.
                                                Yth. Bapak/Ibu ${st.parentName.ifBlank { "Orang Tua/Wali" }},
                                                Wali dari ananda *${st.fullName}* (Kelas ${st.className}).

                                                Kami dari Wali Kelas mengonfirmasikan bahwa pada hari ini ($today), ananda tercatat *$statusLabel* di kelas. Mohon konfirmasi kehadiran atau keterangan (sakit/izin) jika ananda berhalangan hadir.

                                                Terima kasih atas kerja samanya.
                                                Wassalamu'alaikum Wr. Wb.
                                                — Wali Kelas ${st.className} (${user.fullName})
                                            """.trimIndent()
                                            openWhatsAppChat(context, st.parentPhone, waMsg) {
                                                viewModel.showMessage(it)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFE8F8EE), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Chat,
                                            contentDescription = "Chat WhatsApp",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (idx < attentionList.take(5).lastIndex) {
                                HorizontalDivider(color = MiBorderColor.copy(alpha = 0.5f))
                            }
                        }

                        if (attentionList.size > 5) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "+${attentionList.size - 5} siswa lainnya belum terdata kehadirannya",
                                fontSize = 11.sp,
                                color = MiTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        NeumorphicTactileButton(
                            onClick = { viewModel.selectBottomTab(2) },
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.FactCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Absensi Kelas Untuk Update", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // All present / accounted for banner
                    MiCard(
                        borderColor = Color(0xFF10B981).copy(alpha = 0.5f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F8EE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Status Kehadiran Lengkap!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiTextPrimary
                                )
                                Text(
                                    text = "Seluruh siswa kelas ${user.assignedClassName} sudah terdata kehadirannya hari ini.",
                                    fontSize = 12.sp,
                                    color = MiTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 3. Papan Catatan & Pengumuman Kelas
            item {
                MiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Papan Informasi ${user.assignedClassName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MiTextPrimary
                            )
                            Text(
                                text = "Ruang: ${myClass?.roomNumber ?: "-"} • Wali: ${user.fullName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MiTextSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.openModal(ActiveModal.HomeroomAdminModal) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MiMintSoft, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Administrasi",
                                tint = MiEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MiBorderColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Pengumuman Box
                    Surface(
                        color = Color(0xFFF0F4FF),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Campaign,
                                contentDescription = null,
                                tint = MiEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Pengumuman Kelas:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiEmerald
                                )
                                Text(
                                    text = myClass?.classAnnouncement?.ifBlank { "Belum ada pengumuman aktif untuk kelas ini." } ?: "-",
                                    fontSize = 13.sp,
                                    color = MiTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Catatan Kelas Box
                    Surface(
                        color = Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MiBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notes,
                                contentDescription = null,
                                tint = MiTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Catatan Wali Kelas / Inventaris:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiTextSecondary
                                )
                                Text(
                                    text = myClass?.classNotes?.ifBlank { "Belum ada catatan administrasi kelas." } ?: "-",
                                    fontSize = 13.sp,
                                    color = MiTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Monitoring Tunggakan & Tagihan Kelas
            item {
                MiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keuangan & Tunggakan Kelas",
                                style = MaterialTheme.typography.titleMedium,
                                color = MiTextPrimary
                            )
                            Text(
                                text = "Khusus siswa ${user.assignedClassName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MiTextSecondary
                            )
                        }
                        Surface(
                            color = if (classArrears.isNotEmpty()) Color(0xFFFEE2E2) else Color(0xFFE8F8EE),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (classArrears.isNotEmpty()) "${classArrears.size} Tagihan" else "Semua Lunas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (classArrears.isNotEmpty()) Color(0xFFDC2626) else Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF9FAFB),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MiBorderColor),
                            modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Total Nominal Tunggakan",
                                    fontSize = 11.sp,
                                    color = MiTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatRupiah(totalArrearsNominal),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalArrearsNominal > 0) Color(0xFFDC2626) else MiEmerald
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFF9FAFB),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MiBorderColor),
                            modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Siswa Menunggak",
                                    fontSize = 11.sp,
                                    color = MiTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val uniqueStudentsInDebt = classArrears.map { it.studentId }.distinct().size
                                Text(
                                    text = "$uniqueStudentsInDebt Siswa",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    NeumorphicTactileButton(
                        onClick = { viewModel.openModal(ActiveModal.ArrearsManagementModal) },
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth().testTag("wali_lihat_tunggakan_btn")
                    ) {
                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kelola Tunggakan & Kirim Tagihan WA", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun HomeroomActionTile(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, MiBorderColor),
        modifier = modifier.heightIn(min = 68.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
