package com.acesur.solarpvtracker.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesur.solarpvtracker.ui.theme.SkyBlue
import com.acesur.solarpvtracker.ui.theme.SolarGreen
import com.acesur.solarpvtracker.ui.theme.SolarOrange
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Enhanced Bubble Level with compass and target position
 */
@Composable
fun BubbleLevel(
    xTilt: Float = 0f,
    yTilt: Float = 0f,
    targetXOffset: Float = 0f,
    targetYOffset: Float = 0f,
    isOnTarget: Boolean = false,
    showTarget: Boolean = false,
    currentAzimuth: Float = 0f,
    targetAzimuth: Float = 180f,
    combinedError: Float = 90f,
    isNorthernHemisphere: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Beeping logic based on combined error
    // Use rememberUpdatedState to access current values inside the loop without restarting it
    val currentCombinedError by rememberUpdatedState(combinedError)
    
    LaunchedEffect(showTarget) {
        if (!showTarget) return@LaunchedEffect
        
        while (true) {
            val error = currentCombinedError
            when {
                // Within 5 degrees (limits): Continuous beep
                error <= 5f -> {
                    playBeep(context, 150)
                    delay(150)
                }
                // Just outside limits: Fast intermittent
                error <= 6f -> {
                    playBeep(context, 80)
                    delay(300)
                }
                // Farther out: Slower intermittent
                error <= 20f -> {
                    playBeep(context, 80)
                    delay(600)
                }
                // Far out: No beep
                else -> {
                    delay(200) // Check again frequently
                }
            }
        }
    }
    
    val animatedX by animateFloatAsState(
        targetValue = xTilt.coerceIn(-1f, 1f),
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "xTilt"
    )
    
    val animatedY by animateFloatAsState(
        targetValue = yTilt.coerceIn(-1f, 1f),
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "yTilt"
    )
    
    val animatedAzimuth by animateFloatAsState(
        targetValue = currentAzimuth,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 50f),
        label = "azimuth"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compass moved to external control
        
        // Bubble Level Canvas
        Canvas(modifier = modifier.size(220.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 - 20f
            
            // Outer ring
            drawCircle(
                color = Color.LightGray,
                radius = radius + 10f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3f)
            )
            
            // Level circles
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(centerX, centerY)
            )
            
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.2f),
                radius = radius * 0.66f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
            
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.2f),
                radius = radius * 0.33f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
            
            // Cross hairs
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(centerX - radius, centerY),
                end = Offset(centerX + radius, centerY),
                strokeWidth = 1f
            )
            
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(centerX, centerY - radius),
                end = Offset(centerX, centerY + radius),
                strokeWidth = 1f
            )
            
            // Target indicator (optimal position)
            if (showTarget) {
                val targetX = centerX + (targetXOffset * (radius - 30f))
                val targetY = centerY + (targetYOffset * (radius - 30f))
                
                drawCircle(
                    color = SolarOrange,
                    radius = 35f,
                    center = Offset(targetX, targetY),
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                )
                
                drawCircle(
                    color = SolarOrange.copy(alpha = 0.3f),
                    radius = 30f,
                    center = Offset(targetX, targetY)
                )
                
                drawLine(
                    color = SolarOrange,
                    start = Offset(targetX - 20f, targetY),
                    end = Offset(targetX + 20f, targetY),
                    strokeWidth = 2f
                )
                drawLine(
                    color = SolarOrange,
                    start = Offset(targetX, targetY - 20f),
                    end = Offset(targetX, targetY + 20f),
                    strokeWidth = 2f
                )
            } else {
                drawCircle(
                    color = SolarGreen.copy(alpha = 0.5f),
                    radius = 25f,
                    center = Offset(centerX, centerY)
                )
            }
            
            // Bubble position
            val bubbleX = centerX + (animatedX * (radius - 30f))
            val bubbleY = centerY + (animatedY * (radius - 30f))
            
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = 28f,
                center = Offset(bubbleX + 3f, bubbleY + 3f)
            )
            
            val bubbleColor = when {
                isOnTarget && showTarget -> SolarGreen
                showTarget && combinedError <= 5f -> SolarOrange
                showTarget -> SkyBlue
                abs(animatedX) < 0.1f && abs(animatedY) < 0.1f -> SolarGreen
                else -> SkyBlue
            }
            
            drawCircle(
                color = bubbleColor,
                radius = 25f,
                center = Offset(bubbleX, bubbleY)
            )
            
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = 10f,
                center = Offset(bubbleX - 8f, bubbleY - 8f)
            )
        }
    }
}

