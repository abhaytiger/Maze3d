package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.GameUiState

/**
 * Tactile Virtual Tilt Pad and 3D Attitude Indicator.
 * Displays real-time device tilt angle and allows direct touch drag to tilt the board.
 */
@Composable
fun VirtualTiltPad(
    uiState: GameUiState,
    onTouchTilt: (Float, Float, Boolean) -> Unit,
    onToggleGyro: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xDD0B132B),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Control mode status and toggle
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.useGyro) Icons.Default.ScreenRotation else Icons.Default.TouchApp,
                        contentDescription = "Control mode",
                        tint = if (uiState.useGyro) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.useGyro) "GYRO SENSOR" else "TOUCH TILT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (uiState.useGyro) "Tilt device or drag pad" else "Drag circular pad to roll",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(4.dp))
                // Mode switch chip
                FilterChip(
                    selected = uiState.useGyro,
                    onClick = { onToggleGyro(!uiState.useGyro) },
                    label = {
                        Text(
                            text = if (uiState.useGyro) "Switch to Touch" else "Switch to Gyro",
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x3338BDF8),
                        selectedLabelColor = Color(0xFF38BDF8),
                        containerColor = Color(0x22FFFFFF),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.testTag("toggle_gyro_chip")
                )
            }

            // Interactive Attitude / Tilt Ring Pad
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val normX = ((offset.x / size.width) - 0.5f) * 2.2f
                                val normY = ((offset.y / size.height) - 0.5f) * 2.2f
                                onTouchTilt(normX.coerceIn(-1f, 1f), normY.coerceIn(-1f, 1f), true)
                            },
                            onDragEnd = {
                                onTouchTilt(0f, 0f, false)
                            },
                            onDragCancel = {
                                onTouchTilt(0f, 0f, false)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val normX = ((change.position.x / size.width) - 0.5f) * 2.2f
                                val normY = ((change.position.y / size.height) - 0.5f) * 2.2f
                                onTouchTilt(normX.coerceIn(-1f, 1f), normY.coerceIn(-1f, 1f), true)
                            }
                        )
                    }
                    .testTag("virtual_tilt_pad"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    val maxR = size.width * 0.44f

                    // Outer ring
                    drawCircle(
                        color = Color(0x4438BDF8),
                        radius = maxR,
                        center = Offset(cx, cy),
                        style = Stroke(2f)
                    )

                    // Inner crosshairs
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = Offset(cx - 10f, cy),
                        end = Offset(cx + 10f, cy),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = Offset(cx, cy - 10f),
                        end = Offset(cx, cy + 10f),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )

                    // Active Tilt Bubble (attitude indicator)
                    val bubbleX = cx + (uiState.tiltX * maxR * 0.7f).coerceIn(-maxR * 0.8f, maxR * 0.8f)
                    val bubbleY = cy + (uiState.tiltY * maxR * 0.7f).coerceIn(-maxR * 0.8f, maxR * 0.8f)

                    // Bubble glow
                    drawCircle(
                        color = Color(0x6638BDF8),
                        radius = 12f,
                        center = Offset(bubbleX, bubbleY)
                    )
                    // Core bubble
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFF38BDF8)),
                            center = Offset(bubbleX - 2f, bubbleY - 2f),
                            radius = 9f
                        ),
                        radius = 8f,
                        center = Offset(bubbleX, bubbleY)
                    )
                }
            }
        }
    }
}
