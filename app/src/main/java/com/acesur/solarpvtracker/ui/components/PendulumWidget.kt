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
    val isVertical = abs(roll) <= 2f // Using tighter tolerance for color
    val indicatorColor = if (abs(roll) <= 5f) SolarGreen else FestivalRed

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2
            
            // 1. Draw Static Background (Vertical Reference)
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(centerX, 20f),
                end = Offset(centerX, height - 20f),
                strokeWidth = 1.dp.toPx()
            )

            // 2. Draw Rotating Phone
            rotate(degrees = roll, pivot = Offset(centerX, centerY)) {
                val phoneWidth = 40.dp.toPx()
                val phoneHeight = 80.dp.toPx()
                val cornerRadius = 6.dp.toPx()
                
                // Phone Body
                drawRoundRect(
                    color = if (isVertical) SolarGreen else Color.DarkGray,
                    topLeft = Offset(centerX - phoneWidth / 2, centerY - phoneHeight / 2),
                    size = androidx.compose.ui.geometry.Size(phoneWidth, phoneHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Screen outline
                drawRoundRect(
                    color = if (isVertical) SolarGreen.copy(0.3f) else Color.Gray.copy(0.2f),
                    topLeft = Offset(centerX - (phoneWidth - 10f) / 2, centerY - (phoneHeight - 10f) / 2),
                    size = androidx.compose.ui.geometry.Size(phoneWidth - 10f, phoneHeight - 10f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius - 2f)
                )
                
                // Ear speaker / Camera dot
                drawCircle(
                    color = Color.Gray,
                    radius = 2.dp.toPx(),
                    center = Offset(centerX, centerY - phoneHeight / 2 + 8f)
                )
            }

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
