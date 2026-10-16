package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CourseEntity
import com.example.data.model.GradeUtils
import com.example.data.model.SemesterEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GradeTracker Pro", appName)
  }

  @Test
  fun `calculate SGPA correctly`() {
    val courses = listOf(
      CourseEntity(semesterId = 1, courseCode = "CS101", courseName = "Intro CS", creditHours = 4.0, grade = "A", gradePoint = 4.0),
      CourseEntity(semesterId = 1, courseCode = "MATH101", courseName = "Calculus", creditHours = 4.0, grade = "B", gradePoint = 3.0)
    )
    // Quality points = (4*4 + 4*3) = 28 / 8 = 3.5
    val sgpa = GradeUtils.calculateSgpa(courses)
    assertEquals(3.5, sgpa, 0.001)
  }

  @Test
  fun `attendance target consecutive classes computation`() {
    // 20 held, 14 attended -> 70%. Target 75%.
    // To reach 75%: (0.75 * 20 - 14) / 0.25 = (15 - 14) / 0.25 = 4 classes
    val course = CourseEntity(
      semesterId = 1,
      courseCode = "CS202",
      courseName = "Databases",
      creditHours = 3.0,
      classesHeld = 20,
      classesAttended = 14,
      requiredAttendancePercentage = 75.0
    )
    val status = course.getAttendanceSummary(75.0)
    assertEquals(false, status.isSafe)
    assertEquals(4, status.classesToAttend)
  }

  @Test
  fun `individual course target grade feedback computation`() {
    val courseOnTrack = CourseEntity(
      semesterId = 1,
      courseCode = "CS101",
      courseName = "Programming",
      creditHours = 4.0,
      grade = "A",
      gradePoint = 4.0,
      targetGrade = "A"
    )
    val feedbackOnTrack = courseOnTrack.getCourseTargetFeedback()
    assertTrue(feedbackOnTrack.isAchieved)

    val courseAtRisk = CourseEntity(
      semesterId = 1,
      courseCode = "CS205",
      courseName = "OS",
      creditHours = 4.0,
      grade = "B",
      gradePoint = 3.0,
      targetGrade = "A"
    )
    val feedbackAtRisk = courseAtRisk.getCourseTargetFeedback()
    assertEquals(false, feedbackAtRisk.isAchieved)
    assertEquals(-1.0, courseAtRisk.gradePointGap, 0.001)
  }

  @Test
  fun `sort semesters chronologically and by performance`() {
    val sem1 = SemesterEntity(id = 1, semesterNumber = 1, semesterName = "Sem 1")
    val sem2 = SemesterEntity(id = 2, semesterNumber = 2, semesterName = "Sem 2")
    val sem3 = SemesterEntity(id = 3, semesterNumber = 3, semesterName = "Sem 3")

    // Sem 1 SGPA = 3.0, Sem 2 SGPA = 4.0, Sem 3 SGPA = 3.5
    val courses = listOf(
      CourseEntity(semesterId = 1, courseCode = "C1", courseName = "C1", creditHours = 3.0, gradePoint = 3.0),
      CourseEntity(semesterId = 2, courseCode = "C2", courseName = "C2", creditHours = 3.0, gradePoint = 4.0),
      CourseEntity(semesterId = 3, courseCode = "C3", courseName = "C3", creditHours = 3.0, gradePoint = 3.5)
    )

    // Chronological ASC: Sem 1, Sem 2, Sem 3
    val chronoAsc = GradeUtils.sortSemesters(listOf(sem2, sem3, sem1), courses, GradeUtils.SemesterSortOrder.CHRONOLOGICAL_ASC)
    assertEquals(1, chronoAsc[0].semester.semesterNumber)
    assertEquals(2, chronoAsc[1].semester.semesterNumber)
    assertEquals(3, chronoAsc[2].semester.semesterNumber)

    // Chronological DESC: Sem 3, Sem 2, Sem 1
    val chronoDesc = GradeUtils.sortSemesters(listOf(sem2, sem3, sem1), courses, GradeUtils.SemesterSortOrder.CHRONOLOGICAL_DESC)
    assertEquals(3, chronoDesc[0].semester.semesterNumber)
    assertEquals(1, chronoDesc[2].semester.semesterNumber)

    // Performance DESC (Highest GPA): Sem 2 (4.0), Sem 3 (3.5), Sem 1 (3.0)
    val perfDesc = GradeUtils.sortSemesters(listOf(sem1, sem2, sem3), courses, GradeUtils.SemesterSortOrder.PERFORMANCE_DESC)
    assertEquals(2, perfDesc[0].semester.semesterNumber)
    assertEquals(4.0, perfDesc[0].sgpa, 0.001)
    assertEquals(3, perfDesc[1].semester.semesterNumber)
    assertEquals(1, perfDesc[2].semester.semesterNumber)
  }
}
