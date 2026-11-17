package com.climb.companion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// Watches the phone's barometer to find climbing attempts
// Pressure drops as you go up, so the height above where you started standing tells us
// when an attempt starts and ends
// When one ends, onAttempt is called with the highest point reached and how long it took
class AttemptTracker(
    context: Context,
    private val onAttempt: (maxHeight: Double, seconds: Int) -> Unit,
) : SensorEventListener {

    // Numbers to tune on a real phone
    private val startHeight = 0.5   // meters above the ground that count as climbing
    private val groundHeight = 0.3  // lower than this counts as back on the ground
    private val minHeight = 1.5     // attempts that never get this high are ignored

    // The barometer is null if the phone does not have one
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val barometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    val hasBarometer: Boolean = barometer != null

    // What the tracker remembers between readings
    private var running = false
    private var groundPressure = 0f // hPa when standing on the ground, 0 until the first reading
    private var climbing = false
    private var startMillis = 0L
    private var maxHeight = 0.0

    // Starts listening, the ground level is learned again from the first reading
    fun start() {
        if (running || barometer == null) return
        running = true
        groundPressure = 0f
        climbing = false
        sensorManager.registerListener(this, barometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    // An attempt that is still going when this is called is dropped
    fun stop() {
        running = false
        sensorManager.unregisterListener(this)
    }

    // Android calls this every time the barometer has a new reading
    override fun onSensorChanged(event: SensorEvent) {
        val reading = event.values[0]

        // The first reading is the ground level
        if (groundPressure == 0f) {
            groundPressure = reading
            return
        }

        // Turn the pressure difference into meters above the ground
        val height = SensorManager.getAltitude(groundPressure, reading).toDouble()

        if (!climbing) {
            // Went high enough, an attempt has started
            if (height > startHeight) {
                climbing = true
                startMillis = System.currentTimeMillis()
                maxHeight = height
            }
            return
        }

        // Keep track of the highest point
        if (height > maxHeight) maxHeight = height

        // Back on the ground, the attempt is over
        if (height < groundHeight) {
            climbing = false
            if (maxHeight >= minHeight) {
                val seconds = ((System.currentTimeMillis() - startMillis) / 1000).toInt()
                onAttempt(maxHeight, seconds)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
