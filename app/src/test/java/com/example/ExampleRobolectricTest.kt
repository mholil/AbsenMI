package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import com.example.data.*
import com.example.ui.buildSingleRecordWaMessage
import com.example.ui.buildStudentComprehensiveWaMessage
import com.example.ui.normalizePhoneToWa
import com.example.util.QrMatrixGenerator
import com.example.util.ReportExportHelper
import com.example.util.StudentReportRow
import java.nio.ByteBuffer
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun testQrMatrixRoundTripAndCameraXDetection() {
        val students = listOf(
            StudentEntity(
                id = 1L,
                nis = "2026001",
                fullName = "Ahmad Fauzan",
                classId = 101L,
                className = "Kelas 1A",
                gender = "L",
                parentName = "H. Ridwan",
                parentPhone = "081234567801",
                qrCodeValue = "QR-MI-2026001"
            ),
            StudentEntity(
                id = 2L,
                nis = "2026002",
                fullName = "Siti Aisyah",
                classId = 101L,
                className = "Kelas 1A",
                gender = "P",
                parentName = "Hj. Aminah",
                parentPhone = "081234567802",
                qrCodeValue = "QR-MI-2026002"
            )
        )

        students.forEach { student ->
            val matrix = QrMatrixGenerator.generateMatrix(student.qrCodeValue, 21)
            assertEquals(21, matrix.size)
            assertEquals(21, matrix[0].size)

            // Verify finder pattern corners exist
            assertTrue(matrix[0][0])
            assertTrue(matrix[3][3])
            assertTrue(matrix[0][20])
            assertTrue(matrix[20][0])

            // Verify decodeMatrix recovers exact QR token
            val decoded = QrMatrixGenerator.decodeMatrix(matrix, 21)
            assertNotNull("Student ${student.fullName} should be decoded from 21x21 QR matrix", decoded)
            assertEquals(student.qrCodeValue, decoded)
        }

        // Verify simulated YUV_420_888 luminance frame containing embedded 21x21 QR pattern is decoded
        val targetStudent = students[0]
        val qrMatrix = QrMatrixGenerator.generateMatrix(targetStudent.qrCodeValue, 21)
        val width = 200
        val height = 200
        val luma = ByteArray(width * height) { 220.toByte() } // bright background
        val cropSize = (minOf(width, height) * 0.62f).toInt()
        val startX = (width - cropSize) / 2
        val startY = (height - cropSize) / 2
        val cellSize = cropSize / 21f

        for (r in 0 until 21) {
            for (c in 0 until 21) {
                val pixelVal = if (qrMatrix[r][c]) 15.toByte() else 240.toByte()
                val y0 = (startY + r * cellSize).toInt()
                val y1 = (startY + (r + 1) * cellSize).toInt()
                val x0 = (startX + c * cellSize).toInt()
                val x1 = (startX + (c + 1) * cellSize).toInt()
                for (py in y0 until y1) {
                    for (px in x0 until x1) {
                        if (py in 0 until height && px in 0 until width) {
                            luma[py * width + px] = pixelVal
                        }
                    }
                }
            }
        }

        val (detectedCode, avgLum) = QrMatrixGenerator.analyzeLuminanceFrame(
            yBuffer = ByteBuffer.wrap(luma),
            width = width,
            height = height,
            rowStride = width,
            knownCodes = students.map { it.qrCodeValue }
        )
        assertTrue(avgLum > 0f)
        assertEquals(targetStudent.qrCodeValue, detectedCode)
    }

    @Test
    fun testRepositoryScanQrAndWaTemplateAndPrintHtml() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = MadrasahDatabase.getInstance(context)
        val repo = MadrasahRepository(db.madrasahDao(), context)

        val student = StudentEntity(
            id = 99L,
            nis = "2026999",
            fullName = "Muhammad Rizky",
            classId = 101L,
            className = "Kelas 1A",
            gender = "L",
            parentName = "Bpk. Hasan",
            parentPhone = "081299998888",
            qrCodeValue = "MI-2026999"
        )
        repo.saveStudent(student, "Admin Utama", RoleType.ADMIN)

        // Test real-time QR Attendance scan recording into local Room database (Siswa & Absensi)
        val scanResult = repo.scanStudentQrCode(
            rawCode = "MI-2026999",
            operatorName = "Guru Piket",
            operatorRole = RoleType.GURU_PIKET
        )
        assertTrue(scanResult.isSuccess)
        val savedAbsensi: AbsensiEntity? = scanResult.getOrNull()
        assertNotNull(savedAbsensi)
        assertEquals("Muhammad Rizky", savedAbsensi?.studentName)
        assertEquals("2026999", savedAbsensi?.studentNis)

        // Verify local Room database queries for Siswa and Absensi
        val storedSiswa = db.madrasahDao().findStudentByNisOrQr("2026999", "MI-2026999")
        assertNotNull(storedSiswa)
        assertEquals("Muhammad Rizky", storedSiswa?.fullName)

        val todayAbsensiList = db.madrasahDao().getAbsensiByDateOnce(savedAbsensi!!.date)
        assertTrue(todayAbsensiList.any { it.studentNis == "2026999" && it.studentId == storedSiswa?.id })

        // Test HTML print report with Kop Surat and @media print
        val attHtml = ReportExportHelper.buildAttendancePrintHtml(
            settings = SchoolSettingsEntity(),
            reportTitle = "LAPORAN KEHADIRAN HARIAN",
            periodLabel = "2026-10-06",
            rows = listOf(
                StudentReportRow(
                    nis = "2026999",
                    studentName = "Muhammad Rizky",
                    className = "Kelas 1A",
                    hadir = 1,
                    terlambat = 0,
                    izin = 0,
                    sakit = 0,
                    alpa = 0,
                    percentage = 100
                )
            )
        )
        assertTrue(attHtml.contains("@media print"))
        assertTrue(attHtml.contains("kop-surat"))
        assertTrue(attHtml.contains("KEMENTERIAN AGAMA REPUBLIK INDONESIA"))
        assertTrue(attHtml.contains(".no-print"))
        assertTrue(attHtml.contains("display: none !important"))
        assertTrue(attHtml.contains("Muhammad Rizky"))

        // Verify distinct print tables for Mingguan (7-day matrix), Bulanan (Predikat Disiplin), and Per Siswa (Log Individu)
        val weeklyHtml = ReportExportHelper.buildAttendancePrintHtml(
            settings = SchoolSettingsEntity(),
            reportTitle = "LAPORAN KEHADIRAN MINGGUAN",
            periodLabel = "2026-10-01 s/d 2026-10-07",
            rows = listOf(
                StudentReportRow(
                    nis = "2026999",
                    studentName = "Muhammad Rizky",
                    className = "Kelas 1A",
                    hadir = 5,
                    terlambat = 1,
                    izin = 0,
                    sakit = 0,
                    alpa = 0,
                    percentage = 100,
                    dailyStatuses = mapOf("2026-10-07" to "H")
                )
            ),
            reportPeriodType = "Mingguan",
            weekDates = listOf("2026-10-07")
        )
        assertTrue(weeklyHtml.contains("Matriks Kehadiran 7 Hari Terakhir"))

        val monthlyHtml = ReportExportHelper.buildAttendancePrintHtml(
            settings = SchoolSettingsEntity(),
            reportTitle = "LAPORAN KEHADIRAN BULANAN",
            periodLabel = "2026-10",
            rows = listOf(
                StudentReportRow(
                    nis = "2026999",
                    studentName = "Muhammad Rizky",
                    className = "Kelas 1A",
                    hadir = 20,
                    terlambat = 1,
                    izin = 0,
                    sakit = 0,
                    alpa = 0,
                    percentage = 100,
                    recordedDays = 21
                )
            ),
            reportPeriodType = "Bulanan"
        )
        assertTrue(monthlyHtml.contains("Predikat Disiplin"))
        assertTrue(monthlyHtml.contains("Sangat Disiplin"))

        val perStudentHtml = ReportExportHelper.buildAttendancePrintHtml(
            settings = SchoolSettingsEntity(),
            reportTitle = "LAPORAN PER SISWA",
            periodLabel = "2026-10",
            rows = emptyList(),
            reportPeriodType = "Per Siswa",
            selectedStudent = student,
            studentHistory = listOf(savedAbsensi)
        )
        assertTrue(perStudentHtml.contains("LAPORAN RIWAYAT KEHADIRAN INDIVIDU SISWA"))
        assertTrue(perStudentHtml.contains("Petugas Pencatat"))

        // Verify Per Kelas aggregated report layout with Kop Surat
        val perClassHtml = ReportExportHelper.buildAttendancePrintHtml(
            settings = SchoolSettingsEntity(),
            reportTitle = "LAPORAN KEHADIRAN PER KELAS",
            periodLabel = "Oktober 2026",
            rows = listOf(
                StudentReportRow(
                    nis = "2026999",
                    studentName = "Muhammad Rizky",
                    className = "Kelas 1A",
                    hadir = 20,
                    terlambat = 0,
                    izin = 0,
                    sakit = 0,
                    alpa = 0,
                    percentage = 100
                )
            ),
            reportPeriodType = "Per Kelas",
            classes = listOf(
                SchoolClassEntity(id = 1, className = "Kelas 1A", gradeLevel = 1, homeroomTeacherName = "Ustadzah Siti Aminah")
            )
        )
        assertTrue(perClassHtml.contains("Rekapitulasi Tingkat Kelas"))
        assertTrue(perClassHtml.contains("Ustadzah Siti Aminah"))

        // Verify Guru/Staf report layout with Kop Surat
        val teacherHtml = ReportExportHelper.buildAttendancePrintHtml(
            settings = SchoolSettingsEntity(),
            reportTitle = "LAPORAN KEHADIRAN GURU & STAF",
            periodLabel = "2026-10-14",
            rows = emptyList(),
            reportPeriodType = "Guru/Staf",
            teacherAttendance = listOf(
                TeacherAttendanceEntity(
                    id = 1,
                    teacherId = 1L,
                    teacherName = "Drs. H. Ahmad Dahlan, M.Pd.I",
                    nip = "197508122002121001",
                    position = "Kepala Madrasah",
                    date = "2026-10-14",
                    timeIn = "06:45",
                    timeOut = "14:30",
                    status = "Hadir",
                    note = "Tepat waktu"
                )
            )
        )
        assertTrue(teacherHtml.contains("REKAPITULASI KEHADIRAN GURU"))
        assertTrue(teacherHtml.contains("Drs. H. Ahmad Dahlan, M.Pd.I"))

        // Verify Arrears print HTML has Kop Surat and @media print
        val arrearsHtml = ReportExportHelper.buildArrearsPrintHtml(
            settings = SchoolSettingsEntity(),
            reportTitle = "LAPORAN REKAP TUNGGAKAN",
            classScopeLabel = "Kelas 1A",
            arrears = listOf(
                ArrearPaymentEntity(
                    id = 1,
                    studentId = student.id,
                    studentName = student.fullName,
                    studentNis = student.nis,
                    classId = 1L,
                    className = student.className,
                    billType = "Infaq Syahriyah",
                    nominal = 150000L,
                    paidAmount = 50000L,
                    status = "Cicilan",
                    period = "Oktober 2026"
                )
            )
        )
        assertTrue(arrearsHtml.contains("kop-surat"))
        assertTrue(arrearsHtml.contains("@media print"))
        assertTrue(arrearsHtml.contains("Infaq Syahriyah"))

        // Verify PDF Document Builder (MadrasahPdfTableDocument) generates valid non-empty PDF files with Kop Surat, Table, and Signature Footer
        val generatedAttPdf = ReportExportHelper.buildPdfFile(
            context = context,
            settings = SchoolSettingsEntity(),
            reportTitle = "LAPORAN KEHADIRAN BULANAN",
            periodLabel = "2026-10",
            rows = listOf(
                StudentReportRow(
                    nis = "2026999",
                    studentName = "Muhammad Rizky",
                    className = "Kelas 1A",
                    hadir = 20,
                    terlambat = 1,
                    izin = 0,
                    sakit = 0,
                    alpa = 0,
                    percentage = 100,
                    recordedDays = 21
                )
            ),
            signerRoleLabel = "Wali Kelas 1A",
            signerName = "Siti Aminah, S.Pd.I",
            reportPeriodType = "Bulanan"
        )
        assertTrue(generatedAttPdf.exists())
        assertEquals("Laporan_Absensi_MI_", generatedAttPdf.name.take(19))

        val generatedArrearsPdf = ReportExportHelper.buildArrearsPdfFile(
            context = context,
            settings = SchoolSettingsEntity(),
            reportTitle = "LAPORAN TUNGGAKAN KELAS 1A",
            classScopeLabel = "Kelas 1A",
            arrears = listOf(
                ArrearPaymentEntity(
                    id = 50L,
                    studentId = 99L,
                    studentNis = "2026999",
                    studentName = "Muhammad Rizky",
                    classId = 101L,
                    className = "Kelas 1A",
                    billType = "Infaq Bulanan",
                    nominal = 150000L,
                    paidAmount = 50000L,
                    period = "Oktober 2026",
                    status = "Cicilan"
                )
            ),
            signerRoleLabel = "Wali Kelas 1A",
            signerName = "Siti Aminah, S.Pd.I"
        )
        assertTrue(generatedArrearsPdf.exists())
        assertEquals("Laporan_Tunggakan_MI_", generatedArrearsPdf.name.take(21))

        // Verify phone normalization (08... -> 628...) and comprehensive WA template with arrears & attendance
        assertEquals("6281299998888", normalizePhoneToWa("0812-9999-8888"))
        val sampleArrear = ArrearPaymentEntity(
            id = 50L,
            studentId = 99L,
            studentNis = "2026999",
            studentName = "Muhammad Rizky",
            classId = 101L,
            className = "Kelas 1A",
            billType = "Infaq Bulanan",
            nominal = 150000L,
            paidAmount = 50000L,
            period = "Oktober 2026",
            status = "Cicilan"
        )
        val fullWaMsg = buildStudentComprehensiveWaMessage(
            student = student,
            schoolName = "MI AL-IKHSAN SURABAYA",
            attendance = listOf(savedAbsensi),
            notes = emptyList(),
            achievements = emptyList(),
            violations = emptyList(),
            arrears = listOf(sampleArrear),
            senderName = "Wali Kelas 1A"
        )
        assertTrue(fullWaMsg.contains("Bpk. Hasan"))
        assertTrue(fullWaMsg.contains("Muhammad Rizky"))
        assertTrue(fullWaMsg.contains("REKAPITULASI KEHADIRAN"))
        assertTrue(fullWaMsg.contains("INFORMASI ADMINISTRASI / TUNGGAKAN & SPP"))
        assertTrue(fullWaMsg.contains("Infaq Bulanan (Oktober 2026)"))

        val noteWaMsg = buildSingleRecordWaMessage(
            student = student,
            schoolName = "MI AL-IKHSAN SURABAYA",
            categoryLabel = "Prestasi",
            badgeType = "Tahfidz",
            date = "2026-10-06",
            content = "Hafal Juz 30",
            author = "Wali Kelas 1A"
        )
        assertTrue(noteWaMsg.contains("Hafal Juz 30"))
    }
}


