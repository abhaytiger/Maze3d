package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Local persistence for 100 level progress, stars, best times, and player settings.
 */
class GamePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gyro_maze_prefs", Context.MODE_PRIVATE)

    var maxUnlockedLevel: Int
        get() = prefs.getInt(KEY_MAX_UNLOCKED, 1)
        set(value) {
            val current = prefs.getInt(KEY_MAX_UNLOCKED, 1)
            if (value > current) {
                prefs.edit().putInt(KEY_MAX_UNLOCKED, value.coerceIn(1, 100)).apply()
            }
        }

    fun getStarsForLevel(level: Int): Int {
        return prefs.getInt(KEY_STARS_PREFIX + level, 0)
    }

    fun saveStarsForLevel(level: Int, stars: Int) {
        val current = getStarsForLevel(level)
        if (stars > current) {
            prefs.edit().putInt(KEY_STARS_PREFIX + level, stars.coerceIn(0, 3)).apply()
        }
    }

    fun getBestTimeForLevel(level: Int): Float {
        return prefs.getFloat(KEY_TIME_PREFIX + level, 0f)
    }

    fun saveBestTimeForLevel(level: Int, timeSeconds: Float) {
        val current = getBestTimeForLevel(level)
        if (current <= 0f || timeSeconds < current) {
            prefs.edit().putFloat(KEY_TIME_PREFIX + level, timeSeconds).apply()
        }
    }

    fun getTotalStars(): Int {
        var total = 0
        for (i in 1..100) {
            total += getStarsForLevel(i)
        }
        return total
    }

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    var useGyro: Boolean
        get() = prefs.getBoolean(KEY_USE_GYRO, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_GYRO, value).apply()

    var sensitivity: Float
        get() = prefs.getFloat(KEY_SENSITIVITY, 1.2f)
        set(value) = prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()

    companion object {
        private const val KEY_MAX_UNLOCKED = "max_unlocked_level"
        private const val KEY_STARS_PREFIX = "level_stars_"
        private const val KEY_TIME_PREFIX = "level_time_"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_USE_GYRO = "use_gyro"
        private const val KEY_SENSITIVITY = "tilt_sensitivity"
    }
}
