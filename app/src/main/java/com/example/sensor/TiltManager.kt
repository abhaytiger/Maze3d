package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Handles physical device gyro/accelerometer readings, calibration offsets,
 * smoothing, and touch tilt integration.
 */
class TiltManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isGyroAvailable: Boolean = rotationSensor != null || accelSensor != null

    private val _tiltX = MutableStateFlow(0f)
    val tiltX: StateFlow<Float> = _tiltX.asStateFlow()

    private val _tiltY = MutableStateFlow(0f)
    val tiltY: StateFlow<Float> = _tiltY.asStateFlow()

    // Calibration offsets
    private var zeroOffsetX = 0f
    private var zeroOffsetY = 0f

    // User settings
    var sensitivity: Float = 1.2f
    var useGyro: Boolean = true

    // Touch tilt override (for virtual tilt pad or emulator)
    private var touchTiltX = 0f
    private var touchTiltY = 0f
    var isTouchActive: Boolean = false

    // Smoothing filter
    private var smoothedRawX = 0f
    private var smoothedRawY = 0f
    private val filterAlpha = 0.35f

    // Rotation matrix buffers
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun start() {
        if (sensorManager != null) {
            val sensor = rotationSensor ?: accelSensor
            if (sensor != null) {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    fun calibrateZero() {
        zeroOffsetX = smoothedRawX
        zeroOffsetY = smoothedRawY
    }

    fun setTouchTilt(tx: Float, ty: Float, active: Boolean) {
        touchTiltX = tx.coerceIn(-1f, 1f)
        touchTiltY = ty.coerceIn(-1f, 1f)
        isTouchActive = active

        if (!useGyro || isTouchActive) {
            _tiltX.value = touchTiltX * sensitivity
            _tiltY.value = touchTiltY * sensitivity
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !useGyro) return

        var rawPitch = 0f
        var rawRoll = 0f

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            // orientationAngles: [0] azimuth, [1] pitch, [2] roll
            rawRoll = orientationAngles[2]  // tilt left/right
            rawPitch = orientationAngles[1] // tilt forward/back (natural holding angle ~0.5 rad)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]
            // Calculate tilt angle from gravity components
            rawRoll = -atan2(ax, sqrt(ay * ay + az * az))
            rawPitch = atan2(ay, sqrt(ax * ax + az * az)) - 0.45f // Compensate for holding angle
        }

        // Apply low-pass exponential smoothing
        smoothedRawX = smoothedRawX * (1f - filterAlpha) + rawRoll * filterAlpha
        smoothedRawY = smoothedRawY * (1f - filterAlpha) + rawPitch * filterAlpha

        // Only update from sensor if touch is not currently overriding
        if (!isTouchActive) {
            val calibratedX = (smoothedRawX - zeroOffsetX) * 2.2f * sensitivity
            val calibratedY = (smoothedRawY - zeroOffsetY) * 2.2f * sensitivity

            _tiltX.value = calibratedX.coerceIn(-1f, 1f)
            _tiltY.value = calibratedY.coerceIn(-1f, 1f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
