package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AcademicProfileEntity
import com.example.data.model.CourseEntity
import com.example.data.model.GradeUtils
import com.example.data.model.SemesterEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateAndShareReport(
        context: Context,
        profile: AcademicProfileEntity,
        semestersWithCourses: List<Pair<SemesterEntity, List<CourseEntity>>>
    ): Result<File> {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(30, 58, 138) // Deep Blue
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(71, 85, 105) // Slate
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
        }

        var yPos = 40f

        // 1. Header Banner
        canvas.drawText("GRADETRACKER PRO — ACADEMIC PERFORMANCE REPORT", 36f, yPos, titlePaint)
        yPos += 15f
        val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on $dateStr • Secure Local Export", 36f, yPos, subtitlePaint)
        yPos += 15f
        canvas.drawLine(36f, yPos, 559f, yPos, linePaint)
        yPos += 20f

        // 2. Student & Degree Summary Card
        canvas.drawRect(36f, yPos, 559f, yPos + 60f, headerBgPaint)
        canvas.drawText("Student: ${profile.studentName}", 46f, yPos + 18f, boldTextPaint)
        canvas.drawText("Degree: ${profile.degreeName}", 46f, yPos + 34f, textPaint)
        canvas.drawText("University: ${profile.universityName}", 46f, yPos + 50f, textPaint)

        // Calculate Overall CGPA
        val map = semestersWithCourses.toMap()
        val currentCgpa = GradeUtils.calculateCgpa(map)
        val completedCourses = semestersWithCourses.filter { it.first.isCompleted }.flatMap { it.second }
        val completedCredits = completedCourses.sumOf { it.creditHours }
        val completedSemesters = semestersWithCourses.count { it.first.isCompleted }

        canvas.drawText("Current CGPA: ${String.format("%.2f", currentCgpa)} / ${profile.gradingScale}", 320f, yPos + 18f, boldTextPaint)
        canvas.drawText("Target CGPA: ${String.format("%.2f", profile.targetCgpa)} / ${profile.gradingScale}", 320f, yPos + 34f, boldTextPaint)
        canvas.drawText("Credits Completed: ${completedCredits.toInt()} / ${profile.totalDegreeCredits.toInt()} hrs", 320f, yPos + 50f, textPaint)
        yPos += 75f

        // 3. Semesters & Courses Breakdown
        canvas.drawText("SEMESTER-BY-SEMESTER ACADEMIC RECORD", 36f, yPos, sectionPaint)
        yPos += 16f

        for ((semester, courses) in semestersWithCourses) {
            val semSgpa = GradeUtils.calculateSgpa(courses)
            val semCredits = courses.sumOf { it.creditHours }

            // Semester Header
            canvas.drawRect(36f, yPos, 559f, yPos + 18f, headerBgPaint)
            canvas.drawText(
                "${semester.semesterName}  —  SGPA: ${String.format("%.2f", semSgpa)}  (${semCredits.toInt()} Credits)  [${if (semester.isCompleted) "Completed" else "In Progress"}]",
                44f,
                yPos + 13f,
                boldTextPaint
            )
            yPos += 22f

            // Table Header
            canvas.drawText("Course Code & Title", 44f, yPos + 10f, boldTextPaint)
            canvas.drawText("Credits", 290f, yPos + 10f, boldTextPaint)
            canvas.drawText("Grade", 345f, yPos + 10f, boldTextPaint)
            canvas.drawText("GP", 390f, yPos + 10f, boldTextPaint)
            canvas.drawText("Attendance", 435f, yPos + 10f, boldTextPaint)
            canvas.drawText("Syllabus", 500f, yPos + 10f, boldTextPaint)
            yPos += 14f
            canvas.drawLine(36f, yPos, 559f, yPos, linePaint)
            yPos += 12f

            for (course in courses) {
                if (yPos > 790f) break // Avoid overflowing single page
                val courseNameTruncated = if (course.courseName.length > 32) course.courseName.take(30) + ".." else course.courseName
                canvas.drawText("${course.courseCode}: $courseNameTruncated", 44f, yPos, textPaint)
                canvas.drawText("${course.creditHours.toInt()}", 300f, yPos, textPaint)
                canvas.drawText(course.grade, 350f, yPos, boldTextPaint)
                canvas.drawText(String.format("%.1f", course.gradePoint), 392f, yPos, textPaint)
                val attStr = "${String.format("%.0f", course.attendancePercentage)}% (${course.classesAttended}/${course.classesHeld})"
                canvas.drawText(attStr, 435f, yPos, textPaint)
                canvas.drawText("${course.syllabusCompletionPercentage.toInt()}%", 510f, yPos, textPaint)
                yPos += 14f
            }
            yPos += 8f
        }

        // 4. Future Term Target Strategy Breakdown
        val targetBreakdown = GradeUtils.calculateTargetBreakdown(
            currentCgpa = currentCgpa,
            completedCredits = completedCredits,
            completedSemesters = completedSemesters,
            totalDegreeSemesters = profile.totalDegreeSemesters,
            totalDegreeCredits = profile.totalDegreeCredits,
            targetCgpa = profile.targetCgpa,
            isScale10 = profile.gradingScale == "10.0"
        )

        if (yPos <= 760f) {
            yPos += 10f
            canvas.drawText("FUTURE SEMESTERS TARGET GRADE BREAKDOWN", 36f, yPos, sectionPaint)
            yPos += 16f
            canvas.drawRect(36f, yPos, 559f, yPos + 48f, headerBgPaint)

            canvas.drawText(
                "Required Average SGPA for Remaining ${targetBreakdown.remainingSemesters} Semesters: ${String.format("%.2f", targetBreakdown.requiredAverageSgpa)} / ${profile.gradingScale}",
                46f,
                yPos + 16f,
                boldTextPaint
            )
            canvas.drawText(
                "Feasibility Status: ${targetBreakdown.feasibilityMessage}",
                46f,
                yPos + 30f,
                textPaint
            )
            canvas.drawText(
                "Strategic Plan: ${targetBreakdown.recommendedGradeStrategy}",
                46f,
                yPos + 44f,
                textPaint
            )
        }

        pdfDocument.finishPage(page)

        // Save PDF to cache
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()
            val file = File(reportsDir, "GradeTracker_Performance_Report.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            // Launch Share Intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "GradeTracker Pro Academic Performance Report")
                putExtra(Intent.EXTRA_TEXT, "Detailed Academic Performance & Attendance Report generated by GradeTracker Pro.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Academic PDF Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            Result.success(file)
        } catch (e: Exception) {
            pdfDocument.close()
            Result.failure(e)
        }
    }
}
