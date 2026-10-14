package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.*
import com.example.ui.theme.*
import com.example.util.QrMatrixGenerator
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun AttendanceSection(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val user = state.currentUser ?: return
    val isWali = user.role == RoleType.WALI_KELAS
    val canCorrect = user.role == RoleType.ADMIN ||
        (user.role == RoleType.WALI_KELAS && user.canCorrectAttendance) ||
        (user.role == RoleType.GURU_PIKET && user.canCorrectAttendance)

    var subTab by remember { mutableIntStateOf(0) } // 0: Absensi Hari Ini, 1: Riwayat Absensi, 2: Scan QR Real-Time

    val scopedStudents = viewModel.getScopedStudents(state)
    val dateAttMap = remember(state.attendance, state.selectedDateFilter) {
        state.attendance
            .filter { it.date == state.selectedDateFilter }
            .associateBy { it.studentId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Mode Selector: Absensi Harian, Riwayat Absensi, & Scan QR Real-Time (CameraX)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    label = { Text("Absensi Harian (${state.selectedDateFilter})", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Today, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MiDarkGreen,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    label = { Text("Riwayat Absensi", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MiEmerald,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
            if (canCorrect) {
                item {
                    FilterChip(
                        selected = subTab == 2,
                        onClick = {
                            subTab = 2
                            viewModel.setCameraActive(true)
                        },
                        label = { Text("Scan QR Real-Time", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MiDarkGreen,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("attendance_tab_scan_qr")
                    )
                }
            }
        }

        if (subTab == 2) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                ScanQrAttendanceSection(state = state, viewModel = viewModel, isEmbeddedInAttendance = true)
            }
            return@Column
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date Filter + Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.selectedDateFilter,
                onValueChange = { viewModel.setDateFilter(it) },
                label = { Text("Tanggal (YYYY-MM-DD)", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.95f)
            )
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Cari Siswa / NIS", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.05f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ClassFilterRow(
            classes = state.classes,
            selectedClassId = state.selectedClassIdFilter,
            isLockedToAssignedClass = isWali,
            assignedClassName = user.assignedClassName,
            onSelectClass = { viewModel.setClassFilter(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (subTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scopedStudents, key = { it.id }) { st ->
                    val existingAtt = dateAttMap[st.id]
                    val currentStatus = existingAtt?.status ?: AttendanceStatus.BELUM_HADIR

                    MiCard(
                        onClick = {
                            if (existingAtt != null) {
                                viewModel.openModal(ActiveModal.AttendanceDetailModal(existingAtt))
                            } else if (canCorrect) {
                                viewModel.openModal(
                                    ActiveModal.AttendanceRecordForm(
                                        student = st,
                                        date = state.selectedDateFilter,
                                        existing = null,
                                        isCorrection = false
                                    )
                                )
                            }
                        },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = st.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MiTextPrimary
                                )
                                Text(
                                    text = "NIS: ${st.nis} • ${st.className}",
                                    fontSize = 12.sp,
                                    color = MiTextSecondary
                                )
                                if (existingAtt != null) {
                                    Text(
                                        text = "Waktu: ${existingAtt.time} • Ket: ${existingAtt.note.ifBlank { "-" }}" +
                                            if (existingAtt.isCorrected) " (Dikoreksi)" else "",
                                        fontSize = 12.sp,
                                        color = MiEmerald
                                    )
                                }
                            }
                            StatusBadge(status = currentStatus)
                        }

                        if (canCorrect) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MiBorderColor.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(8.dp))

                            if (existingAtt == null) {
                                // Quick status recording buttons (validates duplicate on same date)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AttendanceStatus.ALL_RECORDED.forEach { statusOption ->
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.recordAttendance(
                                                    student = st,
                                                    date = state.selectedDateFilter,
                                                    time = viewModel.repository.currentTimeString(),
                                                    status = statusOption,
                                                    note = "Input Absensi Harian",
                                                    allowCorrection = false
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 38.dp)
                                                .testTag("quick_att_${st.nis}_$statusOption")
                                        ) {
                                            Text(
                                                text = statusOption,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Already recorded today -> offer official "Koreksi Absensi" & "Detail"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { viewModel.openModal(ActiveModal.AttendanceDetailModal(existingAtt)) }
                                    ) {
                                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Detail", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.openModal(
                                                ActiveModal.AttendanceRecordForm(
                                                    student = st,
                                                    date = state.selectedDateFilter,
                                                    existing = existingAtt,
                                                    isCorrection = true
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("correct_att_${st.nis}")
                                    ) {
                                        Icon(Icons.Outlined.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Koreksi Absensi", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Riwayat Absensi
            val historyList = state.attendance.filter { att ->
                val classMatch = when {
                    isWali && user.assignedClassId != null -> att.classId == user.assignedClassId
                    state.selectedClassIdFilter != null -> att.classId == state.selectedClassIdFilter
                    else -> true
                }
                val q = state.searchQuery.trim().lowercase()
                val queryMatch = q.isEmpty() ||
                    att.studentName.lowercase().contains(q) ||
                    att.studentNis.lowercase().contains(q) ||
                    att.date.contains(q)
                classMatch && queryMatch
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyList, key = { it.id }) { att ->
                    MiCard(
                        onClick = { viewModel.openModal(ActiveModal.AttendanceDetailModal(att)) },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${att.studentName} (${att.studentNis})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MiTextPrimary
                                )
                                Text(
                                    text = "${att.className} • Tanggal: ${att.date} • Jam: ${att.time}",
                                    fontSize = 12.sp,
                                    color = MiTextSecondary
                                )
                                Text(
                                    text = "Ket: ${att.note.ifBlank { "-" }} • Petugas: ${att.operatorName}",
                                    fontSize = 12.sp,
                                    color = MiTextMuted
                                )
                            }
                            StatusBadge(status = att.status)
                        }
                    }
                }
            }
        }
    }
}

/**
 * GURU PIKET Tab 1 & Embedded Attendance Scanner: Scan Absen QR Siswa Real-Time (CameraX)
 * Uses CameraX Preview + ImageAnalysis (STRATEGY_KEEP_ONLY_LATEST) to analyze frames in real-time,
 * supports front/back camera switching, flashlight/torch control, automatic debounce,
 * and instant attendance recording.
 */
@Composable
fun ScanQrAttendanceSection(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel,
    isEmbeddedInAttendance: Boolean = false
) {
    val context = LocalContext.current
    var manualCode by remember { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraErrorMsg by remember { mutableStateOf<String?>(null) }
    var useBackCamera by remember { mutableStateOf(true) }
    var isTorchOn by remember { mutableStateOf(false) }
    var analyzedFramesCount by remember { mutableIntStateOf(0) }
    var currentLuminance by remember { mutableFloatStateOf(0f) }
    var lastScannedInfo by remember { mutableStateOf<String?>(null) }
    var lastScanTimestampMs by remember { mutableLongStateOf(0L) }

    val knownQrPayloads = remember(state.students) {
        state.students.map { it.qrCodeValue.ifBlank { "MI-${it.nis}" } }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        viewModel.setCameraActive(granted)
        if (!granted) {
            cameraErrorMsg = "Izin kamera tidak diberikan. Gunakan tombol Scan Cepat Siswa atau ketik kode QR/NIS di bawah."
        } else {
            cameraErrorMsg = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setCameraActive(false)
        }
    }

    val today = state.selectedDateFilter
    val todayAttMap = remember(state.attendance, today) {
        state.attendance.filter { it.date == today }.associateBy { it.studentId }
    }

    val recentTodayScans = remember(state.attendance, today) {
        state.attendance.filter { it.date == today }.take(5)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(if (isEmbeddedInAttendance) Modifier else Modifier.padding(horizontal = 16.dp)),
        contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MiCard(
                backgroundColor = MiDarkGreen,
                borderColor = MiEmerald
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pemindai QR Real-Time (CameraX)",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Arahkan kamera ke Kartu QR Siswa. ImageAnalysis CameraX mendeteksi & mencatat kehadiran otomatis secara real-time.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Switch(
                        checked = state.isCameraActive && hasCameraPermission,
                        onCheckedChange = { wantActive ->
                            if (wantActive) {
                                if (hasCameraPermission) {
                                    cameraErrorMsg = null
                                    viewModel.setCameraActive(true)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            } else {
                                isTorchOn = false
                                viewModel.setCameraActive(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MiEmerald
                        ),
                        modifier = Modifier.testTag("camerax_qr_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.isCameraActive && hasCameraPermission && cameraErrorMsg == null) {
                    CameraPreviewBox(
                        useBackCamera = useBackCamera,
                        isTorchOn = isTorchOn,
                        knownQrPayloads = knownQrPayloads,
                        onFrameStats = { lum ->
                            analyzedFramesCount++
                            currentLuminance = lum
                        },
                        onQrCodeDetected = { detectedCode ->
                            val now = System.currentTimeMillis()
                            if (now - lastScanTimestampMs > 2200L) {
                                lastScanTimestampMs = now
                                lastScannedInfo = "Terdeteksi otomatis: $detectedCode"
                                viewModel.handleQrScan(detectedCode)
                            }
                        },
                        onCameraError = { err ->
                            cameraErrorMsg = err
                            viewModel.setCameraActive(false)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // CameraX Hardware Controls (Switch Front/Back Lens & Torch/Flashlight + Live Telemetry)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    useBackCamera = !useBackCamera
                                    if (!useBackCamera) isTorchOn = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("camera_switch_lens_btn")
                            ) {
                                Icon(Icons.Outlined.Cameraswitch, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (useBackCamera) "Kamera Belakang" else "Kamera Depan", fontSize = 11.5.sp)
                            }

                            if (useBackCamera) {
                                OutlinedButton(
                                    onClick = { isTorchOn = !isTorchOn },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isTorchOn) MiEmerald else Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("camera_torch_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isTorchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isTorchOn) "Lampu Aktif" else "Lampu Senter", fontSize = 11.5.sp)
                                }
                            }
                        }

                        Text(
                            text = "Frame: $analyzedFramesCount • Lum: ${currentLuminance.roundToInt()}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = cameraErrorMsg
                                    ?: "Aktifkan kamera CameraX untuk memindai Kartu QR Siswa secara real-time, atau gunakan input/tombol Scan Cepat di bawah.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (hasCameraPermission) {
                                        cameraErrorMsg = null
                                        viewModel.setCameraActive(true)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MiEmerald),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("activate_camerax_button")
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aktifkan Kamera Scan QR", fontSize = 13.sp)
                            }
                        }
                    }
                }

                if (lastScannedInfo != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MiMintSoft,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MiEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = lastScannedInfo!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiDarkGreen
                                )
                            }
                            TextButton(onClick = { lastScannedInfo = null }) {
                                Text("Tutup", fontSize = 11.sp, color = MiEmerald)
                            }
                        }
                    }
                }
            }
        }

        // Real-Time Recent Scan Feed Today
        if (recentTodayScans.isNotEmpty()) {
            item {
                MiCard(
                    backgroundColor = MiMintSoft,
                    borderColor = MiMintBorder,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Log Kehadiran Real-Time Hari Ini ($today)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiDarkGreen
                        )
                        StatusBadge(status = "${todayAttMap.size}/${state.students.size} Hadir")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    recentTodayScans.take(3).forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${rec.time} — ${rec.studentName} (${rec.className})",
                                fontSize = 12.sp,
                                color = MiTextPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = rec.status,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rec.status == AttendanceStatus.TERLAMBAT) StatusTerlambatText else StatusHadirText
                            )
                        }
                    }
                }
            }
        }

        // Direct QR Code / NIS Scanner Input
        item {
            MiCard {
                Text(
                    text = "Input / Pemindai Barcode Eksternal (Kode QR atau NIS)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MiTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        placeholder = { Text("Contoh: MI-1101 atau 1101", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.QrCode, contentDescription = null, tint = MiEmerald) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_manual_input")
                    )
                    Button(
                        onClick = {
                            if (manualCode.isNotBlank()) {
                                lastScannedInfo = "Input Kode QR: ${manualCode.trim()}"
                                viewModel.handleQrScan(manualCode)
                                manualCode = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MiEmerald),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("qr_scan_submit_button")
                    ) {
                        Text("Catat QR", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Class Filter + One-Tap QR Card Simulation for Fast Operator Workflow
        item {
            Text(
                text = "Daftar Kartu QR Siswa (Ketuk untuk Simulasi Scan / Lihat QR)",
                style = MaterialTheme.typography.titleMedium,
                color = MiTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            ClassFilterRow(
                classes = state.classes,
                selectedClassId = state.selectedClassIdFilter,
                isLockedToAssignedClass = state.currentUser?.role == RoleType.WALI_KELAS,
                assignedClassName = state.currentUser?.assignedClassName ?: "",
                onSelectClass = { viewModel.setClassFilter(it) }
            )
        }

        val studentsToScan = viewModel.getScopedStudents(state)
        items(studentsToScan, key = { it.id }) { st ->
            val att = todayAttMap[st.id]
            val qrCode = st.qrCodeValue.ifBlank { "MI-${st.nis}" }
            MiCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = st.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MiTextPrimary
                        )
                        Text(
                            text = "Kode QR: $qrCode • ${st.className}",
                            fontSize = 12.sp,
                            color = MiTextSecondary
                        )
                        if (att != null) {
                            Text(
                                text = "Tercatat: ${att.status} pukul ${att.time}",
                                fontSize = 12.sp,
                                color = MiEmerald,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.openModal(ActiveModal.StudentQrCard(st)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode2,
                                contentDescription = "Lihat Kartu QR",
                                tint = MiDarkGreen
                            )
                        }

                        if (att == null) {
                            Button(
                                onClick = {
                                    lastScannedInfo = "Scan Kartu QR: ${st.fullName} ($qrCode)"
                                    viewModel.handleQrScan(qrCode)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MiDarkGreen),
                                modifier = Modifier.testTag("scan_student_btn_${st.nis}")
                            ) {
                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Hadir", fontSize = 12.sp)
                            }
                        } else {
                            StatusBadge(status = att.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewBox(
    useBackCamera: Boolean,
    isTorchOn: Boolean,
    knownQrPayloads: List<String>,
    onFrameStats: (Float) -> Unit,
    onQrCodeDetected: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    val currentKnownPayloads by rememberUpdatedState(knownQrPayloads)
    val currentOnFrameStats by rememberUpdatedState(onFrameStats)
    val currentOnQrDetected by rememberUpdatedState(onQrCodeDetected)

    DisposableEffect(Unit) {
        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
            }
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(isTorchOn, activeCamera) {
        try {
            if (activeCamera?.cameraInfo?.hasFlashUnit() == true) {
                activeCamera?.cameraControl?.enableTorch(isTorchOn)
            }
        } catch (_: Exception) {
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "qr_laser_scan")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(2.dp, MiEmerald, RoundedCornerShape(16.dp))
            .testTag("camerax_preview_box"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    try {
                                        val planes = imageProxy.planes
                                        if (planes.isNotEmpty()) {
                                            val yPlane = planes[0]
                                            val (decodedQr, avgLum) = QrMatrixGenerator.analyzeLuminanceFrame(
                                                yBuffer = yPlane.buffer,
                                                width = imageProxy.width,
                                                height = imageProxy.height,
                                                rowStride = yPlane.rowStride,
                                                knownCodes = currentKnownPayloads
                                            )
                                            ContextCompat.getMainExecutor(context).execute {
                                                currentOnFrameStats(avgLum)
                                                if (decodedQr != null) {
                                                    currentOnQrDetected(decodedQr)
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {
                                    } finally {
                                        imageProxy.close()
                                    }
                                }
                            }

                        val preferredSelector = if (useBackCamera) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }

                        val finalSelector = when {
                            cameraProvider.hasCamera(preferredSelector) -> preferredSelector
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                            else -> null
                        }

                        if (finalSelector == null) {
                            onCameraError("Perangkat/Emulator ini tidak memiliki sensor kamera fisik. Gunakan tombol Scan Hadir atau input kode QR di bawah.")
                            return@addListener
                        }

                        cameraProvider.unbindAll()
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            finalSelector,
                            preview,
                            imageAnalysis
                        )
                        activeCamera = cam
                        if (cam.cameraInfo.hasFlashUnit()) {
                            cam.cameraControl.enableTorch(isTorchOn)
                        }
                    } catch (e: Exception) {
                        onCameraError("Kamera perangkat tidak tersedia: ${e.localizedMessage ?: "Gunakan input kode QR di bawah."}")
                    }
                }, ContextCompat.getMainExecutor(context))
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Reticle + Animated Laser Line Overlay
        Box(
            modifier = Modifier
                .size(156.dp)
                .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val yPos = size.height * laserProgress
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(10f, yPos),
                    end = Offset(size.width - 10f, yPos),
                    strokeWidth = 4f
                )
            }
        }

        // Status Badge at bottom of viewfinder
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "CameraX ImageAnalysis Aktif • Posisikan QR di dalam kotak",
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * GURU PIKET Tab 2: Belum Hadir (Daftar siswa yang belum memiliki absensi hari ini)
 */
@Composable
fun BelumHadirSection(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val context = LocalContext.current
    val today = state.selectedDateFilter
    val recordedIds = remember(state.attendance, today) {
        state.attendance.filter { it.date == today }.map { it.studentId }.toSet()
    }
    val unrecordedStudents = viewModel.getScopedStudents(state).filter { it.id !in recordedIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        MiCard(
            backgroundColor = MiMintSoft,
            borderColor = MiMintBorder,
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daftar Siswa Belum Hadir ($today)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MiDarkGreen
                    )
                    Text(
                        text = "Tandai status Hadir, Terlambat, Izin, Sakit, atau Alpa secara cepat.",
                        fontSize = 12.sp,
                        color = MiTextSecondary
                    )
                }
                StatusBadge(status = "${unrecordedStudents.size} Siswa")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ClassFilterRow(
            classes = state.classes,
            selectedClassId = state.selectedClassIdFilter,
            isLockedToAssignedClass = false,
            assignedClassName = "",
            onSelectClass = { viewModel.setClassFilter(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (unrecordedStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = MiEmerald,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Alhamdulillah, seluruh siswa pada filter kelas ini sudah tercatat absensinya hari ini!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(unrecordedStudents, key = { it.id }) { st ->
                    MiCard(contentPadding = PaddingValues(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = st.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MiTextPrimary
                                )
                                Text(
                                    text = "NIS: ${st.nis} • ${st.className} • Wali: ${st.parentName.ifBlank { "-" }} (${st.parentPhone.ifBlank { "-" }})",
                                    fontSize = 12.sp,
                                    color = MiTextSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val unpaid = state.arrears.filter { it.studentId == st.id && it.status != "Lunas" }
                                        val waMsg = buildBelumHadirWaMessage(
                                            student = st,
                                            schoolName = state.settings.schoolName,
                                            date = today,
                                            unpaidArrears = unpaid
                                        )
                                        openWhatsAppChat(
                                            context = context,
                                            rawPhone = st.parentPhone,
                                            message = waMsg,
                                            onFeedback = { viewModel.showMessage(it) }
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MiEmerald),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .heightIn(min = 34.dp)
                                        .testTag("belum_hadir_wa_btn_${st.nis}")
                                ) {
                                    Icon(Icons.Outlined.Chat, contentDescription = null, tint = MiEmerald, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kirim WA", fontSize = 11.sp, color = MiDarkGreen, fontWeight = FontWeight.SemiBold)
                                }
                                StatusBadge(status = AttendanceStatus.BELUM_HADIR)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AttendanceStatus.ALL_RECORDED.forEach { stOpt ->
                                OutlinedButton(
                                    onClick = {
                                        viewModel.recordAttendance(
                                            student = st,
                                            date = today,
                                            time = viewModel.repository.currentTimeString(),
                                            status = stOpt,
                                            note = "Ditandai oleh Guru Piket",
                                            allowCorrection = false
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 38.dp)
                                ) {
                                    Text(stOpt, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * KEPALA SEKOLAH Tab 4: Terlambat (Monitoring Keterlambatan Siswa & Rekap)
 */
@Composable
fun LateMonitoringSection(
    state: MadrasahUiState,
    viewModel: MadrasahViewModel
) {
    val allLateRecords = state.attendance.filter { att ->
        att.status == AttendanceStatus.TERLAMBAT &&
            (state.selectedClassIdFilter == null || att.classId == state.selectedClassIdFilter)
    }
    val dateFilteredLate = allLateRecords.filter {
        state.selectedDateFilter.isBlank() || it.date == state.selectedDateFilter
    }

    // Aggregate frequency of lateness per student across all dates
    val lateCountByStudent = allLateRecords.groupingBy { it.studentId }.eachCount()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        MiCard(
            backgroundColor = StatusTerlambatBg,
            borderColor = StatusTerlambatText.copy(alpha = 0.35f),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monitoring Keterlambatan Siswa",
                        style = MaterialTheme.typography.titleMedium,
                        color = StatusTerlambatText
                    )
                    Text(
                        text = "Batas Jam Masuk: ${state.settings.timeInSchedule} (Toleransi ${state.settings.lateThresholdTime})",
                        fontSize = 12.sp,
                        color = MiTextSecondary
                    )
                }
                Text(
                    text = "${dateFilteredLate.size} Siswa",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusTerlambatText
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.selectedDateFilter,
                onValueChange = { viewModel.setDateFilter(it) },
                label = { Text("Filter Tanggal (YYYY-MM-DD)", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { viewModel.setDateFilter("") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text("Semua Tanggal", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ClassFilterRow(
            classes = state.classes,
            selectedClassId = state.selectedClassIdFilter,
            isLockedToAssignedClass = false,
            assignedClassName = "",
            onSelectClass = { viewModel.setClassFilter(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (dateFilteredLate.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak ada siswa terlambat pada filter tanggal/kelas ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dateFilteredLate, key = { it.id }) { att ->
                    val totalTimesLate = lateCountByStudent[att.studentId] ?: 1
                    MiCard(
                        onClick = { viewModel.openModal(ActiveModal.AttendanceDetailModal(att)) },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${att.studentName} (${att.studentNis})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MiTextPrimary
                                )
                                Text(
                                    text = "${att.className} • Tanggal: ${att.date} • Jam Datang: ${att.time}",
                                    fontSize = 13.sp,
                                    color = StatusTerlambatText,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Alasan: ${att.note.ifBlank { "-" }} • Total Terlambat: ${totalTimesLate}x",
                                    fontSize = 12.sp,
                                    color = MiTextSecondary
                                )
                            }
                            StatusBadge(status = "${totalTimesLate}x Telat")
                        }
                    }
                }
            }
        }
    }
}
