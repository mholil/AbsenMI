package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.util.ReportExportHelper

@Composable
fun AllModalsHost(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val activeModal = state.activeModal
    if (activeModal is ActiveModal.None) return

    when (activeModal) {
        is ActiveModal.LogoutConfirmation -> {
            LogoutConfirmationModal(
                onDismiss = { viewModel.closeModal() },
                onConfirmLogout = { viewModel.confirmLogout() }
            )
        }
        is ActiveModal.StudentForm -> {
            StudentFormModal(
                existing = activeModal.existing,
                preselectedClassId = activeModal.preselectedClassId,
                classes = state.classes,
                currentUser = state.currentUser,
                onDismiss = { viewModel.closeModal() },
                onSave = { viewModel.saveStudent(it) },
                onDelete = { viewModel.deleteStudent(it) }
            )
        }
        is ActiveModal.StudentDetail -> {
            val student = state.students.find { it.id == activeModal.studentId }
            if (student != null) {
                StudentDetailModal(
                    student = student,
                    initialTab = activeModal.initialTab,
                    state = state,
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeModal() }
                )
            } else {
                viewModel.closeModal()
            }
        }
        is ActiveModal.NoteRecordForm -> {
            NoteRecordFormModal(
                modalData = activeModal,
                defaultDate = state.selectedDateFilter,
                onDismiss = {
                    viewModel.openModal(ActiveModal.StudentDetail(activeModal.student.id))
                },
                onCloseCompletely = { viewModel.closeModal() },
                onSave = { cat, existingId, recType, content, date, forwardBk ->
                    viewModel.saveStudentRecord(
                        student = activeModal.student,
                        category = cat,
                        existingId = existingId,
                        type = recType,
                        content = content,
                        date = date,
                        forwardToBk = forwardBk
                    )
                }
            )
        }
        is ActiveModal.StudentQrCard -> {
            StudentQrCardModal(
                student = activeModal.student,
                settings = state.settings,
                onDismiss = { viewModel.closeModal() },
                onMessage = { viewModel.showMessage(it) }
            )
        }
        is ActiveModal.AttendanceRecordForm -> {
            AttendanceRecordFormModal(
                modalData = activeModal,
                defaultTime = viewModel.repository.currentTimeString(),
                onDismiss = { viewModel.closeModal() },
                onSave = { status, time, note ->
                    viewModel.recordAttendance(
                        student = activeModal.student,
                        date = activeModal.date,
                        time = time,
                        status = status,
                        note = note,
                        allowCorrection = activeModal.isCorrection || activeModal.existing != null
                    )
                }
            )
        }
        is ActiveModal.AttendanceDetailModal -> {
            AttendanceDetailModalView(
                att = activeModal.attendance,
                canCorrect = state.currentUser?.canCorrectAttendance == true || state.currentUser?.role == RoleType.ADMIN,
                onDismiss = { viewModel.closeModal() },
                onOpenCorrection = {
                    val student = state.students.find { it.id == activeModal.attendance.studentId }
                    if (student != null) {
                        viewModel.openModal(
                            ActiveModal.AttendanceRecordForm(
                                student = student,
                                date = activeModal.attendance.date,
                                existing = activeModal.attendance,
                                isCorrection = true
                            )
                        )
                    } else {
                        viewModel.closeModal()
                    }
                }
            )
        }
        is ActiveModal.TeacherForm -> {
            TeacherFormModal(
                existing = activeModal.existing,
                onDismiss = { viewModel.closeModal() },
                onSave = { viewModel.saveTeacher(it) },
                onDelete = { viewModel.deleteTeacher(it) }
            )
        }
        is ActiveModal.TeacherAttendanceForm -> {
            TeacherAttendanceFormModal(
                teacher = activeModal.teacher,
                defaultDate = state.selectedDateFilter,
                defaultTime = viewModel.repository.currentTimeString(),
                onDismiss = { viewModel.closeModal() },
                onSave = { viewModel.saveTeacherAttendance(it) }
            )
        }
        is ActiveModal.ClassForm -> {
            ClassFormModal(
                existing = activeModal.existing,
                onDismiss = { viewModel.closeModal() },
                onSave = { viewModel.saveClass(it) },
                onDelete = { viewModel.deleteClass(it) }
            )
        }
        is ActiveModal.UserAccountForm -> {
            UserAccountFormModal(
                existing = activeModal.existing,
                classes = state.classes,
                onDismiss = { viewModel.closeModal() },
                onSave = { account, newPass -> viewModel.saveUserAccount(account, newPass) },
                onDelete = { viewModel.deleteUserAccount(it) }
            )
        }
        is ActiveModal.ResetPasswordModal -> {
            ResetPasswordModalDialog(
                targetUser = activeModal.targetUser,
                onDismiss = { viewModel.closeModal() },
                onSavePassword = { newPass ->
                    viewModel.saveUserAccount(activeModal.targetUser, newPass)
                }
            )
        }
        is ActiveModal.ArrearsManagementModal -> {
            ArrearsManagementModalView(
                state = state,
                viewModel = viewModel
            )
        }
        is ActiveModal.WaArrearTemplateModal -> {
            WaArrearTemplateModalView(
                state = state,
                viewModel = viewModel
            )
        }
        is ActiveModal.WaArrearMessagePreview -> {
            WaArrearMessagePreviewModal(
                arrear = activeModal.arrear,
                student = activeModal.student,
                state = state,
                viewModel = viewModel
            )
        }
        is ActiveModal.PrintDocumentPreviewModal -> {
            PrintDocumentPreviewModalView(
                modalData = activeModal,
                state = state,
                viewModel = viewModel
            )
        }
        is ActiveModal.ArrearForm -> {
            ArrearFormModalDialog(
                existing = activeModal.existing,
                preselectedStudent = activeModal.preselectedStudent,
                students = state.students,
                onDismiss = { viewModel.openModal(ActiveModal.ArrearsManagementModal) },
                onCloseAll = { viewModel.closeModal() },
                onSave = { viewModel.saveArrear(it) }
            )
        }
        is ActiveModal.HomeroomAdminModal -> {
            HomeroomAdminModalView(
                state = state,
                viewModel = viewModel
            )
        }
        is ActiveModal.AdminSettings -> {
            AdminSettingsModalView(
                state = state,
                viewModel = viewModel
            )
        }
        is ActiveModal.BackupRestoreModal -> {
            BackupRestoreModalView(
                state = state,
                viewModel = viewModel
            )
        }
        ActiveModal.None -> {}
    }
}

@Composable
private fun LogoutConfirmationModal(
    onDismiss: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    MiModalShell(
        title = "Konfirmasi Keluar",
        subtitle = "Sesi akun Anda akan diakhiri",
        onDismiss = onDismiss,
        onSave = onConfirmLogout,
        saveText = "Ya, Keluar",
        cancelText = "Batal"
    ) {
        Text(
            text = "Apakah Anda yakin ingin keluar dari aplikasi Absensi MI? Seluruh data absensi dan administrasi telah tersimpan aman di perangkat.",
            style = MaterialTheme.typography.bodyMedium,
            color = MiTextPrimary
        )
    }
}

