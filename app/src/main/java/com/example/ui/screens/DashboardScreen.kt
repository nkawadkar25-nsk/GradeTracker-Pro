package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.data.model.GradeUtils
import com.example.ui.components.AcademicProgressChart
import com.example.ui.components.AttendanceGauge
import com.example.ui.components.SemesterGpaData
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.GradeTrackerViewModel
import com.example.ui.viewmodel.ScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: GradeTrackerViewModel) {
    val profile by viewModel.profile.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val allCourses by viewModel.allCourses.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val currentSortOrder by viewModel.dashboardSortOrder.collectAsState()

    val semestersWithCourses = remember(semesters, allCourses) {
        semesters.associateWith { sem -> allCourses.filter { it.semesterId == sem.id } }
    }

    val currentCgpa = remember(semestersWithCourses) {
        GradeUtils.calculateCgpa(semestersWithCourses)
    }

    val activeSemester = remember(semesters) {
        semesters.firstOrNull { !it.isCompleted } ?: semesters.lastOrNull()
    }

    val activeCourses = remember(activeSemester, allCourses) {
        if (activeSemester != null) allCourses.filter { it.semesterId == activeSemester.id } else emptyList()
    }

    val activeSgpa = remember(activeCourses) {
        GradeUtils.calculateSgpa(activeCourses)
    }

    // Overall attendance calculation across active courses
    val totalHeld = remember(activeCourses) { activeCourses.sumOf { it.classesHeld } }
    val totalAttended = remember(activeCourses) { activeCourses.sumOf { it.classesAttended } }
    val overallAttendancePct = remember(totalHeld, totalAttended) {
        if (totalHeld > 0) (totalAttended.toDouble() / totalHeld) * 100.0 else 100.0
    }

    // Chart Data Points (sorted chronologically for accurate timeline)
    val chartDataPoints = remember(semesters, allCourses) {
        var runningQualityPoints = 0.0
        var runningCredits = 0.0
        semesters.sortedBy { it.semesterNumber }.map { sem ->
            val cList = allCourses.filter { it.semesterId == sem.id }
            val sgpa = GradeUtils.calculateSgpa(cList)
            runningQualityPoints += cList.sumOf { it.creditHours * it.gradePoint }
            runningCredits += cList.sumOf { it.creditHours }
            val cgpa = if (runningCredits > 0) runningQualityPoints / runningCredits else sgpa
            SemesterGpaData(
                semesterLabel = "Sem ${sem.semesterNumber}",
                sgpa = sgpa,
                cgpa = cgpa
            )
        }
    }

    // Sorted Semesters based on user selection
    val sortedSemesters = remember(semesters, allCourses, currentSortOrder) {
        GradeUtils.sortSemesters(semesters, allCourses, currentSortOrder)
    }

    val targetCgpa = profile?.targetCgpa ?: 3.85
    val isScale10 = profile?.gradingScale == "10.0"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek Minimalist Header with Compact Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = profile?.studentName ?: "Scholar",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${profile?.degreeName ?: "Degree Program"} • ${profile?.universityName ?: "University"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TTS Audio Briefing
                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                val briefing = "Hello ${profile?.studentName ?: "Scholar"}. Your cumulative GPA is ${String.format("%.2f", currentCgpa)}, targeting ${String.format("%.2f", targetCgpa)}. Current semester SGPA is ${String.format("%.2f", activeSgpa)} with ${overallAttendancePct.toInt()} percent overall attendance."
                                viewModel.speakBriefing(briefing)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isSpeaking) StatusWarning else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .testTag("tts_briefing_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Audio Briefing",
                            tint = if (isSpeaking) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Export PDF Action
                    IconButton(
                        onClick = { viewModel.exportPdfReport {} },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Minimalist Unified KPI Card (Decluttered & Clean)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CGPA Metric
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CGPA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.2f", currentCgpa),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = AcademicBluePrimary
                        )
                        Text(
                            text = "Target: ${String.format("%.2f", targetCgpa)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentCgpa >= targetCgpa) StatusSuccess else StatusWarning,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    // Active SGPA Metric
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "Active SGPA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.2f", activeSgpa),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                        Text(
                            text = activeSemester?.semesterName?.take(13) ?: "Active Term",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Attendance Gauge
                    AttendanceGauge(
                        percentage = overallAttendancePct,
                        threshold = profile?.defaultAttendanceThreshold ?: 75.0,
                        gaugeSize = 74.dp,
                        strokeWidth = 7.dp
                    )
                }
            }
        }

        // 3. Academic Progression Chart (Minimalist Container)
        item {
            AcademicProgressChart(
                dataPoints = chartDataPoints,
                targetCgpa = targetCgpa,
                maxScale = if (isScale10) 10.0 else 4.0
            )
        }

        // 4. Academic Semesters Section with Interactive Sorting (Chronological & Performance)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Academic Semesters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${sortedSemesters.size}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TextButton(
                        onClick = { viewModel.navigateTo(ScreenDestination.Semesters) },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Manage", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sorting Filter Chips: Chronological vs Performance
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GradeUtils.SemesterSortOrder.values()) { order ->
                        val isSelected = currentSortOrder == order
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDashboardSortOrder(order) },
                            label = {
                                Text(
                                    text = order.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = when (order) {
                                            GradeUtils.SemesterSortOrder.CHRONOLOGICAL_ASC -> Icons.Default.ArrowUpward
                                            GradeUtils.SemesterSortOrder.CHRONOLOGICAL_DESC -> Icons.Default.ArrowDownward
                                            GradeUtils.SemesterSortOrder.PERFORMANCE_DESC -> Icons.Default.Star
                                            GradeUtils.SemesterSortOrder.PERFORMANCE_ASC -> Icons.AutoMirrored.Filled.TrendingDown
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("sort_chip_${order.name}")
                        )
                    }
                }
            }
        }

        // 5. Sorted Semester Cards (Minimalist, Decluttered List)
        if (sortedSemesters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No academic semesters recorded yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(sortedSemesters) { perf ->
                MinimalistSemesterCard(
                    perf = perf,
                    onClick = { viewModel.navigateTo(ScreenDestination.Semesters) }
                )
            }
        }

        // 6. Active Courses & Attendance Quick Actions (Decluttered)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Courses & Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { viewModel.navigateTo(ScreenDestination.Attendance) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("All Attendance", fontSize = 12.sp)
                }
            }
        }

        if (activeCourses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active courses for this semester.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(activeCourses) { course ->
                DashboardCourseItem(
                    course = course,
                    onAttended = { viewModel.recordAttendance(course.id, true) },
                    onMissed = { viewModel.recordAttendance(course.id, false) },
                    onNotify = {
                        viewModel.triggerTestNotification(
                            courseName = "${course.courseCode} ${course.courseName}",
                            startTime = course.classSchedule.ifBlank { "10:00 AM" },
                            room = "Lecture Hall 101",
                            attendancePct = course.attendancePercentage
                        )
                    }
                )
            }
        }
    }
}

