package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles tactile haptics and real-time synthesized audio feedback for rolling ball events.
 */
class TactileHaptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var toneGen: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 65)
    } catch (_: Exception) {
        null
    }

    var isHapticsEnabled: Boolean = true
    var isSoundEnabled: Boolean = true

    private var lastWallSoundTime = 0L

    fun onWallHit(intensity: Float) {
        val now = System.currentTimeMillis()
        if (now - lastWallSoundTime < 60) return // Throttle wall sound
        lastWallSoundTime = now

        if (isHapticsEnabled && vibrator != null) {
            val duration = (12 + intensity * 25).toLong()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = (intensity * 220).toInt().coerceIn(20, 255)
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }

        if (isSoundEnabled) {
            try {
                val tone = if (intensity > 0.6f) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
                toneGen?.startTone(tone, 30)
            } catch (_: Exception) {}
        }
    }

    fun onStarCollected() {
        if (isHapticsEnabled && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 25, 40, 35), intArrayOf(0, 180, 0, 240), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 25, 40, 35), -1)
            }
        }

        if (isSoundEnabled) {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
            } catch (_: Exception) {}
        }
    }

    fun onHoleFall() {
        if (isHapticsEnabled && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 40, 50, 60, 60, 80),
                        intArrayOf(0, 100, 0, 180, 0, 255),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 40, 50, 60, 60, 80), -1)
            }
        }

        if (isSoundEnabled) {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
            } catch (_: Exception) {}
        }
    }

    fun onLevelVictory(scope: CoroutineScope) {
        if (isHapticsEnabled && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 60, 50, 60, 120),
                        intArrayOf(0, 200, 0, 220, 0, 255),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 60, 50, 60, 120), -1)
            }
        }

        if (isSoundEnabled) {
            scope.launch(Dispatchers.Default) {
                try {
                    toneGen?.startTone(ToneGenerator.TONE_DTMF_0, 70)
                    delay(90)
                    toneGen?.startTone(ToneGenerator.TONE_DTMF_5, 90)
                    delay(110)
                    toneGen?.startTone(ToneGenerator.TONE_DTMF_9, 180)
                } catch (_: Exception) {}
            }
        }
    }

    fun release() {
        toneGen?.release()
        toneGen = null
    }
}
