package com.example.data.db

import androidx.room.*
import com.example.data.model.AcademicProfileEntity
import com.example.data.model.ClassScheduleEntity
import com.example.data.model.CourseEntity
import com.example.data.model.SemesterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY semesterNumber ASC")
    fun getAllSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters ORDER BY semesterNumber ASC")
    suspend fun getAllSemestersList(): List<SemesterEntity>

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getSemesterById(id: Long): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity): Long

    @Update
    suspend fun updateSemester(semester: SemesterEntity)

    @Delete
    suspend fun deleteSemester(semester: SemesterEntity)

    @Query("DELETE FROM semesters")
    suspend fun clearAll()
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY id ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY id ASC")
    suspend fun getAllCoursesList(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY id ASC")
    fun getCoursesBySemester(semesterId: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY id ASC")
    suspend fun getCoursesBySemesterList(semesterId: Long): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteCoursesBySemester(semesterId: Long)

    @Query("DELETE FROM courses")
    suspend fun clearAll()
}

@Dao
interface AcademicProfileDao {
    @Query("SELECT * FROM academic_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<AcademicProfileEntity?>

    @Query("SELECT * FROM academic_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): AcademicProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AcademicProfileEntity)

    @Update
    suspend fun updateProfile(profile: AcademicProfileEntity)
}

@Dao
interface ClassScheduleDao {
    @Query("SELECT * FROM class_schedules ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSchedules(): Flow<List<ClassScheduleEntity>>

    @Query("SELECT * FROM class_schedules ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllSchedulesList(): List<ClassScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ClassScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ClassScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ClassScheduleEntity)

    @Query("DELETE FROM class_schedules")
    suspend fun clearAll()
}
