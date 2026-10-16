package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailDialog(
    initialCourse: CourseEntity?,
    semesterId: Long,
    isScale10: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (CourseEntity) -> Unit
) {
    var courseCode by remember { mutableStateOf(initialCourse?.courseCode ?: "") }
    var courseName by remember { mutableStateOf(initialCourse?.courseName ?: "") }
    var creditHoursStr by remember { mutableStateOf(initialCourse?.creditHours?.toString() ?: "3.0") }
    var selectedGrade by remember { mutableStateOf(initialCourse?.grade ?: if (isScale10) "A+" else "A") }
    var selectedTargetGrade by remember { mutableStateOf(initialCourse?.targetGrade ?: if (isScale10) "O" else "A") }
    var classesHeldStr by remember { mutableStateOf(initialCourse?.classesHeld?.toString() ?: "0") }
    var classesAttendedStr by remember { mutableStateOf(initialCourse?.classesAttended?.toString() ?: "0") }
    var reqAttendanceStr by remember { mutableStateOf(initialCourse?.requiredAttendancePercentage?.toString() ?: "75.0") }
    var syllabusProgress by remember { mutableStateOf((initialCourse?.syllabusCompletionPercentage ?: 0.0).toFloat()) }
    var instructorName by remember { mutableStateOf(initialCourse?.instructorName ?: "") }
    var classSchedule by remember { mutableStateOf(initialCourse?.classSchedule ?: "") }

    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var targetGradeDropdownExpanded by remember { mutableStateOf(false) }
    val gradesList = remember(isScale10) { GradeUtils.getGrades(isScale10) }

    // Real-time calculation of course target feedback
    val currentGp = remember(selectedGrade, isScale10) { GradeUtils.getGradePoint(selectedGrade, isScale10) }
    val targetGp = remember(selectedTargetGrade, isScale10) { GradeUtils.getGradePoint(selectedTargetGrade, isScale10) }
    val gap = remember(currentGp, targetGp) { currentGp - targetGp }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCourse == null) "Add Course" else "Edit Course",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code (e.g., CS201)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("course_code_input")
                )

                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Course Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("course_name_input")
                )

                OutlinedTextField(
                    value = creditHoursStr,
                    onValueChange = { creditHoursStr = it },
                    label = { Text("Credit Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("credit_hours_input")
                )

                // Grades & Target Setting
                Text(
                    text = "Grades & Individual Course Target",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Current Grade Dropdown
                    ExposedDropdownMenuBox(
                        expanded = gradeDropdownExpanded,
                        onExpandedChange = { gradeDropdownExpanded = !gradeDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedGrade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Current Grade") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("grade_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = gradeDropdownExpanded,
                            onDismissRequest = { gradeDropdownExpanded = false }
                        ) {
                            gradesList.forEach { gradeDef ->
                                DropdownMenuItem(
                                    text = { Text("${gradeDef.letter} (${gradeDef.point})") },
                                    onClick = {
                                        selectedGrade = gradeDef.letter
                                        gradeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Target Grade Dropdown
                    ExposedDropdownMenuBox(
                        expanded = targetGradeDropdownExpanded,
                        onExpandedChange = { targetGradeDropdownExpanded = !targetGradeDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedTargetGrade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Grade") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetGradeDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("target_grade_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = targetGradeDropdownExpanded,
                            onDismissRequest = { targetGradeDropdownExpanded = false }
                        ) {
                            gradesList.forEach { gradeDef ->
                                DropdownMenuItem(
                                    text = { Text("${gradeDef.letter} (${gradeDef.point})") },
                                    onClick = {
                                        selectedTargetGrade = gradeDef.letter
                                        targetGradeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Live Course Target Feedback Card
                val feedbackBg = when {
                    gap >= 0.0 -> StatusSuccess.copy(alpha = 0.12f)
                    gap >= -0.35 -> StatusWarning.copy(alpha = 0.12f)
                    else -> StatusCritical.copy(alpha = 0.12f)
                }
                val feedbackColor = when {
                    gap >= 0.0 -> StatusSuccess
                    gap >= -0.35 -> StatusWarning
                    else -> StatusCritical
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = feedbackBg,
                    modifier = Modifier.fillMaxWidth().testTag("target_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                gap >= 0.0 -> Icons.Default.CheckCircle
                                gap >= -0.35 -> Icons.Default.Info
                                else -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = feedbackColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when {
                                    gap > 0.0 -> "Exceeding Target (+${String.format("%.1f", gap)} GP)"
                                    gap == 0.0 -> "Target Met! (Current: $selectedGrade = Target: $selectedTargetGrade)"
                                    gap >= -0.35 -> "Near Target (${String.format("%.1f", gap)} GP gap)"
                                    else -> "Target At Risk (${String.format("%.1f", gap)} GP gap)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = feedbackColor
                            )
                            Text(
                                text = if (gap >= 0.0) {
                                    "On track to achieve course target $selectedTargetGrade!"
                                } else {
                                    "Needs +${String.format("%.1f", -gap)} GP to hit target $selectedTargetGrade."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Attendance Section
                Text(
                    text = "Attendance & Classes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = classesAttendedStr,
                        onValueChange = { classesAttendedStr = it },
                        label = { Text("Attended") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("classes_attended_input")
                    )

                    OutlinedTextField(
                        value = classesHeldStr,
                        onValueChange = { classesHeldStr = it },
                        label = { Text("Held") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("classes_held_input")
                    )

                    OutlinedTextField(
                        value = reqAttendanceStr,
                        onValueChange = { reqAttendanceStr = it },
                        label = { Text("Min %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("req_attendance_input")
                    )
                }

                // Syllabus Completion
                Text(
                    text = "Expected Syllabus Completion: ${syllabusProgress.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = syllabusProgress,
                    onValueChange = { syllabusProgress = it },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth().testTag("syllabus_slider")
                )

                OutlinedTextField(
                    value = instructorName,
                    onValueChange = { instructorName = it },
                    label = { Text("Instructor (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = classSchedule,
                    onValueChange = { classSchedule = it },
                    label = { Text("Schedule (e.g., Mon, Wed 10:00 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val credits = creditHoursStr.toDoubleOrNull() ?: 3.0
                    val held = classesHeldStr.toIntOrNull() ?: 0
                    val attended = (classesAttendedStr.toIntOrNull() ?: 0).coerceAtMost(held)
                    val reqAtt = reqAttendanceStr.toDoubleOrNull() ?: 75.0
                    val gp = GradeUtils.getGradePoint(selectedGrade, isScale10)

                    val updatedCourse = (initialCourse ?: CourseEntity(
                        semesterId = semesterId,
                        courseCode = "",
                        courseName = "",
                        creditHours = credits
                    )).copy(
                        semesterId = semesterId,
                        courseCode = courseCode.ifBlank { "COURSE" },
                        courseName = courseName.ifBlank { "Untitled Course" },
                        creditHours = credits,
                        grade = selectedGrade,
                        gradePoint = gp,
                        targetGrade = selectedTargetGrade,
                        classesHeld = held,
                        classesAttended = attended,
                        requiredAttendancePercentage = reqAtt,
                        syllabusCompletionPercentage = syllabusProgress.toDouble(),
                        instructorName = instructorName,
                        classSchedule = classSchedule
                    )
                    onSave(updatedCourse)
                },
                modifier = Modifier.testTag("save_course_button")
            ) {
                Text("Save Course")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
