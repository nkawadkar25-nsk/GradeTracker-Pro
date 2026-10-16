package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.data.model.GradeUtils
import com.example.data.model.SemesterEntity
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.GradeTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemestersScreen(viewModel: GradeTrackerViewModel) {
    val semesters by viewModel.semesters.collectAsState()
    val allCourses by viewModel.allCourses.collectAsState()
    val profile by viewModel.profile.collectAsState()

    var showAddSemesterDialog by remember { mutableStateOf(false) }
    var courseToEdit by remember { mutableStateOf<CourseEntity?>(null) }
    var targetSemesterForCourse by remember { mutableStateOf<Long?>(null) }
    var showCourseDialog by remember { mutableStateOf(false) }

    val isScale10 = profile?.gradingScale == "10.0"

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSemesterDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_semester_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Semester")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("semesters_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Semesters & Course Grades",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Calculate SGPA and manage subjects for each academic term.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (semesters.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No semesters found. Tap + to add your first semester!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(semesters) { semester ->
                    val courses = allCourses.filter { it.semesterId == semester.id }
                    val sgpa = GradeUtils.calculateSgpa(courses)
                    val totalCredits = courses.sumOf { it.creditHours }

                    SemesterCard(
                        semester = semester,
                        courses = courses,
                        sgpa = sgpa,
                        totalCredits = totalCredits,
                        isScale10 = isScale10,
                        onAddCourse = {
                            targetSemesterForCourse = semester.id
                            courseToEdit = null
                            showCourseDialog = true
                        },
                        onEditCourse = { course ->
                            targetSemesterForCourse = semester.id
                            courseToEdit = course
                            showCourseDialog = true
                        },
                        onDeleteCourse = { course -> viewModel.deleteCourse(course) },
                        onDeleteSemester = { viewModel.deleteSemester(semester) },
                        onToggleCompleted = {
                            viewModel.addSemester(
                                number = semester.semesterNumber,
                                name = semester.semesterName,
                                isCompleted = !semester.isCompleted
                            )
                        }
                    )
                }
            }
        }
    }

    // Add Semester Dialog
    if (showAddSemesterDialog) {
        var semNumberStr by remember { mutableStateOf((semesters.size + 1).toString()) }
        var semName by remember { mutableStateOf("Semester ${semesters.size + 1}") }
        var isCompleted by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddSemesterDialog = false },
            title = { Text("Add New Semester", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = semNumberStr,
                        onValueChange = { semNumberStr = it },
                        label = { Text("Semester Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = semName,
                        onValueChange = { semName = it },
                        label = { Text("Semester Title (e.g., Spring 2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { isCompleted = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark as Completed Term")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = semNumberStr.toIntOrNull() ?: (semesters.size + 1)
                        viewModel.addSemester(number = num, name = semName, isCompleted = isCompleted)
                        showAddSemesterDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSemesterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Course Detail Add/Edit Dialog
    if (showCourseDialog && targetSemesterForCourse != null) {
        CourseDetailDialog(
            initialCourse = courseToEdit,
            semesterId = targetSemesterForCourse!!,
            isScale10 = isScale10,
            onDismiss = {
                showCourseDialog = false
                courseToEdit = null
            },
            onSave = { course ->
                viewModel.saveCourse(course)
                showCourseDialog = false
                courseToEdit = null
            }
        )
    }
}

@Composable
fun SemesterCard(
    semester: SemesterEntity,
    courses: List<CourseEntity>,
    sgpa: Double,
    totalCredits: Double,
    isScale10: Boolean,
    onAddCourse: () -> Unit,
    onEditCourse: (CourseEntity) -> Unit,
    onDeleteCourse: (CourseEntity) -> Unit,
    onDeleteSemester: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = semester.semesterName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${totalCredits.toInt()} Credits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (semester.isCompleted) "Completed" else "In Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (semester.isCompleted) StatusSuccess else AcademicBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // SGPA Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AcademicBluePrimary.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format("%.2f", sgpa),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AcademicBluePrimary
                        )
                        Text(
                            text = "SGPA",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = AcademicBluePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Courses List inside Semester
            if (courses.isEmpty()) {
                Text(
                    text = "No subjects added to this semester yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                courses.forEach { course ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val targetFeedback = course.getCourseTargetFeedback()
                        val feedbackColor = when (targetFeedback.urgencyLevel) {
                            com.example.data.model.UrgencyLevel.SAFE -> StatusSuccess
                            com.example.data.model.UrgencyLevel.MODERATE -> StatusWarning
                            com.example.data.model.UrgencyLevel.HIGH -> StatusCritical
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${course.courseCode}: ${course.courseName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${course.creditHours.toInt()} Credits • Attendance: ${course.attendancePercentage.toInt()}% • Syllabus: ${course.syllabusCompletionPercentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Target: ${course.targetGrade}  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = feedbackColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${targetFeedback.statusLabel} (${targetFeedback.gapText})",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = feedbackColor
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${course.grade} (${course.gradePoint})",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { onEditCourse(course) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Course", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDeleteCourse(course) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Course", tint = StatusCritical, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom action row for semester card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddCourse,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Course", fontSize = 12.sp)
                }

                TextButton(
                    onClick = onDeleteSemester,
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusCritical)
                ) {
                    Text("Delete Term", fontSize = 12.sp)
                }
            }
        }
    }
}
