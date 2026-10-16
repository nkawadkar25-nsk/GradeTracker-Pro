package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun AttendanceGauge(
    percentage: Double,
    threshold: Double = 75.0,
    gaugeSize: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.toFloat().coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 900),
        label = "attendance_anim"
    )

    val gaugeColor = when {
        percentage >= threshold -> StatusSuccess
        percentage >= (threshold - 5.0) -> StatusWarning
        else -> StatusCritical
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(gaugeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = kotlin.math.min(size.width, size.height) - stroke
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )

            // Background Track Arc (270 degrees)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Progress Arc
            val sweep = (animatedPercentage / 100f) * 270f
            drawArc(
                color = gaugeColor,
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (gaugeSize > 100.dp) 22.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor
                )
            )
            Text(
                text = if (percentage >= threshold) "Safe" else "Critical",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (gaugeSize > 100.dp) 11.sp else 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
