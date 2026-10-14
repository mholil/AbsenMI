package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MadrasahRepository(
    private val dao: MadrasahDao,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("absensi_mi_session", Context.MODE_PRIVATE)

    val usersFlow: Flow<List<UserEntity>> = dao.observeUsers()
    val classesFlow: Flow<List<SchoolClassEntity>> = dao.observeClasses()
    val studentsFlow: Flow<List<StudentEntity>> = dao.observeStudents()
    val siswaFlow: Flow<List<SiswaEntity>> = dao.observeStudents()
    val teachersFlow: Flow<List<TeacherEntity>> = dao.observeTeachers()
    val attendanceFlow: Flow<List<AttendanceEntity>> = dao.observeAttendance()
    val absensiFlow: Flow<List<AbsensiEntity>> = dao.observeAttendance()
    val siswaWithAbsensiFlow: Flow<List<SiswaWithAbsensi>> = dao.observeAllSiswaWithAbsensi()

    fun observeAbsensiByDate(date: String): Flow<List<AbsensiEntity>> = dao.observeAbsensiByDate(date)
    fun observeAbsensiBySiswa(studentId: Long): Flow<List<AbsensiEntity>> = dao.observeAbsensiBySiswa(studentId)
    fun observeSiswaWithAbsensi(studentId: Long): Flow<SiswaWithAbsensi?> = dao.observeSiswaWithAbsensi(studentId)
    val teacherAttendanceFlow: Flow<List<TeacherAttendanceEntity>> = dao.observeTeacherAttendance()
    val notesFlow: Flow<List<StudentNoteEntity>> = dao.observeNotes()
    val achievementsFlow: Flow<List<StudentAchievementEntity>> = dao.observeAchievements()
    val violationsFlow: Flow<List<StudentViolationEntity>> = dao.observeViolations()
    val arrearsFlow: Flow<List<ArrearPaymentEntity>> = dao.observeArrears()
    val settingsFlow: Flow<SchoolSettingsEntity?> = dao.observeSettings()
    val academicYearsFlow: Flow<List<AcademicYearEntity>> = dao.observeAcademicYears()
    val activityLogsFlow: Flow<List<ActivityLogEntity>> = dao.observeActivityLogs()

    fun getSavedSessionUserId(): Long = prefs.getLong("active_user_id", -1L)

    fun saveSessionUserId(userId: Long) {
        prefs.edit().putLong("active_user_id", userId).apply()
    }

    fun clearSession() {
        prefs.edit().remove("active_user_id").apply()
    }

    companion object {
        val DEFAULT_WA_ARREAR_TEMPLATE = """
Assalamu'alaikum Wr. Wb.
Yth. Bapak/Ibu *{nama_wali}*
Orang Tua/Wali dari ananda *{nama_siswa}* (NIS: {nis} - {kelas})

Semoga Bapak/Ibu beserta keluarga senantiasa dalam keadaan sehat wal'afiat.

Melalui pesan ini, kami dari *{sekolah}* menyampaikan informasi administrasi keuangan madrasah untuk ananda:
• Nama Siswa: *{nama_siswa}* ({kelas})
• Jenis Tagihan: *{jenis_tagihan}*
• Periode Tagihan: *{periode}*
• Nominal Tagihan: *{nominal}*
• Sudah Dibayar: *{dibayar}*
• Sisa Tunggakan: *{sisa_tunggakan}*
• Status: *{status}*

Mohon kiranya Bapak/Ibu dapat melakukan penyelesaian administrasi melalui Tata Usaha (TU) Madrasah atau menghubungi Wali Kelas. Apabila pembayaran telah dilakukan, mohon abaikan pesan ini atau konfirmasikan bukti pembayaran.

Atas perhatian dan kerja sama Bapak/Ibu, kami ucapkan terima kasih. Jazakumullahu Khairan Katsiran.
Wassalamu'alaikum Wr. Wb.

Hormat kami,
Wali {kelas} — *{wali_kelas}*
        """.trimIndent()

        val PRESET_WA_ARREAR_RINGKAS = """
Assalamu'alaikum Wr. Wb. Bapak/Ibu *{nama_wali}*,
Kami menginformasikan tagihan administrasi *{sekolah}* untuk ananda *{nama_siswa}* ({kelas}):
- Tagihan: *{jenis_tagihan}*
- Periode: *{periode}*
- Nominal: *{nominal}* (Sisa Tunggakan: *{sisa_tunggakan}*)

Mohon kesediaannya untuk dapat diselesaikan melalui TU Madrasah / Wali Kelas. Terima kasih.
Wassalamu'alaikum Wr. Wb. — *{wali_kelas}*
        """.trimIndent()

        val PRESET_WA_ARREAR_KONFIRMASI = """
Assalamu'alaikum Wr. Wb. Yth. Bapak/Ibu *{nama_wali}*,
Berikut rincian status pembayaran ananda *{nama_siswa}* (NIS: {nis} - {kelas}) di *{sekolah}*:
• Tagihan: *{jenis_tagihan}*
• Periode: *{periode}*
• Total Nominal: *{nominal}*
• Telah Dibayar: *{dibayar}*
• Sisa Tunggakan: *{sisa_tunggakan}* ({status})

Terima kasih atas kerja sama Bapak/Ibu.
Hormat kami, Wali {kelas} (*{wali_kelas}*)
        """.trimIndent()
    }

    fun getWaArrearTemplate(classId: Long?): String {
        val key = "wa_arrear_template_${classId ?: 0L}"
        return prefs.getString(key, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_WA_ARREAR_TEMPLATE
    }

    fun saveWaArrearTemplate(classId: Long?, template: String) {
        val key = "wa_arrear_template_${classId ?: 0L}"
        prefs.edit().putString(key, template.trim()).apply()
    }

    fun resetWaArrearTemplate(classId: Long?): String {
        val key = "wa_arrear_template_${classId ?: 0L}"
        prefs.edit().remove(key).apply()
        return DEFAULT_WA_ARREAR_TEMPLATE
    }

    fun currentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun currentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    fun currentTimestampString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    suspend fun ensureInitialBaselineData() {
        val existingSettings = dao.getSettingsOnce()
        if (existingSettings == null) {
            dao.saveSettings(SchoolSettingsEntity())
        } else if (existingSettings.schoolName == "MI Al-Hidayah Nusantara" || existingSettings.address.contains("Cibinong")) {
            dao.saveSettings(
                existingSettings.copy(
                    schoolName = "MI AL-IKHSAN SURABAYA",
                    address = "Dupak Masigit I Buntu No.18 Jepara Bubutan Surabaya",
                    email = if (existingSettings.email == "info@mialhidayah.sch.id") "info@mialikhsan.sch.id" else existingSettings.email
                )
            )
        }

        val existingYears = dao.getAllAcademicYearsOnce()
        if (existingYears.isEmpty()) {
            dao.insertAcademicYear(
                AcademicYearEntity(
                    yearName = "2026/2027",
                    semester = "Ganjil",
                    isActive = true,
                    calendarNotes = "14 Jul 2026: Hari Pertama Masuk Madrasah\n17 Agu 2026: Upacara HUT RI\n28 Sep - 3 Okt 2026: Penilaian Tengah Semester (PTS)\n14 - 19 Des 2026: Penilaian Akhir Semester (PAS)"
                )
            )
            dao.insertAcademicYear(
                AcademicYearEntity(
                    yearName = "2025/2026",
                    semester = "Genap",
                    isActive = false,
                    calendarNotes = "Arsip Tahun Ajaran 2025/2026 Semester Genap"
                )
            )
        }

        val existingClasses = dao.getAllClassesOnce()
        val classMap = mutableMapOf<String, Long>()
        if (existingClasses.isEmpty()) {
            val initialClasses = listOf(
                SchoolClassEntity(
                    className = "Kelas 1A",
                    gradeLevel = 1,
                    homeroomTeacherName = "Siti Aminah, S.Pd.I",
                    roomNumber = "R-101",
                    classNotes = "Fokus pembiasaan sholat dhuha dan baca tulis Al-Qur'an (Iqra).",
                    classAnnouncement = "Harap membawa buku prestasi Iqra setiap hari Senin-Kamis."
                ),
                SchoolClassEntity(
                    className = "Kelas 2A",
                    gradeLevel = 2,
                    homeroomTeacherName = "Budi Santoso, S.Pd",
                    roomNumber = "R-102",
                    classNotes = "Target hafalan Juz Amma surat pendek An-Nas s.d Ad-Dhuha.",
                    classAnnouncement = "Jadwal olahraga setiap hari Rabu pagi."
                ),
                SchoolClassEntity(
                    className = "Kelas 3A",
                    gradeLevel = 3,
                    homeroomTeacherName = "Nurul Hidayah, S.Ag",
                    roomNumber = "R-103",
                    classNotes = "Pembiasaan sholat dzuhur berjamaah di musholla madrasah.",
                    classAnnouncement = "Pengumpulan tugas Bahasa Arab maksimal hari Jumat."
                ),
                SchoolClassEntity(
                    className = "Kelas 4A",
                    gradeLevel = 4,
                    homeroomTeacherName = "Lukman Hakim, S.Pd.I",
                    roomNumber = "R-201",
                    classNotes = "Persiapan ekstrakurikuler Pramuka Penggalang.",
                    classAnnouncement = "Sabtu depan latihan gabungan Pramuka."
                ),
                SchoolClassEntity(
                    className = "Kelas 5A",
                    gradeLevel = 5,
                    homeroomTeacherName = "Dewi Sartika, M.Pd",
                    roomNumber = "R-202",
                    classNotes = "Pendalaman materi ANBK Literasi & Numerasi MI.",
                    classAnnouncement = "Simulasi ANBK dilaksanakan di ruang lab komputer."
                ),
                SchoolClassEntity(
                    className = "Kelas 6A",
                    gradeLevel = 6,
                    homeroomTeacherName = "Rizky Pratama, S.Pd",
                    roomNumber = "R-203",
                    classNotes = "Persiapan Ujian Madrasah dan Wisuda Tahfidz Juz 30.",
                    classAnnouncement = "Bimbingan belajar tambahan setiap Selasa & Kamis pukul 13:45."
                )
            )
            for (c in initialClasses) {
                val id = dao.insertClass(c)
                classMap[c.className] = id
            }
        } else {
            existingClasses.forEach { classMap[it.className] = it.id }
        }

        val existingUsers = dao.getAllUsersOnce()
        if (existingUsers.isEmpty()) {
            val kelas1AId = classMap["Kelas 1A"] ?: 1L
            val kelas2AId = classMap["Kelas 2A"] ?: 2L
            val initialUsers = listOf(
                UserEntity(
                    username = "admin",
                    fullName = "H. Muhammad Kholil, S.Kom",
                    nip = "198505132010011001",
                    role = RoleType.ADMIN,
                    assignedClassId = null,
                    assignedClassName = "Seluruh Madrasah",
                    passwordHash = PasswordSecurity.sha256("admin123"),
                    legacyPassword = "",
                    canAddStudent = true,
                    canCorrectAttendance = true,
                    phone = "081234567801"
                ),
                UserEntity(
                    username = "kepsek",
                    fullName = "H. Ahmad Fauzi, S.Pd.I, M.Pd",
                    nip = "197504122005011004",
                    role = RoleType.KEPALA_SEKOLAH,
                    assignedClassId = null,
                    assignedClassName = "Seluruh Madrasah",
                    passwordHash = PasswordSecurity.sha256("kepsek123"),
                    legacyPassword = "",
                    canAddStudent = false,
                    canCorrectAttendance = false,
                    phone = "081234567802"
                ),
                UserEntity(
                    username = "wali1a",
                    fullName = "Siti Aminah, S.Pd.I",
                    nip = "198809212015032002",
                    role = RoleType.WALI_KELAS,
                    assignedClassId = kelas1AId,
                    assignedClassName = "Kelas 1A",
                    passwordHash = PasswordSecurity.sha256("wali123"),
                    legacyPassword = "",
                    canAddStudent = true,
                    canCorrectAttendance = true,
                    phone = "081234567803"
                ),
                UserEntity(
                    username = "wali2a",
                    fullName = "Budi Santoso, S.Pd",
                    nip = "198911142016011005",
                    role = RoleType.WALI_KELAS,
                    assignedClassId = kelas2AId,
                    assignedClassName = "Kelas 2A",
                    passwordHash = PasswordSecurity.sha256("wali123"),
                    legacyPassword = "",
                    canAddStudent = true,
                    canCorrectAttendance = true,
                    phone = "081234567804"
                ),
                UserEntity(
                    username = "piket",
                    fullName = "Ust. Hasan Basri, S.Pd.I",
                    nip = "199103082018011007",
                    role = RoleType.GURU_PIKET,
                    assignedClassId = null,
                    assignedClassName = "Operator Piket Harian",
                    passwordHash = PasswordSecurity.sha256("piket123"),
                    legacyPassword = "",
                    canAddStudent = false,
                    canCorrectAttendance = true,
                    phone = "081234567805"
                )
            )
            initialUsers.forEach { dao.insertUser(it) }
        }

        val existingTeachers = dao.getAllTeachersOnce()
        if (existingTeachers.isEmpty()) {
            val initialTeachers = listOf(
                TeacherEntity(
                    nip = "197504122005011004",
                    fullName = "H. Ahmad Fauzi, S.Pd.I, M.Pd",
                    position = "Kepala Madrasah",
                    subjectOrClass = "Manajemen & Supervisi",
                    phone = "081234567802",
                    address = "Jl. KH. Hasyim Ashari No. 12"
                ),
                TeacherEntity(
                    nip = "198809212015032002",
                    fullName = "Siti Aminah, S.Pd.I",
                    position = "Wali Kelas",
                    subjectOrClass = "Kelas 1A",
                    phone = "081234567803",
                    address = "Perum Griya Asri Blok B4"
                ),
                TeacherEntity(
                    nip = "198911142016011005",
                    fullName = "Budi Santoso, S.Pd",
                    position = "Wali Kelas",
                    subjectOrClass = "Kelas 2A",
                    phone = "081234567804",
                    address = "Jl. Melati Indah No. 8"
                ),
                TeacherEntity(
                    nip = "199007192017012003",
                    fullName = "Nurul Hidayah, S.Ag",
                    position = "Wali Kelas",
                    subjectOrClass = "Kelas 3A",
                    phone = "081234567806",
                    address = "Jl. Pesantren Al-Hikmah No. 3"
                ),
                TeacherEntity(
                    nip = "199103082018011007",
                    fullName = "Ust. Hasan Basri, S.Pd.I",
                    position = "Guru Mapel / Piket",
                    subjectOrClass = "Al-Qur'an Hadits & Fiqih",
                    phone = "081234567805",
                    address = "Kp. Baru RT 04/02"
                ),
                TeacherEntity(
                    nip = "199306122019032009",
                    fullName = "Hj. Fatimah Zahra, S.Psi",
                    position = "Guru BK",
                    subjectOrClass = "Bimbingan Konseling Siswa",
                    phone = "081234567809",
                    address = "Jl. Cempaka Putih No. 21"
                )
            )
            val today = currentDateString()
            initialTeachers.forEach { t ->
                val tId = dao.insertTeacher(t)
                dao.insertTeacherAttendance(
                    TeacherAttendanceEntity(
                        teacherId = tId,
                        teacherName = t.fullName,
                        nip = t.nip,
                        position = t.position,
                        date = today,
                        timeIn = "06:48",
                        timeOut = "13:45",
                        status = AttendanceStatus.HADIR,
                        note = "Hadir tepat waktu"
                    )
                )
            }
        }

        val existingStudents = dao.getAllStudentsOnce()
        if (existingStudents.isEmpty()) {
            val k1A = classMap["Kelas 1A"] ?: 1L
            val k2A = classMap["Kelas 2A"] ?: 2L
            val k3A = classMap["Kelas 3A"] ?: 3L
            val k4A = classMap["Kelas 4A"] ?: 4L
            val k5A = classMap["Kelas 5A"] ?: 5L
            val k6A = classMap["Kelas 6A"] ?: 6L

            val initialStudents = listOf(
                StudentEntity(nis = "1101", fullName = "Ahmad Zaki Mubarak", gender = "L", classId = k1A, className = "Kelas 1A", parentName = "H. Ridwan", parentPhone = "08131001001", address = "Jl. Mawar RT 01/02", birthPlaceDate = "Bogor, 12 Mei 2019", qrCodeValue = "MI-1101"),
                StudentEntity(nis = "1102", fullName = "Aisyah Humaira Putri", gender = "P", classId = k1A, className = "Kelas 1A", parentName = "Syamsul Bahri", parentPhone = "08131001002", address = "Jl. Kenanga No. 14", birthPlaceDate = "Bogor, 03 Agu 2019", qrCodeValue = "MI-1102"),
                StudentEntity(nis = "1103", fullName = "Muhammad Al-Fatih", gender = "L", classId = k1A, className = "Kelas 1A", parentName = "Abdul Karim", parentPhone = "08131001003", address = "Perum Bukit Asri A2", birthPlaceDate = "Depok, 21 Jan 2019", qrCodeValue = "MI-1103"),
                StudentEntity(nis = "1104", fullName = "Khadijah Azzahra", gender = "P", classId = k1A, className = "Kelas 1A", parentName = "Lukmanul Hakim", parentPhone = "08131001004", address = "Kp. Sawah RT 03/01", birthPlaceDate = "Bogor, 18 Sep 2019", qrCodeValue = "MI-1104"),
                StudentEntity(nis = "1105", fullName = "Umar Faruq Ibrahim", gender = "L", classId = k1A, className = "Kelas 1A", parentName = "Ibrahim Khalil", parentPhone = "08131001005", address = "Jl. Anggrek No. 7", birthPlaceDate = "Bogor, 09 Nov 2019", qrCodeValue = "MI-1105"),
                StudentEntity(nis = "2101", fullName = "Bilal Ramadhan", gender = "L", classId = k2A, className = "Kelas 2A", parentName = "M. Yusuf", parentPhone = "08132001001", address = "Jl. Dahlia No. 11", birthPlaceDate = "Bogor, 15 Jun 2018", qrCodeValue = "MI-2101"),
                StudentEntity(nis = "2102", fullName = "Fatimah Az-Zahra", gender = "P", classId = k2A, className = "Kelas 2A", parentName = "Zainal Abidin", parentPhone = "08132001002", address = "Perum Taman Melati C5", birthPlaceDate = "Jakarta, 28 Feb 2018", qrCodeValue = "MI-2102"),
                StudentEntity(nis = "2103", fullName = "Naufal Dzaki Pratama", gender = "L", classId = k2A, className = "Kelas 2A", parentName = "Hendra Pratama", parentPhone = "08132001003", address = "Jl. Flamboyan No. 19", birthPlaceDate = "Bogor, 04 Jul 2018", qrCodeValue = "MI-2103"),
                StudentEntity(nis = "3101", fullName = "Salman Al-Farisi", gender = "L", classId = k3A, className = "Kelas 3A", parentName = "Munawar", parentPhone = "08133001001", address = "Kp. Kramat RT 02/04", birthPlaceDate = "Bogor, 10 Okt 2017", qrCodeValue = "MI-3101"),
                StudentEntity(nis = "3102", fullName = "Maryam Salsabila", gender = "P", classId = k3A, className = "Kelas 3A", parentName = "Fikri Haikal", parentPhone = "08133001002", address = "Jl. Cendana No. 5", birthPlaceDate = "Bogor, 22 Des 2017", qrCodeValue = "MI-3102"),
                StudentEntity(nis = "4101", fullName = "Rafa Aditya Rahman", gender = "L", classId = k4A, className = "Kelas 4A", parentName = "Arief Rahman", parentPhone = "08134001001", address = "Jl. Merpati No. 32", birthPlaceDate = "Bogor, 14 Mar 2016", qrCodeValue = "MI-4101"),
                StudentEntity(nis = "5101", fullName = "Zahra Amira Husna", gender = "P", classId = k5A, className = "Kelas 5A", parentName = "Husni Thamrin", parentPhone = "08135001001", address = "Perum Grand Madani D9", birthPlaceDate = "Bogor, 07 Apr 2015", qrCodeValue = "MI-5101"),
                StudentEntity(nis = "6101", fullName = "Rayhan Hafidz Al-Qarni", gender = "L", classId = k6A, className = "Kelas 6A", parentName = "H. Qomaruddin", parentPhone = "08136001001", address = "Jl. Raya Madrasah No. 8", birthPlaceDate = "Bogor, 19 Jan 2014", qrCodeValue = "MI-6101")
            )

            val today = currentDateString()
            val insertedIds = mutableListOf<Pair<Long, StudentEntity>>()
            for (s in initialStudents) {
                val id = dao.insertStudent(s)
                insertedIds.add(id to s.copy(id = id))
            }

            // Seed initial attendance today for most students, leaving a couple "Belum Hadir" for Guru Piket testing
            val initialStatuses = listOf(
                Triple(0, AttendanceStatus.HADIR, "06:50" to "Scan QR tepat waktu"),
                Triple(1, AttendanceStatus.HADIR, "06:54" to "Scan QR tepat waktu"),
                Triple(2, AttendanceStatus.TERLAMBAT, "07:22" to "Terlambat karena hujan deras"),
                Triple(3, AttendanceStatus.IZIN, "07:05" to "Izin acara keluarga (Surat orang tua)"),
                Triple(5, AttendanceStatus.HADIR, "06:52" to "Scan QR tepat waktu"),
                Triple(6, AttendanceStatus.SAKIT, "07:10" to "Demam, istirahat di rumah"),
                Triple(8, AttendanceStatus.HADIR, "06:49" to "Scan QR tepat waktu"),
                Triple(9, AttendanceStatus.TERLAMBAT, "07:28" to "Ban sepeda bocor di jalan"),
                Triple(10, AttendanceStatus.HADIR, "06:55" to "Scan QR tepat waktu"),
                Triple(11, AttendanceStatus.ALPA, "07:35" to "Belum ada keterangan wali")
            )
            for ((index, status, timeNote) in initialStatuses) {
                if (index < insertedIds.size) {
                    val (sId, st) = insertedIds[index]
                    dao.insertAttendance(
                        AttendanceEntity(
                            studentId = sId,
                            studentNis = st.nis,
                            studentName = st.fullName,
                            classId = st.classId,
                            className = st.className,
                            date = today,
                            time = timeNote.first,
                            status = status,
                            note = timeNote.second,
                            operatorName = "Ust. Hasan Basri, S.Pd.I",
                            operatorRole = RoleType.GURU_PIKET
                        )
                    )
                }
            }

            // Seed multi-day historical attendance (H-1 to H-6) so Laporan Mingguan, Bulanan, and Per Siswa have realistic multi-day records
            val sdfHist = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsedToday = try { sdfHist.parse(today) } catch (_: Exception) { null }
            if (parsedToday != null) {
                for (dayOffset in 1..6) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = parsedToday
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -dayOffset)
                    val histDate = sdfHist.format(cal.time)
                    insertedIds.forEachIndexed { idx, (sId, st) ->
                        val histStatus = when ((idx + dayOffset) % 9) {
                            2 -> AttendanceStatus.TERLAMBAT
                            5 -> AttendanceStatus.IZIN
                            7 -> AttendanceStatus.SAKIT
                            else -> AttendanceStatus.HADIR
                        }
                        val histTime = when (histStatus) {
                            AttendanceStatus.TERLAMBAT -> "07:2${(idx % 6) + 1}"
                            AttendanceStatus.IZIN, AttendanceStatus.SAKIT -> "07:05"
                            else -> "06:4${(idx % 9) + 1}"
                        }
                        val histNote = when (histStatus) {
                            AttendanceStatus.TERLAMBAT -> "Terlambat hadir pagi"
                            AttendanceStatus.IZIN -> "Izin keperluan keluarga"
                            AttendanceStatus.SAKIT -> "Sakit (Konfirmasi orang tua)"
                            else -> "Scan QR tepat waktu"
                        }
                        dao.insertAttendance(
                            AttendanceEntity(
                                studentId = sId,
                                studentNis = st.nis,
                                studentName = st.fullName,
                                classId = st.classId,
                                className = st.className,
                                date = histDate,
                                time = histTime,
                                status = histStatus,
                                note = histNote,
                                operatorName = "Ust. Hasan Basri, S.Pd.I",
                                operatorRole = RoleType.GURU_PIKET
                            )
                        )
                    }
                }
            }

            // Seed sample notes, achievements, violations, and arrears
            if (insertedIds.isNotEmpty()) {
                val s1 = insertedIds[0].second
                val s2 = insertedIds[1].second
                val s3 = insertedIds[2].second
                val s6 = insertedIds[5].second

                dao.insertNote(
                    StudentNoteEntity(
                        studentId = s1.id,
                        studentName = s1.fullName,
                        classId = s1.classId,
                        className = s1.className,
                        type = "Perkembangan Tahfidz",
                        content = "Alhamdulillah bacaan tajwid dan makhraj huruf meningkat pesat, sudah lancar surat Al-Ghasyiyah.",
                        date = today,
                        forwardToBk = false,
                        authorName = "Siti Aminah, S.Pd.I"
                    )
                )
                dao.insertAchievement(
                    StudentAchievementEntity(
                        studentId = s2.id,
                        studentName = s2.fullName,
                        classId = s2.classId,
                        className = s2.className,
                        type = "Tahfidz & Keagamaan",
                        content = "Juara 1 Lomba Tahfidz Juz 30 Tingkat Kecamatan MI.",
                        date = today,
                        forwardToBk = false,
                        authorName = "Siti Aminah, S.Pd.I"
                    )
                )
                dao.insertViolation(
                    StudentViolationEntity(
                        studentId = s3.id,
                        studentName = s3.fullName,
                        classId = s3.classId,
                        className = s3.className,
                        type = "Kedisiplinan",
                        content = "Datang terlambat lebih dari 15 menit dan atribut topi belum lengkap.",
                        date = today,
                        forwardToBk = true,
                        authorName = "Ust. Hasan Basri, S.Pd.I"
                    )
                )

                dao.insertArrear(
                    ArrearPaymentEntity(
                        studentId = s1.id,
                        studentNis = s1.nis,
                        studentName = s1.fullName,
                        classId = s1.classId,
                        className = s1.className,
                        billType = "Infaq Syahriyah Bulanan",
                        nominal = 150000L,
                        paidAmount = 150000L,
                        period = "Oktober 2026",
                        status = "Lunas",
                        paymentHistory = "02 Okt 2026: Dibayar lunas Rp 150.000 via TU",
                        updatedAt = today
                    )
                )
                dao.insertArrear(
                    ArrearPaymentEntity(
                        studentId = s3.id,
                        studentNis = s3.nis,
                        studentName = s3.fullName,
                        classId = s3.classId,
                        className = s3.className,
                        billType = "Buku Paket Semester Ganjil",
                        nominal = 250000L,
                        paidAmount = 100000L,
                        period = "Semester Ganjil 2026/2027",
                        status = "Cicilan",
                        paymentHistory = "15 Sep 2026: Angsuran 1 Rp 100.000",
                        updatedAt = today
                    )
                )
                dao.insertArrear(
                    ArrearPaymentEntity(
                        studentId = s6.id,
                        studentNis = s6.nis,
                        studentName = s6.fullName,
                        classId = s6.classId,
                        className = s6.className,
                        billType = "Infaq Syahriyah Bulanan",
                        nominal = 150000L,
                        paidAmount = 0L,
                        period = "Oktober 2026",
                        status = "Belum Lunas",
                        paymentHistory = "Belum ada pembayaran",
                        updatedAt = today
                    )
                )
            }

            dao.insertActivityLog(
                ActivityLogEntity(
                    timestamp = currentTimestampString(),
                    actorName = "Sistem Absensi MI",
                    actorRole = "SISTEM",
                    actionTitle = "Inisialisasi Basis Data",
                    description = "Basis data madrasah siap digunakan beserta akun, kelas, siswa, dan absensi."
                )
            )
        }
    }

    suspend fun authenticate(usernameInput: String, passwordInput: String): Result<UserEntity> {
        val user = dao.findUserByUsername(usernameInput.trim())
            ?: return Result.failure(Exception("Akun pengguna '$usernameInput' tidak ditemukan."))

        if (!user.isActive) {
            return Result.failure(Exception("Akun ini sedang dinonaktifkan oleh Administrator."))
        }

        val (isValid, needsMigration) = PasswordSecurity.verifyPassword(passwordInput, user)
        if (!isValid) {
            return Result.failure(Exception("Kata sandi tidak sesuai. Silakan periksa kembali."))
        }

        val finalUser = if (needsMigration) {
            val migrated = user.copy(
                passwordHash = PasswordSecurity.sha256(passwordInput.trim()),
                legacyPassword = ""
            )
            dao.updateUser(migrated)
            migrated
        } else {
            user
        }

        saveSessionUserId(finalUser.id)
        logActivity(
            actorName = finalUser.fullName,
            actorRole = RoleType.label(finalUser.role),
            actionTitle = "Login Berhasil",
            description = "Masuk ke sistem sebagai ${RoleType.label(finalUser.role)}"
        )
        return Result.success(finalUser)
    }

    suspend fun findUserById(userId: Long): UserEntity? = dao.findUserById(userId)

    suspend fun logActivity(
        actorName: String,
        actorRole: String,
        actionTitle: String,
        description: String
    ) {
        dao.insertActivityLog(
            ActivityLogEntity(
                timestamp = currentTimestampString(),
                actorName = actorName,
                actorRole = actorRole,
                actionTitle = actionTitle,
                description = description
            )
        )
    }

    /**
     * Records or corrects student attendance for a specific date.
     * Validates duplicate attendance on the same date:
     * - If allowCorrection is false and a record already exists on that date, returns failure message.
     * - If allowCorrection is true, updates the existing record as a valid correction.
     */
    suspend fun recordOrCorrectAttendance(
        student: StudentEntity,
        date: String,
        time: String,
        status: String,
        note: String,
        operatorName: String,
        operatorRole: String,
        allowCorrection: Boolean
    ): Result<AttendanceEntity> {
        val existing = dao.findAttendanceByStudentAndDate(student.id, date)
        if (existing != null && !allowCorrection) {
            return Result.failure(
                Exception("${student.fullName} (${student.nis}) sudah tercatat '${existing.status}' pada tanggal $date pukul ${existing.time}. Gunakan fitur Koreksi Absensi untuk mengubah.")
            )
        }

        val entity = if (existing != null) {
            existing.copy(
                time = time.ifBlank { existing.time },
                status = status,
                note = note,
                operatorName = operatorName,
                operatorRole = operatorRole,
                isCorrected = true,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            AttendanceEntity(
                studentId = student.id,
                studentNis = student.nis,
                studentName = student.fullName,
                classId = student.classId,
                className = student.className,
                date = date,
                time = time.ifBlank { currentTimeString() },
                status = status,
                note = note,
                operatorName = operatorName,
                operatorRole = operatorRole,
                isCorrected = false,
                updatedAt = System.currentTimeMillis()
            )
        }

        dao.insertAttendance(entity)
        val actionLabel = if (existing != null) "Koreksi Absensi" else "Catat Absensi"
        logActivity(
            actorName = operatorName,
            actorRole = RoleType.label(operatorRole),
            actionTitle = actionLabel,
            description = "${student.fullName} (${student.className}) -> $status ($date)"
        )
        return Result.success(entity)
    }

    suspend fun scanStudentQrCode(
        rawCode: String,
        operatorName: String,
        operatorRole: String,
        allowCorrection: Boolean = false
    ): Result<AttendanceEntity> {
        val clean = rawCode.trim()
        val extractedNis = clean.removePrefix("MI-").removePrefix("mi-").trim()
        val student = dao.findStudentByNisOrQr(extractedNis, clean)
            ?: return Result.failure(Exception("QR Code / NIS '$clean' tidak terdaftar pada data siswa."))

        val settings = dao.getSettingsOnce() ?: SchoolSettingsEntity()
        val nowTime = currentTimeString()
        val isLate = settings.enableAutoLateDetection && nowTime > settings.lateThresholdTime
        val status = if (isLate) AttendanceStatus.TERLAMBAT else AttendanceStatus.HADIR
        val note = if (isLate) "Scan QR (Melewati batas ${settings.lateThresholdTime})" else "Scan QR Tepat Waktu"

        return recordOrCorrectAttendance(
            student = student,
            date = currentDateString(),
            time = nowTime,
            status = status,
            note = note,
            operatorName = operatorName,
            operatorRole = operatorRole,
            allowCorrection = allowCorrection
        )
    }

    // --- CRUD OPERATIONS ---
    suspend fun saveStudent(student: StudentEntity, actorName: String, actorRole: String) {
        val withQr = if (student.qrCodeValue.isBlank()) student.copy(qrCodeValue = "MI-${student.nis}") else student
        dao.insertStudent(withQr)
        logActivity(actorName, RoleType.label(actorRole), "Simpan Data Siswa", "${student.fullName} (${student.nis}) - ${student.className}")
    }

    suspend fun deleteStudent(student: StudentEntity, actorName: String, actorRole: String) {
        dao.deleteStudent(student)
        logActivity(actorName, RoleType.label(actorRole), "Hapus Data Siswa", "${student.fullName} (${student.nis})")
    }

    suspend fun saveTeacher(teacher: TeacherEntity, actorName: String, actorRole: String) {
        dao.insertTeacher(teacher)
        logActivity(actorName, RoleType.label(actorRole), "Simpan Guru/Staf", "${teacher.fullName} (${teacher.position})")
    }

    suspend fun deleteTeacher(teacher: TeacherEntity, actorName: String, actorRole: String) {
        dao.deleteTeacher(teacher)
        logActivity(actorName, RoleType.label(actorRole), "Hapus Guru/Staf", teacher.fullName)
    }

    suspend fun saveTeacherAttendance(item: TeacherAttendanceEntity, actorName: String, actorRole: String) {
        dao.insertTeacherAttendance(item)
        logActivity(actorName, RoleType.label(actorRole), "Absensi Guru/Staf", "${item.teacherName} -> ${item.status} (${item.date})")
    }

    suspend fun saveClass(schoolClass: SchoolClassEntity, actorName: String, actorRole: String) {
        dao.insertClass(schoolClass)
        logActivity(actorName, RoleType.label(actorRole), "Simpan Kelas", "${schoolClass.className} (Wali: ${schoolClass.homeroomTeacherName})")
    }

    suspend fun deleteClass(schoolClass: SchoolClassEntity, actorName: String, actorRole: String) {
        dao.deleteClass(schoolClass)
        logActivity(actorName, RoleType.label(actorRole), "Hapus Kelas", schoolClass.className)
    }

    suspend fun saveUserAccount(
        user: UserEntity,
        newRawPassword: String?,
        actorName: String,
        actorRole: String
    ) {
        val finalUser = if (!newRawPassword.isNullOrBlank()) {
            user.copy(
                passwordHash = PasswordSecurity.sha256(newRawPassword.trim()),
                legacyPassword = ""
            )
        } else {
            user
        }
        dao.insertUser(finalUser)
        logActivity(actorName, RoleType.label(actorRole), "Simpan Akun Pengguna", "${finalUser.fullName} (@${finalUser.username}) - ${RoleType.label(finalUser.role)}")
    }

    suspend fun deleteUserAccount(user: UserEntity, actorName: String, actorRole: String) {
        dao.deleteUser(user)
        logActivity(actorName, RoleType.label(actorRole), "Hapus Akun Pengguna", "${user.fullName} (@${user.username})")
    }

    suspend fun saveNote(note: StudentNoteEntity, actorName: String, actorRole: String) {
        dao.insertNote(note)
        logActivity(actorName, RoleType.label(actorRole), "Simpan Catatan Siswa", "${note.studentName}: ${note.type}")
    }

    suspend fun deleteNote(note: StudentNoteEntity, actorName: String, actorRole: String) {
        dao.deleteNote(note)
        logActivity(actorName, RoleType.label(actorRole), "Hapus Catatan Siswa", "${note.studentName}: ${note.type}")
    }

    suspend fun saveAchievement(achievement: StudentAchievementEntity, actorName: String, actorRole: String) {
        dao.insertAchievement(achievement)
        logActivity(actorName, RoleType.label(actorRole), "Simpan Prestasi Siswa", "${achievement.studentName}: ${achievement.type}")
    }

    suspend fun deleteAchievement(achievement: StudentAchievementEntity, actorName: String, actorRole: String) {
        dao.deleteAchievement(achievement)
        logActivity(actorName, RoleType.label(actorRole), "Hapus Prestasi Siswa", "${achievement.studentName}: ${achievement.type}")
    }

    suspend fun saveViolation(violation: StudentViolationEntity, actorName: String, actorRole: String) {
        dao.insertViolation(violation)
        logActivity(actorName, RoleType.label(actorRole), "Simpan Pelanggaran Siswa", "${violation.studentName}: ${violation.type}")
    }

    suspend fun deleteViolation(violation: StudentViolationEntity, actorName: String, actorRole: String) {
        dao.deleteViolation(violation)
        logActivity(actorName, RoleType.label(actorRole), "Hapus Pelanggaran Siswa", "${violation.studentName}: ${violation.type}")
    }

    suspend fun saveArrear(arrear: ArrearPaymentEntity, actorName: String, actorRole: String) {
        dao.insertArrear(arrear.copy(updatedAt = currentDateString()))
        logActivity(actorName, RoleType.label(actorRole), "Simpan Tagihan/Tunggakan", "${arrear.studentName} - ${arrear.billType} (${arrear.status})")
    }

    suspend fun deleteArrear(arrear: ArrearPaymentEntity, actorName: String, actorRole: String) {
        dao.deleteArrear(arrear)
        logActivity(actorName, RoleType.label(actorRole), "Hapus Tagihan/Tunggakan", "${arrear.studentName} - ${arrear.billType}")
    }

    suspend fun saveSettings(settings: SchoolSettingsEntity, actorName: String, actorRole: String) {
        dao.saveSettings(settings)
        logActivity(actorName, RoleType.label(actorRole), "Perbarui Pengaturan Madrasah", "${settings.schoolName} (${settings.activeAcademicYear} - ${settings.activeSemester})")
    }

    suspend fun saveAcademicYear(year: AcademicYearEntity, actorName: String, actorRole: String) {
        dao.insertAcademicYear(year)
        logActivity(actorName, RoleType.label(actorRole), "Simpan Tahun Ajaran", "${year.yearName} (${year.semester})")
    }

    suspend fun deleteAcademicYear(year: AcademicYearEntity, actorName: String, actorRole: String) {
        dao.deleteAcademicYear(year)
    }

    // --- BACKUP & RESTORE JSON (NO PLAINTEXT PASSWORDS) ---
    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("app", "ABSENSI MI — ADMINISTRASI & KEHADIRAN SISWA")
        root.put("version", 1)
        root.put("exportedAt", currentTimestampString())

        val settings = dao.getSettingsOnce() ?: SchoolSettingsEntity()
        val settingsObj = JSONObject().apply {
            put("schoolName", settings.schoolName)
            put("nsm", settings.nsm)
            put("npsn", settings.npsn)
            put("address", settings.address)
            put("phone", settings.phone)
            put("email", settings.email)
            put("headmasterName", settings.headmasterName)
            put("headmasterNip", settings.headmasterNip)
            put("timeInSchedule", settings.timeInSchedule)
            put("lateThresholdTime", settings.lateThresholdTime)
            put("timeOutSchedule", settings.timeOutSchedule)
            put("activeAcademicYear", settings.activeAcademicYear)
            put("activeSemester", settings.activeSemester)
            put("enableNotifications", settings.enableNotifications)
            put("enableAutoLateDetection", settings.enableAutoLateDetection)
            put("scheduleSummary", settings.scheduleSummary)
        }
        root.put("school_settings", settingsObj)

        // Users: NEVER export plaintext password; only export passwordHash & metadata
        val usersArr = JSONArray()
        dao.getAllUsersOnce().forEach { u ->
            usersArr.put(JSONObject().apply {
                put("id", u.id)
                put("username", u.username)
                put("fullName", u.fullName)
                put("nip", u.nip)
                put("role", u.role)
                put("assignedClassId", u.assignedClassId ?: -1L)
                put("assignedClassName", u.assignedClassName)
                put("passwordHash", u.passwordHash)
                put("canAddStudent", u.canAddStudent)
                put("canCorrectAttendance", u.canCorrectAttendance)
                put("isActive", u.isActive)
                put("phone", u.phone)
            })
        }
        root.put("users", usersArr)

        val classesArr = JSONArray()
        dao.getAllClassesOnce().forEach { c ->
            classesArr.put(JSONObject().apply {
                put("id", c.id)
                put("className", c.className)
                put("gradeLevel", c.gradeLevel)
                put("homeroomTeacherName", c.homeroomTeacherName)
                put("roomNumber", c.roomNumber)
                put("classNotes", c.classNotes)
                put("classAnnouncement", c.classAnnouncement)
            })
        }
        root.put("classes", classesArr)

        val studentsArr = JSONArray()
        dao.getAllStudentsOnce().forEach { s ->
            studentsArr.put(JSONObject().apply {
                put("id", s.id)
                put("nis", s.nis)
                put("fullName", s.fullName)
                put("gender", s.gender)
                put("classId", s.classId)
                put("className", s.className)
                put("parentName", s.parentName)
                put("parentPhone", s.parentPhone)
                put("address", s.address)
                put("birthPlaceDate", s.birthPlaceDate)
                put("status", s.status)
                put("qrCodeValue", s.qrCodeValue)
            })
        }
        root.put("students", studentsArr)

        val teachersArr = JSONArray()
        dao.getAllTeachersOnce().forEach { t ->
            teachersArr.put(JSONObject().apply {
                put("id", t.id)
                put("nip", t.nip)
                put("fullName", t.fullName)
                put("position", t.position)
                put("subjectOrClass", t.subjectOrClass)
                put("phone", t.phone)
                put("address", t.address)
                put("status", t.status)
            })
        }
        root.put("teachers", teachersArr)

        val attendanceArr = JSONArray()
        dao.getAllAttendanceOnce().forEach { a ->
            attendanceArr.put(JSONObject().apply {
                put("id", a.id)
                put("studentId", a.studentId)
                put("studentNis", a.studentNis)
                put("studentName", a.studentName)
                put("classId", a.classId)
                put("className", a.className)
                put("date", a.date)
                put("time", a.time)
                put("status", a.status)
                put("note", a.note)
                put("operatorName", a.operatorName)
                put("operatorRole", a.operatorRole)
                put("isCorrected", a.isCorrected)
            })
        }
        root.put("attendance", attendanceArr)

        val notesArr = JSONArray()
        dao.getAllNotesOnce().forEach { n ->
            notesArr.put(JSONObject().apply {
                put("id", n.id)
                put("studentId", n.studentId)
                put("studentName", n.studentName)
                put("classId", n.classId)
                put("className", n.className)
                put("type", n.type)
                put("content", n.content)
                put("date", n.date)
                put("forwardToBk", n.forwardToBk)
                put("authorName", n.authorName)
            })
        }
        root.put("notes", notesArr)

        val achievementsArr = JSONArray()
        dao.getAllAchievementsOnce().forEach { ac ->
            achievementsArr.put(JSONObject().apply {
                put("id", ac.id)
                put("studentId", ac.studentId)
                put("studentName", ac.studentName)
                put("classId", ac.classId)
                put("className", ac.className)
                put("type", ac.type)
                put("content", ac.content)
                put("date", ac.date)
                put("forwardToBk", ac.forwardToBk)
                put("authorName", ac.authorName)
            })
        }
        root.put("achievements", achievementsArr)

        val violationsArr = JSONArray()
        dao.getAllViolationsOnce().forEach { v ->
            violationsArr.put(JSONObject().apply {
                put("id", v.id)
                put("studentId", v.studentId)
                put("studentName", v.studentName)
                put("classId", v.classId)
                put("className", v.className)
                put("type", v.type)
                put("content", v.content)
                put("date", v.date)
                put("forwardToBk", v.forwardToBk)
                put("authorName", v.authorName)
            })
        }
        root.put("violations", violationsArr)

        val arrearsArr = JSONArray()
        dao.getAllArrearsOnce().forEach { ar ->
            arrearsArr.put(JSONObject().apply {
                put("id", ar.id)
                put("studentId", ar.studentId)
                put("studentNis", ar.studentNis)
                put("studentName", ar.studentName)
                put("classId", ar.classId)
                put("className", ar.className)
                put("billType", ar.billType)
                put("nominal", ar.nominal)
                put("paidAmount", ar.paidAmount)
                put("period", ar.period)
                put("status", ar.status)
                put("paymentHistory", ar.paymentHistory)
                put("updatedAt", ar.updatedAt)
            })
        }
        root.put("arrears_payments", arrearsArr)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String, actorName: String, actorRole: String): Result<String> {
        return try {
            val root = JSONObject(jsonString)
            var importedCount = 0

            root.optJSONObject("school_settings")?.let { s ->
                dao.saveSettings(
                    SchoolSettingsEntity(
                        id = 1,
                        schoolName = s.optString("schoolName", "MI AL-IKHSAN SURABAYA"),
                        nsm = s.optString("nsm", "111232010045"),
                        npsn = s.optString("npsn", "60708912"),
                        address = s.optString("address", ""),
                        phone = s.optString("phone", ""),
                        email = s.optString("email", ""),
                        headmasterName = s.optString("headmasterName", ""),
                        headmasterNip = s.optString("headmasterNip", ""),
                        timeInSchedule = s.optString("timeInSchedule", "07:00"),
                        lateThresholdTime = s.optString("lateThresholdTime", "07:15"),
                        timeOutSchedule = s.optString("timeOutSchedule", "13:30"),
                        activeAcademicYear = s.optString("activeAcademicYear", "2026/2027"),
                        activeSemester = s.optString("activeSemester", "Ganjil"),
                        enableNotifications = s.optBoolean("enableNotifications", true),
                        enableAutoLateDetection = s.optBoolean("enableAutoLateDetection", true),
                        scheduleSummary = s.optString("scheduleSummary", "")
                    )
                )
            }

            root.optJSONArray("classes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    dao.insertClass(
                        SchoolClassEntity(
                            id = c.optLong("id", 0L),
                            className = c.optString("className"),
                            gradeLevel = c.optInt("gradeLevel", 1),
                            homeroomTeacherName = c.optString("homeroomTeacherName"),
                            roomNumber = c.optString("roomNumber"),
                            classNotes = c.optString("classNotes"),
                            classAnnouncement = c.optString("classAnnouncement")
                        )
                    )
                    importedCount++
                }
            }

            root.optJSONArray("users")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val u = arr.getJSONObject(i)
                    val rawLegacy = u.optString("password", "")
                    val hash = u.optString("passwordHash", "").ifBlank {
                        if (rawLegacy.isNotBlank()) PasswordSecurity.sha256(rawLegacy) else PasswordSecurity.sha256("123456")
                    }
                    val classIdVal = u.optLong("assignedClassId", -1L)
                    dao.insertUser(
                        UserEntity(
                            id = u.optLong("id", 0L),
                            username = u.optString("username"),
                            fullName = u.optString("fullName"),
                            nip = u.optString("nip"),
                            role = u.optString("role", RoleType.WALI_KELAS),
                            assignedClassId = if (classIdVal > 0) classIdVal else null,
                            assignedClassName = u.optString("assignedClassName"),
                            passwordHash = hash,
                            legacyPassword = rawLegacy,
                            canAddStudent = u.optBoolean("canAddStudent", true),
                            canCorrectAttendance = u.optBoolean("canCorrectAttendance", true),
                            isActive = u.optBoolean("isActive", true),
                            phone = u.optString("phone")
                        )
                    )
                    importedCount++
                }
            }

            root.optJSONArray("students")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    val nis = s.optString("nis")
                    dao.insertStudent(
                        StudentEntity(
                            id = s.optLong("id", 0L),
                            nis = nis,
                            fullName = s.optString("fullName"),
                            gender = s.optString("gender", "L"),
                            classId = s.optLong("classId", 1L),
                            className = s.optString("className", "Kelas 1A"),
                            parentName = s.optString("parentName"),
                            parentPhone = s.optString("parentPhone"),
                            address = s.optString("address"),
                            birthPlaceDate = s.optString("birthPlaceDate"),
                            status = s.optString("status", "Aktif"),
                            qrCodeValue = s.optString("qrCodeValue", "MI-$nis")
                        )
                    )
                    importedCount++
                }
            }

            root.optJSONArray("teachers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val t = arr.getJSONObject(i)
                    dao.insertTeacher(
                        TeacherEntity(
                            id = t.optLong("id", 0L),
                            nip = t.optString("nip"),
                            fullName = t.optString("fullName"),
                            position = t.optString("position"),
                            subjectOrClass = t.optString("subjectOrClass"),
                            phone = t.optString("phone"),
                            address = t.optString("address"),
                            status = t.optString("status", "Aktif")
                        )
                    )
                    importedCount++
                }
            }

            root.optJSONArray("attendance")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONObject(i)
                    dao.insertAttendance(
                        AttendanceEntity(
                            id = a.optLong("id", 0L),
                            studentId = a.optLong("studentId"),
                            studentNis = a.optString("studentNis"),
                            studentName = a.optString("studentName"),
                            classId = a.optLong("classId"),
                            className = a.optString("className"),
                            date = a.optString("date"),
                            time = a.optString("time"),
                            status = a.optString("status"),
                            note = a.optString("note"),
                            operatorName = a.optString("operatorName"),
                            operatorRole = a.optString("operatorRole"),
                            isCorrected = a.optBoolean("isCorrected", false)
                        )
                    )
                    importedCount++
                }
            }

            root.optJSONArray("notes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val n = arr.getJSONObject(i)
                    dao.insertNote(
                        StudentNoteEntity(
                            id = n.optLong("id", 0L),
                            studentId = n.optLong("studentId"),
                            studentName = n.optString("studentName"),
                            classId = n.optLong("classId"),
                            className = n.optString("className"),
                            type = n.optString("type", "Catatan"),
                            content = n.optString("content"),
                            date = n.optString("date"),
                            forwardToBk = n.optBoolean("forwardToBk", false),
                            authorName = n.optString("authorName")
                        )
                    )
                    importedCount++
                }
            }

            root.optJSONArray("achievements")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val ac = arr.getJSONObject(i)
                    dao.insertAchievement(
                        StudentAchievementEntity(
                            id = ac.optLong("id", 0L),
                            studentId = ac.optLong("studentId"),
                            studentName = ac.optString("studentName"),
                            classId = ac.optLong("classId"),
                            className = ac.optString("className"),
                            type = ac.optString("type", "Prestasi"),
                            content = ac.optString("content"),
                            date = ac.optString("date"),
                            forwardToBk = ac.optBoolean("forwardToBk", false),
                            authorName = ac.optString("authorName")
                        )
                    )
                    importedCount++
                }
            }

            root.optJSONArray("violations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val v = arr.getJSONObject(i)
                    dao.insertViolation(
                        StudentViolationEntity(
                            id = v.optLong("id", 0L),
                            studentId = v.optLong("studentId"),
                            studentName = v.optString("studentName"),
                            classId = v.optLong("classId"),
                            className = v.optString("className"),
                            type = v.optString("type", "Pelanggaran"),
                            content = v.optString("content"),
                            date = v.optString("date"),
                            forwardToBk = v.optBoolean("forwardToBk", false),
                            authorName = v.optString("authorName")
                        )
                    )
                    importedCount++
                }
            }

            root.optJSONArray("arrears_payments")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val ar = arr.getJSONObject(i)
                    dao.insertArrear(
                        ArrearPaymentEntity(
                            id = ar.optLong("id", 0L),
                            studentId = ar.optLong("studentId"),
                            studentNis = ar.optString("studentNis"),
                            studentName = ar.optString("studentName"),
                            classId = ar.optLong("classId"),
                            className = ar.optString("className"),
                            billType = ar.optString("billType"),
                            nominal = ar.optLong("nominal", 0L),
                            paidAmount = ar.optLong("paidAmount", 0L),
                            period = ar.optString("period"),
                            status = ar.optString("status", "Belum Lunas"),
                            paymentHistory = ar.optString("paymentHistory"),
                            updatedAt = ar.optString("updatedAt")
                        )
                    )
                    importedCount++
                }
            }

            logActivity(actorName, RoleType.label(actorRole), "Pemulihan / Import JSON", "Berhasil memulihkan $importedCount entitas data.")
            Result.success("Berhasil mengimpor & menggabungkan $importedCount data ke sistem.")
        } catch (e: Exception) {
            Result.failure(Exception("Format JSON tidak valid: ${e.localizedMessage}"))
        }
    }
}
