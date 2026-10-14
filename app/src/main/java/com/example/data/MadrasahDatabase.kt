package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.security.MessageDigest

@Database(
    entities = [
        UserEntity::class,
        SchoolClassEntity::class,
        StudentEntity::class,
        TeacherEntity::class,
        AttendanceEntity::class,
        TeacherAttendanceEntity::class,
        StudentNoteEntity::class,
        StudentAchievementEntity::class,
        StudentViolationEntity::class,
        ArrearPaymentEntity::class,
        SchoolSettingsEntity::class,
        AcademicYearEntity::class,
        ActivityLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MadrasahDatabase : RoomDatabase() {
    abstract fun madrasahDao(): MadrasahDao

    companion object {
        @Volatile
        private var INSTANCE: MadrasahDatabase? = null

        fun getInstance(context: Context): MadrasahDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MadrasahDatabase::class.java,
                    "absensi_mi_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object PasswordSecurity {
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies password against stored SHA-256 hash or legacy baseline password format.
     * Returns Pair(isValid, needsMigrationToHash).
     */
    fun verifyPassword(rawInput: String, user: UserEntity): Pair<Boolean, Boolean> {
        val trimmed = rawInput.trim()
        val inputHash = sha256(trimmed)

        if (user.passwordHash.isNotBlank() && user.passwordHash.equals(inputHash, ignoreCase = true)) {
            return Pair(true, user.legacyPassword.isNotBlank())
        }
        // Check if passwordHash itself stored a legacy format or plaintext from baseline import
        if (user.passwordHash == trimmed) {
            return Pair(true, true)
        }
        if (user.legacyPassword.isNotBlank() && user.legacyPassword == trimmed) {
            return Pair(true, true)
        }
        return Pair(false, false)
    }
}
