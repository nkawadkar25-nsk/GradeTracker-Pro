package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.CourseEntity
import com.example.ui.components.AttendanceGauge
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.GradeTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: GradeTrackerViewModel) {
    val allCourses by viewModel.allCourses.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val profile by viewModel.profile.collectAsState()

    // Global Threshold Selector
    var selectedThreshold by remember { mutableStateOf(profile?.defaultAttendanceThreshold ?: 75.0) }
    var selectedSemesterFilterId by remember { mutableStateOf<Long?>(null) }

    val activeSemester = remember(semesters) {
        semesters.firstOrNull { !it.isCompleted } ?: semesters.lastOrNull()
    }

    val filteredCourses = remember(allCourses, selectedSemesterFilterId, activeSemester) {
        val targetSemId = selectedSemesterFilterId ?: activeSemester?.id
        if (targetSemId != null) {
            allCourses.filter { it.semesterId == targetSemId }
        } else {
            allCourses
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("attendance_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Attendance & Syllabus Analyzer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track classes held, attended, and calculate consecutive classes required to maintain your minimum threshold.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Attendance Threshold Selector Card
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
                            text = "Required Attendance Threshold",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AcademicBluePrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${selectedThreshold.toInt()}%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AcademicBluePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = selectedThreshold.toFloat(),
                        onValueChange = { selectedThreshold = it.toDouble() },
                        valueRange = 50f..95f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth().testTag("attendance_threshold_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("50% (Permissive)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("75% (Standard)", style = MaterialTheme.typography.labelSmall, color = AcademicBluePrimary, fontWeight = FontWeight.Bold)
                        Text("90% (Strict)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Term Filter Chips
        if (semesters.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSemesterFilterId == null,
                        onClick = { selectedSemesterFilterId = null },
                        label = { Text("Active Term") }
                    )
                    semesters.forEach { sem ->
                        FilterChip(
                            selected = selectedSemesterFilterId == sem.id,
                            onClick = { selectedSemesterFilterId = sem.id },
                            label = { Text("Sem ${sem.semesterNumber}") }
                        )
                    }
                }
            }
        }

        if (filteredCourses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No courses found in this term.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(filteredCourses) { course ->
                AttendanceCourseCard(
                    course = course,
                    targetThreshold = selectedThreshold,
                    onAttended = { viewModel.recordAttendance(course.id, true) },
                    onMissed = { viewModel.recordAttendance(course.id, false) },
                    onSyllabusChange = { newProgress ->
                        viewModel.saveCourse(course.copy(syllabusCompletionPercentage = newProgress))
                    }
                )
            }
        }
    }
}

@Composable
fun AttendanceCourseCard(
    course: CourseEntity,
    targetThreshold: Double,
    onAttended: () -> Unit,
    onMissed: () -> Unit,
    onSyllabusChange: (Double) -> Unit
) {
    val status = course.getAttendanceSummary(targetThreshold)
    var editSyllabus by remember { mutableStateOf(false) }
    var currentSyllabus by remember(course.syllabusCompletionPercentage) {
        mutableStateOf(course.syllabusCompletionPercentage.toFloat())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Code, Name, Circular Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${course.courseCode} • ${course.courseName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (course.instructorName.isNotBlank()) {
                        Text(
                            text = "Instructor: ${course.instructorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${course.classesAttended} Attended / ${course.classesHeld} Total Classes Held",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AttendanceGauge(
                    percentage = course.attendancePercentage,
                    threshold = targetThreshold,
                    gaugeSize = 80.dp,
                    strokeWidth = 8.dp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attendance Computed Advice Pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (status.isSafe) StatusSuccess.copy(alpha = 0.12f) else StatusCritical.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (status.isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (status.isSafe) StatusSuccess else StatusCritical,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (status.isSafe) "Above Target Status" else "Action Required",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (status.isSafe) StatusSuccess else StatusCritical
                        )
                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Syllabus Progress Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expected Syllabus Completion: ${course.syllabusCompletionPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = { editSyllabus = !editSyllabus },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(if (editSyllabus) "Done" else "Adjust %", fontSize = 11.sp)
                }
            }

            if (editSyllabus) {
                Slider(
                    value = currentSyllabus,
                    onValueChange = {
                        currentSyllabus = it
                        onSyllabusChange(it.toDouble())
                    },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(
                    progress = { (course.syllabusCompletionPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = AcademicBluePrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1-Tap Attendance Recording
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAttended,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Attended Class (+1)", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = onMissed,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(contentColor = StatusCritical),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Missed Class (+1)", fontSize = 12.sp)
                }
            }
        }
    }
}
