package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.*
import com.example.util.QrMatrixGenerator
import java.text.NumberFormat
import java.util.Locale

/**
 * Standard Card with clean modern minimalist design:
 * - Crisp, clean surface (pure white or themed dark green)
 * - Uniform subtle border for sharp visual boundary
 * - Soft, natural ambient drop shadow for gentle depth
 */
@Composable
fun MiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MiSurface,
    borderColor: Color = MiBorderColor,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    elevation: Dp = 1.5.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val isDarkCard = backgroundColor == MiDarkGreen

    val borderStroke = if (borderColor != MiBorderColor) {
        BorderStroke(1.dp, borderColor)
    } else if (isDarkCard) {
        BorderStroke(1.dp, MiEmeraldLight.copy(alpha = 0.4f))
    } else {
        BorderStroke(1.dp, MiBorderColor)
    }

    val spotColor = if (isDarkCard) MiDarkGreen.copy(alpha = 0.25f) else Color(0x12000000)
    val ambientColor = if (isDarkCard) MiDarkGreen.copy(alpha = 0.12f) else Color(0x08000000)

    val baseModifier = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = ambientColor,
            spotColor = spotColor,
            clip = false
        )
        .clip(shape)
        .background(backgroundColor)
        .border(borderStroke, shape)
        .let {
            if (onClick != null) it.clickable(onClick = onClick) else it
        }

    Column(
        modifier = baseModifier.padding(contentPadding),
        content = content
    )
}

/**
 * Metric Card with subtle neumorphic depth and soft raised icon well
 */
