package com.acesur.solarpvtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesur.solarpvtracker.ui.theme.SkyBlue
import com.acesur.solarpvtracker.ui.theme.SolarOrange
import com.acesur.solarpvtracker.ui.theme.SunYellow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TiltGauge(
    angle: Float,
    modifier: Modifier = Modifier,
    targetAngle: Float? = null
) {
    // 0 degrees = East (Right), 90 degrees = North (Top)
    // Canvas coordinate system: 0 is Right, positive is Clockwise.
    // So 90 mathematical degrees = -90 canvas degrees (Counter-Clockwise).
    
    val animatedAngle by animateFloatAsState(
        targetValue = angle,
        animationSpec = tween(durationMillis = 100),
        label = "angle"
    )
    
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier.size(280.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 - 30f
        
        // Outer circle
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.5f),
            radius = radius + 20f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
        
        // Background arc for the active quadrant (0 to 90)
        drawArc(
            color = Color.LightGray.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = -90f,
            useCenter = true,
            topLeft = Offset(centerX - radius - 20f, centerY - radius - 20f),
            size = Size((radius + 20f) * 2, (radius + 20f) * 2)
        )
        
        // Thick arc for 0-90 scale
        drawArc(
            color = Color.LightGray.copy(alpha = 0.5f),
            startAngle = 0f,
            sweepAngle = -90f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 15f, cap = StrokeCap.Round)
        )
        
        // Crosshairs (Axis lines)
        drawLine(
            color = Color.LightGray,
            start = Offset(centerX - radius - 10f, centerY),
            end = Offset(centerX + radius + 10f, centerY),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.LightGray,
            start = Offset(centerX, centerY - radius - 10f),
            end = Offset(centerX, centerY + radius + 10f),
            strokeWidth = 1f
        )
        
        // Draw degree markings (0 to 90)
        for (i in 0..90 step 15) {
            // Convert angle to radians for placement
            // Mathematical angle i corresponds to Canvas angle -i
            val angleRad = Math.toRadians(-i.toDouble())
            val innerRadius = radius - 20f
            val outerRadius = radius
            
            val startX = centerX + (innerRadius * cos(angleRad)).toFloat()
            val startY = centerY + (innerRadius * sin(angleRad)).toFloat()
            val endX = centerX + (outerRadius * cos(angleRad)).toFloat()
            val endY = centerY + (outerRadius * sin(angleRad)).toFloat()
            
            drawLine(
                color = Color.Gray,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (i % 30 == 0) 3f else 1.5f
            )
            
            // Draw degree labels
            val textRadius = radius + 25f
            val textX = centerX + (textRadius * cos(angleRad)).toFloat()
            val textY = centerY + (textRadius * sin(angleRad)).toFloat()
            
            // Adjust text alignment based on position
            val textSize = textMeasurer.measure("${i}°")
            val textOffsetX = textX - textSize.size.width / 2
            val textOffsetY = textY - textSize.size.height / 2
            
            drawText(
                textMeasurer = textMeasurer,
                text = "${i}°",
                topLeft = Offset(textOffsetX, textOffsetY),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            )
        }
        
        // Target angle indicator (if set)
        targetAngle?.let { target ->
            val targetAngleRad = Math.toRadians(-target.toDouble())
            val targetX = centerX + (radius * cos(targetAngleRad)).toFloat()
            val targetY = centerY + (radius * sin(targetAngleRad)).toFloat()
            
            drawLine(
                color = SkyBlue,
                start = Offset(centerX, centerY),
                end = Offset(targetX, targetY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            
            drawCircle(
                color = SkyBlue,
                radius = 8f,
                center = Offset(targetX, targetY)
            )
        }
        
        // Current angle indicator (needle)
        val currentAngleRad = Math.toRadians(-animatedAngle.toDouble())
        val needleLength = radius - 10f
        val needleX = centerX + (needleLength * cos(currentAngleRad)).toFloat()
        val needleY = centerY + (needleLength * sin(currentAngleRad)).toFloat()
        
        // Needle shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.2f),
            start = Offset(centerX + 2f, centerY + 2f),
            end = Offset(needleX + 2f, needleY + 2f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        
        // Main needle
        drawLine(
            color = SolarOrange,
            start = Offset(centerX, centerY),
            end = Offset(needleX, needleY),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        
        // Center pivot
        drawCircle(
            color = SunYellow,
            radius = 12f,
            center = Offset(centerX, centerY)
        )
        
        drawCircle(
            color = SolarOrange,
            radius = 6f,
            center = Offset(centerX, centerY)
        )
    }
}
