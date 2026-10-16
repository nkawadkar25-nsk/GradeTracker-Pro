package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.AcademicProfileEntity
import com.example.data.model.ClassScheduleEntity
import com.example.data.model.CourseEntity
import com.example.data.model.SemesterEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class GradeTrackerRepository(private val database: AppDatabase) {
    private val semesterDao = database.semesterDao()
    private val courseDao = database.courseDao()
    private val profileDao = database.academicProfileDao()
    private val scheduleDao = database.classScheduleDao()

    val semesters: Flow<List<SemesterEntity>> = semesterDao.getAllSemesters()
    val allCourses: Flow<List<CourseEntity>> = courseDao.getAllCourses()
    val profile: Flow<AcademicProfileEntity?> = profileDao.getProfile()
    val schedules: Flow<List<ClassScheduleEntity>> = scheduleDao.getAllSchedules()

    fun getCoursesForSemester(semesterId: Long): Flow<List<CourseEntity>> =
        courseDao.getCoursesBySemester(semesterId)

    suspend fun getProfileSync(): AcademicProfileEntity {
        return profileDao.getProfileSync() ?: AcademicProfileEntity()
    }

    suspend fun updateProfile(profile: AcademicProfileEntity) {
        profileDao.insertProfile(profile)
    }

    suspend fun addSemester(semester: SemesterEntity): Long {
        return semesterDao.insertSemester(semester)
    }

    suspend fun updateSemester(semester: SemesterEntity) {
        semesterDao.updateSemester(semester)
    }

    suspend fun deleteSemester(semester: SemesterEntity) {
        courseDao.deleteCoursesBySemester(semester.id)
        semesterDao.deleteSemester(semester)
    }

    suspend fun addCourse(course: CourseEntity): Long {
        return courseDao.insertCourse(course)
    }

    suspend fun updateCourse(course: CourseEntity) {
        courseDao.updateCourse(course)
    }

    suspend fun deleteCourse(course: CourseEntity) {
        courseDao.deleteCourse(course)
    }

    suspend fun markAttendance(courseId: Long, attended: Boolean) {
        val course = courseDao.getCourseById(courseId) ?: return
        val newHeld = course.classesHeld + 1
        val newAttended = if (attended) course.classesAttended + 1 else course.classesAttended
        courseDao.updateCourse(course.copy(classesHeld = newHeld, classesAttended = newAttended))
    }

    suspend fun addSchedule(schedule: ClassScheduleEntity): Long {
        return scheduleDao.insertSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: ClassScheduleEntity) {
        scheduleDao.deleteSchedule(schedule)
    }

    suspend fun resetToDefaultDemo() {
        semesterDao.clearAll()
        courseDao.clearAll()
        scheduleDao.clearAll()
        AppDatabase.populateInitialData(database)
    }

    // Cross-Device Sync JSON Export
    suspend fun exportDatabaseToJson(): String {
        val root = JSONObject()
        val profile = profileDao.getProfileSync() ?: AcademicProfileEntity()
        val profileJson = JSONObject().apply {
            put("studentName", profile.studentName)
            put("degreeName", profile.degreeName)
            put("universityName", profile.universityName)
            put("targetCgpa", profile.targetCgpa)
            put("totalDegreeSemesters", profile.totalDegreeSemesters)
            put("totalDegreeCredits", profile.totalDegreeCredits)
            put("gradingScale", profile.gradingScale)
            put("defaultAttendanceThreshold", profile.defaultAttendanceThreshold)
            put("biometricLockEnabled", profile.biometricLockEnabled)
            put("darkModePreference", profile.darkModePreference)
        }
        root.put("profile", profileJson)

        val semesters = semesterDao.getAllSemestersList()
        val semestersArray = JSONArray()
        for (sem in semesters) {
            val semJson = JSONObject().apply {
                put("semesterNumber", sem.semesterNumber)
                put("semesterName", sem.semesterName)
                put("isCompleted", sem.isCompleted)
                put("orderIndex", sem.orderIndex)

                val courses = courseDao.getCoursesBySemesterList(sem.id)
                val coursesArray = JSONArray()
                for (c in courses) {
                    val cJson = JSONObject().apply {
                        put("courseCode", c.courseCode)
                        put("courseName", c.courseName)
                        put("creditHours", c.creditHours)
                        put("grade", c.grade)
                        put("gradePoint", c.gradePoint)
                        put("classesHeld", c.classesHeld)
                        put("classesAttended", c.classesAttended)
                        put("requiredAttendancePercentage", c.requiredAttendancePercentage)
                        put("syllabusCompletionPercentage", c.syllabusCompletionPercentage)
                        put("targetGrade", c.targetGrade)
                        put("instructorName", c.instructorName)
                        put("classSchedule", c.classSchedule)
                    }
                    coursesArray.put(cJson)
                }
                put("courses", coursesArray)
            }
            semestersArray.put(semJson)
        }
        root.put("semesters", semestersArray)

        val schedules = scheduleDao.getAllSchedulesList()
        val schedArray = JSONArray()
        for (s in schedules) {
            val sJson = JSONObject().apply {
                put("courseName", s.courseName)
                put("dayOfWeek", s.dayOfWeek)
                put("startTime", s.startTime)
                put("room", s.room)
            }
            schedArray.put(sJson)
        }
        root.put("schedules", schedArray)

        return root.toString(2)
    }

    // Cross-Device Sync JSON Import
    suspend fun importDatabaseFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                profileDao.insertProfile(
                    AcademicProfileEntity(
                        id = 1,
                        studentName = p.optString("studentName", "Alex Morgan"),
                        degreeName = p.optString("degreeName", "B.S. Computer Science"),
                        universityName = p.optString("universityName", "University"),
                        targetCgpa = p.optDouble("targetCgpa", 3.85),
                        totalDegreeSemesters = p.optInt("totalDegreeSemesters", 8),
                        totalDegreeCredits = p.optDouble("totalDegreeCredits", 128.0),
                        gradingScale = p.optString("gradingScale", "4.0"),
                        defaultAttendanceThreshold = p.optDouble("defaultAttendanceThreshold", 75.0),
                        biometricLockEnabled = p.optBoolean("biometricLockEnabled", false),
                        darkModePreference = p.optString("darkModePreference", "SYSTEM")
                    )
                )
            }

            if (root.has("semesters")) {
                semesterDao.clearAll()
                courseDao.clearAll()
                val semestersArray = root.getJSONArray("semesters")
                for (i in 0 until semestersArray.length()) {
                    val sObj = semestersArray.getJSONObject(i)
                    val semId = semesterDao.insertSemester(
                        SemesterEntity(
                            semesterNumber = sObj.optInt("semesterNumber", i + 1),
                            semesterName = sObj.optString("semesterName", "Semester ${i + 1}"),
                            isCompleted = sObj.optBoolean("isCompleted", false),
                            orderIndex = sObj.optInt("orderIndex", i + 1)
                        )
                    )
                    if (sObj.has("courses")) {
                        val cArray = sObj.getJSONArray("courses")
                        for (j in 0 until cArray.length()) {
                            val cObj = cArray.getJSONObject(j)
                            courseDao.insertCourse(
                                CourseEntity(
                                    semesterId = semId,
                                    courseCode = cObj.optString("courseCode", "CS10$j"),
                                    courseName = cObj.optString("courseName", "Course $j"),
                                    creditHours = cObj.optDouble("creditHours", 3.0),
                                    grade = cObj.optString("grade", "A"),
                                    gradePoint = cObj.optDouble("gradePoint", 4.0),
                                    classesHeld = cObj.optInt("classesHeld", 0),
                                    classesAttended = cObj.optInt("classesAttended", 0),
                                    requiredAttendancePercentage = cObj.optDouble("requiredAttendancePercentage", 75.0),
                                    syllabusCompletionPercentage = cObj.optDouble("syllabusCompletionPercentage", 0.0),
                                    targetGrade = cObj.optString("targetGrade", "A"),
                                    instructorName = cObj.optString("instructorName", ""),
                                    classSchedule = cObj.optString("classSchedule", "")
                                )
                            )
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