@Composable
private fun StudentFormModal(
    existing: StudentEntity?,
    preselectedClassId: Long?,
    classes: List<SchoolClassEntity>,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onSave: (StudentEntity) -> Unit,
    onDelete: (StudentEntity) -> Unit
) {
    val isWali = currentUser?.role == RoleType.WALI_KELAS
    val lockedClassId = if (isWali) currentUser?.assignedClassId else preselectedClassId
    val initialClass = classes.find { it.id == (existing?.classId ?: lockedClassId) } ?: classes.firstOrNull()

    var nis by remember { mutableStateOf(existing?.nis ?: "") }
    var fullName by remember { mutableStateOf(existing?.fullName ?: "") }
    var gender by remember { mutableStateOf(existing?.gender ?: "L") }
    var selectedClass by remember { mutableStateOf(initialClass) }
    var parentName by remember { mutableStateOf(existing?.parentName ?: "") }
    var parentPhone by remember { mutableStateOf(existing?.parentPhone ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var birthPlaceDate by remember { mutableStateOf(existing?.birthPlaceDate ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "Aktif") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    MiModalShell(
        title = if (existing == null) "Tambah Data Siswa" else "Edit Data Siswa",
        subtitle = selectedClass?.className ?: "Data Siswa MI",
        onDismiss = onDismiss,
        onSave = {
            if (nis.isBlank() || fullName.isBlank()) {
                errorMsg = "NIS dan Nama Lengkap Siswa wajib diisi."
                return@MiModalShell
            }
            val cls = selectedClass ?: return@MiModalShell
            onSave(
                StudentEntity(
                    id = existing?.id ?: 0L,
                    nis = nis.trim(),
                    fullName = fullName.trim(),
                    gender = gender,
                    classId = cls.id,
                    className = cls.className,
                    parentName = parentName.trim(),
                    parentPhone = parentPhone.trim(),
                    address = address.trim(),
                    birthPlaceDate = birthPlaceDate.trim(),
                    status = status,
                    qrCodeValue = "MI-${nis.trim()}"
                )
            )
        },
        saveText = "Simpan Siswa"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (errorMsg != null) {
                Text(text = errorMsg!!, color = StatusAlpaText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nis,
                    onValueChange = { nis = it; errorMsg = null },
                    label = { Text("NIS") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.9f).testTag("student_nis_input")
                )
                OutlinedTextField(
                    value = birthPlaceDate,
                    onValueChange = { birthPlaceDate = it },
                    label = { Text("Tempat, Tgl Lahir") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.1f)
                )
            }
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; errorMsg = null },
                label = { Text("Nama Lengkap Siswa") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("student_name_input")
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Jenis Kelamin:", fontSize = 13.sp, color = MiTextSecondary)
                FilterChip(
                    selected = gender == "L",
                    onClick = { gender = "L" },
                    label = { Text("Laki-laki (L)") }
                )
                FilterChip(
                    selected = gender == "P",
                    onClick = { gender = "P" },
                    label = { Text("Perempuan (P)") }
                )
            }

            if (!isWali) {
                Text("Pilih Kelas:", fontSize = 13.sp, color = MiTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    classes.take(4).forEach { c ->
                        FilterChip(
                            selected = selectedClass?.id == c.id,
                            onClick = { selectedClass = c },
                            label = { Text(c.className, fontSize = 12.sp) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = parentName,
                onValueChange = { parentName = it },
                label = { Text("Nama Orang Tua / Wali") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = parentPhone,
                onValueChange = { parentPhone = it },
                label = { Text("Nomor HP / WhatsApp Orang Tua") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Alamat Lengkap") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Status:", fontSize = 13.sp, color = MiTextSecondary)
                listOf("Aktif", "Mutasi", "Arsip").forEach { stOpt ->
                    FilterChip(
                        selected = status == stOpt,
                        onClick = { status = stOpt },
                        label = { Text(stOpt, fontSize = 12.sp) }
                    )
                }
            }

            if (existing != null && currentUser?.role == RoleType.ADMIN) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { onDelete(existing) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlpaText),
                    border = BorderStroke(1.dp, StatusAlpaText),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus Siswa Ini")
                }
            }
        }
    }
}

@Composable
private fun StudentDetailModal(
    student: StudentEntity,
    initialTab: Int,
    state: MadrasahUiState,
    viewModel: MadrasahViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUser = state.currentUser
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val stNotes = state.notes.filter { it.studentId == student.id }
    val stAchievements = state.achievements.filter { it.studentId == student.id }
    val stViolations = state.violations.filter { it.studentId == student.id }
    val stArrears = state.arrears.filter { it.studentId == student.id }
    val stAttendance = state.attendance.filter { it.studentId == student.id }

    MiModalShell(
        title = student.fullName,
        subtitle = "NIS: ${student.nis} • ${student.className} • Status: ${student.status}",
        onDismiss = onDismiss,
        onSave = {
            viewModel.openModal(ActiveModal.StudentForm(existing = student))
        },
        saveText = "Edit Profil",
        cancelText = "Tutup"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Quick Action Bar: Kirim Pesan WhatsApp (Rekap Kehadiran, Catatan & Tunggakan) + Cetak PDF Tunggakan Ortu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val waMsg = buildStudentComprehensiveWaMessage(
                            student = student,
                            schoolName = state.settings.schoolName,
                            attendance = state.attendance,
                            notes = state.notes,
                            achievements = state.achievements,
                            violations = state.violations,
                            arrears = state.arrears,
                            senderName = currentUser?.fullName ?: "Wali Kelas / Administrasi"
                        )
                        openWhatsAppChat(
                            context = context,
                            rawPhone = student.parentPhone,
                            message = waMsg,
                            onFeedback = { viewModel.showMessage(it) }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("student_detail_wa_btn")
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kirim Pesan WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        ReportExportHelper.generateAndShareStudentArrearsPdfForParent(
                            context = context,
                            settings = state.settings,
                            student = student,
                            arrears = stArrears,
                            signerName = currentUser?.fullName ?: "",
                            onResult = { viewModel.showMessage(it) }
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MiDarkGreen),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("student_detail_arrears_pdf_btn")
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF Rekap Tunggakan", fontSize = 12.sp, color = MiDarkGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MiSurfaceVariant,
                contentColor = MiDarkGreen,
                edgePadding = 4.dp
            ) {
                listOf(
                    "1. Catatan (${stNotes.size})",
                    "2. Prestasi (${stAchievements.size})",
                    "3. Pelanggaran (${stViolations.size})",
                    "4. Tunggakan & Wali (${stArrears.size})"
                ).forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    Button(
                        onClick = {
                            viewModel.openModal(
                                ActiveModal.NoteRecordForm(
                                    student = student,
                                    category = "Catatan"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MiEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_note_btn")
                    ) {
                        Icon(Icons.Outlined.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Catatan Siswa")
                    }
                    if (stNotes.isEmpty()) {
                        Text("Belum ada catatan perkembangan untuk siswa ini.", fontSize = 13.sp, color = MiTextSecondary)
                    } else {
                        stNotes.forEach { n ->
                            RecordItemCard(
                                badge = n.type,
                                date = n.date,
                                content = n.content,
                                forwardToBk = n.forwardToBk,
                                author = n.authorName,
                                onSendWa = {
                                    val msg = buildSingleRecordWaMessage(
                                        student = student,
                                        schoolName = state.settings.schoolName,
                                        categoryLabel = "Catatan Perkembangan",
                                        badgeType = n.type,
                                        date = n.date,
                                        content = n.content,
                                        author = n.authorName
                                    )
                                    openWhatsAppChat(context, student.parentPhone, msg) { viewModel.showMessage(it) }
                                },
                                onEdit = {
                                    viewModel.openModal(
                                        ActiveModal.NoteRecordForm(
                                            student = student,
                                            category = "Catatan",
                                            existingId = n.id,
                                            initialType = n.type,
                                            initialContent = n.content,
                                            initialDate = n.date,
                                            initialForwardToBk = n.forwardToBk
                                        )
                                    )
                                },
                                onDelete = { viewModel.deleteNote(n) }
                            )
                        }
                    }
                }
                1 -> {
                    Button(
                        onClick = {
                            viewModel.openModal(
                                ActiveModal.NoteRecordForm(
                                    student = student,
                                    category = "Prestasi"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MiDarkGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_achievement_btn")
                    ) {
                        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Prestasi Siswa")
                    }
                    if (stAchievements.isEmpty()) {
                        Text("Belum ada data prestasi tercatat.", fontSize = 13.sp, color = MiTextSecondary)
                    } else {
                        stAchievements.forEach { ac ->
                            RecordItemCard(
                                badge = ac.type,
                                date = ac.date,
                                content = ac.content,
                                forwardToBk = ac.forwardToBk,
                                author = ac.authorName,
                                onSendWa = {
                                    val msg = buildSingleRecordWaMessage(
                                        student = student,
                                        schoolName = state.settings.schoolName,
                                        categoryLabel = "Prestasi",
                                        badgeType = ac.type,
                                        date = ac.date,
                                        content = ac.content,
                                        author = ac.authorName
                                    )
                                    openWhatsAppChat(context, student.parentPhone, msg) { viewModel.showMessage(it) }
                                },
                                onEdit = {
                                    viewModel.openModal(
                                        ActiveModal.NoteRecordForm(
                                            student = student,
                                            category = "Prestasi",
                                            existingId = ac.id,
                                            initialType = ac.type,
                                            initialContent = ac.content,
                                            initialDate = ac.date,
                                            initialForwardToBk = ac.forwardToBk
                                        )
                                    )
                                },
                                onDelete = { viewModel.deleteAchievement(ac) }
                            )
                        }
                    }
                }
                2 -> {
                    Button(
                        onClick = {
                            viewModel.openModal(
                                ActiveModal.NoteRecordForm(
                                    student = student,
                                    category = "Pelanggaran"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusAlpaText),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_violation_btn")
                    ) {
                        Icon(Icons.Outlined.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Catat Pelanggaran / Pembinaan")
                    }
                    if (stViolations.isEmpty()) {
                        Text("Alhamdulillah, tidak ada catatan pelanggaran.", fontSize = 13.sp, color = MiTextSecondary)
                    } else {
                        stViolations.forEach { v ->
                            RecordItemCard(
                                badge = v.type,
                                date = v.date,
                                content = v.content,
                                forwardToBk = v.forwardToBk,
                                author = v.authorName,
                                onSendWa = {
                                    val msg = buildSingleRecordWaMessage(
                                        student = student,
                                        schoolName = state.settings.schoolName,
                                        categoryLabel = "Pembinaan & Kedisiplinan",
                                        badgeType = v.type,
                                        date = v.date,
                                        content = v.content,
                                        author = v.authorName
                                    )
                                    openWhatsAppChat(context, student.parentPhone, msg) { viewModel.showMessage(it) }
                                },
                                onEdit = {
                                    viewModel.openModal(
                                        ActiveModal.NoteRecordForm(
                                            student = student,
                                            category = "Pelanggaran",
                                            existingId = v.id,
                                            initialType = v.type,
                                            initialContent = v.content,
                                            initialDate = v.date,
                                            initialForwardToBk = v.forwardToBk
                                        )
                                    )
                                },
                                onDelete = { viewModel.deleteViolation(v) }
                            )
                        }
                    }
                }
                else -> {
                    Surface(
                        color = MiMintSoft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Informasi Orang Tua / Wali & Alamat", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MiDarkGreen)
                            Text("Orang Tua / Wali: ${student.parentName.ifBlank { "-" }}", fontSize = 13.sp)
                            Text("Nomor HP / WA: ${student.parentPhone.ifBlank { "-" }}", fontSize = 13.sp)
                            Text("Tempat, Tanggal Lahir: ${student.birthPlaceDate.ifBlank { "-" }}", fontSize = 13.sp)
                            Text("Alamat: ${student.address.ifBlank { "-" }}", fontSize = 13.sp)
                            Text("Total Kehadiran Tercatat: ${stAttendance.size} Hari", fontSize = 13.sp, color = MiEmerald, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tunggakan & SPP Siswa", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MiDarkGreen)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    ReportExportHelper.generateAndShareStudentArrearsPdfForParent(
                                        context = context,
                                        settings = state.settings,
                                        student = student,
                                        arrears = stArrears,
                                        signerName = currentUser?.fullName ?: "",
                                        onResult = { viewModel.showMessage(it) }
                                    )
                                }
                            ) {
                                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = MiDarkGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cetak PDF", fontSize = 12.sp, color = MiDarkGreen)
                            }
                            TextButton(onClick = { viewModel.openModal(ActiveModal.WaArrearTemplateModal) }) {
                                Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = MiEmerald)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Template WA", fontSize = 12.sp, color = MiEmerald)
                            }
                        }
                    }
                    if (stArrears.isEmpty()) {
                        Text("Tidak ada tagihan atau tunggakan.", fontSize = 13.sp, color = MiTextSecondary)
                    } else {
                        stArrears.forEach { ar ->
                            val sisa = (ar.nominal - ar.paidAmount).coerceAtLeast(0L)
                            Surface(
                                color = MiSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MiBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${ar.billType} (${ar.period})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Tagihan: ${formatRupiah(ar.nominal)} • Dibayar: ${formatRupiah(ar.paidAmount)} • Sisa: ${formatRupiah(sisa)}",
                                                fontSize = 12.sp,
                                                color = MiTextSecondary
                                            )
                                        }
                                        StatusBadge(status = ar.status)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.openModal(
                                                    ActiveModal.WaArrearMessagePreview(
                                                        arrear = ar,
                                                        student = student
                                                    )
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MiEmerald),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("ingatkan_wa_btn_${ar.id}")
                                        ) {
                                            Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ingatkan WA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordItemCard(
    badge: String,
    date: String,
    content: String,
    forwardToBk: Boolean,
    author: String,
    onSendWa: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MiSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MiBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = badge)
                Text(text = date, fontSize = 12.sp, color = MiTextMuted)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = content, fontSize = 14.sp, color = MiTextPrimary)
            if (forwardToBk) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Diteruskan ke Guru BK",
                    fontSize = 12.sp,
                    color = StatusTerlambatText,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Oleh: $author", fontSize = 11.sp, color = MiTextMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onSendWa,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Outlined.Chat, contentDescription = "Kirim WA", tint = MiEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kirim WA", fontSize = 11.5.sp, color = MiEmerald, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MiEmerald, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus", tint = StatusAlpaText, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRecordFormModal(
    modalData: ActiveModal.NoteRecordForm,
    defaultDate: String,
    onDismiss: () -> Unit,
    onCloseCompletely: () -> Unit,
    onSave: (String, Long, String, String, String, Boolean) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(modalData.category) }
    var recordType by remember {
        mutableStateOf(
            modalData.initialType.ifBlank {
                when (modalData.category) {
                    "Prestasi" -> "Akademik / Tahfidz"
                    "Pelanggaran" -> "Kedisiplinan"
                    else -> "Catatan Wali Kelas"
                }
            }
        )
    }
    var content by remember { mutableStateOf(modalData.initialContent) }
    var date by remember { mutableStateOf(modalData.initialDate.ifBlank { defaultDate }) }
    var forwardToBk by remember { mutableStateOf(modalData.initialForwardToBk) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    MiModalShell(
        title = if (modalData.existingId == 0L) "Tambah $selectedCategory Siswa" else "Edit $selectedCategory Siswa",
        subtitle = "${modalData.student.fullName} (${modalData.student.className})",
        onDismiss = onCloseCompletely,
        onSave = {
            if (content.isBlank()) {
                errorMsg = "Isi catatan tidak boleh kosong."
                return@MiModalShell
            }
            onSave(selectedCategory, modalData.existingId, recordType.trim(), content.trim(), date.trim(), forwardToBk)
        },
        saveText = "Simpan",
        cancelText = "Batal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (errorMsg != null) {
                Text(text = errorMsg!!, color = StatusAlpaText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Text("Kategori Catatan:", fontSize = 13.sp, color = MiTextSecondary, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Catatan", "Prestasi", "Pelanggaran").forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            if (modalData.existingId == 0L) {
                                recordType = when (cat) {
                                    "Prestasi" -> "Akademik / Tahfidz"
                                    "Pelanggaran" -> "Kedisiplinan"
                                    else -> "Catatan Perkembangan"
                                }
                            }
                        },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = recordType,
                onValueChange = { recordType = it },
                label = { Text("Jenis $selectedCategory") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("note_type_input")
            )

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Tanggal (YYYY-MM-DD)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("note_date_input")
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it; errorMsg = null },
                label = { Text("Isi Catatan / Uraian Lengkap") },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("note_content_input")
            )

            Surface(
                color = MiSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { forwardToBk = !forwardToBk }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = forwardToBk,
                        onCheckedChange = { forwardToBk = it },
                        modifier = Modifier.testTag("note_forward_bk_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Teruskan ke Guru BK (Bimbingan Konseling)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiTextPrimary
                        )
                        Text(
                            text = "Tandai agar Guru BK menerima tindak lanjut pembinaan siswa",
                            fontSize = 11.sp,
                            color = MiTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentQrCardModal(
    student: StudentEntity,
    settings: SchoolSettingsEntity,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val qrValue = student.qrCodeValue.ifBlank { "MI-${student.nis}" }

    MiModalShell(
        title = "Kartu QR Absensi Siswa",
        subtitle = settings.schoolName,
        onDismiss = onDismiss,
        onSave = {
            ReportExportHelper.printStudentQrCardPdf(
                context = context,
                settings = settings,
                student = student,
                onResult = onMessage
            )
        },
        saveText = "Unduh / Cetak PDF",
        cancelText = "Tutup"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MiMintSoft,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, MiEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = settings.schoolName.uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiDarkGreen
                    )
                    Text(
                        text = "KARTU IDENTITAS & ABSENSI QR SISWA",
                        fontSize = 11.sp,
                        color = MiEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    QrCodeCanvasView(qrContent = qrValue, sizeDp = 165.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = student.fullName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiTextPrimary
                    )
                    Text(
                        text = "NIS: ${student.nis} • ${student.className}",
                        fontSize = 13.sp,
                        color = MiTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MiDarkGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "KODE QR: $qrValue",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceRecordFormModal(
    modalData: ActiveModal.AttendanceRecordForm,
    defaultTime: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var status by remember { mutableStateOf(modalData.existing?.status ?: AttendanceStatus.HADIR) }
    var time by remember { mutableStateOf(modalData.existing?.time ?: defaultTime) }
    var note by remember { mutableStateOf(modalData.existing?.note ?: "") }

    MiModalShell(
        title = if (modalData.isCorrection) "Koreksi Absensi Siswa" else "Catat Absensi Siswa",
        subtitle = "${modalData.student.fullName} (${modalData.student.className}) • ${modalData.date}",
        onDismiss = onDismiss,
        onSave = { onSave(status, time, note) },
        saveText = "Simpan Absensi"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pilih Status Kehadiran:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MiTextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceStatus.ALL_RECORDED.forEach { opt ->
                    FilterChip(
                        selected = status == opt,
                        onClick = { status = opt },
                        label = { Text(opt, fontSize = 12.sp) }
                    )
                }
            }
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Waktu Absen (HH:mm)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Keterangan / Alasan Koreksi") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AttendanceDetailModalView(
    att: AttendanceEntity,
    canCorrect: Boolean,
    onDismiss: () -> Unit,
    onOpenCorrection: () -> Unit
) {
    MiModalShell(
        title = "Detail Kehadiran Siswa",
        subtitle = "${att.date} • Pukul ${att.time}",
        onDismiss = onDismiss,
        onSave = if (canCorrect) onOpenCorrection else null,
        saveText = "Koreksi Status",
        cancelText = "Tutup"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(att.studentName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MiTextPrimary)
                StatusBadge(status = att.status)
            }
            Text("NIS: ${att.studentNis} • Kelas: ${att.className}", fontSize = 13.sp, color = MiTextSecondary)
            HorizontalDivider(color = MiBorderColor)
            Text("Waktu Pencatatan: ${att.time}", fontSize = 13.sp)
            Text("Keterangan: ${att.note.ifBlank { "-" }}", fontSize = 13.sp)
            Text("Dicatat Oleh: ${att.operatorName} (${RoleType.label(att.operatorRole)})", fontSize = 13.sp)
            if (att.isCorrected) {
                Text(
                    text = "Status: Data ini telah melalui Koreksi Absensi resmi.",
                    fontSize = 12.sp,
                    color = StatusTerlambatText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TeacherFormModal(
    existing: TeacherEntity?,
    onDismiss: () -> Unit,
    onSave: (TeacherEntity) -> Unit,
    onDelete: (TeacherEntity) -> Unit
) {
    var nip by remember { mutableStateOf(existing?.nip ?: "") }
    var fullName by remember { mutableStateOf(existing?.fullName ?: "") }
    var position by remember { mutableStateOf(existing?.position ?: "Wali Kelas") }
    var subjectOrClass by remember { mutableStateOf(existing?.subjectOrClass ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }

    MiModalShell(
        title = if (existing == null) "Tambah Guru / Staf" else "Edit Guru / Staf",
        onDismiss = onDismiss,
        onSave = {
            if (fullName.isNotBlank()) {
                onSave(
                    TeacherEntity(
                        id = existing?.id ?: 0L,
                        nip = nip.trim(),
                        fullName = fullName.trim(),
                        position = position.trim(),
                        subjectOrClass = subjectOrClass.trim(),
                        phone = phone.trim(),
                        address = address.trim()
                    )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(value = nip, onValueChange = { nip = it }, label = { Text("NIP / NUPTK") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Nama Lengkap & Gelar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = position, onValueChange = { position = it }, label = { Text("Jabatan (Wali Kelas / Guru Mapel / BK / Staf)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = subjectOrClass, onValueChange = { subjectOrClass = it }, label = { Text("Kelas Ampuan / Mata Pelajaran") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Nomor HP / WA") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
            if (existing != null) {
                OutlinedButton(
                    onClick = { onDelete(existing) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlpaText),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hapus Data Guru/Staf")
                }
            }
        }
    }
}

@Composable
private fun TeacherAttendanceFormModal(
    teacher: TeacherEntity,
    defaultDate: String,
    defaultTime: String,
    onDismiss: () -> Unit,
    onSave: (TeacherAttendanceEntity) -> Unit
) {
    var status by remember { mutableStateOf(AttendanceStatus.HADIR) }
    var timeIn by remember { mutableStateOf(defaultTime) }
    var timeOut by remember { mutableStateOf("13:45") }
    var note by remember { mutableStateOf("Hadir mengajar sesuai jadwal") }

    MiModalShell(
        title = "Absensi Guru / Staf",
        subtitle = "${teacher.fullName} (${teacher.position})",
        onDismiss = onDismiss,
        onSave = {
            onSave(
                TeacherAttendanceEntity(
                    teacherId = teacher.id,
                    teacherName = teacher.fullName,
                    nip = teacher.nip,
                    position = teacher.position,
                    date = defaultDate,
                    timeIn = timeIn,
                    timeOut = timeOut,
                    status = status,
                    note = note
                )
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Hadir", "Terlambat", "Izin", "Sakit", "Dinas").forEach { opt ->
                    FilterChip(selected = status == opt, onClick = { status = opt }, label = { Text(opt, fontSize = 12.sp) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = timeIn, onValueChange = { timeIn = it }, label = { Text("Jam Masuk") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = timeOut, onValueChange = { timeOut = it }, label = { Text("Jam Pulang") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Keterangan") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ClassFormModal(
    existing: SchoolClassEntity?,
    onDismiss: () -> Unit,
    onSave: (SchoolClassEntity) -> Unit,
    onDelete: (SchoolClassEntity) -> Unit
) {
    var className by remember { mutableStateOf(existing?.className ?: "") }
    var gradeLevel by remember { mutableStateOf((existing?.gradeLevel ?: 1).toString()) }
    var homeroomTeacher by remember { mutableStateOf(existing?.homeroomTeacherName ?: "") }
    var roomNumber by remember { mutableStateOf(existing?.roomNumber ?: "") }
    var classNotes by remember { mutableStateOf(existing?.classNotes ?: "") }
    var announcement by remember { mutableStateOf(existing?.classAnnouncement ?: "") }

    MiModalShell(
        title = if (existing == null) "Tambah Kelas Baru" else "Edit Kelas & Wali Kelas",
        onDismiss = onDismiss,
        onSave = {
            if (className.isNotBlank()) {
                onSave(
                    SchoolClassEntity(
                        id = existing?.id ?: 0L,
                        className = className.trim(),
                        gradeLevel = gradeLevel.toIntOrNull() ?: 1,
                        homeroomTeacherName = homeroomTeacher.trim(),
                        roomNumber = roomNumber.trim(),
                        classNotes = classNotes.trim(),
                        classAnnouncement = announcement.trim()
                    )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Nama Kelas (Mis: Kelas 1A)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = gradeLevel, onValueChange = { gradeLevel = it }, label = { Text("Tingkat (1-6)") }, modifier = Modifier.width(110.dp))
            }
            OutlinedTextField(value = homeroomTeacher, onValueChange = { homeroomTeacher = it }, label = { Text("Nama Wali Kelas") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = roomNumber, onValueChange = { roomNumber = it }, label = { Text("Ruang Kelas") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = classNotes, onValueChange = { classNotes = it }, label = { Text("Catatan Administrasi Kelas") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = announcement, onValueChange = { announcement = it }, label = { Text("Pengumuman Kelas") }, modifier = Modifier.fillMaxWidth())
            if (existing != null) {
                OutlinedButton(
                    onClick = { onDelete(existing) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlpaText),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hapus Kelas")
                }
            }
        }
    }
}

@Composable
private fun UserAccountFormModal(
    existing: UserEntity?,
    classes: List<SchoolClassEntity>,
    onDismiss: () -> Unit,
    onSave: (UserEntity, String?) -> Unit,
    onDelete: (UserEntity) -> Unit
) {
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var fullName by remember { mutableStateOf(existing?.fullName ?: "") }
    var nip by remember { mutableStateOf(existing?.nip ?: "") }
    var role by remember { mutableStateOf(existing?.role ?: RoleType.WALI_KELAS) }
    var assignedClass by remember {
        mutableStateOf(classes.find { it.id == existing?.assignedClassId } ?: classes.firstOrNull())
    }
    var newPassword by remember { mutableStateOf("") }
    var canAddStudent by remember { mutableStateOf(existing?.canAddStudent ?: true) }
    var canCorrectAttendance by remember { mutableStateOf(existing?.canCorrectAttendance ?: true) }

    MiModalShell(
        title = if (existing == null) "Tambah Akun Pengguna" else "Edit Akun & Hak Akses",
        subtitle = "Manajemen Role & Hak Akses Madrasah",
        onDismiss = onDismiss,
        onSave = {
            if (username.isNotBlank() && fullName.isNotBlank()) {
                val isWali = role == RoleType.WALI_KELAS
                onSave(
                    UserEntity(
                        id = existing?.id ?: 0L,
                        username = username.trim(),
                        fullName = fullName.trim(),
                        nip = nip.trim(),
                        role = role,
                        assignedClassId = if (isWali) assignedClass?.id else null,
                        assignedClassName = if (isWali) (assignedClass?.className ?: "") else "Seluruh Madrasah",
                        passwordHash = existing?.passwordHash ?: "",
                        canAddStudent = canAddStudent,
                        canCorrectAttendance = canCorrectAttendance,
                        isActive = true
                    ),
                    newPassword.takeIf { it.isNotBlank() } ?: if (existing == null) "123456" else null
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username Login") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Nama Lengkap Pengguna") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = nip, onValueChange = { nip = it }, label = { Text("NIP") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Pilih Role Pengguna:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(RoleType.ADMIN, RoleType.WALI_KELAS, RoleType.KEPALA_SEKOLAH, RoleType.GURU_PIKET).forEach { r ->
                    FilterChip(
                        selected = role == r,
                        onClick = { role = r },
                        label = { Text(RoleType.label(r), fontSize = 11.sp) }
                    )
                }
            }

            if (role == RoleType.WALI_KELAS) {
                Text("Kelas Ampuan Wali Kelas:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    classes.take(4).forEach { c ->
                        FilterChip(
                            selected = assignedClass?.id == c.id,
                            onClick = { assignedClass = c },
                            label = { Text(c.className, fontSize = 11.sp) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text(if (existing == null) "Password Awal (Default: 123456)" else "Password Baru (Kosongkan jika tidak diubah)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = canAddStudent, onCheckedChange = { canAddStudent = it })
                Text("Izinkan Tambah/Edit Data Siswa", fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = canCorrectAttendance, onCheckedChange = { canCorrectAttendance = it })
                Text("Izinkan Koreksi Absensi", fontSize = 13.sp)
            }

            if (existing != null) {
                OutlinedButton(
                    onClick = { onDelete(existing) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlpaText),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hapus Akun Pengguna")
                }
            }
        }
    }
}

@Composable
private fun ResetPasswordModalDialog(
    targetUser: UserEntity,
    onDismiss: () -> Unit,
    onSavePassword: (String) -> Unit
) {
    var newPass by remember { mutableStateOf("") }
    MiModalShell(
        title = "Reset Password Akun",
        subtitle = "${targetUser.fullName} (@${targetUser.username})",
        onDismiss = onDismiss,
        onSave = {
            if (newPass.isNotBlank()) onSavePassword(newPass)
        },
        saveText = "Simpan Password"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Password baru akan disimpan menggunakan hash SHA-256 yang aman.",
                fontSize = 13.sp,
                color = MiTextSecondary
            )
            OutlinedTextField(
                value = newPass,
                onValueChange = { newPass = it },
                label = { Text("Masukkan Kata Sandi Baru") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ArrearsManagementModalView(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val user = state.currentUser ?: return
    val isAdmin = user.role == RoleType.ADMIN
    val isWali = user.role == RoleType.WALI_KELAS

    val scopedArrears = if (isWali && user.assignedClassId != null) {
        state.arrears.filter { it.classId == user.assignedClassId }
    } else {
        state.arrears
    }

    val totalNominal = scopedArrears.sumOf { it.nominal }
    val totalPaid = scopedArrears.sumOf { it.paidAmount }
    val totalUnpaid = (totalNominal - totalPaid).coerceAtLeast(0L)

    MiModalShell(
        title = if (isWali) "Tunggakan Siswa ${user.assignedClassName}" else "Manajemen Tunggakan & Tagihan MI",
        subtitle = "Total Sisa Tunggakan: ${formatRupiah(totalUnpaid)}",
        onDismiss = { viewModel.closeModal() },
        onSave = if (isAdmin) {
            { viewModel.openModal(ActiveModal.ArrearForm(null)) }
        } else null,
        saveText = "+ Tambah Tagihan",
        cancelText = "Tutup"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MiMintSoft,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Tagihan", fontSize = 11.sp, color = MiTextSecondary)
                        Text(formatRupiah(totalNominal), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MiDarkGreen)
                    }
                    Column {
                        Text("Sudah Dibayar", fontSize = 11.sp, color = MiTextSecondary)
                        Text(formatRupiah(totalPaid), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MiEmerald)
                    }
                    Column {
                        Text("Total Tunggakan", fontSize = 11.sp, color = MiTextSecondary)
                        Text(formatRupiah(totalUnpaid), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusAlpaText)
                    }
                }
            }

            val context = LocalContext.current
            val scopeLabel = if (isWali) user.assignedClassName else "Seluruh Kelas MI"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val html = ReportExportHelper.buildArrearsPrintHtml(
                            settings = state.settings,
                            reportTitle = "LAPORAN TAGIHAN & TUNGGAKAN SISWA",
                            classScopeLabel = scopeLabel,
                            arrears = scopedArrears,
                            signerRoleLabel = RoleType.label(user.role),
                            signerName = user.fullName
                        )
                        viewModel.openModal(
                            ActiveModal.PrintDocumentPreviewModal(
                                documentTitle = "Laporan Tunggakan ($scopeLabel)",
                                htmlContent = html,
                                isArrearsReport = true,
                                periodOrScopeLabel = scopeLabel
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MiDarkGreen),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("print_arrears_report_btn")
                ) {
                    Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak Kop Surat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        ReportExportHelper.generateAndShareArrearsPdf(
                            context = context,
                            settings = state.settings,
                            reportTitle = "LAPORAN TAGIHAN & TUNGGAKAN SISWA",
                            classScopeLabel = scopeLabel,
                            arrears = scopedArrears,
                            onResult = { viewModel.showMessage(it) }
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MiDarkGreen),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pdf_arrears_report_btn")
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unduh PDF", fontSize = 12.sp, color = MiDarkGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = { viewModel.openModal(ActiveModal.WaArrearTemplateModal) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MiEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_wa_template_editor_btn")
            ) {
                Icon(Icons.Outlined.Chat, contentDescription = null, tint = MiEmerald, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Atur Template WA Tunggakan untuk Orang Tua",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiDarkGreen
                )
            }

            if (scopedArrears.isEmpty()) {
                Text("Belum ada data tunggakan / tagihan.", fontSize = 13.sp, color = MiTextSecondary)
            } else {
                scopedArrears.forEach { ar ->
                    val sisa = (ar.nominal - ar.paidAmount).coerceAtLeast(0L)
                    val matchedStudent = state.students.find { it.id == ar.studentId }
                    val autoWaSnippet = remember(ar, matchedStudent, state.settings) {
                        viewModel.formatWaArrearMessage(ar, matchedStudent)
                    }
                    Surface(
                        color = MiSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MiBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${ar.studentName} (${ar.studentNis})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${ar.className} • ${ar.billType} (${ar.period})", fontSize = 12.sp, color = MiEmerald)
                                    if (matchedStudent != null && matchedStudent.parentName.isNotBlank()) {
                                        Text(
                                            text = "Wali: ${matchedStudent.parentName} (${matchedStudent.parentPhone.ifBlank { "No HP belum diisi" }})",
                                            fontSize = 11.sp,
                                            color = MiTextSecondary
                                        )
                                    }
                                }
                                StatusBadge(status = ar.status)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Nominal: ${formatRupiah(ar.nominal)} | Dibayar: ${formatRupiah(ar.paidAmount)} | Sisa: ${formatRupiah(sisa)}",
                                fontSize = 12.sp,
                                color = MiTextPrimary
                            )
                            if (ar.paymentHistory.isNotBlank()) {
                                Text("Riwayat: ${ar.paymentHistory}", fontSize = 11.sp, color = MiTextSecondary)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MiMintSoft.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.8.dp, MiMintBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Format WA Otomatis (Nama, Nominal & Periode):",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MiDarkGreen
                                    )
                                    Text(
                                        text = autoWaSnippet.lines().take(4).joinToString(" • "),
                                        fontSize = 11.sp,
                                        color = MiTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
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
                                        modifier = Modifier.testTag("wa_ortu_btn_${ar.studentNis}")
                                    ) {
                                        Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(15.dp))
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
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("pdf_ortu_btn_${ar.studentNis}")
                                        ) {
                                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MiDarkGreen, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PDF Ortu", fontSize = 11.5.sp, color = MiDarkGreen, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Row {
                                    TextButton(onClick = { viewModel.openModal(ActiveModal.ArrearForm(existing = ar)) }) {
                                        Text("Pelunasan / Edit", fontSize = 12.sp, color = MiEmerald)
                                    }
                                    if (isAdmin) {
                                        TextButton(onClick = { viewModel.deleteArrear(ar) }) {
                                            Text("Hapus", fontSize = 12.sp, color = StatusAlpaText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaArrearTemplateModalView(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val user = state.currentUser ?: return
    val context = LocalContext.current
    val isWali = user.role == RoleType.WALI_KELAS
    var templateText by remember { mutableStateOf(viewModel.getWaArrearTemplate()) }

    val scopedArrears = if (isWali && user.assignedClassId != null) {
        state.arrears.filter { it.classId == user.assignedClassId }
    } else {
        state.arrears
    }
    var selectedPreviewIndex by remember { mutableIntStateOf(0) }
    val sampleArrear = scopedArrears.getOrNull(selectedPreviewIndex)
        ?: scopedArrears.firstOrNull { it.status != "Lunas" }
        ?: scopedArrears.firstOrNull()
    val sampleStudent = sampleArrear?.let { ar -> state.students.find { it.id == ar.studentId } }

    val availablePlaceholders = listOf(
        "{nama_siswa}",
        "{nominal}",
        "{periode}",
        "{sisa_tunggakan}",
        "{jenis_tagihan}",
        "{nama_wali}",
        "{nis}",
        "{kelas}",
        "{dibayar}",
        "{status}",
        "{sekolah}",
        "{wali_kelas}"
    )

    MiModalShell(
        title = "Template WA Tunggakan Otomatis",
        subtitle = if (isWali) "Khusus Wali ${user.assignedClassName} (Nama Siswa, Nominal & Periode Otomatis)" else "Template Pesan Tagihan Wali Murid",
        onDismiss = { viewModel.closeModal() },
        onSave = {
            viewModel.saveWaArrearTemplate(templateText, returnToArrearsModal = true)
        },
        saveText = "Simpan Template",
        cancelText = "Batal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Pilih gaya template cepat atau sesuaikan sendiri. Variabel {nama_siswa}, {nominal}, dan {periode} akan otomatis terisi sesuai data tagihan masing-masing siswa:",
                fontSize = 12.sp,
                color = MiTextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { templateText = MadrasahRepository.DEFAULT_WA_ARREAR_TEMPLATE },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).testTag("preset_wa_resmi_btn")
                ) {
                    Text("Resmi & Sopan", fontSize = 11.sp, color = MiDarkGreen)
                }
                OutlinedButton(
                    onClick = { templateText = MadrasahRepository.PRESET_WA_ARREAR_RINGKAS },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).testTag("preset_wa_ringkas_btn")
                ) {
                    Text("Pengingat Ringkas", fontSize = 11.sp, color = MiEmerald)
                }
                OutlinedButton(
                    onClick = { templateText = MadrasahRepository.PRESET_WA_ARREAR_KONFIRMASI },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).testTag("preset_wa_rincian_btn")
                ) {
                    Text("Rincian Cicilan", fontSize = 11.sp, color = MiDarkGreen)
                }
            }

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(availablePlaceholders.size) { idx ->
                    val tag = availablePlaceholders[idx]
                    AssistChip(
                        onClick = { templateText = "$templateText $tag" },
                        label = { Text(tag, fontSize = 11.sp, color = MiDarkGreen) }
                    )
                }
            }

            OutlinedTextField(
                value = templateText,
                onValueChange = { templateText = it },
                label = { Text("Isi Template Pesan WhatsApp Otomatis") },
                minLines = 8,
                maxLines = 13,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wa_template_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        templateText = viewModel.resetWaArrearTemplate()
                        viewModel.showMessage("Template WA dikembalikan ke format bawaan.")
                    }
                ) {
                    Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset ke Default", fontSize = 12.sp, color = MiEmerald)
                }

                TextButton(
                    onClick = { viewModel.openModal(ActiveModal.ArrearsManagementModal) }
                ) {
                    Text("Lihat Daftar Tunggakan", fontSize = 12.sp, color = MiDarkGreen)
                }
            }

            if (scopedArrears.isNotEmpty() && sampleArrear != null) {
                HorizontalDivider(color = MiBorderColor)
                Text(
                    text = "Pilih Siswa untuk Uji Pratinjau & Kirim Langsung:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiTextSecondary
                )
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(scopedArrears.size) { idx ->
                        val item = scopedArrears[idx]
                        FilterChip(
                            selected = selectedPreviewIndex == idx,
                            onClick = { selectedPreviewIndex = idx },
                            label = {
                                Text(
                                    text = "${item.studentName} (${item.period})",
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                val previewRendered = viewModel.formatWaArrearMessage(
                    arrear = sampleArrear,
                    student = sampleStudent,
                    customTemplate = templateText
                )
                Surface(
                    color = MiMintSoft,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MiMintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Hasil Otomatis: ${sampleArrear.studentName} • ${formatRupiah(sampleArrear.nominal)} • ${sampleArrear.period}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiDarkGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = previewRendered,
                            fontSize = 12.sp,
                            color = MiTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.saveWaArrearTemplate(templateText, returnToArrearsModal = false)
                                sendWhatsAppToParent(
                                    context = context,
                                    rawPhone = sampleStudent?.parentPhone ?: "",
                                    message = previewRendered,
                                    onFeedback = { viewModel.showMessage(it) }
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MiEmerald),
                            modifier = Modifier.fillMaxWidth().testTag("send_preview_wa_btn")
                        ) {
                            Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simpan & Kirim WA ke Wali ${sampleArrear.studentName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaArrearMessagePreviewModal(
    arrear: ArrearPaymentEntity,
    student: StudentEntity?,
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val context = LocalContext.current
    val studentAllUnpaidArrears = remember(arrear, state.arrears) {
        state.arrears.filter { it.studentId == arrear.studentId && it.status != "Lunas" }
    }
    var includeAllBills by remember { mutableStateOf(false) }
    var phoneInput by remember(student) { mutableStateOf(student?.parentPhone ?: "") }
    var messageBody by remember(arrear, student, includeAllBills) {
        mutableStateOf(
            if (includeAllBills && studentAllUnpaidArrears.size > 1) {
                viewModel.formatWaStudentAllArrearsMessage(student, studentAllUnpaidArrears)
            } else {
                viewModel.formatWaArrearMessage(arrear, student)
            }
        )
    }

    MiModalShell(
        title = "Kirim WA Tunggakan ke Orang Tua",
        subtitle = "${arrear.studentName} (${arrear.className}) • Wali: ${student?.parentName?.ifBlank { "-" } ?: "-"}",
        onDismiss = { viewModel.closeModal() },
        onSave = {
            sendWhatsAppToParent(
                context = context,
                rawPhone = phoneInput,
                message = messageBody,
                onFeedback = { viewModel.showMessage(it) }
            )
            viewModel.closeModal()
        },
        saveText = "Kirim via WhatsApp",
        cancelText = "Batal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MiMintSoft,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MiMintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Data Tagihan Terisi Otomatis:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiDarkGreen
                    )
                    Text(
                        text = "• Nama Siswa: ${arrear.studentName} (NIS: ${arrear.studentNis})\n• Jenis & Periode: ${arrear.billType} (${arrear.period})\n• Nominal Tagihan: ${formatRupiah(arrear.nominal)} (Sisa: ${formatRupiah((arrear.nominal - arrear.paidAmount).coerceAtLeast(0L))})",
                        fontSize = 11.5.sp,
                        color = MiTextPrimary
                    )
                }
            }

            if (studentAllUnpaidArrears.size > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeAllBills = !includeAllBills }
                ) {
                    Checkbox(
                        checked = includeAllBills,
                        onCheckedChange = { includeAllBills = it }
                    )
                    Text(
                        text = "Gabungkan semua tagihan belum lunas siswa ini (${studentAllUnpaidArrears.size} periode)",
                        fontSize = 12.sp,
                        color = MiDarkGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text("Nomor WhatsApp Orang Tua / Wali") },
                placeholder = { Text("Contoh: 08131001001 atau 62813...") },
                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = MiEmerald) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wa_parent_phone_input")
            )

            OutlinedTextField(
                value = messageBody,
                onValueChange = { messageBody = it },
                label = { Text("Pesan WhatsApp (Siap Dikirim / Dapat Diedit)") },
                minLines = 8,
                maxLines = 13,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wa_message_body_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Pesan WA Tunggakan", messageBody))
                        viewModel.showMessage("Teks pesan WA berhasil disalin ke papan klip!")
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wa_copy_message_btn")
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin Pesan", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.openModal(ActiveModal.WaArrearTemplateModal) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ubah Template", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun sendWhatsAppToParent(
    context: Context,
    rawPhone: String,
    message: String,
    onFeedback: (String) -> Unit
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Pesan WA Tunggakan", message))

    val digitsOnly = rawPhone.filter { it.isDigit() }
    val normalizedPhone = when {
        digitsOnly.startsWith("0") -> "62" + digitsOnly.drop(1)
        digitsOnly.startsWith("62") -> digitsOnly
        digitsOnly.isNotEmpty() -> "62$digitsOnly"
        else -> ""
    }

    try {
        if (normalizedPhone.isNotBlank()) {
            val encodedText = Uri.encode(message)
            val waUri = Uri.parse("https://wa.me/$normalizedPhone?text=$encodedText")
            val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onFeedback("Membuka WhatsApp ke +$normalizedPhone (Pesan juga disalin).")
        } else {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Pesan WA Tunggakan"))
            onFeedback("Pesan disalin & siap dibagikan ke WhatsApp.")
        }
    } catch (e: Exception) {
        onFeedback("Pesan WA telah disalin ke papan klip (aplikasi WhatsApp tidak terdeteksi).")
    }
}

@Composable
private fun PrintDocumentPreviewModalView(
    modalData: ActiveModal.PrintDocumentPreviewModal,
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val context = LocalContext.current
    val user = state.currentUser

    MiModalShell(
        title = "Pratinjau Cetak: ${modalData.documentTitle}",
        subtitle = "Kop Surat Madrasah & CSS @media print aktif (Elemen UI disembunyikan saat cetak)",
        onDismiss = { viewModel.closeModal() },
        onSave = {
            ReportExportHelper.printHtmlDocument(
                context = context,
                jobName = modalData.documentTitle.replace(" ", "_"),
                htmlContent = modalData.htmlContent,
                fallbackAction = {
                    if (modalData.isArrearsReport) {
                        val isWali = user?.role == RoleType.WALI_KELAS
                        val scopedArrears = if (isWali && user?.assignedClassId != null) {
                            state.arrears.filter { it.classId == user.assignedClassId }
                        } else {
                            state.arrears
                        }
                        ReportExportHelper.generateAndShareArrearsPdf(
                            context = context,
                            settings = state.settings,
                            reportTitle = modalData.documentTitle,
                            classScopeLabel = modalData.periodOrScopeLabel,
                            arrears = scopedArrears,
                            onResult = { viewModel.showMessage(it) }
                        )
                    } else {
                        val rows = viewModel.buildReportRows(state)
                        ReportExportHelper.generateAndSharePdf(
                            context = context,
                            settings = state.settings,
                            reportTitle = modalData.documentTitle,
                            periodLabel = modalData.periodOrScopeLabel,
                            rows = rows,
                            onResult = { viewModel.showMessage(it) }
                        )
                    }
                },
                onResult = { viewModel.showMessage(it) }
            )
            viewModel.closeModal()
        },
        saveText = "Cetak Sekarang",
        cancelText = "Batal"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pratinjau Kertas A4 (Kop Surat & Tabel Resmi):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiDarkGreen
                )
                TextButton(
                    onClick = {
                        if (modalData.isArrearsReport) {
                            val isWali = user?.role == RoleType.WALI_KELAS
                            val scopedArrears = if (isWali && user?.assignedClassId != null) {
                                state.arrears.filter { it.classId == user.assignedClassId }
                            } else {
                                state.arrears
                            }
                            ReportExportHelper.generateAndShareArrearsPdf(
                                context = context,
                                settings = state.settings,
                                reportTitle = modalData.documentTitle,
                                classScopeLabel = modalData.periodOrScopeLabel,
                                arrears = scopedArrears,
                                onResult = { viewModel.showMessage(it) }
                            )
                        } else {
                            val rows = viewModel.buildReportRows(state)
                            ReportExportHelper.generateAndSharePdf(
                                context = context,
                                settings = state.settings,
                                reportTitle = modalData.documentTitle,
                                periodLabel = modalData.periodOrScopeLabel,
                                rows = rows,
                                onResult = { viewModel.showMessage(it) }
                            )
                        }
                    }
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp), tint = MiEmerald)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan PDF", fontSize = 12.sp, color = MiEmerald)
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MiBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(410.dp)
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            loadDataWithBaseURL(null, modalData.htmlContent, "text/html", "UTF-8", null)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, modalData.htmlContent, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ArrearFormModalDialog(
    existing: ArrearPaymentEntity?,
    preselectedStudent: StudentEntity?,
    students: List<StudentEntity>,
    onDismiss: () -> Unit,
    onCloseAll: () -> Unit,
    onSave: (ArrearPaymentEntity) -> Unit
) {
    var selectedStudent by remember {
        mutableStateOf(
            preselectedStudent
                ?: students.find { it.id == existing?.studentId }
                ?: students.firstOrNull()
        )
    }
    var billType by remember { mutableStateOf(existing?.billType ?: "Infaq Syahriyah Bulanan") }
    var nominalStr by remember { mutableStateOf((existing?.nominal ?: 150000L).toString()) }
    var paidStr by remember { mutableStateOf((existing?.paidAmount ?: 0L).toString()) }
    var period by remember { mutableStateOf(existing?.period ?: "Oktober 2026") }
    var paymentHistory by remember { mutableStateOf(existing?.paymentHistory ?: "") }

    MiModalShell(
        title = if (existing == null) "Tambah Tagihan Siswa" else "Update Pembayaran / Tagihan",
        onDismiss = onCloseAll,
        onSave = {
            val st = selectedStudent ?: return@MiModalShell
            val nom = nominalStr.toLongOrNull() ?: 0L
            val paid = paidStr.toLongOrNull() ?: 0L
            val computedStatus = when {
                paid >= nom && nom > 0 -> "Lunas"
                paid > 0 -> "Cicilan"
                else -> "Belum Lunas"
            }
            onSave(
                ArrearPaymentEntity(
                    id = existing?.id ?: 0L,
                    studentId = st.id,
                    studentNis = st.nis,
                    studentName = st.fullName,
                    classId = st.classId,
                    className = st.className,
                    billType = billType.trim(),
                    nominal = nom,
                    paidAmount = paid,
                    period = period.trim(),
                    status = computedStatus,
                    paymentHistory = paymentHistory.trim()
                )
            )
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Pilih Siswa:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                students.take(3).forEach { s ->
                    FilterChip(
                        selected = selectedStudent?.id == s.id,
                        onClick = { selectedStudent = s },
                        label = { Text(s.fullName.take(12), fontSize = 11.sp) }
                    )
                }
            }
            OutlinedTextField(value = billType, onValueChange = { billType = it }, label = { Text("Jenis Tagihan") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = period, onValueChange = { period = it }, label = { Text("Periode (Bulan / Semester)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = { nominalStr = it },
                    label = { Text("Nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = paidStr,
                    onValueChange = { paidStr = it },
                    label = { Text("Sudah Dibayar (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = paymentHistory,
                onValueChange = { paymentHistory = it },
                label = { Text("Catatan Riwayat Pembayaran") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HomeroomAdminModalView(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val user = state.currentUser ?: return
    val myClass = state.classes.find { it.id == user.assignedClassId } ?: state.classes.firstOrNull()
    if (myClass == null) {
        viewModel.closeModal()
        return
    }

    var roomNumber by remember(myClass) { mutableStateOf(myClass.roomNumber) }
    var classNotes by remember(myClass) { mutableStateOf(myClass.classNotes) }
    var classAnnouncement by remember(myClass) { mutableStateOf(myClass.classAnnouncement) }

    MiModalShell(
        title = "Administrasi ${myClass.className}",
        subtitle = "Wali Kelas: ${user.fullName}",
        onDismiss = { viewModel.closeModal() },
        onSave = {
            viewModel.saveClass(
                myClass.copy(
                    roomNumber = roomNumber.trim(),
                    classNotes = classNotes.trim(),
                    classAnnouncement = classAnnouncement.trim()
                ),
                stayInHomeroomModal = false
            )
        },
        saveText = "Simpan Administrasi"
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = roomNumber,
                onValueChange = { roomNumber = it },
                label = { Text("Kode / Nomor Ruang Kelas") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = classNotes,
                onValueChange = { classNotes = it },
                label = { Text("Catatan Administrasi & Inventaris Kelas") },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = classAnnouncement,
                onValueChange = { classAnnouncement = it },
                label = { Text("Pengumuman Kelas (Untuk Siswa & Wali Murid)") },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AdminSettingsModalView(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val s = state.settings
    var schoolName by remember(s) { mutableStateOf(s.schoolName) }
    var nsm by remember(s) { mutableStateOf(s.nsm) }
    var npsn by remember(s) { mutableStateOf(s.npsn) }
    var address by remember(s) { mutableStateOf(s.address) }
    var headmasterName by remember(s) { mutableStateOf(s.headmasterName) }
    var headmasterNip by remember(s) { mutableStateOf(s.headmasterNip) }
    var timeIn by remember(s) { mutableStateOf(s.timeInSchedule) }
    var lateTime by remember(s) { mutableStateOf(s.lateThresholdTime) }
    var timeOut by remember(s) { mutableStateOf(s.timeOutSchedule) }
    var academicYear by remember(s) { mutableStateOf(s.activeAcademicYear) }
    var semester by remember(s) { mutableStateOf(s.activeSemester) }
    var scheduleSummary by remember(s) { mutableStateOf(s.scheduleSummary) }
    var enableNotif by remember(s) { mutableStateOf(s.enableNotifications) }
    var enableAutoLate by remember(s) { mutableStateOf(s.enableAutoLateDetection) }
    var calendarNotes by remember {
        mutableStateOf(
            state.academicYears.firstOrNull()?.calendarNotes
                ?: "14 Jul 2026: Hari Pertama Masuk\n28 Sep 2026: PTS Ganjil\n14 Des 2026: PAS Ganjil"
        )
    }

    MiModalShell(
        title = "Pengaturan Madrasah (Admin)",
        subtitle = "Konfigurasi Profil, Jam Absen, Kalender & Backup",
        onDismiss = { viewModel.closeModal() },
        onSave = {
            viewModel.saveSchoolSettings(
                s.copy(
                    schoolName = schoolName.trim(),
                    nsm = nsm.trim(),
                    npsn = npsn.trim(),
                    address = address.trim(),
                    headmasterName = headmasterName.trim(),
                    headmasterNip = headmasterNip.trim(),
                    timeInSchedule = timeIn.trim(),
                    lateThresholdTime = lateTime.trim(),
                    timeOutSchedule = timeOut.trim(),
                    activeAcademicYear = academicYear.trim(),
                    activeSemester = semester.trim(),
                    scheduleSummary = scheduleSummary.trim(),
                    enableNotifications = enableNotif,
                    enableAutoLateDetection = enableAutoLate
                ),
                AcademicYearEntity(
                    id = state.academicYears.firstOrNull()?.id ?: 0L,
                    yearName = academicYear.trim(),
                    semester = semester.trim(),
                    isActive = true,
                    calendarNotes = calendarNotes.trim()
                )
            )
            viewModel.closeModal()
        },
        saveText = "Simpan Pengaturan"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.setDataSectionTab(3)
                        viewModel.selectBottomTab(1)
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Kelola Akun", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { viewModel.openModal(ActiveModal.WaArrearTemplateModal) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Template WA", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { viewModel.openModal(ActiveModal.BackupRestoreModal) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Backup Data", fontSize = 11.sp)
                }
            }

            Text("1. Profil Madrasah", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MiDarkGreen)
            OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text("Nama Madrasah") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nsm, onValueChange = { nsm = it }, label = { Text("NSM") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = npsn, onValueChange = { npsn = it }, label = { Text("NPSN") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Madrasah") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = headmasterName, onValueChange = { headmasterName = it }, label = { Text("Nama Kepala Madrasah") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = headmasterNip, onValueChange = { headmasterNip = it }, label = { Text("NIP Kepala Madrasah") }, modifier = Modifier.fillMaxWidth())

            Text("2. Jam Masuk, Batas Terlambat & Jam Pulang", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MiDarkGreen)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = timeIn, onValueChange = { timeIn = it }, label = { Text("Jam Masuk") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = lateTime, onValueChange = { lateTime = it }, label = { Text("Batas Telat") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = timeOut, onValueChange = { timeOut = it }, label = { Text("Jam Pulang") }, modifier = Modifier.weight(1f))
            }

            Text("3. Tahun Ajaran, Semester, Jadwal & Kalender Akademik", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MiDarkGreen)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = academicYear, onValueChange = { academicYear = it }, label = { Text("Tahun Ajaran") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("Semester") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = scheduleSummary, onValueChange = { scheduleSummary = it }, label = { Text("Jadwal KBM & Ekstrakurikuler") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = calendarNotes, onValueChange = { calendarNotes = it }, label = { Text("Kalender Akademik") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = enableAutoLate, onCheckedChange = { enableAutoLate = it })
                Text("Deteksi Otomatis Status Terlambat Saat Scan QR", fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = enableNotif, onCheckedChange = { enableNotif = it })
                Text("Aktifkan Notifikasi & Log Aktivitas Sistem", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun BackupRestoreModalView(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val context = LocalContext.current
    var jsonText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.exportBackupJson { generated ->
            jsonText = generated
        }
    }

    MiModalShell(
        title = "Backup, Restore & Export/Import JSON",
        subtitle = "Aman tanpa menyimpan password plaintext",
        onDismiss = { viewModel.closeModal() },
        onSave = {
            if (jsonText.isNotBlank()) {
                viewModel.importBackupJson(jsonText)
            }
        },
        saveText = "Restore / Import JSON",
        cancelText = "Batal"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "File JSON cadangan mencakup seluruh tabel terpisah (users [hash only], students, teachers, classes, attendance, notes, achievements, violations, arrears_payments, school_settings).",
                fontSize = 12.sp,
                color = MiTextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.exportBackupJson { generated ->
                            jsonText = generated
                            ReportExportHelper.shareBackupJsonFile(
                                context = context,
                                jsonContent = generated,
                                onResult = { viewModel.showMessage(it) }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiDarkGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("export_json_file_button")
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export File JSON", fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                label = { Text("Isi JSON Backup (Dapat diedit atau ditempel untuk Restore)") },
                minLines = 8,
                maxLines = 12,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("backup_json_textarea")
            )
        }
    }
}
