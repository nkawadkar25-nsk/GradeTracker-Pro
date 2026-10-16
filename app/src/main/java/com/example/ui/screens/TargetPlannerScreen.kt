package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GradeUtils
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicDarkTertiary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.GradeTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetPlannerScreen(viewModel: GradeTrackerViewModel) {
    val profile by viewModel.profile.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val allCourses by viewModel.allCourses.collectAsState()

    val semestersWithCourses = remember(semesters, allCourses) {
        semesters.associateWith { sem -> allCourses.filter { it.semesterId == sem.id } }
    }

    val currentCgpa = remember(semestersWithCourses) {
        GradeUtils.calculateCgpa(semestersWithCourses)
    }

    val completedCourses = remember(semestersWithCourses) {
        semestersWithCourses.filter { it.key.isCompleted }.flatMap { it.value }
    }
    val completedCredits = remember(completedCourses) {
        completedCourses.sumOf { it.creditHours }
    }
    val completedSemestersCount = remember(semesters) {
        semesters.count { it.isCompleted }
    }

    val isScale10 = profile?.gradingScale == "10.0"
    val maxScale = if (isScale10) 10.0 else 4.0

    var targetCgpaInput by remember(profile?.targetCgpa) {
        mutableStateOf((profile?.targetCgpa ?: 3.85).toFloat())
    }
    var totalSemestersInput by remember(profile?.totalDegreeSemesters) {
        mutableStateOf((profile?.totalDegreeSemesters ?: 8).toFloat())
    }
    var totalCreditsInput by remember(profile?.totalDegreeCredits) {
        mutableStateOf((profile?.totalDegreeCredits ?: 128.0).toFloat())
    }

    val targetBreakdown = remember(
        currentCgpa,
        completedCredits,
        completedSemestersCount,
        totalSemestersInput,
        totalCreditsInput,
        targetCgpaInput,
        isScale10
    ) {
        GradeUtils.calculateTargetBreakdown(
            currentCgpa = currentCgpa,
            completedCredits = completedCredits,
            completedSemesters = completedSemestersCount,
            totalDegreeSemesters = totalSemestersInput.toInt(),
            totalDegreeCredits = totalCreditsInput.toDouble(),
            targetCgpa = targetCgpaInput.toDouble(),
            isScale10 = isScale10
        )
    }

    // Hypothetical Simulator for next term
    var simulatedNextTermSgpa by remember { mutableStateOf(targetBreakdown.requiredAverageSgpa.toFloat().coerceIn(0f, maxScale.toFloat())) }

    val simulatedCgpa = remember(currentCgpa, completedCredits, simulatedNextTermSgpa) {
        val nextTermCredits = 16.0
        val totalNewCreds = completedCredits + nextTermCredits
        val totalPts = (currentCgpa * completedCredits) + (simulatedNextTermSgpa * nextTermCredits)
        if (totalNewCreds > 0) totalPts / totalNewCreds else currentCgpa
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("target_planner_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Target CGPA & Future Grade Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Simulate what SGPA and course grades you need in upcoming semesters to hit your graduation target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Target CGPA Slider & Profile Setup Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Graduation CGPA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AcademicDarkTertiary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${String.format("%.2f", targetCgpaInput)} / ${profile?.gradingScale ?: "4.0"}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AcademicDarkTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = targetCgpaInput,
                        onValueChange = {
                            targetCgpaInput = it
                            viewModel.updateProfile(
                                name = profile?.studentName ?: "Scholar",
                                degree = profile?.degreeName ?: "",
                                university = profile?.universityName ?: "",
                                targetCgpa = it.toDouble(),
                                totalSemesters = totalSemestersInput.toInt(),
                                totalCredits = totalCreditsInput.toDouble(),
                                scale = profile?.gradingScale ?: "4.0",
                                defaultAttendance = profile?.defaultAttendanceThreshold ?: 75.0
                            )
                        },
                        valueRange = (if (isScale10) 4f else 2f)..(maxScale.toFloat()),
                        steps = 40,
                        modifier = Modifier.fillMaxWidth().testTag("target_cgpa_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current CGPA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.2f", currentCgpa), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AcademicBluePrimary)
                        }
                        Column {
                            Text("Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${completedCredits.toInt()} / ${totalCreditsInput.toInt()} hrs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Remaining Terms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${targetBreakdown.remainingSemesters} sem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Computed Future Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (targetBreakdown.isFeasible) MaterialTheme.colorScheme.primaryContainer else StatusCritical.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Required Future SGPA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (targetBreakdown.isFeasible) MaterialTheme.colorScheme.onPrimaryContainer else StatusCritical
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (targetBreakdown.isFeasible) StatusSuccess else StatusCritical
                        ) {
                            Text(
                                text = "${String.format("%.2f", targetBreakdown.requiredAverageSgpa)} / ${profile?.gradingScale ?: "4.0"}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = targetBreakdown.feasibilityMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (targetBreakdown.isFeasible) MaterialTheme.colorScheme.onPrimaryContainer else StatusCritical
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = targetBreakdown.recommendedGradeStrategy,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // 3. Recommended Grade Combination Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Subject Grade Blueprint for Upcoming Terms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To maintain an average SGPA of ${String.format("%.2f", targetBreakdown.requiredAverageSgpa)} per semester (assuming standard 4 subjects / 15 credits per term):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val req = targetBreakdown.requiredAverageSgpa
                    val samplePlan = when {
                        req >= 3.8 -> listOf(
                            Triple("Subject 1 (4 cr)", "A+", 4.0),
                            Triple("Subject 2 (4 cr)", "A", 4.0),
                            Triple("Subject 3 (4 cr)", "A", 4.0),
                            Triple("Subject 4 (3 cr)", "A-", 3.7)
                        )
                        req >= 3.4 -> listOf(
                            Triple("Subject 1 (4 cr)", "A", 4.0),
                            Triple("Subject 2 (4 cr)", "A-", 3.7),
                            Triple("Subject 3 (4 cr)", "B+", 3.3),
                            Triple("Subject 4 (3 cr)", "B+", 3.3)
                        )
                        req >= 3.0 -> listOf(
                            Triple("Subject 1 (4 cr)", "A-", 3.7),
                            Triple("Subject 2 (4 cr)", "B+", 3.3),
                            Triple("Subject 3 (4 cr)", "B", 3.0),
                            Triple("Subject 4 (3 cr)", "B", 3.0)
                        )
                        else -> listOf(
                            Triple("Subject 1 (4 cr)", "B+", 3.3),
                            Triple("Subject 2 (4 cr)", "B", 3.0),
                            Triple("Subject 3 (4 cr)", "B-", 2.7),
                            Triple("Subject 4 (3 cr)", "C+", 2.3)
                        )
                    }

                    samplePlan.forEach { (sub, grade, pt) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sub, style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AcademicBluePrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "$grade (${String.format("%.1f", pt)})",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AcademicBluePrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Interactive Next-Semester SGPA Simulator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Next Semester SGPA Impact Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Slide to test what happens if you score a particular SGPA in your next semester (16 credits).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simulated SGPA:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            String.format("%.2f", simulatedNextTermSgpa),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }

                    Slider(
                        value = simulatedNextTermSgpa,
                        onValueChange = { simulatedNextTermSgpa = it },
                        valueRange = 0f..(maxScale.toFloat()),
                        steps = 40,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("New Cumulative CGPA:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                String.format("%.2f", simulatedCgpa),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (simulatedCgpa >= targetCgpaInput) StatusSuccess else StatusWarning
                            )
                        }
                    }
                }
            }
        }
    }
}
