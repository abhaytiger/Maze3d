package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TactileHaptics
import com.example.data.GamePreferences
import com.example.model.MazeGenerator
import com.example.model.MazeLevel
import com.example.model.WorldTheme
import com.example.physics.BallPhysics
import com.example.physics.PhysicsEvent
import com.example.physics.VisualParticle
import com.example.sensor.TiltManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameUiState(
    val currentLevelNumber: Int = 1,
    val theme: WorldTheme = WorldTheme.CLASSIC_TEAK,
    val level: MazeLevel = MazeGenerator.generate(1),
    val ballX: Float = 0.1f,
    val ballY: Float = 0.1f,
    val ballRadius: Float = 0.02f,
    val rollAngleX: Float = 0f,
    val rollAngleY: Float = 0f,
    val isFalling: Boolean = false,
    val fallProgress: Float = 0f,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val elapsedTime: Float = 0f,
    val collectedGems: Int = 0,
    val totalGemsInLevel: Int = 3,
    val isPaused: Boolean = false,
    val isLevelCompleted: Boolean = false,
    val starsEarned: Int = 0,
    val isNewRecord: Boolean = false,
    val bestTime: Float = 0f,
    val maxUnlockedLevel: Int = 1,
    val totalStars: Int = 0,
    val isGyroAvailable: Boolean = true,
    val useGyro: Boolean = true,
    val sensitivity: Float = 1.2f,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isWorldSelectOpen: Boolean = false,
    val isPauseDialogOpen: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = GamePreferences(application)
    val tiltManager = TiltManager(application)
    val haptics = TactileHaptics(application)

    private var currentLevel: MazeLevel = MazeGenerator.generate(1)
    private val physics = BallPhysics(currentLevel)

    private val _uiState = MutableStateFlow(
        GameUiState(
            currentLevelNumber = 1,
            theme = currentLevel.theme,
            level = currentLevel,
            ballX = physics.x,
            ballY = physics.y,
            ballRadius = currentLevel.ballRadius,
            maxUnlockedLevel = preferences.maxUnlockedLevel,
            totalStars = preferences.getTotalStars(),
            isGyroAvailable = tiltManager.isGyroAvailable,
            useGyro = preferences.useGyro,
            sensitivity = preferences.sensitivity,
            isSoundEnabled = preferences.isSoundEnabled,
            isHapticsEnabled = preferences.isHapticsEnabled,
            bestTime = preferences.getBestTimeForLevel(1)
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _particles = MutableStateFlow<List<VisualParticle>>(emptyList())
    val particles: StateFlow<List<VisualParticle>> = _particles.asStateFlow()

    private var gameLoopJob: Job? = null
    private var levelStartTime: Long = System.currentTimeMillis()
    private var accumulatedTime: Float = 0f

    init {
        // Sync preferences to managers
        tiltManager.useGyro = preferences.useGyro
        tiltManager.sensitivity = preferences.sensitivity
        haptics.isSoundEnabled = preferences.isSoundEnabled
        haptics.isHapticsEnabled = preferences.isHapticsEnabled

        tiltManager.start()
        loadLevel(1)
        startGameLoop()
    }

    fun loadLevel(levelNum: Int) {
        val lvl = levelNum.coerceIn(1, 100)
        currentLevel = MazeGenerator.generate(lvl)
        physics.updateLevel(currentLevel)

        accumulatedTime = 0f
        levelStartTime = System.currentTimeMillis()

        val best = preferences.getBestTimeForLevel(lvl)
        _uiState.value = _uiState.value.copy(
            currentLevelNumber = lvl,
            theme = currentLevel.theme,
            level = currentLevel,
            ballX = physics.x,
            ballY = physics.y,
            ballRadius = currentLevel.ballRadius,
            rollAngleX = 0f,
            rollAngleY = 0f,
            isFalling = false,
            fallProgress = 0f,
            elapsedTime = 0f,
            collectedGems = 0,
            totalGemsInLevel = currentLevel.gems.size,
            isPaused = false,
            isLevelCompleted = false,
            starsEarned = 0,
            isNewRecord = false,
            bestTime = best,
            maxUnlockedLevel = preferences.maxUnlockedLevel,
            totalStars = preferences.getTotalStars(),
            isWorldSelectOpen = false,
            isPauseDialogOpen = false
        )
        _particles.value = emptyList()
    }

    fun restartLevel() {
        loadLevel(_uiState.value.currentLevelNumber)
    }

    fun nextLevel() {
        val next = _uiState.value.currentLevelNumber + 1
        if (next <= 100) {
            loadLevel(next)
        }
    }

    fun togglePause() {
        val paused = !_uiState.value.isPaused
        _uiState.value = _uiState.value.copy(
            isPaused = paused,
            isPauseDialogOpen = paused
        )
    }

    fun resumeGame() {
        _uiState.value = _uiState.value.copy(
            isPaused = false,
            isPauseDialogOpen = false
        )
    }

    fun openWorldSelect() {
        _uiState.value = _uiState.value.copy(
            isWorldSelectOpen = true,
            isPauseDialogOpen = false,
            isPaused = true
        )
    }

    fun closeWorldSelect() {
        _uiState.value = _uiState.value.copy(
            isWorldSelectOpen = false,
            isPaused = false
        )
    }

    fun calibrateZero() {
        tiltManager.calibrateZero()
    }

    fun setTouchTilt(tx: Float, ty: Float, active: Boolean) {
        tiltManager.setTouchTilt(tx, ty, active)
    }

    fun setUseGyro(use: Boolean) {
        preferences.useGyro = use
        tiltManager.useGyro = use
        _uiState.value = _uiState.value.copy(useGyro = use)
    }

    fun setSensitivity(sens: Float) {
        val clamped = sens.coerceIn(0.5f, 2.5f)
        preferences.sensitivity = clamped
        tiltManager.sensitivity = clamped
        _uiState.value = _uiState.value.copy(sensitivity = clamped)
    }

    fun toggleSound() {
        val newVal = !preferences.isSoundEnabled
        preferences.isSoundEnabled = newVal
        haptics.isSoundEnabled = newVal
        _uiState.value = _uiState.value.copy(isSoundEnabled = newVal)
    }

    fun toggleHaptics() {
        val newVal = !preferences.isHapticsEnabled
        preferences.isHapticsEnabled = newVal
        haptics.isHapticsEnabled = newVal
        _uiState.value = _uiState.value.copy(isHapticsEnabled = newVal)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()

            while (isActive) {
                delay(16) // ~60 FPS simulation step
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.008f, 0.033f)
                lastTime = now

                val state = _uiState.value
                if (state.isPaused || state.isLevelCompleted) {
                    continue
                }

                accumulatedTime += dt

                val tx = tiltManager.tiltX.value
                val ty = tiltManager.tiltY.value

                // Step physics
                physics.step(tx, ty, dt)

                // Process physics events
                for (event in physics.pendingEvents) {
                    when (event) {
                        is PhysicsEvent.WallHit -> haptics.onWallHit(event.intensity)
                        is PhysicsEvent.StarCollected -> haptics.onStarCollected()
                        is PhysicsEvent.FellInHole -> haptics.onHoleFall()
                        is PhysicsEvent.ReachedGoal -> onLevelCleared()
                        is PhysicsEvent.SpeedBoosted -> haptics.onWallHit(0.4f)
                    }
                }

                // Update particle snapshot
                _particles.value = physics.particles.toList()

                val collectedCount = currentLevel.gems.count { it.isCollected }

                _uiState.value = _uiState.value.copy(
                    ballX = physics.x,
                    ballY = physics.y,
                    rollAngleX = physics.rollAngleX,
                    rollAngleY = physics.rollAngleY,
                    isFalling = physics.isFalling,
                    fallProgress = physics.fallProgress,
                    tiltX = tx,
                    tiltY = ty,
                    elapsedTime = accumulatedTime,
                    collectedGems = collectedCount
                )
            }
        }
    }

    private fun onLevelCleared() {
        val lvl = currentLevel.levelNumber
        val time = accumulatedTime

        // Calculate stars earned:
        // 1 Star: Always awarded for finishing
        // 2 Stars: Finish under parTime OR collect 2+ gems
        // 3 Stars: Finish under targetTime AND collect all 3 gems
        val collectedCount = currentLevel.gems.count { it.isCollected }
        var stars = 1
        if (time <= currentLevel.parTimeSeconds || collectedCount >= 2) {
            stars = 2
        }
        if (time <= currentLevel.targetTimeSeconds && collectedCount == currentLevel.gems.size) {
            stars = 3
        }

        val previousBest = preferences.getBestTimeForLevel(lvl)
        val isNewRec = previousBest <= 0f || time < previousBest

        // Save progress
        preferences.saveStarsForLevel(lvl, stars)
        preferences.saveBestTimeForLevel(lvl, time)
        preferences.maxUnlockedLevel = lvl + 1

        haptics.onLevelVictory(viewModelScope)

        _uiState.value = _uiState.value.copy(
            isLevelCompleted = true,
            starsEarned = stars,
            isNewRecord = isNewRec,
            bestTime = preferences.getBestTimeForLevel(lvl),
            maxUnlockedLevel = preferences.maxUnlockedLevel,
            totalStars = preferences.getTotalStars()
        )
    }

    override fun onCleared() {
        super.onCleared()
        tiltManager.stop()
        haptics.release()
    }
}
