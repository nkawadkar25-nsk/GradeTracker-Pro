package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicDarkTertiary
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

data class SemesterGpaData(
    val semesterLabel: String,
    val sgpa: Double,
    val cgpa: Double
)

@Composable
fun AcademicProgressChart(
    dataPoints: List<SemesterGpaData>,
    targetCgpa: Double,
    maxScale: Double = 4.0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Academic Progress & CGPA Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AcademicBluePrimary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "CGPA", style = MaterialTheme.typography.labelSmall)

                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(StatusSuccess, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "SGPA", style = MaterialTheme.typography.labelSmall)

                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AcademicDarkTertiary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Target", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add semester courses to visualize your academic growth.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val lineColor = AcademicBluePrimary
                val sgpaLineColor = StatusSuccess
                val targetColor = AcademicDarkTertiary
                val gridColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 30f
                    val topPadding = 20f
                    val leftPadding = 40f
                    val rightPadding = 20f

                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding

                    // Grid lines (0.0, 1.0, 2.0, 3.0, 4.0 or equivalent)
                    val steps = 4
                    for (i in 0..steps) {
                        val y = topPadding + (chartHeight / steps) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1f
                        )
                    }

                    // Target CGPA Line
                    val targetY = topPadding + chartHeight * (1f - (targetCgpa / maxScale).toFloat().coerceIn(0f, 1f))
                    drawLine(
                        color = targetColor,
                        start = Offset(leftPadding, targetY),
                        end = Offset(width - rightPadding, targetY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )

                    val xStep = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1) else chartWidth / 2

                    // Build CGPA Path & Points
                    val cgpaPath = Path()
                    val cgpaAreaPath = Path()
                    val sgpaPath = Path()

                    val cgpaPoints = mutableListOf<Offset>()
                    val sgpaPoints = mutableListOf<Offset>()

                    dataPoints.forEachIndexed { index, item ->
                        val x = if (dataPoints.size == 1) leftPadding + chartWidth / 2 else leftPadding + index * xStep
                        val cgpaY = topPadding + chartHeight * (1f - (item.cgpa / maxScale).toFloat().coerceIn(0f, 1f))
                        val sgpaY = topPadding + chartHeight * (1f - (item.sgpa / maxScale).toFloat().coerceIn(0f, 1f))

                        cgpaPoints.add(Offset(x, cgpaY))
                        sgpaPoints.add(Offset(x, sgpaY))

                        if (index == 0) {
                            cgpaPath.moveTo(x, cgpaY)
                            cgpaAreaPath.moveTo(x, topPadding + chartHeight)
                            cgpaAreaPath.lineTo(x, cgpaY)
                            sgpaPath.moveTo(x, sgpaY)
                        } else {
                            cgpaPath.lineTo(x, cgpaY)
                            cgpaAreaPath.lineTo(x, cgpaY)
                            sgpaPath.lineTo(x, sgpaY)
                        }
                    }

                    // Complete Area Path
                    if (dataPoints.isNotEmpty()) {
                        val lastX = cgpaPoints.last().x
                        cgpaAreaPath.lineTo(lastX, topPadding + chartHeight)
                        cgpaAreaPath.close()

                        // Draw Gradient Area under CGPA
                        drawPath(
                            path = cgpaAreaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.25f),
                                    lineColor.copy(alpha = 0.02f)
                                ),
                                startY = topPadding,
                                endY = topPadding + chartHeight
                            )
                        )

                        // Draw SGPA Line (Dashed)
                        drawPath(
                            path = sgpaPath,
                            color = sgpaLineColor,
                            style = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                        )

                        // Draw CGPA Line (Solid)
                        drawPath(
                            path = cgpaPath,
                            color = lineColor,
                            style = Stroke(width = 3.5f)
                        )

                        // Draw Point Circles
                        sgpaPoints.forEach { pt ->
                            drawCircle(color = Color.White, radius = 5f, center = pt)
                            drawCircle(color = sgpaLineColor, radius = 3.5f, center = pt)
                        }

                        cgpaPoints.forEach { pt ->
                            drawCircle(color = Color.White, radius = 7f, center = pt)
                            drawCircle(color = lineColor, radius = 4.5f, center = pt)
                        }
                    }
                }

                // X-Axis Semester Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dataPoints.forEach { point ->
                        Text(
                            text = point.semesterLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyllabusProgressBarChart(
    courses: List<Pair<String, Double>>, // (Course Code, Syllabus %)
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Syllabus Completion Tracker",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (courses.isEmpty()) {
                Text(
                    text = "No active courses to track syllabus progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                courses.forEach { (code, progress) ->
                    val progressFloat = (progress / 100.0).toFloat().coerceIn(0f, 1f)
                    val barColor = if (progress >= 80) StatusSuccess else if (progress >= 50) AcademicBluePrimary else StatusWarning

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${progress.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = barColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFloat)
                                    .height(8.dp)
                                    .background(barColor, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