@Composable
fun CompassIndicator(
    currentAzimuth: Float,
    targetAzimuth: Float,
    isNorthernHemisphere: Boolean,
    azimuthError: Float,
    modifier: Modifier = Modifier
) {
    val targetDirection = if (isNorthernHemisphere) "S" else "N"
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Target direction label
        Text(
            text = "Point $targetDirection (${targetAzimuth.toInt()}°)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when {
                azimuthError <= 1f -> SolarGreen
                azimuthError <= 10f -> SolarOrange
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Compass
        Canvas(modifier = Modifier.size(80.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 - 10f
            
            // Outer compass ring
            drawCircle(
                color = Color.DarkGray,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3f)
            )
            
            // Inner circle
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.2f),
                radius = radius - 5f,
                center = Offset(centerX, centerY)
            )
            
            // Degree markings (every 30 degrees)
            for (degree in 0 until 360 step 30) {
                val angleRad = Math.toRadians(degree.toDouble() - 90)
                val innerRadius = radius - 15f
                val outerRadius = radius - 5f
                
                val startX = centerX + (innerRadius * cos(angleRad)).toFloat()
                val startY = centerY + (innerRadius * sin(angleRad)).toFloat()
                val endX = centerX + (outerRadius * cos(angleRad)).toFloat()
                val endY = centerY + (outerRadius * sin(angleRad)).toFloat()
                
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (degree % 90 == 0) 3f else 1.5f
                )
            }
            
            // Small ticks every 10 degrees
            for (degree in 0 until 360 step 10) {
                if (degree % 30 != 0) {
                    val angleRad = Math.toRadians(degree.toDouble() - 90)
                    val innerRadius = radius - 10f
                    val outerRadius = radius - 5f
                    
                    val startX = centerX + (innerRadius * cos(angleRad)).toFloat()
                    val startY = centerY + (innerRadius * sin(angleRad)).toFloat()
                    val endX = centerX + (outerRadius * cos(angleRad)).toFloat()
                    val endY = centerY + (outerRadius * sin(angleRad)).toFloat()
                    
                    drawLine(
                        color = Color.Gray,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1f
                    )
                }
            }
            
            // Target direction marker (orange triangle)
            val targetAngleRad = Math.toRadians(targetAzimuth.toDouble() - 90)
            val targetMarkerRadius = radius - 20f
            val targetX = centerX + (targetMarkerRadius * cos(targetAngleRad)).toFloat()
            val targetY = centerY + (targetMarkerRadius * sin(targetAngleRad)).toFloat()
            
            rotate(degrees = targetAzimuth, pivot = Offset(targetX, targetY)) {
                val trianglePath = Path().apply {
                    moveTo(targetX, targetY - 12f)
                    lineTo(targetX - 8f, targetY + 6f)
                    lineTo(targetX + 8f, targetY + 6f)
                    close()
                }
                drawPath(trianglePath, SolarOrange)
            }
            
            // Compass needle (rotates based on azimuth)
            rotate(degrees = -currentAzimuth, pivot = Offset(centerX, centerY)) {
                // North pointer (red)
                val northPath = Path().apply {
                    moveTo(centerX, centerY - radius + 25f)
                    lineTo(centerX - 8f, centerY)
                    lineTo(centerX + 8f, centerY)
                    close()
                }
                drawPath(northPath, Color.Red)
                
                // South pointer (white/gray)
                val southPath = Path().apply {
                    moveTo(centerX, centerY + radius - 25f)
                    lineTo(centerX - 8f, centerY)
                    lineTo(centerX + 8f, centerY)
                    close()
                }
                drawPath(southPath, Color.White)
                drawPath(southPath, Color.DarkGray, style = Stroke(width = 1f))
                
                // Center pivot
                drawCircle(
                    color = Color.DarkGray,
                    radius = 8f,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(centerX, centerY)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Current azimuth reading
        Text(
            text = String.format("%.0f°", currentAzimuth),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                azimuthError <= 1f -> SolarGreen
                azimuthError <= 10f -> SolarOrange
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private fun playBeep(context: Context, durationMs: Int = 80) {
    try {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            toneGenerator.release()
        }, durationMs.toLong() + 20)
    } catch (e: Exception) {
        // Ignore audio errors
    }
}