/**
 * Minimalist, decluttered semester tile for sorted dashboard overview
 */
@Composable
fun MinimalistSemesterCard(
    perf: GradeUtils.SemesterPerformance,
    onClick: () -> Unit
) {
    val sem = perf.semester

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("sorted_semester_card_${sem.semesterNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sem.semesterName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (sem.isCompleted) StatusSuccess.copy(alpha = 0.12f) else AcademicBluePrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (sem.isCompleted) "Completed" else "In Progress",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sem.isCompleted) StatusSuccess else AcademicBluePrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${perf.courseCount} Subjects • ${perf.totalCredits.toInt()} Credit Hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // SGPA Performance Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when {
                    perf.sgpa >= 3.7 -> StatusSuccess.copy(alpha = 0.14f)
                    perf.sgpa >= 3.0 -> AcademicBluePrimary.copy(alpha = 0.12f)
                    perf.sgpa > 0.0 -> StatusWarning.copy(alpha = 0.14f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (perf.sgpa >= 3.8) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Top Performance",
                            tint = StatusSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "${String.format("%.2f", perf.sgpa)} SGPA",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            perf.sgpa >= 3.7 -> StatusSuccess
                            perf.sgpa >= 3.0 -> AcademicBluePrimary
                            perf.sgpa > 0.0 -> StatusWarning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCourseItem(
    course: CourseEntity,
    onAttended: () -> Unit,
    onMissed: () -> Unit,
    onNotify: () -> Unit
) {
    val attendanceStatus = course.getAttendanceSummary()
    val targetFeedback = course.getCourseTargetFeedback()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${course.courseCode} • ${course.courseName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${course.creditHours.toInt()} Credits  •  Current: ${course.grade} (${course.gradePoint})  •  Target: ${course.targetGrade}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (attendanceStatus.isSafe) StatusSuccess.copy(alpha = 0.15f) else StatusCritical.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${String.format("%.0f", course.attendancePercentage)}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (attendanceStatus.isSafe) StatusSuccess else StatusCritical
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Course Target Proximity Feedback Pill
            val targetColor = when (targetFeedback.urgencyLevel) {
                com.example.data.model.UrgencyLevel.SAFE -> StatusSuccess
                com.example.data.model.UrgencyLevel.MODERATE -> StatusWarning
                com.example.data.model.UrgencyLevel.HIGH -> StatusCritical
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = targetColor.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (targetFeedback.isAchieved) Icons.Default.CheckCircle else Icons.Default.Flag,
                        contentDescription = null,
                        tint = targetColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "${targetFeedback.statusLabel} (${targetFeedback.gapText})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = targetColor
                        )
                        Text(
                            text = targetFeedback.feedbackMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Attendance Insight
            Text(
                text = attendanceStatus.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (attendanceStatus.isSafe) MaterialTheme.colorScheme.onSurfaceVariant else StatusCritical,
                fontWeight = if (!attendanceStatus.isSafe) FontWeight.SemiBold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: +Attended, +Missed, Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAttended,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusSuccess)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Attended (+1)", fontSize = 11.sp, color = StatusSuccess)
                }

                OutlinedButton(
                    onClick = onMissed,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusCritical)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Missed (+1)", fontSize = 11.sp, color = StatusCritical)
                }

                IconButton(
                    onClick = onNotify,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.NotificationAdd, contentDescription = "Send Reminder", tint = AcademicBluePrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
