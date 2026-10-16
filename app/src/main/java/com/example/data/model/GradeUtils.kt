package com.example.data.model

data class GradeDefinition(
    val letter: String,
    val point: Double,
    val description: String
)

object GradeUtils {
    val scale4Points = listOf(
        GradeDefinition("A+", 4.0, "Outstanding (95-100%)"),
        GradeDefinition("A", 4.0, "Excellent (90-94%)"),
        GradeDefinition("A-", 3.7, "Very Good (85-89%)"),
        GradeDefinition("B+", 3.3, "Good (80-84%)"),
        GradeDefinition("B", 3.0, "Above Average (75-79%)"),
        GradeDefinition("B-", 2.7, "Average (70-74%)"),
        GradeDefinition("C+", 2.3, "Fair (65-69%)"),
        GradeDefinition("C", 2.0, "Satisfactory (60-64%)"),
        GradeDefinition("C-", 1.7, "Pass (55-59%)"),
        GradeDefinition("D", 1.0, "Marginal Pass (50-54%)"),
        GradeDefinition("F", 0.0, "Fail (<50%)")
    )

    val scale10Points = listOf(
        GradeDefinition("O", 10.0, "Outstanding"),
        GradeDefinition("A+", 9.0, "Excellent"),
        GradeDefinition("A", 8.0, "Very Good"),
        GradeDefinition("B+", 7.0, "Good"),
        GradeDefinition("B", 6.0, "Above Average"),
        GradeDefinition("C", 5.0, "Average"),
        GradeDefinition("P", 4.0, "Pass"),
        GradeDefinition("F", 0.0, "Fail")
    )

    fun getGradePoint(letter: String, isScale10: Boolean = false): Double {
        val list = if (isScale10) scale10Points else scale4Points
        return list.firstOrNull { it.letter.equals(letter.trim(), ignoreCase = true) }?.point ?: 0.0
    }

    fun getGrades(isScale10: Boolean = false): List<GradeDefinition> {
        return if (isScale10) scale10Points else scale4Points
    }

    fun calculateSgpa(courses: List<CourseEntity>): Double {
        val gradedCourses = courses.filter { it.creditHours > 0 }
        val totalCredits = gradedCourses.sumOf { it.creditHours }
        if (totalCredits <= 0.0) return 0.0
        val totalQualityPoints = gradedCourses.sumOf { it.creditHours * it.gradePoint }
        return (totalQualityPoints / totalCredits)
    }

    fun calculateCgpa(semestersWithCourses: Map<SemesterEntity, List<CourseEntity>>): Double {
        var totalQualityPoints = 0.0
        var totalCredits = 0.0

        for ((semester, courses) in semestersWithCourses) {
            val graded = courses.filter { it.creditHours > 0 }
            totalQualityPoints += graded.sumOf { it.creditHours * it.gradePoint }
            totalCredits += graded.sumOf { it.creditHours }
        }

        return if (totalCredits > 0) totalQualityPoints / totalCredits else 0.0
    }

    data class TargetBreakdown(
        val targetCgpa: Double,
        val currentCgpa: Double,
        val completedCredits: Double,
        val completedSemesters: Int,
        val totalSemesters: Int,
        val remainingSemesters: Int,
        val remainingCredits: Double,
        val requiredAverageSgpa: Double,
        val isFeasible: Boolean,
        val feasibilityMessage: String,
        val recommendedGradeStrategy: String
    )

