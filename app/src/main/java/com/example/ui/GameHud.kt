package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.GameUiState

/**
 * Top HUD displaying Level badge, theme title, animated stars, elapsed time, and action buttons.
 */
@Composable
fun GameHud(
    uiState: GameUiState,
    onOpenWorldSelect: () -> Unit,
    onCalibrate: () -> Unit,
    onRestart: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xDD0F172A),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level & World Theme Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable { onOpenWorldSelect() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("level_select_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Select Level",
                    tint = uiState.theme.wallBevelColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "LEVEL ${uiState.currentLevelNumber}/100",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = uiState.theme.title,
                        fontSize = 10.sp,
                        color = uiState.theme.wallBevelColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Collected Gems / Stars indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..3) {
                    val isEarned = i <= uiState.collectedGems
                    Icon(
                        imageVector = if (isEarned) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Star $i",
                        tint = if (isEarned) Color(0xFFFBBF24) else Color(0x44FFFFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Timer display
            val totalSeconds = uiState.elapsedTime
            val minutes = (totalSeconds / 60).toInt()
            val seconds = (totalSeconds % 60).toInt()
            val tenths = ((totalSeconds * 10) % 10).toInt()
            val timeString = String.format("%02d:%02d.%d", minutes, seconds, tenths)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = timeString,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (totalSeconds > uiState.level.parTimeSeconds) Color(0xFFF87171) else Color(0xFF38BDF8)
                )
            }

            // Quick Actions: Calibrate Zero, Restart, Pause
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCalibrate,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("calibrate_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = "Calibrate Zero Tilt",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Level",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause Game",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
