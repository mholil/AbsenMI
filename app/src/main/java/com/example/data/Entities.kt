package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

typealias SiswaEntity = StudentEntity
typealias AbsensiEntity = AttendanceEntity

data class SiswaWithAbsensi(
    @Embedded val siswa: StudentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "studentId"
    )
    val riwayatAbsensi: List<AttendanceEntity>
)

object RoleType {
    const val ADMIN = "ADMIN"
    const val WALI_KELAS = "WALI_KELAS"
    const val KEPALA_SEKOLAH = "KEPALA_SEKOLAH"
    const val GURU_PIKET = "GURU_PIKET"

    fun label(role: String): String = when (role) {
        ADMIN -> "Administrator"
        WALI_KELAS -> "Wali Kelas"
        KEPALA_SEKOLAH -> "Kepala Madrasah"
        GURU_PIKET -> "Guru Piket"
        else -> role
    }
}

object AttendanceStatus {
    const val HADIR = "Hadir"
    const val TERLAMBAT = "Terlambat"
    const val IZIN = "Izin"
    const val SAKIT = "Sakit"
    const val ALPA = "Alpa"
    const val BELUM_HADIR = "Belum Hadir"

    val ALL_RECORDED = listOf(HADIR, TERLAMBAT, IZIN, SAKIT, ALPA)
}

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val nip: String = "",
    val role: String, // ADMIN, WALI_KELAS, KEPALA_SEKOLAH, GURU_PIKET
    val assignedClassId: Long? = null,
    val assignedClassName: String = "",
    val passwordHash: String, // SHA-256 hash
    val legacyPassword: String = "", // Supports transparent legacy baseline login migration
    val canAddStudent: Boolean = true,
    val canCorrectAttendance: Boolean = true,
    val isActive: Boolean = true,
    val phone: String = ""
)

@Entity(tableName = "classes")
data class SchoolClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val className: String, // e.g., "Kelas 1A", "Kelas 2A", etc.
    val gradeLevel: Int = 1,
    val homeroomTeacherName: String = "",
    val roomNumber: String = "",
    val classNotes: String = "",
    val classAnnouncement: String = ""
)

@Entity(
    tableName = "students",
    indices = [Index(value = ["nis"], unique = true)]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nis: String,
    val fullName: String,
    val gender: String = "L", // "L" or "P"
    val classId: Long,
    val className: String,
    val parentName: String = "",
    val parentPhone: String = "",
    val address: String = "",
    val birthPlaceDate: String = "",
    val status: String = "Aktif", // "Aktif", "Mutasi", "Arsip", "Lulus"
    val qrCodeValue: String = "" // Defaults to "MI-NIS"
)

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nip: String,
    val fullName: String,
    val position: String, // Guru Kelas, Guru Mapel, Staf TU, Kepala Madrasah, Guru BK
    val subjectOrClass: String = "",
    val phone: String = "",
    val address: String = "",
    val status: String = "Aktif"
)

@Entity(
    tableName = "attendance",
    indices = [Index(value = ["studentId", "date"], unique = true)]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentNis: String,
    val studentName: String,
    val classId: Long,
    val className: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val status: String, // Hadir, Terlambat, Izin, Sakit, Alpa
    val note: String = "",
    val operatorName: String = "",
    val operatorRole: String = "",
    val isCorrected: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "teacher_attendance")
data class TeacherAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teacherId: Long,
    val teacherName: String,
    val nip: String,
    val position: String,
    val date: String, // YYYY-MM-DD
    val timeIn: String,
    val timeOut: String = "-",
    val status: String, // Hadir, Terlambat, Izin, Sakit, Dinas Luar
    val note: String = ""
)

@Entity(tableName = "notes")
data class StudentNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String,
    val classId: Long,
    val className: String,
    val type: String = "Catatan", // Catatan Umum, Perkembangan, Konseling
    val content: String,
    val date: String, // YYYY-MM-DD
    val forwardToBk: Boolean = false,
    val authorName: String = ""
)

@Entity(tableName = "achievements")
data class StudentAchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String,
    val classId: Long,
    val className: String,
    val type: String = "Akademik", // Akademik, Non-Akademik, Tahfidz, Keagamaan
    val content: String,
    val date: String, // YYYY-MM-DD
    val forwardToBk: Boolean = false,
    val authorName: String = ""
)

@Entity(tableName = "violations")
data class StudentViolationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String,
    val classId: Long,
    val className: String,
    val type: String = "Kedisiplinan", // Kedisiplinan, Kerapian, Keterlambatan, Sikap
    val content: String,
    val date: String, // YYYY-MM-DD
    val forwardToBk: Boolean = false,
    val authorName: String = ""
)

@Entity(tableName = "arrears_payments")
data class ArrearPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentNis: String,
    val studentName: String,
    val classId: Long,
    val className: String,
    val billType: String, // Infaq Bulanan, Buku Paket, Seragam, Kegiatan Madrasah, Daftar Ulang
    val nominal: Long, // Total bill in IDR
    val paidAmount: Long = 0L, // Amount paid in IDR
    val period: String, // e.g., "Oktober 2026" or "Semester Ganjil"
    val status: String, // "Belum Lunas", "Cicilan", "Lunas"
    val paymentHistory: String = "", // Formatted payment logs
    val updatedAt: String = ""
)

@Entity(tableName = "school_settings")
data class SchoolSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "MI AL-IKHSAN SURABAYA",
    val nsm: String = "111232010045",
    val npsn: String = "60708912",
    val address: String = "Dupak Masigit I Buntu No.18 Jepara Bubutan Surabaya",
    val phone: String = "031-3522100",
    val email: String = "info@mialikhsan.sch.id",
    val headmasterName: String = "H. Ahmad Fauzi, S.Pd.I, M.Pd",
    val headmasterNip: String = "197504122005011004",
    val timeInSchedule: String = "07:00",
    val lateThresholdTime: String = "07:15",
    val timeOutSchedule: String = "13:30",
    val activeAcademicYear: String = "2026/2027",
    val activeSemester: String = "Ganjil",
    val enableNotifications: Boolean = true,
    val enableAutoLateDetection: Boolean = true,
    val scheduleSummary: String = "Senin-Kamis: 07:00 - 13:30 | Jumat: 07:00 - 11:15 | Sabtu: Ekstrakurikuler Pramuka & Tahfidz"
)

@Entity(tableName = "academic_years")
data class AcademicYearEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearName: String, // e.g. "2026/2027"
    val semester: String, // "Ganjil" / "Genap"
    val isActive: Boolean = false,
    val calendarNotes: String = ""
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: String,
    val actorName: String,
    val actorRole: String,
    val actionTitle: String,
    val description: String
)
