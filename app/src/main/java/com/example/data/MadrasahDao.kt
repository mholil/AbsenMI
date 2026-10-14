package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MadrasahDao {

    // --- USERS ---
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    // --- CLASSES ---
    @Query("SELECT * FROM classes ORDER BY gradeLevel ASC, className ASC")
    fun observeClasses(): Flow<List<SchoolClassEntity>>

    @Query("SELECT * FROM classes ORDER BY gradeLevel ASC, className ASC")
    suspend fun getAllClassesOnce(): List<SchoolClassEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(schoolClass: SchoolClassEntity): Long

    @Delete
    suspend fun deleteClass(schoolClass: SchoolClassEntity)

    // --- STUDENTS (SISWA) ---
    @Query("SELECT * FROM students ORDER BY className ASC, fullName ASC")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY fullName ASC")
    fun observeSiswaByClass(classId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY className ASC, fullName ASC")
    suspend fun getAllStudentsOnce(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE nis = :nis OR qrCodeValue = :qr LIMIT 1")
    suspend fun findStudentByNisOrQr(nis: String, qr: String): StudentEntity?

    @Transaction
    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    fun observeSiswaWithAbsensi(studentId: Long): Flow<SiswaWithAbsensi?>

    @Transaction
    @Query("SELECT * FROM students ORDER BY className ASC, fullName ASC")
    fun observeAllSiswaWithAbsensi(): Flow<List<SiswaWithAbsensi>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    // --- TEACHERS ---
    @Query("SELECT * FROM teachers ORDER BY fullName ASC")
    fun observeTeachers(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers ORDER BY fullName ASC")
    suspend fun getAllTeachersOnce(): List<TeacherEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity): Long

    @Delete
    suspend fun deleteTeacher(teacher: TeacherEntity)

    // --- STUDENT ATTENDANCE (ABSENSI) ---
    @Query("SELECT * FROM attendance ORDER BY date DESC, time DESC, id DESC")
    fun observeAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY time DESC, id DESC")
    fun observeAbsensiByDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC, time DESC")
    fun observeAbsensiBySiswa(studentId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance ORDER BY date DESC, time DESC, id DESC")
    suspend fun getAllAttendanceOnce(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY time DESC, id DESC")
    suspend fun getAbsensiByDateOnce(date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun findAttendanceByStudentAndDate(studentId: Long, date: String): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    // --- TEACHER ATTENDANCE ---
    @Query("SELECT * FROM teacher_attendance ORDER BY date DESC, timeIn DESC")
    fun observeTeacherAttendance(): Flow<List<TeacherAttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacherAttendance(item: TeacherAttendanceEntity): Long

    // --- STUDENT NOTES ---
    @Query("SELECT * FROM notes ORDER BY date DESC, id DESC")
    fun observeNotes(): Flow<List<StudentNoteEntity>>

    @Query("SELECT * FROM notes ORDER BY date DESC, id DESC")
    suspend fun getAllNotesOnce(): List<StudentNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudentNoteEntity): Long

    @Delete
    suspend fun deleteNote(note: StudentNoteEntity)

    // --- STUDENT ACHIEVEMENTS ---
    @Query("SELECT * FROM achievements ORDER BY date DESC, id DESC")
    fun observeAchievements(): Flow<List<StudentAchievementEntity>>

    @Query("SELECT * FROM achievements ORDER BY date DESC, id DESC")
    suspend fun getAllAchievementsOnce(): List<StudentAchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: StudentAchievementEntity): Long

    @Delete
    suspend fun deleteAchievement(achievement: StudentAchievementEntity)

    // --- STUDENT VIOLATIONS ---
    @Query("SELECT * FROM violations ORDER BY date DESC, id DESC")
    fun observeViolations(): Flow<List<StudentViolationEntity>>

    @Query("SELECT * FROM violations ORDER BY date DESC, id DESC")
    suspend fun getAllViolationsOnce(): List<StudentViolationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViolation(violation: StudentViolationEntity): Long

    @Delete
    suspend fun deleteViolation(violation: StudentViolationEntity)

    // --- ARREARS & PAYMENTS ---
    @Query("SELECT * FROM arrears_payments ORDER BY className ASC, studentName ASC")
    fun observeArrears(): Flow<List<ArrearPaymentEntity>>

    @Query("SELECT * FROM arrears_payments ORDER BY className ASC, studentName ASC")
    suspend fun getAllArrearsOnce(): List<ArrearPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArrear(arrear: ArrearPaymentEntity): Long

    @Delete
    suspend fun deleteArrear(arrear: ArrearPaymentEntity)

    // --- SCHOOL SETTINGS ---
    @Query("SELECT * FROM school_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<SchoolSettingsEntity?>

    @Query("SELECT * FROM school_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): SchoolSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SchoolSettingsEntity)

    // --- ACADEMIC YEARS ---
    @Query("SELECT * FROM academic_years ORDER BY isActive DESC, yearName DESC")
    fun observeAcademicYears(): Flow<List<AcademicYearEntity>>

    @Query("SELECT * FROM academic_years ORDER BY isActive DESC, yearName DESC")
    suspend fun getAllAcademicYearsOnce(): List<AcademicYearEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicYear(year: AcademicYearEntity): Long

    @Delete
    suspend fun deleteAcademicYear(year: AcademicYearEntity)

    // --- ACTIVITY LOGS ---
    @Query("SELECT * FROM activity_logs ORDER BY id DESC LIMIT 150")
    fun observeActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long
}
