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
import androidx.compose.runtime.*
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
import com.example.util.ReportExportHelper

@Composable
fun DataAndStudentsSection(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val user = state.currentUser ?: return
    val isAdmin = user.role == RoleType.ADMIN
    val isWali = user.role == RoleType.WALI_KELAS
    val context = LocalContext.current

    var waliFilterType by remember { mutableStateOf("ALL") }
    val rawStudents = viewModel.getScopedStudents(state)
    val students = if (isWali) {
        when (waliFilterType) {
            "L" -> rawStudents.filter { it.gender.startsWith("L", ignoreCase = true) }
            "P" -> rawStudents.filter { it.gender.startsWith("P", ignoreCase = true) }
            "NOTES" -> rawStudents.filter { st ->
                state.notes.any { it.studentId == st.id } ||
                state.achievements.any { it.studentId == st.id } ||
                state.violations.any { it.studentId == st.id }
            }
            "ARREARS" -> rawStudents.filter { st ->
                state.arrears.any { it.studentId == st.id && it.status != "Lunas" && (it.nominal - it.paidAmount) > 0L }
            }
            else -> rawStudents
        }
    } else {
        rawStudents
    }

    val maleCount = rawStudents.count { it.gender.startsWith("L", ignoreCase = true) }
    val femaleCount = rawStudents.count { it.gender.startsWith("P", ignoreCase = true) }
    val notesCount = rawStudents.count { st ->
        state.notes.any { it.studentId == st.id } ||
        state.achievements.any { it.studentId == st.id } ||
        state.violations.any { it.studentId == st.id }
    }
    val arrearsCount = rawStudents.count { st ->
        state.arrears.any { it.studentId == st.id && it.status != "Lunas" && (it.nominal - it.paidAmount) > 0L }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Admin has 5 sub-tabs: Siswa, Guru/Staf, Kelas, Akun Pengguna, Tunggakan & SPP
        // Wali Kelas only manages their own class's Siswa (+ Administrasi Kelas & Tunggakan Kelas access)
        if (isAdmin) {
            ScrollableTabRow(
                selectedTabIndex = state.dataSectionTab,
                containerColor = MiSurface,
                contentColor = Color(0xFF4978FF),
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                val tabs = listOf(
                    "Siswa (${state.students.size})",
                    "Guru & Staf (${state.teachers.size})",
                    "Kelas (${state.classes.size})",
                    "Akun (${state.users.size})",
                    "Tunggakan & SPP (${state.arrears.size})"
                )
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = state.dataSectionTab == idx,
                        onClick = { viewModel.setDataSectionTab(idx) },
                        text = {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (state.dataSectionTab == idx) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else if (isWali) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x334978FF)),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4978FF),
                                    Color(0xFF8051E9)
                                )
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Data Siswa ${user.assignedClassName}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Wali: ${user.fullName} • TP ${state.settings.activeAcademicYear}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Groups,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${rawStudents.size} Siswa",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Breakdown Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "♂ $maleCount Laki-laki",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "♀ $femaleCount Perempuan",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "⚠ $arrearsCount Tunggakan",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Homeroom Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.openModal(ActiveModal.ArrearsManagementModal) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF4978FF)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 36.dp)
                            ) {
                                Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tunggakan SPP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.openModal(ActiveModal.HomeroomAdminModal) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.22f),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 36.dp)
                            ) {
                                Icon(Icons.Outlined.Campaign, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Info / Catatan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Search and Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Cari nama, NIS, NIP, kelas...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MiTextSecondary)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("data_search_input")
            )

            val canAdd = when {
                isAdmin -> true
                isWali && state.dataSectionTab == 0 -> user.canAddStudent
                else -> false
            }

            if (canAdd) {
                Button(
                    onClick = {
                        when (if (isAdmin) state.dataSectionTab else 0) {
                            0 -> viewModel.openModal(
                                ActiveModal.StudentForm(
                                    existing = null,
                                    preselectedClassId = if (isWali) user.assignedClassId else state.selectedClassIdFilter
                                )
                            )
                            1 -> viewModel.openModal(ActiveModal.TeacherForm(null))
                            2 -> viewModel.openModal(ActiveModal.ClassForm(null))
                            3 -> viewModel.openModal(ActiveModal.UserAccountForm(null))
                            4 -> viewModel.openModal(ActiveModal.ArrearForm(null))
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4978FF)),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("data_add_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Tambah Data")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val activeSubTab = if (isAdmin) state.dataSectionTab else 0

        if (isWali) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                val chips = listOf(
                    "ALL" to "Semua (${rawStudents.size})",
                    "L" to "Laki-laki ($maleCount)",
                    "P" to "Perempuan ($femaleCount)",
                    "NOTES" to "Catatan ($notesCount)",
                    "ARREARS" to "Tunggakan ($arrearsCount)"
                )
                items(chips) { (key, label) ->
                    val isSelected = waliFilterType == key
                    Surface(
                        onClick = { waliFilterType = key },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF4978FF) else MiSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF4978FF) else MiBorderColor
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MiTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        } else if (isAdmin && (activeSubTab == 0 || activeSubTab == 4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ClassFilterRow(
                        classes = state.classes,
                        selectedClassId = state.selectedClassIdFilter,
                        isLockedToAssignedClass = false,
                        assignedClassName = user.assignedClassName,
                        onSelectClass = { viewModel.setClassFilter(it) }
                    )
                }
                if (activeSubTab == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { viewModel.openModal(ActiveModal.BackupRestoreModal) },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Outlined.ImportExport, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import/Export", fontSize = 12.sp, color = Color(0xFF4978FF))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (activeSubTab) {
            0 -> StudentsListContent(
                students = students,
                attendance = state.attendance,
                notes = state.notes,
                achievements = state.achievements,
                violations = state.violations,
                arrears = state.arrears,
                schoolName = state.settings.schoolName,
                senderName = user.fullName,
                onSelectStudent = { st -> viewModel.openModal(ActiveModal.StudentDetail(st.id)) },
                onShowQr = { st -> viewModel.openModal(ActiveModal.StudentQrCard(st)) },
                onEditStudent = { st -> viewModel.openModal(ActiveModal.StudentForm(existing = st)) },
                onAddNote = { st ->
                    viewModel.openModal(
                        ActiveModal.NoteRecordForm(
                            student = st,
                            category = "Catatan",
                            initialDate = state.selectedDateFilter
                        )
                    )
                },
                onFeedback = { viewModel.showMessage(it) }
            )
            1 -> TeachersListContent(
                teachers = state.teachers.filter {
                    val q = state.searchQuery.trim().lowercase()
                    q.isEmpty() || it.fullName.lowercase().contains(q) || it.nip.lowercase().contains(q) || it.position.lowercase().contains(q)
                },
                onEdit = { viewModel.openModal(ActiveModal.TeacherForm(it)) },
                onRecordAttendance = { viewModel.openModal(ActiveModal.TeacherAttendanceForm(it)) },
                onDelete = { viewModel.deleteTeacher(it) }
            )
            2 -> ClassesListContent(
                classes = state.classes.filter {
                    val q = state.searchQuery.trim().lowercase()
                    q.isEmpty() || it.className.lowercase().contains(q) || it.homeroomTeacherName.lowercase().contains(q)
                },
                students = state.students,
                onEdit = { viewModel.openModal(ActiveModal.ClassForm(it)) },
                onDelete = { viewModel.deleteClass(it) }
            )
            3 -> UserAccountsListContent(
                users = state.users.filter {
                    val q = state.searchQuery.trim().lowercase()
                    q.isEmpty() || it.fullName.lowercase().contains(q) || it.username.lowercase().contains(q) || it.role.lowercase().contains(q)
                },
                onEdit = { viewModel.openModal(ActiveModal.UserAccountForm(it)) },
                onResetPassword = { viewModel.openModal(ActiveModal.ResetPasswordModal(it)) },
                onDelete = { viewModel.deleteUserAccount(it) }
            )
            4 -> AdminArrearsTabContent(
                state = state,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun StudentsListContent(
    students: List<StudentEntity>,
    attendance: List<AttendanceEntity>,
    notes: List<StudentNoteEntity>,
    achievements: List<StudentAchievementEntity>,
    violations: List<StudentViolationEntity>,
    arrears: List<ArrearPaymentEntity>,
    schoolName: String,
    senderName: String,
    onSelectStudent: (StudentEntity) -> Unit,
    onShowQr: (StudentEntity) -> Unit,
    onEditStudent: (StudentEntity) -> Unit,
    onAddNote: (StudentEntity) -> Unit,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    if (students.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = MiTextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Belum ada data siswa yang sesuai filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(students, key = { it.id }) { st ->
            val stNotes = notes.count { it.studentId == st.id }
            val stAch = achievements.count { it.studentId == st.id }
            val stViol = violations.count { it.studentId == st.id }
            val unpaidCount = arrears.count { it.studentId == st.id && it.status != "Lunas" && (it.nominal - it.paidAmount) > 0L }

            MiCard(
                onClick = { onSelectStudent(st) },
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMale = st.gender.startsWith("L", ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(2.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.2f))
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (isMale) listOf(Color(0xFF4978FF), Color(0xFF72C7FF))
                                    else listOf(Color(0xFF8051E9), Color(0xFFC9A8FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = st.fullName.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = st.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MiTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(status = st.status)
                        }
                        Text(
                            text = "NIS: ${st.nis} • ${st.className} (${st.gender})",
                            fontSize = 13.sp,
                            color = MiTextSecondary
                        )
                        if (st.parentName.isNotBlank() || st.parentPhone.isNotBlank()) {
                            Text(
                                text = "Wali: ${st.parentName.ifBlank { "-" }} • HP: ${st.parentPhone.ifBlank { "-" }}",
                                fontSize = 12.sp,
                                color = MiTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MiBorderColor.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Record Indicators Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stNotes > 0) {
                        SmallRecordPill(label = "Catatan: $stNotes", tint = Color(0xFF4978FF))
                    }
                    if (stAch > 0) {
                        SmallRecordPill(label = "Prestasi: $stAch", tint = StatusIzinText)
                    }
                    if (stViol > 0) {
                        SmallRecordPill(label = "Pelanggaran: $stViol", tint = StatusAlpaText)
                    }
                    if (unpaidCount > 0) {
                        SmallRecordPill(label = "Tunggakan: $unpaidCount", tint = StatusTerlambatText)
                    }
                    if (stNotes == 0 && stAch == 0 && stViol == 0 && unpaidCount == 0) {
                        SmallRecordPill(label = "Tertib & Lunas", tint = Color(0xFF10B981))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Row (full width, comfortable touch targets, responsive)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val waMsg = buildStudentComprehensiveWaMessage(
                                student = st,
                                schoolName = schoolName,
                                attendance = attendance,
                                notes = notes,
                                achievements = achievements,
                                violations = violations,
                                arrears = arrears,
                                senderName = senderName
                            )
                            openWhatsAppChat(
                                context = context,
                                rawPhone = st.parentPhone,
                                message = waMsg,
                                onFeedback = onFeedback
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF25D366)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1E8E3E)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .testTag("student_wa_btn_${st.nis}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Chat,
                            contentDescription = "Kirim WA Orang Tua",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Kirim WA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = { onShowQr(st) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF4978FF).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFF4978FF).copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("student_qr_btn_${st.nis}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode2,
                                    contentDescription = "Kartu QR Siswa",
                                    tint = Color(0xFF4978FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = { onAddNote(st) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF8051E9).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFF8051E9).copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("student_note_btn_${st.nis}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.NoteAdd,
                                    contentDescription = "Tambah Catatan/Prestasi/Pelanggaran",
                                    tint = Color(0xFF8051E9),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = { onEditStudent(st) },
                            shape = RoundedCornerShape(10.dp),
                            color = MiBorderColor.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MiBorderColor),
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("student_edit_btn_${st.nis}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Siswa",
                                    tint = MiTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminArrearsTabContent(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val context = LocalContext.current
    val user = state.currentUser ?: return
    val filteredArrears = state.arrears.filter { ar ->
        val classMatch = state.selectedClassIdFilter == null || ar.classId == state.selectedClassIdFilter
        val q = state.searchQuery.trim().lowercase()
        val queryMatch = q.isEmpty() ||
            ar.studentName.lowercase().contains(q) ||
            ar.studentNis.lowercase().contains(q) ||
            ar.billType.lowercase().contains(q) ||
            ar.period.lowercase().contains(q)
        classMatch && queryMatch
    }
    val scopeLabel = state.classes.find { it.id == state.selectedClassIdFilter }?.className ?: "Seluruh Kelas MI"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            MiCard(contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = "Cetak & Bagikan Rekap Tunggakan (PDF Resmi)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Dokumen PDF rapi dengan Kop Surat Madrasah untuk wali murid",
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
                            ReportExportHelper.generateAndShareArrearsPdf(
                                context = context,
                                settings = state.settings,
                                reportTitle = "LAPORAN REKAP TUNGGAKAN & SPP SISWA",
                                classScopeLabel = scopeLabel,
                                arrears = filteredArrears,
                                onResult = { viewModel.showMessage(it) }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MiMintBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MiMintSoft,
                            contentColor = MiDarkGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("admin_arrears_pdf_btn")
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            val html = ReportExportHelper.buildArrearsPrintHtml(
                                settings = state.settings,
                                reportTitle = "LAPORAN REKAP TUNGGAKAN & SPP SISWA",
                                classScopeLabel = scopeLabel,
                                arrears = filteredArrears,
                                signerRoleLabel = RoleType.label(user.role),
                                signerName = user.fullName
                            )
                            viewModel.openModal(
                                ActiveModal.PrintDocumentPreviewModal(
                                    documentTitle = "Rekap Tunggakan ($scopeLabel)",
                                    htmlContent = html,
                                    isArrearsReport = true,
                                    periodOrScopeLabel = scopeLabel
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MiMintBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MiMintSoft,
                            contentColor = MiDarkGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("admin_arrears_preview_btn")
                    ) {
                        Icon(Icons.Outlined.Print, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            val totalSisaUnpaid = filteredArrears.sumOf { (it.nominal - it.paidAmount).coerceAtLeast(0L) }
            KopSuratHeaderCard(
                settings = state.settings,
                reportTitle = "Rekap Tunggakan & SPP ($scopeLabel)",
                subtitleRight = "Sisa: ${formatRupiah(totalSisaUnpaid)}"
            )
        }

        items(filteredArrears, key = { it.id }) { ar ->
            val sisa = (ar.nominal - ar.paidAmount).coerceAtLeast(0L)
            val matchedStudent = state.students.find { it.id == ar.studentId }
            MiCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${ar.studentName} (${ar.studentNis})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MiTextPrimary
                        )
                        Text(
                            text = "${ar.className} • ${ar.billType} (${ar.period})",
                            fontSize = 12.5.sp,
                            color = MiEmerald,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tagihan: ${formatRupiah(ar.nominal)} • Dibayar: ${formatRupiah(ar.paidAmount)} • Sisa: ${formatRupiah(sisa)}",
                            fontSize = 12.sp,
                            color = MiTextSecondary
                        )
                    }
                    StatusBadge(status = ar.status)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                viewModel.openModal(
                                    ActiveModal.WaArrearMessagePreview(
                                        arrear = ar,
                                        student = matchedStudent
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MiEmerald),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_wa_tagihan_btn_${ar.id}")
                        ) {
                            Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WA Tagihan", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (matchedStudent != null) {
                            OutlinedButton(
                                onClick = {
                                    val studentArrears = state.arrears.filter { it.studentId == matchedStudent.id }
                                    ReportExportHelper.generateAndShareStudentArrearsPdfForParent(
                                        context = context,
                                        settings = state.settings,
                                        student = matchedStudent,
                                        arrears = studentArrears,
                                        signerName = user.fullName,
                                        onResult = { viewModel.showMessage(it) }
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MiDarkGreen),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Ortu", fontSize = 11.5.sp, color = MiDarkGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Row {
                        TextButton(onClick = { viewModel.openModal(ActiveModal.ArrearForm(existing = ar)) }) {
                            Text("Edit / Pelunasan", fontSize = 12.sp, color = MiEmerald)
                        }
                        TextButton(onClick = { viewModel.deleteArrear(ar) }) {
                            Text("Hapus", fontSize = 12.sp, color = StatusAlpaText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallRecordPill(label: String, tint: Color) {
    Surface(
        color = tint.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = tint,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun TeachersListContent(
    teachers: List<TeacherEntity>,
    onEdit: (TeacherEntity) -> Unit,
    onRecordAttendance: (TeacherEntity) -> Unit,
    onDelete: (TeacherEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(teachers, key = { it.id }) { t ->
            MiCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MiTextPrimary
                        )
                        Text(
                            text = "NIP: ${t.nip} • ${t.position}",
                            fontSize = 13.sp,
                            color = MiEmerald,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tugas: ${t.subjectOrClass} • HP: ${t.phone}",
                            fontSize = 12.sp,
                            color = MiTextSecondary
                        )
                    }
                    StatusBadge(status = t.status)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onRecordAttendance(t) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Outlined.HowToReg, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Catat Absen Guru", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onEdit(t) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Guru", tint = MiEmerald)
                    }
                    IconButton(onClick = { onDelete(t) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus Guru", tint = StatusAlpaText)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassesListContent(
    classes: List<SchoolClassEntity>,
    students: List<StudentEntity>,
    onEdit: (SchoolClassEntity) -> Unit,
    onDelete: (SchoolClassEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(classes, key = { it.id }) { c ->
            val count = students.count { it.classId == c.id }
            MiCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${c.className} (Tingkat ${c.gradeLevel})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MiTextPrimary
                        )
                        Text(
                            text = "Wali Kelas: ${c.homeroomTeacherName.ifBlank { "-" }} • Ruang: ${c.roomNumber.ifBlank { "-" }}",
                            fontSize = 13.sp,
                            color = MiTextSecondary
                        )
                        if (c.classNotes.isNotBlank()) {
                            Text(
                                text = "Catatan: ${c.classNotes}",
                                fontSize = 12.sp,
                                color = MiTextMuted
                            )
                        }
                    }
                    Surface(
                        color = MiMintSoft,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "$count Siswa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiDarkGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { onEdit(c) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Kelas", tint = MiEmerald)
                    }
                    IconButton(onClick = { onDelete(c) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus Kelas", tint = StatusAlpaText)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAccountsListContent(
    users: List<UserEntity>,
    onEdit: (UserEntity) -> Unit,
    onResetPassword: (UserEntity) -> Unit,
    onDelete: (UserEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(users, key = { it.id }) { u ->
            MiCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = u.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MiTextPrimary
                        )
                        Text(
                            text = "@${u.username} • ${RoleType.label(u.role)}",
                            fontSize = 13.sp,
                            color = MiEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Akses: ${u.assignedClassName.ifBlank { "Seluruh Sekolah" }} • NIP: ${u.nip.ifBlank { "-" }}",
                            fontSize = 12.sp,
                            color = MiTextSecondary
                        )
                    }
                    StatusBadge(status = if (u.isActive) "Aktif" else "Nonaktif")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onResetPassword(u) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Password", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = { onEdit(u) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Akun", tint = MiEmerald)
                    }
                    IconButton(onClick = { onDelete(u) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus Akun", tint = StatusAlpaText)
                    }
                }
            }
        }
    }
}