@Composable
fun StatMetricCard(
    label: String,
    valueText: String,
    subtitle: String = "",
    accentColor: Color = MiEmerald,
    bgTint: Color = MiMintSoft,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    MiCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        elevation = 3.5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MiTextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Clean, soft tinted icon container
            Surface(
                color = bgTint,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Proportional statistic font size: 24-26sp per specification
        Text(
            text = valueText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MiTextPrimary,
            lineHeight = 28.sp
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MiTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tactile Button with clean modern minimalist finish:
 * - Crisp solid surface (emerald/dark green for primary, pure white for secondary)
 * - Clean 1dp subtle border
 * - Soft gentle drop shadow for subtle physical elevation
 */
@Composable
fun NeumorphicTactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    shape: CornerBasedShape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val bgColor = if (isPrimary) MiDarkGreen else Color.White
    val borderStroke = if (isPrimary) {
        BorderStroke(1.dp, MiEmeraldLight.copy(alpha = 0.4f))
    } else {
        BorderStroke(1.dp, MiBorderColor)
    }

    val spotColor = if (isPrimary) MiDarkGreen.copy(alpha = 0.25f) else Color(0x12000000)
    val ambientColor = if (isPrimary) MiDarkGreen.copy(alpha = 0.12f) else Color(0x06000000)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = bgColor,
        border = borderStroke,
        modifier = modifier
            .shadow(
                elevation = 1.5.dp,
                shape = shape,
                ambientColor = ambientColor,
                spotColor = spotColor
            )
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Recessed / Debossed box for inset fields, filter rows, or search bars
 */
@Composable
fun NeumorphicDebossedBox(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(14.dp),
    backgroundColor: Color = NeuSunkenSurface,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            NeuSunkenBorderTop.copy(alpha = 0.7f),
                            NeuSunkenBorderBottom.copy(alpha = 0.9f)
                        )
                    )
                ),
                shape
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        AttendanceStatus.HADIR, "Lunas", "Aktif" -> StatusHadirBg to StatusHadirText
        AttendanceStatus.TERLAMBAT, "Cicilan" -> StatusTerlambatBg to StatusTerlambatText
        AttendanceStatus.IZIN -> StatusIzinBg to StatusIzinText
        AttendanceStatus.SAKIT -> StatusSakitBg to StatusSakitText
        AttendanceStatus.ALPA, "Belum Lunas" -> StatusAlpaBg to StatusAlpaText
        else -> StatusBelumBg to StatusBelumText
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            0.8.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.85f),
                    fg.copy(alpha = 0.25f)
                )
            )
        ),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Text(
            text = status,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun QrCodeCanvasView(
    qrContent: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 150.dp
) {
    val matrix = QrMatrixGenerator.generateMatrix(qrContent.ifBlank { "MI-0000" }, 21)
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, MiBorderColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grid = 21
            val cellW = size.width / grid
            val cellH = size.height / grid
            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = MiDarkGreen,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW + 0.6f, cellH + 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standard Modal Shell adhering strictly to rule:
 * - Has tombol X (top-right) that immediately closes the modal
 * - Has tombol Batal and tombol Simpan (when showFooterButtons = true)
 * - Clean 20dp rounded corners, proportional font
 */
@Composable
fun MiModalShell(
    title: String,
    subtitle: String = "",
    onDismiss: () -> Unit,
    onSave: (() -> Unit)? = null,
    saveText: String = "Simpan",
    cancelText: String = "Batal",
    showFooterButtons: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 540.dp)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(20.dp),
            color = MiSurface,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, MiBorderColor)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with Title and mandatory X button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiDarkGreen)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .testTag("modal_close_x_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup Modal (X)",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Body content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    content = content
                )

                // Footer buttons (Batal & Simpan)
                if (showFooterButtons) {
                    HorizontalDivider(color = MiBorderColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MiSurfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MiBorderColor),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("modal_cancel_button")
                        ) {
                            Text(
                                text = cancelText,
                                fontSize = 14.sp,
                                color = MiTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (onSave != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = onSave,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MiEmerald),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("modal_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = saveText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
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
fun ClassFilterRow(
    classes: List<com.example.data.SchoolClassEntity>,
    selectedClassId: Long?,
    isLockedToAssignedClass: Boolean,
    assignedClassName: String,
    onSelectClass: (Long?) -> Unit
) {
    if (isLockedToAssignedClass) {
        Surface(
            color = MiMintSoft,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MiMintBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Class,
                    contentDescription = null,
                    tint = MiEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kelas Ampuan: $assignedClassName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiDarkGreen
                )
            }
        }
    } else {
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedClassId == null,
                    onClick = { onSelectClass(null) },
                    label = { Text("Semua Kelas", fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MiDarkGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(classes.size) { idx ->
                val c = classes[idx]
                FilterChip(
                    selected = selectedClassId == c.id,
                    onClick = { onSelectClass(c.id) },
                    label = { Text(c.className, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MiEmerald,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

fun formatRupiah(amount: Long): String {
    val nf = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    nf.maximumFractionDigits = 0
    return nf.format(amount)
}

/**
 * Normalizes phone number to international 628... format for https://wa.me/
 */
fun normalizePhoneToWa(rawPhone: String): String {
    val digitsOnly = rawPhone.filter { it.isDigit() }
    return when {
        digitsOnly.startsWith("0") -> "62" + digitsOnly.drop(1)
        digitsOnly.startsWith("62") -> digitsOnly
        digitsOnly.isNotEmpty() -> "62$digitsOnly"
        else -> ""
    }
}

/**
 * Opens https://wa.me/<628...>?text=... and copies message to clipboard as fallback
 */
fun openWhatsAppChat(
    context: Context,
    rawPhone: String,
    message: String,
    onFeedback: (String) -> Unit
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Pesan WA Wali Siswa", message))

    val normalizedPhone = normalizePhoneToWa(rawPhone)
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
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Kirim Pesan WhatsApp Orang Tua"))
            onFeedback("Nomor HP belum diisi. Pesan telah disalin & siap dibagikan ke WhatsApp.")
        }
    } catch (e: Exception) {
        onFeedback("Pesan WA telah disalin ke papan klip.")
    }
}

/**
 * Builds comprehensive official Madrasah WhatsApp message for a student:
 * - Salam pembuka & nama wali
 * - Identitas siswa (Nama, NIS, Kelas)
 * - Rekapitulasi Kehadiran (Hadir, Terlambat, Izin, Sakit, Alpa)
 * - Catatan Perilaku / Prestasi / Pelanggaran terbaru
 * - Rincian Informasi Administrasi / Tunggakan & SPP (daftar tagihan belum lunas, sisa nominal, total tunggakan)
 */
fun buildStudentComprehensiveWaMessage(
    student: StudentEntity,
    schoolName: String,
    attendance: List<AttendanceEntity>,
    notes: List<StudentNoteEntity>,
    achievements: List<StudentAchievementEntity>,
    violations: List<StudentViolationEntity>,
    arrears: List<ArrearPaymentEntity>,
    senderName: String = "Wali Kelas / Administrasi"
): String {
    val stAtt = attendance.filter { it.studentId == student.id }
    val hadir = stAtt.count { it.status == AttendanceStatus.HADIR }
    val telat = stAtt.count { it.status == AttendanceStatus.TERLAMBAT }
    val izin = stAtt.count { it.status == AttendanceStatus.IZIN }
    val sakit = stAtt.count { it.status == AttendanceStatus.SAKIT }
    val alpa = stAtt.count { it.status == AttendanceStatus.ALPA }

    val stNotes = notes.filter { it.studentId == student.id }
    val stAch = achievements.filter { it.studentId == student.id }
    val stViol = violations.filter { it.studentId == student.id }
    val unpaidArrears = arrears.filter { it.studentId == student.id && it.status != "Lunas" && (it.nominal - it.paidAmount) > 0L }

    val waliLabel = student.parentName.ifBlank { "Orang Tua/Wali Siswa" }

    val sb = StringBuilder()
    sb.appendLine("Assalamu'alaikum Wr. Wb.")
    sb.appendLine("Yth. Bapak/Ibu *$waliLabel*")
    sb.appendLine("Orang Tua/Wali dari ananda *${student.fullName}* (NIS: ${student.nis} - ${student.className})")
    sb.appendLine()
    sb.appendLine("Semoga Bapak/Ibu beserta keluarga senantiasa dalam keadaan sehat wal'afiat.")
    sb.appendLine("Berikut kami sampaikan laporan perkembangan kehadiran, catatan, serta informasi administrasi ananda di *$schoolName*:")
    sb.appendLine()
    sb.appendLine("*1. REKAPITULASI KEHADIRAN*")
    sb.appendLine("• Hadir: $hadir hari")
    sb.appendLine("• Terlambat: $telat hari")
    sb.appendLine("• Izin: $izin hari")
    sb.appendLine("• Sakit: $sakit hari")
    sb.appendLine("• Alpa: $alpa hari")

    if (stNotes.isNotEmpty() || stAch.isNotEmpty() || stViol.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("*2. CATATAN PERKEMBANGAN / PRESTASI / PEMBINAAN*")
        stAch.take(2).forEach { ac ->
            sb.appendLine("• [Prestasi - ${ac.type} (${ac.date})]: ${ac.content}")
        }
        stNotes.take(2).forEach { n ->
            sb.appendLine("• [Catatan - ${n.type} (${n.date})]: ${n.content}")
        }
        stViol.take(2).forEach { v ->
            sb.appendLine("• [Pembinaan - ${v.type} (${v.date})]: ${v.content}")
        }
    }

    sb.appendLine()
    sb.appendLine("*3. INFORMASI ADMINISTRASI / TUNGGAKAN & SPP*")
    if (unpaidArrears.isEmpty()) {
        sb.appendLine("• Alhamdulillah, administrasi pembayaran saat ini berstatus *Lunas / Tidak Ada Tunggakan*.")
    } else {
        var totalSisa = 0L
        unpaidArrears.forEachIndexed { idx, ar ->
            val sisa = (ar.nominal - ar.paidAmount).coerceAtLeast(0L)
            totalSisa += sisa
            sb.appendLine("${idx + 1}. ${ar.billType} (${ar.period}) — Sisa: *${formatRupiah(sisa)}* [${ar.status}]")
        }
        sb.appendLine("• *Total Sisa Tunggakan: ${formatRupiah(totalSisa)}*")
    }

    sb.appendLine()
    sb.appendLine("Demikian informasi ini kami sampaikan. Atas perhatian dan kerja sama Bapak/Ibu, kami ucapkan terima kasih.")
    sb.appendLine("Wassalamu'alaikum Wr. Wb.")
    sb.appendLine()
    sb.append("Hormat kami,\n*$senderName*\n$schoolName")
    return sb.toString()
}

/**
 * Builds official WhatsApp notification for a specific Catatan / Prestasi / Pelanggaran item
 */
fun buildSingleRecordWaMessage(
    student: StudentEntity,
    schoolName: String,
    categoryLabel: String,
    badgeType: String,
    date: String,
    content: String,
    author: String
): String {
    val waliLabel = student.parentName.ifBlank { "Orang Tua/Wali Siswa" }
    return """
Assalamu'alaikum Wr. Wb.
Yth. Bapak/Ibu *$waliLabel*
Orang Tua/Wali dari ananda *${student.fullName}* (NIS: ${student.nis} - ${student.className})

Melalui pesan ini pihak *$schoolName* menyampaikan informasi *$categoryLabel Siswa*:
• Kategori: *$badgeType*
• Tanggal: $date
• Keterangan: "$content"
• Dicatat oleh: $author

Atas perhatian dan kerja sama Bapak/Ibu dalam mendampingi perkembangan ananda, kami ucapkan terima kasih.
Wassalamu'alaikum Wr. Wb.
    """.trimIndent()
}

/**
 * Builds official WhatsApp notification for Guru Piket when a student has not arrived yet (Belum Hadir)
 */
fun buildBelumHadirWaMessage(
    student: StudentEntity,
    schoolName: String,
    date: String,
    unpaidArrears: List<ArrearPaymentEntity> = emptyList()
): String {
    val waliLabel = student.parentName.ifBlank { "Orang Tua/Wali Siswa" }
    val arrearLine = if (unpaidArrears.isNotEmpty()) {
        val totalSisa = unpaidArrears.sumOf { (it.nominal - it.paidAmount).coerceAtLeast(0L) }
        "\n\n*Catatan Administrasi:* Terdapat tagihan belum lunas sejumlah *${formatRupiah(totalSisa)}* (${unpaidArrears.joinToString(", ") { "${it.billType} ${it.period}" }})."
    } else ""

    return """
Assalamu'alaikum Wr. Wb.
Yth. Bapak/Ibu *$waliLabel*
Orang Tua/Wali dari ananda *${student.fullName}* (NIS: ${student.nis} - ${student.className})

Kami dari petugas piket *$schoolName* menginformasikan bahwa pada hari ini (*$date*), ananda *${student.fullName}* tercatat *Belum Hadir* di madrasah.

Mohon konfirmasi dari Bapak/Ibu apabila ananda sedang berhalangan hadir (Sakit / Izin) agar dapat kami catat pada buku absensi harian madrasah.$arrearLine

Terima kasih atas perhatian dan kerja samanya.
Wassalamu'alaikum Wr. Wb.
    """.trimIndent()
}