    fun calculateTargetBreakdown(
        currentCgpa: Double,
        completedCredits: Double,
        completedSemesters: Int,
        totalDegreeSemesters: Int,
        totalDegreeCredits: Double,
        targetCgpa: Double,
        isScale10: Boolean = false
    ): TargetBreakdown {
        val maxPoint = if (isScale10) 10.0 else 4.0
        val remainingSemesters = (totalDegreeSemesters - completedSemesters).coerceAtLeast(1)
        val remainingCredits = (totalDegreeCredits - completedCredits).coerceAtLeast(remainingSemesters * 12.0)

        // Using credit-weighted formula: (Target * TotalCredits - CurrentCGPA * DoneCredits) / RemainingCredits
        val totalExpectedCredits = completedCredits + remainingCredits
        val requiredPoints = (targetCgpa * totalExpectedCredits) - (currentCgpa * completedCredits)
        val requiredAverageSgpa = if (remainingCredits > 0) requiredPoints / remainingCredits else 0.0

        val isFeasible = requiredAverageSgpa <= maxPoint && requiredAverageSgpa >= 0.0

        val (msg, strategy) = when {
            requiredAverageSgpa > maxPoint -> {
                Pair(
                    "Exceeds maximum achievable grade (${String.format("%.2f", requiredAverageSgpa)} > $maxPoint).",
                    "To reach $targetCgpa, retake 1-2 earlier low-credit courses (D/F) or adjust your target."
                )
            }
            requiredAverageSgpa >= (maxPoint * 0.92) -> {
                Pair(
                    "High Distinction required (~${String.format("%.2f", requiredAverageSgpa)} SGPA).",
                    "Aim for straight A+ / A grades in all upcoming subjects. Prioritize 4-credit courses."
                )
            }
            requiredAverageSgpa >= (maxPoint * 0.80) -> {
                Pair(
                    "Strong performance required (~${String.format("%.2f", requiredAverageSgpa)} SGPA).",
                    "Target a mix of A and B+ grades. Keep attendance above 85% to maximize continuous assessment marks."
                )
            }
            requiredAverageSgpa >= (maxPoint * 0.65) -> {
                Pair(
                    "Balanced target (~${String.format("%.2f", requiredAverageSgpa)} SGPA).",
                    "Consistently score B/B+ grades across all remaining terms to smoothly reach your target."
                )
            }
            else -> {
                Pair(
                    "Very achievable goal (~${String.format("%.2f", requiredAverageSgpa)} SGPA).",
                    "Current CGPA gives you a solid buffer. Maintaining standard passing grades (C+/B) will secure your goal."
                )
            }
        }

        return TargetBreakdown(
            targetCgpa = targetCgpa,
            currentCgpa = currentCgpa,
            completedCredits = completedCredits,
            completedSemesters = completedSemesters,
            totalSemesters = totalDegreeSemesters,
            remainingSemesters = remainingSemesters,
            remainingCredits = remainingCredits,
            requiredAverageSgpa = requiredAverageSgpa,
            isFeasible = isFeasible,
            feasibilityMessage = msg,
            recommendedGradeStrategy = strategy
        )
    }

    enum class SemesterSortOrder(val displayName: String) {
        CHRONOLOGICAL_ASC("Chronological (Oldest)"),
        CHRONOLOGICAL_DESC("Chronological (Newest)"),
        PERFORMANCE_DESC("Top GPA (Highest)"),
        PERFORMANCE_ASC("Needs Attention (Lowest)")
    }

    data class SemesterPerformance(
        val semester: SemesterEntity,
        val sgpa: Double,
        val totalCredits: Double,
        val courseCount: Int,
        val completedCount: Int
    )

    fun sortSemesters(
        semesters: List<SemesterEntity>,
        allCourses: List<CourseEntity>,
        sortOrder: SemesterSortOrder
    ): List<SemesterPerformance> {
        val items = semesters.map { sem ->
            val courses = allCourses.filter { it.semesterId == sem.id }
            SemesterPerformance(
                semester = sem,
                sgpa = calculateSgpa(courses),
                totalCredits = courses.sumOf { it.creditHours },
                courseCount = courses.size,
                completedCount = courses.count { it.gradePoint > 0 }
            )
        }

        return when (sortOrder) {
            SemesterSortOrder.CHRONOLOGICAL_ASC -> items.sortedBy { it.semester.semesterNumber }
            SemesterSortOrder.CHRONOLOGICAL_DESC -> items.sortedByDescending { it.semester.semesterNumber }
            SemesterSortOrder.PERFORMANCE_DESC -> items.sortedWith(
                compareByDescending<SemesterPerformance> { it.sgpa }
                    .thenBy { it.semester.semesterNumber }
            )
            SemesterSortOrder.PERFORMANCE_ASC -> items.sortedWith(
                compareBy<SemesterPerformance> { it.sgpa }
                    .thenBy { it.semester.semesterNumber }
            )
        }
    }
}
