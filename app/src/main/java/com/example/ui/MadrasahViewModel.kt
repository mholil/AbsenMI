package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.StudentReportRow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ActiveModal {
    data object None : ActiveModal
    data object LogoutConfirmation : ActiveModal
    data object AdminSettings : ActiveModal
    data object HomeroomAdminModal : ActiveModal
    data object ArrearsManagementModal : ActiveModal
    data object WaArrearTemplateModal : ActiveModal
    data object BackupRestoreModal : ActiveModal

    data class WaArrearMessagePreview(
        val arrear: ArrearPaymentEntity,
        val student: StudentEntity?
    ) : ActiveModal

    data class PrintDocumentPreviewModal(
        val documentTitle: String,
        val htmlContent: String,
        val isArrearsReport: Boolean = false,
        val periodOrScopeLabel: String = ""
    ) : ActiveModal

    data class StudentForm(val existing: StudentEntity? = null, val preselectedClassId: Long? = null) : ActiveModal
    data class StudentDetail(val studentId: Long, val initialTab: Int = 0) : ActiveModal
    data class StudentQrCard(val student: StudentEntity) : ActiveModal

    data class NoteRecordForm(
        val student: StudentEntity,
        val category: String, // "Catatan", "Prestasi", "Pelanggaran"
        val existingId: Long = 0L,
        val initialType: String = "",
        val initialContent: String = "",
        val initialDate: String = "",
        val initialForwardToBk: Boolean = false
    ) : ActiveModal

    data class AttendanceRecordForm(
        val student: StudentEntity,
        val date: String,
        val existing: AttendanceEntity? = null,
        val isCorrection: Boolean = false
    ) : ActiveModal

    data class AttendanceDetailModal(val attendance: AttendanceEntity) : ActiveModal

    data class TeacherForm(val existing: TeacherEntity? = null) : ActiveModal
    data class TeacherAttendanceForm(val teacher: TeacherEntity) : ActiveModal
    data class ClassForm(val existing: SchoolClassEntity? = null) : ActiveModal
    data class UserAccountForm(val existing: UserEntity? = null) : ActiveModal
    data class ResetPasswordModal(val targetUser: UserEntity) : ActiveModal
    data class ArrearForm(val existing: ArrearPaymentEntity? = null, val preselectedStudent: StudentEntity? = null) : ActiveModal
}

data class MadrasahUiState(
    val isInitialized: Boolean = false,
    val currentUser: UserEntity? = null,
    val loginError: String? = null,
    val selectedBottomTab: Int = 0,
    val activeModal: ActiveModal = ActiveModal.None,
    val isCameraActive: Boolean = false,
    val toastMessage: String? = null,

    // Filters
    val selectedDateFilter: String = "",
    val selectedClassIdFilter: Long? = null, // null means "Semua Kelas" (or scoped class for Wali Kelas)
    val searchQuery: String = "",
    val dataSectionTab: Int = 0, // 0: Siswa, 1: Guru/Staf, 2: Kelas, 3: Akun Pengguna
    val reportPeriodType: String = "Harian", // Harian, Mingguan, Bulanan, Per Siswa, Per Kelas, Guru/Staf

    // Data lists from Room
    val users: List<UserEntity> = emptyList(),
    val classes: List<SchoolClassEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val teachers: List<TeacherEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList(),
    val teacherAttendance: List<TeacherAttendanceEntity> = emptyList(),
    val notes: List<StudentNoteEntity> = emptyList(),
    val achievements: List<StudentAchievementEntity> = emptyList(),
    val violations: List<StudentViolationEntity> = emptyList(),
    val arrears: List<ArrearPaymentEntity> = emptyList(),
    val settings: SchoolSettingsEntity = SchoolSettingsEntity(),
    val academicYears: List<AcademicYearEntity> = emptyList(),
    val activityLogs: List<ActivityLogEntity> = emptyList()
)

class MadrasahViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MadrasahDatabase.getInstance(application)
    val repository = MadrasahRepository(database.madrasahDao(), application)

    private val _uiState = MutableStateFlow(
        MadrasahUiState(selectedDateFilter = repository.currentDateString())
    )
    val uiState: StateFlow<MadrasahUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialBaselineData()
            val savedUserId = repository.getSavedSessionUserId()
            val restoredUser = if (savedUserId > 0) repository.findUserById(savedUserId) else null
            _uiState.update { state ->
                state.copy(
                    isInitialized = true,
                    currentUser = restoredUser,
                    selectedClassIdFilter = if (restoredUser?.role == RoleType.WALI_KELAS) restoredUser.assignedClassId else null
                )
            }
        }

        // Observe all Room tables reactively
        viewModelScope.launch {
            repository.usersFlow.collect { list ->
                _uiState.update { state ->
                    val updatedCurrent = state.currentUser?.let { cur -> list.find { it.id == cur.id } ?: cur }
                    state.copy(users = list, currentUser = updatedCurrent)
                }
            }
        }
        viewModelScope.launch {
            repository.classesFlow.collect { list -> _uiState.update { it.copy(classes = list) } }
        }
        viewModelScope.launch {
            repository.studentsFlow.collect { list -> _uiState.update { it.copy(students = list) } }
        }
        viewModelScope.launch {
            repository.teachersFlow.collect { list -> _uiState.update { it.copy(teachers = list) } }
        }
        viewModelScope.launch {
            repository.attendanceFlow.collect { list -> _uiState.update { it.copy(attendance = list) } }
        }
        viewModelScope.launch {
            repository.teacherAttendanceFlow.collect { list -> _uiState.update { it.copy(teacherAttendance = list) } }
        }
        viewModelScope.launch {
            repository.notesFlow.collect { list -> _uiState.update { it.copy(notes = list) } }
        }
        viewModelScope.launch {
            repository.achievementsFlow.collect { list -> _uiState.update { it.copy(achievements = list) } }
        }
        viewModelScope.launch {
            repository.violationsFlow.collect { list -> _uiState.update { it.copy(violations = list) } }
        }
        viewModelScope.launch {
            repository.arrearsFlow.collect { list -> _uiState.update { it.copy(arrears = list) } }
        }
        viewModelScope.launch {
            repository.settingsFlow.collect { s ->
                if (s != null) _uiState.update { it.copy(settings = s) }
            }
        }
        viewModelScope.launch {
            repository.academicYearsFlow.collect { list -> _uiState.update { it.copy(academicYears = list) } }
        }
        viewModelScope.launch {
            repository.activityLogsFlow.collect { list -> _uiState.update { it.copy(activityLogs = list) } }
        }
    }

    /**
     * Rule: When opening a new modal from an old modal, close the old modal first
     * by replacing activeModal in a single atomic state transition.
     */
    fun openModal(modal: ActiveModal) {
        _uiState.update { it.copy(activeModal = modal) }
    }

    fun closeModal() {
        _uiState.update { it.copy(activeModal = ActiveModal.None) }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun setCameraActive(active: Boolean) {
        _uiState.update { it.copy(isCameraActive = active) }
    }

    fun selectBottomTab(index: Int) {
        _uiState.update {
            it.copy(
                selectedBottomTab = index,
                activeModal = ActiveModal.None
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setDateFilter(date: String) {
        _uiState.update { it.copy(selectedDateFilter = date) }
    }

    fun setClassFilter(classId: Long?) {
        val current = _uiState.value.currentUser
        // Enforce Wali Kelas class isolation
        if (current?.role == RoleType.WALI_KELAS && current.assignedClassId != null) {
            _uiState.update { it.copy(selectedClassIdFilter = current.assignedClassId) }
        } else {
            _uiState.update { it.copy(selectedClassIdFilter = classId) }
        }
    }

    fun setDataSectionTab(tab: Int) {
        _uiState.update { it.copy(dataSectionTab = tab, searchQuery = "") }
    }

    fun setReportPeriodType(type: String) {
        _uiState.update { it.copy(reportPeriodType = type) }
    }

    // --- AUTHENTICATION & LOGOUT ---
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(loginError = "Username dan kata sandi wajib diisi.") }
            return
        }
        viewModelScope.launch {
            val result = repository.authenticate(username, password)
            result.onSuccess { user ->
                val defaultClassFilter = if (user.role == RoleType.WALI_KELAS) user.assignedClassId else null
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        loginError = null,
                        selectedBottomTab = 0,
                        activeModal = ActiveModal.None,
                        isCameraActive = false,
                        selectedClassIdFilter = defaultClassFilter,
                        searchQuery = "",
                        toastMessage = "Selamat datang, ${user.fullName}"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(loginError = err.message ?: "Gagal masuk ke sistem.") }
            }
        }
    }

    fun clearLoginError() {
        _uiState.update { it.copy(loginError = null) }
    }

    fun requestLogoutConfirmation() {
        openModal(ActiveModal.LogoutConfirmation)
    }

    /**
     * Mandatory Logout behavior:
     * 1. After confirmation "Ya":
     *    - Close all modals immediately
     *    - Stop camera if active
     *    - Clear active session
     *    - Return directly to LOGIN screen without needing to press X again.
     */
    fun confirmLogout() {
        repository.clearSession()
        _uiState.update {
            it.copy(
                currentUser = null,
                loginError = null,
                selectedBottomTab = 0,
                activeModal = ActiveModal.None,
                isCameraActive = false,
                searchQuery = "",
                selectedClassIdFilter = null,
                toastMessage = "Anda telah keluar dari sesi aplikasi."
            )
        }
    }

    // --- SCOPED DATA HELPERS ---
    fun getScopedStudents(state: MadrasahUiState = _uiState.value): List<StudentEntity> {
        val user = state.currentUser ?: return emptyList()
        val base = if (user.role == RoleType.WALI_KELAS && user.assignedClassId != null) {
            state.students.filter { it.classId == user.assignedClassId }
        } else if (state.selectedClassIdFilter != null) {
            state.students.filter { it.classId == state.selectedClassIdFilter }
        } else {
            state.students
        }
        val q = state.searchQuery.trim().lowercase()
        return if (q.isEmpty()) base else base.filter {
            it.fullName.lowercase().contains(q) ||
                it.nis.lowercase().contains(q) ||
                it.className.lowercase().contains(q) ||
                it.parentName.lowercase().contains(q)
        }
    }

    fun getScopedAttendanceForDate(state: MadrasahUiState = _uiState.value, date: String = state.selectedDateFilter): List<AttendanceEntity> {
        val user = state.currentUser ?: return emptyList()
        return state.attendance.filter { att ->
            val dateMatch = if (date.isBlank()) true else att.date == date
            val classMatch = when {
                user.role == RoleType.WALI_KELAS && user.assignedClassId != null -> att.classId == user.assignedClassId
                state.selectedClassIdFilter != null -> att.classId == state.selectedClassIdFilter
                else -> true
            }
            dateMatch && classMatch
        }
    }

    fun getReportPeriodSubtitle(state: MadrasahUiState = _uiState.value): String {
        val baseDate = state.selectedDateFilter.ifBlank { repository.currentDateString() }
        return when (state.reportPeriodType) {
            "Harian" -> "Tanggal Harian: $baseDate"
            "Mingguan" -> {
                val start7 = try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val parsed = sdf.parse(baseDate)
                    if (parsed != null) {
                        val cal = java.util.Calendar.getInstance()
                        cal.time = parsed
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -6)
                        sdf.format(cal.time)
                    } else baseDate
                } catch (_: Exception) {
                    baseDate
                }
                "Rentang 7 Hari: $start7 s/d $baseDate"
            }
            "Bulanan" -> "Rekap Bulan: ${baseDate.take(7)}"
            else -> "Periode: $baseDate"
        }
    }

    fun buildReportRows(state: MadrasahUiState = _uiState.value): List<StudentReportRow> {
        val students = getScopedStudents(state)
        val baseDate = state.selectedDateFilter.ifBlank { repository.currentDateString() }
        val start7DaysDate = try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val parsed = sdf.parse(baseDate)
            if (parsed != null) {
                val cal = java.util.Calendar.getInstance()
                cal.time = parsed
                cal.add(java.util.Calendar.DAY_OF_YEAR, -6)
                sdf.format(cal.time)
            } else baseDate
        } catch (_: Exception) {
            baseDate
        }

        val filteredAttendance = state.attendance.filter { att ->
            when (state.reportPeriodType) {
                "Harian" -> att.date == baseDate
                "Mingguan" -> att.date in start7DaysDate..baseDate
                "Bulanan" -> {
                    val monthPrefix = baseDate.take(7) // YYYY-MM
                    att.date.startsWith(monthPrefix)
                }
                else -> true
            }
        }

        return students.map { st ->
            val records = filteredAttendance.filter { it.studentId == st.id }
            val h = records.count { it.status == AttendanceStatus.HADIR }
            val t = records.count { it.status == AttendanceStatus.TERLAMBAT }
            val i = records.count { it.status == AttendanceStatus.IZIN }
            val s = records.count { it.status == AttendanceStatus.SAKIT }
            val a = records.count { it.status == AttendanceStatus.ALPA }
            val totalRecorded = h + t + i + s + a
            val presentCount = h + t
            val pct = if (totalRecorded > 0) ((presentCount * 100f) / totalRecorded).toInt() else 0
            val latestRec = records.maxByOrNull { "${it.date}_${it.time}" }
            val dailyMap = records.associate { rec ->
                val shortCode = when (rec.status) {
                    AttendanceStatus.HADIR -> "H"
                    AttendanceStatus.TERLAMBAT -> "T"
                    AttendanceStatus.IZIN -> "I"
                    AttendanceStatus.SAKIT -> "S"
                    AttendanceStatus.ALPA -> "A"
                    else -> "-"
                }
                rec.date to shortCode
            }
            StudentReportRow(
                nis = st.nis,
                studentName = st.fullName,
                className = st.className,
                hadir = h,
                terlambat = t,
                izin = i,
                sakit = s,
                alpa = a,
                percentage = pct,
                latestStatus = latestRec?.status ?: AttendanceStatus.BELUM_HADIR,
                latestTime = latestRec?.time ?: "-",
                latestNote = latestRec?.note ?: "",
                recordedDays = totalRecorded,
                dailyStatuses = dailyMap
            )
        }
    }

    // --- ACTIONS ---
    fun recordAttendance(
        student: StudentEntity,
        date: String,
        time: String,
        status: String,
        note: String,
        allowCorrection: Boolean
    ) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val res = repository.recordOrCorrectAttendance(
                student = student,
                date = date,
                time = time,
                status = status,
                note = note,
                operatorName = user.fullName,
                operatorRole = user.role,
                allowCorrection = allowCorrection
            )
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        activeModal = ActiveModal.None,
                        toastMessage = "Absensi ${student.fullName} tercatat: $status"
                    )
                }
            }.onFailure { err ->
                showMessage(err.message ?: "Gagal menyimpan absensi")
            }
        }
    }

    fun handleQrScan(rawCode: String, allowCorrection: Boolean = false) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val res = repository.scanStudentQrCode(
                rawCode = rawCode,
                operatorName = user.fullName,
                operatorRole = user.role,
                allowCorrection = allowCorrection
            )
            res.onSuccess { att ->
                showMessage("Scan Berhasil: ${att.studentName} (${att.className}) — ${att.status} [${att.time}]")
            }.onFailure { err ->
                showMessage(err.message ?: "QR tidak dikenali")
            }
        }
    }

    fun saveStudent(student: StudentEntity) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.saveStudent(student, user.fullName, user.role)
            _uiState.update {
                it.copy(
                    activeModal = ActiveModal.None,
                    toastMessage = "Data siswa ${student.fullName} berhasil disimpan."
                )
            }
        }
    }

    fun deleteStudent(student: StudentEntity) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.deleteStudent(student, user.fullName, user.role)
            _uiState.update {
                it.copy(
                    activeModal = ActiveModal.None,
                    toastMessage = "Data siswa ${student.fullName} telah dihapus."
                )
            }
        }
    }

    fun saveStudentRecord(
        student: StudentEntity,
        category: String,
        existingId: Long,
        type: String,
        content: String,
        date: String,
        forwardToBk: Boolean
    ) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            when (category) {
                "Prestasi" -> repository.saveAchievement(
                    StudentAchievementEntity(
                        id = existingId,
                        studentId = student.id,
                        studentName = student.fullName,
                        classId = student.classId,
                        className = student.className,
                        type = type.ifBlank { "Prestasi Akademik" },
                        content = content,
                        date = date,
                        forwardToBk = forwardToBk,
                        authorName = user.fullName
                    ),
                    user.fullName,
                    user.role
                )
                "Pelanggaran" -> repository.saveViolation(
                    StudentViolationEntity(
                        id = existingId,
                        studentId = student.id,
                        studentName = student.fullName,
                        classId = student.classId,
                        className = student.className,
                        type = type.ifBlank { "Kedisiplinan" },
                        content = content,
                        date = date,
                        forwardToBk = forwardToBk,
                        authorName = user.fullName
                    ),
                    user.fullName,
                    user.role
                )
                else -> repository.saveNote(
                    StudentNoteEntity(
                        id = existingId,
                        studentId = student.id,
                        studentName = student.fullName,
                        classId = student.classId,
                        className = student.className,
                        type = type.ifBlank { "Catatan Perkembangan" },
                        content = content,
                        date = date,
                        forwardToBk = forwardToBk,
                        authorName = user.fullName
                    ),
                    user.fullName,
                    user.role
                )
            }
            // After saving: modal closes and returns directly to StudentDetail profile so new data is immediately visible!
            val tabIdx = when (category) {
                "Prestasi" -> 1
                "Pelanggaran" -> 2
                else -> 0
            }
            _uiState.update {
                it.copy(
                    activeModal = ActiveModal.StudentDetail(student.id, initialTab = tabIdx),
                    toastMessage = "$category untuk ${student.fullName} berhasil disimpan."
                )
            }
        }
    }

    fun deleteNote(note: StudentNoteEntity) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.deleteNote(note, user.fullName, user.role)
            showMessage("Catatan dihapus.")
        }
    }

    fun deleteAchievement(achievement: StudentAchievementEntity) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.deleteAchievement(achievement, user.fullName, user.role)
            showMessage("Data prestasi dihapus.")
        }
    }

    fun deleteViolation(violation: StudentViolationEntity) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.deleteViolation(violation, user.fullName, user.role)
            showMessage("Catatan pelanggaran dihapus.")
        }
    }

    fun saveTeacher(teacher: TeacherEntity) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.saveTeacher(teacher, user.fullName, user.role)
            _uiState.update {
                it.copy(
                    activeModal = ActiveModal.None,
                    toastMessage = "Data guru/staf ${teacher.fullName} disimpan."
                )
            }
        }
    }

    fun deleteTeacher(teacher: TeacherEntity) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.deleteTeacher(teacher, user.fullName, user.role)
            showMessage("Data guru/staf dihapus.")
        }
    }

    fun saveTeacherAttendance(item: TeacherAttendanceEntity) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.saveTeacherAttendance(item, user.fullName, user.role)
            _uiState.update {
                it.copy(
                    activeModal = ActiveModal.None,
                    toastMessage = "Kehadiran ${item.teacherName} tercatat."
                )
            }
        }
    }

    fun saveClass(schoolClass: SchoolClassEntity, stayInHomeroomModal: Boolean = false) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.saveClass(schoolClass, user.fullName, user.role)
            _uiState.update {
                it.copy(
                    activeModal = if (stayInHomeroomModal) ActiveModal.HomeroomAdminModal else ActiveModal.None,
                    toastMessage = "Data ${schoolClass.className} berhasil disimpan."
                )
            }
        }
    }

    fun deleteClass(schoolClass: SchoolClassEntity) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.deleteClass(schoolClass, user.fullName, user.role)
            showMessage("${schoolClass.className} dihapus.")
        }
    }

    fun saveUserAccount(account: UserEntity, newRawPassword: String?) {
        val user = _uiState.value.currentUser ?: return
        if (user.role != RoleType.ADMIN) {
            showMessage("Akses ditolak: Hanya Administrator yang dapat mengelola akun pengguna.")
            return
        }
        viewModelScope.launch {
            repository.saveUserAccount(account, newRawPassword, user.fullName, user.role)
            _uiState.update {
                it.copy(
                    activeModal = ActiveModal.None,
                    toastMessage = "Akun @${account.username} berhasil disimpan."
                )
            }
        }
    }

    fun deleteUserAccount(account: UserEntity) {
        val user = _uiState.value.currentUser ?: return
        if (user.role != RoleType.ADMIN) return
        if (account.id == user.id) {
            showMessage("Tidak dapat menghapus akun Admin yang sedang digunakan.")
            return
        }
        viewModelScope.launch {
            repository.deleteUserAccount(account, user.fullName, user.role)
            showMessage("Akun @${account.username} dihapus.")
        }
    }

    fun saveArrear(arrear: ArrearPaymentEntity) {
        val user = _uiState.value.currentUser ?: return
        if (user.role != RoleType.ADMIN) {
            showMessage("Hanya Admin yang dapat mengubah konfigurasi tagihan madrasah.")
            return
        }
        viewModelScope.launch {
            repository.saveArrear(arrear, user.fullName, user.role)
            _uiState.update {
                it.copy(
                    activeModal = ActiveModal.ArrearsManagementModal,
                    toastMessage = "Tagihan ${arrear.studentName} berhasil disimpan."
                )
            }
        }
    }

    fun deleteArrear(arrear: ArrearPaymentEntity) {
        val user = _uiState.value.currentUser ?: return
        if (user.role != RoleType.ADMIN) return
        viewModelScope.launch {
            repository.deleteArrear(arrear, user.fullName, user.role)
            showMessage("Data tagihan dihapus.")
        }
    }

    fun saveSchoolSettings(newSettings: SchoolSettingsEntity, newAcademicYear: AcademicYearEntity? = null) {
        val user = _uiState.value.currentUser ?: return
        if (user.role != RoleType.ADMIN) {
            showMessage("Hanya Admin yang dapat mengubah pengaturan madrasah global.")
            return
        }
        viewModelScope.launch {
            repository.saveSettings(newSettings, user.fullName, user.role)
            if (newAcademicYear != null) {
                repository.saveAcademicYear(newAcademicYear, user.fullName, user.role)
            }
            showMessage("Pengaturan madrasah berhasil disimpan.")
        }
    }

    fun getWaArrearTemplate(): String {
        val classId = _uiState.value.currentUser?.assignedClassId
        return repository.getWaArrearTemplate(classId)
    }

    fun saveWaArrearTemplate(template: String, returnToArrearsModal: Boolean = true) {
        val user = _uiState.value.currentUser ?: return
        repository.saveWaArrearTemplate(user.assignedClassId, template)
        viewModelScope.launch {
            repository.logActivity(
                actorName = user.fullName,
                actorRole = RoleType.label(user.role),
                actionTitle = "Simpan Template WA Tunggakan",
                description = "Memperbarui template pesan WhatsApp tunggakan untuk orang tua/wali (${user.assignedClassName})"
            )
        }
        _uiState.update {
            it.copy(
                activeModal = if (returnToArrearsModal) ActiveModal.ArrearsManagementModal else ActiveModal.None,
                toastMessage = "Template WA tunggakan berhasil disimpan."
            )
        }
    }

    fun resetWaArrearTemplate(): String {
        val user = _uiState.value.currentUser
        return repository.resetWaArrearTemplate(user?.assignedClassId)
    }

    fun formatWaArrearMessage(
        arrear: ArrearPaymentEntity,
        student: StudentEntity?,
        customTemplate: String? = null
    ): String {
        val state = _uiState.value
        val template = customTemplate ?: getWaArrearTemplate()
        val sisa = (arrear.nominal - arrear.paidAmount).coerceAtLeast(0L)
        val waliName = student?.parentName?.takeIf { it.isNotBlank() } ?: "Orang Tua / Wali Siswa"
        val homeroomTeacher = state.classes.find { it.id == arrear.classId }?.homeroomTeacherName
            ?.takeIf { it.isNotBlank() }
            ?: state.currentUser?.fullName
            ?: "Wali Kelas"

        return template
            .replace("{nama_wali}", waliName)
            .replace("{nama_siswa}", arrear.studentName)
            .replace("{nis}", arrear.studentNis)
            .replace("{kelas}", arrear.className)
            .replace("{jenis_tagihan}", arrear.billType)
            .replace("{periode}", arrear.period)
            .replace("{nominal}", formatRupiah(arrear.nominal))
            .replace("{dibayar}", formatRupiah(arrear.paidAmount))
            .replace("{sisa_tunggakan}", formatRupiah(sisa))
            .replace("{status}", arrear.status)
            .replace("{sekolah}", state.settings.schoolName)
            .replace("{wali_kelas}", homeroomTeacher)
    }

    fun formatWaStudentAllArrearsMessage(
        student: StudentEntity?,
        studentArrears: List<ArrearPaymentEntity>,
        customTemplate: String? = null
    ): String {
        if (studentArrears.isEmpty()) return ""
        if (studentArrears.size == 1) {
            return formatWaArrearMessage(studentArrears.first(), student, customTemplate)
        }
        val first = studentArrears.first()
        val combinedBillTypes = studentArrears.joinToString(", ") { "${it.billType} (${it.period})" }
        val combinedPeriods = studentArrears.map { it.period }.distinct().joinToString(", ")
        val totalNominal = studentArrears.sumOf { it.nominal }
        val totalPaid = studentArrears.sumOf { it.paidAmount }
        val totalUnpaid = (totalNominal - totalPaid).coerceAtLeast(0L)
        val overallStatus = when {
            totalUnpaid == 0L -> "Lunas"
            totalPaid > 0L -> "Cicilan (${studentArrears.size} Tagihan)"
            else -> "Belum Lunas (${studentArrears.size} Tagihan)"
        }
        val syntheticArrear = first.copy(
            billType = combinedBillTypes,
            period = combinedPeriods,
            nominal = totalNominal,
            paidAmount = totalPaid,
            status = overallStatus
        )
        return formatWaArrearMessage(syntheticArrear, student, customTemplate)
    }

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onReady(json)
        }
    }

    fun importBackupJson(json: String) {
        val user = _uiState.value.currentUser ?: return
        if (user.role != RoleType.ADMIN) {
            showMessage("Hanya Admin yang dapat memulihkan cadangan seluruh madrasah.")
            return
        }
        viewModelScope.launch {
            val res = repository.importBackupJson(json, user.fullName, user.role)
            res.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        activeModal = ActiveModal.None,
                        toastMessage = msg
                    )
                }
            }.onFailure { err ->
                showMessage(err.message ?: "Gagal memulihkan data")
            }
        }
    }
}
