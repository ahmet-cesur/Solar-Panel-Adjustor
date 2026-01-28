package com.acesur.solarpvtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acesur.solarpvtracker.ui.theme.FestivalRed
import com.acesur.solarpvtracker.ui.theme.SolarGreen
import kotlin.math.abs

@Composable
fun PendulumWidget(
    roll: Float, // Rotation around Y axis (sideways tilt)
    modifier: Modifier = Modifier
) {
    // Determine if we are valid (vertical within +/- 5 degrees)
    val isVertical = abs(roll) <= 5f
    val indicatorColor = if (isVertical) SolarGreen else FestivalRed

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val topY = 0f
            
            // Draw background / reference frame
            // A semi-circle or arc at the top? Or just a vertical dotted line for reference
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(centerX, topY),
                end = Offset(centerX, height * 0.8f),
                strokeWidth = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Draw the Pendulum
            // We rotate around the top center point (centerX, topY)
            // Roll is in degrees. +Roll means tilt right? We just rotate by -roll to simulate gravity acting on pendulum
            // Actually if phone tilts right, pendulum should swing left relative to phone?
            // If phone tilts right (positive roll), the "down" vector is to the left in the phone's frame.
            // So we rotate by -roll.
            
            rotate(degrees = -roll, pivot = Offset(centerX, topY)) {
                // String
                drawLine(
                    color = indicatorColor,
                    start = Offset(centerX, topY),
                    end = Offset(centerX, height * 0.75f),
                    strokeWidth = 4.dp.toPx()
                )
                
                // Bob (Weight)
                drawCircle(
                    color = indicatorColor,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, height * 0.75f)
                )
                
                // Draw arrow head at bottom? Optional.
            }
            
            // Draw arc for safe zone (+/- 5 deg)
            // Arc center is (centerX, topY), radius is height * 0.75
            // Start angle: 90 - 5 = 85? No, 0 degrees is usually 3 o'clock in Canvas.
            // Down is 90 degrees.
            // So range is 85 to 95 degrees.
            // In Compose drawArc: startAngle and sweepAngle.
            // 90 degrees is Down.
            // -5 to +5 relative to Down (90).
            // So 85 start, 10 sweep.
            drawArc(
                color = SolarGreen.copy(alpha = 0.3f),
                startAngle = 85f,
                sweepAngle = 10f,
                useCenter = true,
                topLeft = Offset(centerX - height*0.75f, topY - height*0.75f),
                size = androidx.compose.ui.geometry.Size(height*1.5f, height*1.5f)
            )
        }
        
        // Text reading
        Text(
            text = String.format("%.1f°", roll),
            modifier = Modifier.align(Alignment.BottomCenter),
            color = indicatorColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
