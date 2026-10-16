package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterNumber: Int,
    val semesterName: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long,
    val courseCode: String,
    val courseName: String,
    val creditHours: Double,
    val grade: String = "A",
    val gradePoint: Double = 4.0,
    val classesHeld: Int = 0,
    val classesAttended: Int = 0,
    val requiredAttendancePercentage: Double = 75.0,
    val syllabusCompletionPercentage: Double = 0.0,
    val targetGrade: String = "A",
    val instructorName: String = "",
    val classSchedule: String = ""
) {
    val attendancePercentage: Double
        get() = if (classesHeld > 0) (classesAttended.toDouble() / classesHeld) * 100.0 else 100.0

    val targetGradePoint: Double
        get() = GradeUtils.getGradePoint(targetGrade)

    val gradePointGap: Double
        get() = gradePoint - targetGradePoint

    val isTargetAchieved: Boolean
        get() = gradePoint >= targetGradePoint

    fun getCourseTargetFeedback(): CourseTargetFeedback {
        val gap = gradePointGap
        return when {
            gap > 0.0 -> CourseTargetFeedback(
                isAchieved = true,
                statusLabel = "Exceeding Target",
                gapText = "+${String.format("%.1f", gap)} GP",
                feedbackMessage = "Outstanding! You're performing above your target ($grade > $targetGrade). Maintain consistency in remaining assignments.",
                urgencyLevel = UrgencyLevel.SAFE
            )
            gap == 0.0 -> CourseTargetFeedback(
                isAchieved = true,
                statusLabel = "Target Met",
                gapText = "On Target",
                feedbackMessage = "Right on track! Your current grade ($grade) matches your course target ($targetGrade). Keep up the good work on exams.",
                urgencyLevel = UrgencyLevel.SAFE
            )
            gap >= -0.35 -> CourseTargetFeedback(
                isAchieved = false,
                statusLabel = "Near Target",
                gapText = "${String.format("%.1f", gap)} GP",
                feedbackMessage = "Close to target ($grade vs target $targetGrade). Scoring high on the next quiz/project will bridge this gap.",
                urgencyLevel = UrgencyLevel.MODERATE
            )
            else -> CourseTargetFeedback(
                isAchieved = false,
                statusLabel = "Target At Risk",
                gapText = "${String.format("%.1f", gap)} GP",
                feedbackMessage = "Needs attention: currently at $grade ($gradePoint) vs target $targetGrade ($targetGradePoint). Requires +${String.format("%.1f", -gap)} GP boost.",
                urgencyLevel = UrgencyLevel.HIGH
            )
        }
    }

    // Attendance computation
    fun getAttendanceSummary(targetPercentage: Double = requiredAttendancePercentage): AttendanceStatus {
        if (classesHeld == 0) {
            return AttendanceStatus(
                currentPercentage = 100.0,
                isSafe = true,
                classesToAttend = 0,
                classesCanMiss = 0,
                message = "No classes held yet. Ready for the semester!"
            )
        }

        val currentPct = attendancePercentage
        val threshold = targetPercentage / 100.0

        return if (currentPct < targetPercentage) {
            val numerator = (threshold * classesHeld) - classesAttended
            val denominator = 1.0 - threshold
            val needed = if (denominator > 0) kotlin.math.ceil(numerator / denominator).toInt().coerceAtLeast(1) else 1
            AttendanceStatus(
                currentPercentage = currentPct,
                isSafe = false,
                classesToAttend = needed,
                classesCanMiss = 0,
                message = "Attend next $needed consecutive class${if (needed > 1) "es" else ""} to reach ${targetPercentage.toInt()}%."
            )
        } else {
            val canMiss = kotlin.math.floor((classesAttended - threshold * classesHeld) / threshold).toInt().coerceAtLeast(0)
            AttendanceStatus(
                currentPercentage = currentPct,
                isSafe = true,
                classesToAttend = 0,
                classesCanMiss = canMiss,
                message = if (canMiss > 0) "You can safely miss $canMiss class${if (canMiss > 1) "es" else ""}." else "Attendance is on the line. Don't miss the next class!"
            )
        }
    }
}

enum class UrgencyLevel {
    SAFE, MODERATE, HIGH
}

data class CourseTargetFeedback(
    val isAchieved: Boolean,
    val statusLabel: String,
    val gapText: String,
    val feedbackMessage: String,
    val urgencyLevel: UrgencyLevel
)

data class AttendanceStatus(
    val currentPercentage: Double,
    val isSafe: Boolean,
    val classesToAttend: Int,
    val classesCanMiss: Int,
    val message: String
)

@Entity(tableName = "academic_profile")
data class AcademicProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val studentName: String = "Alex Morgan",
    val degreeName: String = "B.S. Computer Science",
    val universityName: String = "University Tech",
    val targetCgpa: Double = 3.85,
    val totalDegreeSemesters: Int = 8,
    val totalDegreeCredits: Double = 128.0,
    val gradingScale: String = "4.0",
    val defaultAttendanceThreshold: Double = 75.0,
    val biometricLockEnabled: Boolean = false,
    val darkModePreference: String = "SYSTEM" // SYSTEM, LIGHT, DARK
)

@Entity(tableName = "class_schedules")
data class ClassScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long = 0,
    val courseName: String,
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val startTime: String, // "10:00 AM"
    val room: String = "Hall A-101",
    val alertBeforeMinutes: Int = 15
)
