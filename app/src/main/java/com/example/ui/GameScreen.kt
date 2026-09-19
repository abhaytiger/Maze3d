package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.viewmodel.GameViewModel

/**
 * Main game screen container with HUD, 3D Tilt Labyrinth, Virtual Tilt Pad, and Dialogs.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val particles by viewModel.particles.collectAsState()

    // Immersive background gradient blending smoothly with current world theme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070B14),
                        uiState.theme.boardBackground,
                        Color(0xFF030712)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top HUD
            GameHud(
                uiState = uiState,
                onOpenWorldSelect = { viewModel.openWorldSelect() },
                onCalibrate = { viewModel.calibrateZero() },
                onRestart = { viewModel.restartLevel() },
                onPause = { viewModel.togglePause() }
            )

            // 2. Central 3D Labyrinth Canvas
            LabyrinthCanvas(
                uiState = uiState,
                particles = particles,
                onDragTilt = { tx, ty, active ->
                    viewModel.setTouchTilt(tx, ty, active)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // 3. Bottom Virtual Tilt Pad / Attitude Indicator
            VirtualTiltPad(
                uiState = uiState,
                onTouchTilt = { tx, ty, active ->
                    viewModel.setTouchTilt(tx, ty, active)
                },
                onToggleGyro = { useGyro ->
                    viewModel.setUseGyro(useGyro)
                }
            )
        }

        // 4. Modals and Dialogs
        WorldSelectSheet(
            uiState = uiState,
            onSelectLevel = { levelNum ->
                viewModel.loadLevel(levelNum)
            },
            onDismiss = {
                viewModel.closeWorldSelect()
            },
            getStarsForLevel = { lvl ->
                viewModel.preferences.getStarsForLevel(lvl)
            }
        )

        LevelCompleteDialog(
            uiState = uiState,
            onNextLevel = { viewModel.nextLevel() },
            onReplay = { viewModel.restartLevel() },
            onWorldSelect = { viewModel.openWorldSelect() }
        )

        PauseDialog(
            uiState = uiState,
            onResume = { viewModel.resumeGame() },
            onRestart = { viewModel.restartLevel() },
            onWorldSelect = { viewModel.openWorldSelect() },
            onCalibrate = { viewModel.calibrateZero() },
            onToggleGyro = { viewModel.setUseGyro(it) },
            onSensitivityChange = { viewModel.setSensitivity(it) },
            onToggleSound = { viewModel.toggleSound() },
            onToggleHaptics = { viewModel.toggleHaptics() }
        )
    }
}
