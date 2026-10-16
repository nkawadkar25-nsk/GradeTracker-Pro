package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AcademicProfileEntity
import com.example.data.model.ClassScheduleEntity
import com.example.data.model.CourseEntity
import com.example.data.model.SemesterEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SemesterEntity::class,
        CourseEntity::class,
        AcademicProfileEntity::class,
        ClassScheduleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun courseDao(): CourseDao
    abstract fun academicProfileDao(): AcademicProfileDao
    abstract fun classScheduleDao(): ClassScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gradetracker_pro.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val profileDao = database.academicProfileDao()
            val semesterDao = database.semesterDao()
            val courseDao = database.courseDao()
            val scheduleDao = database.classScheduleDao()

            // Initial Profile
            profileDao.insertProfile(
                AcademicProfileEntity(
                    id = 1,
                    studentName = "Alex Morgan",
                    degreeName = "B.S. Computer Science",
                    universityName = "Metropolitan Tech",
                    targetCgpa = 3.85,
                    totalDegreeSemesters = 8,
                    totalDegreeCredits = 128.0,
                    gradingScale = "4.0",
                    defaultAttendanceThreshold = 75.0,
                    biometricLockEnabled = false,
                    darkModePreference = "SYSTEM"
                )
            )

            // Semester 1
            val sem1Id = semesterDao.insertSemester(
                SemesterEntity(semesterNumber = 1, semesterName = "Semester 1 (Fall 2024)", isCompleted = true, orderIndex = 1)
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem1Id,
                    courseCode = "CS101",
                    courseName = "Introduction to Programming",
                    creditHours = 4.0,
                    grade = "A",
                    gradePoint = 4.0,
                    classesHeld = 32,
                    classesAttended = 30,
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 100.0,
                    instructorName = "Dr. Alan Turing"
                )
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem1Id,
                    courseCode = "MATH101",
                    courseName = "Calculus I",
                    creditHours = 4.0,
                    grade = "A-",
                    gradePoint = 3.7,
                    classesHeld = 30,
                    classesAttended = 27,
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 100.0,
                    instructorName = "Prof. Newton"
                )
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem1Id,
                    courseCode = "PHY101",
                    courseName = "Physics Mechanics & Lab",
                    creditHours = 4.0,
                    grade = "B+",
                    gradePoint = 3.3,
                    classesHeld = 28,
                    classesAttended = 24,
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 100.0,
                    instructorName = "Dr. Faraday"
                )
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem1Id,
                    courseCode = "ENG101",
                    courseName = "Technical Writing",
                    creditHours = 3.0,
                    grade = "A",
                    gradePoint = 4.0,
                    classesHeld = 24,
                    classesAttended = 23,
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 100.0,
                    instructorName = "Prof. Orwell"
                )
            )

            // Semester 2
            val sem2Id = semesterDao.insertSemester(
                SemesterEntity(semesterNumber = 2, semesterName = "Semester 2 (Spring 2025)", isCompleted = true, orderIndex = 2)
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem2Id,
                    courseCode = "CS102",
                    courseName = "Data Structures & OOP",
                    creditHours = 4.0,
                    grade = "A",
                    gradePoint = 4.0,
                    classesHeld = 34,
                    classesAttended = 32,
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 100.0,
                    instructorName = "Dr. Knuth"
                )
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem2Id,
                    courseCode = "MATH102",
                    courseName = "Discrete Mathematics",
                    creditHours = 3.0,
                    grade = "B+",
                    gradePoint = 3.3,
                    classesHeld = 28,
                    classesAttended = 22,
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 100.0,
                    instructorName = "Dr. Lovelace"
                )
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem2Id,
                    courseCode = "CS105",
                    courseName = "Digital Logic & Computer Org",
                    creditHours = 4.0,
                    grade = "A-",
                    gradePoint = 3.7,
                    classesHeld = 30,
                    classesAttended = 28,
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 100.0,
                    instructorName = "Prof. Shannon"
                )
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem2Id,
                    courseCode = "STAT201",
                    courseName = "Probability & Statistics",
                    creditHours = 3.0,
                    grade = "A",
                    gradePoint = 4.0,
                    classesHeld = 26,
                    classesAttended = 25,
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 100.0,
                    instructorName = "Dr. Bayes"
                )
            )

            // Semester 3 (Current / Active)
            val sem3Id = semesterDao.insertSemester(
                SemesterEntity(semesterNumber = 3, semesterName = "Semester 3 (Fall 2025 - Active)", isCompleted = false, orderIndex = 3)
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem3Id,
                    courseCode = "CS201",
                    courseName = "Algorithms Design & Analysis",
                    creditHours = 4.0,
                    grade = "A",
                    gradePoint = 4.0,
                    classesHeld = 24,
                    classesAttended = 22, // 91.6% attendance
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 70.0,
                    instructorName = "Dr. Cormen",
                    classSchedule = "Mon, Wed 10:00 AM"
                )
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem3Id,
                    courseCode = "CS202",
                    courseName = "Database Management Systems",
                    creditHours = 3.0,
                    grade = "A-",
                    gradePoint = 3.7,
                    classesHeld = 20,
                    classesAttended = 14, // 70.0% attendance (Warning: under 75%!)
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 65.0,
                    instructorName = "Dr. Codd",
                    classSchedule = "Tue, Thu 02:00 PM"
                )
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem3Id,
                    courseCode = "MATH201",
                    courseName = "Linear Algebra & Applications",
                    creditHours = 3.0,
                    grade = "B+",
                    gradePoint = 3.3,
                    classesHeld = 22,
                    classesAttended = 18, // 81.8% attendance
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 80.0,
                    instructorName = "Prof. Strang",
                    classSchedule = "Mon, Wed 01:00 PM"
                )
            )
            courseDao.insertCourse(
                CourseEntity(
                    semesterId = sem3Id,
                    courseCode = "CS205",
                    courseName = "Operating Systems Principles",
                    creditHours = 4.0,
                    grade = "B",
                    gradePoint = 3.0,
                    classesHeld = 22,
                    classesAttended = 19, // 86.3% attendance
                    requiredAttendancePercentage = 75.0,
                    syllabusCompletionPercentage = 60.0,
                    instructorName = "Dr. Tanenbaum",
                    classSchedule = "Fri 09:00 AM"
                )
            )

            // Initial Class Schedules for Reminders & Notifications
            scheduleDao.insertSchedule(
                ClassScheduleEntity(
                    courseName = "CS201 - Algorithms Design",
                    dayOfWeek = "Monday",
                    startTime = "10:00 AM",
                    room = "Turing Hall 204",
                    alertBeforeMinutes = 15
                )
            )
            scheduleDao.insertSchedule(
                ClassScheduleEntity(
                    courseName = "CS202 - Database Systems",
                    dayOfWeek = "Tuesday",
                    startTime = "02:00 PM",
                    room = "Codd Lab 102",
                    alertBeforeMinutes = 15
                )
            )
            scheduleDao.insertSchedule(
                ClassScheduleEntity(
                    courseName = "MATH201 - Linear Algebra",
                    dayOfWeek = "Wednesday",
                    startTime = "01:00 PM",
                    room = "Strang Lecture Hall 3",
                    alertBeforeMinutes = 15
                )
            )
        }
    }
}
