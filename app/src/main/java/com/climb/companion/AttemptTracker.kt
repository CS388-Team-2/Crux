package com.climb.companion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// Watches the phone's sensors to find climbing attempts
// Pressure drops as you go up, so the height above where you started standing tells us when an attempt starts and ends
// The accelerometer guesses how far we moved sideways, so we can draw an x y graph
// When an attempt ends, onAttempt is called with the highest point, how long it took
// and the path as text like "0,0;0.1,0.5" (x meters, height meters)
class AttemptTracker(
    context: Context,
    private val onAttempt: (maxHeight: Double, seconds: Int, path: String) -> Unit,
) : SensorEventListener {

    // Numbers to tune on a real phone
    private val startHeight = 0.5   // meters above the ground that count as climbing
    private val groundHeight = 0.3  // lower than this counts as back on the ground
    private val minHeight = 1.5     // attempts that never get this high are ignored
    private val fade = 0.95         // sideways speed shrinks a little each reading so x does not run away

    // A sensor is null if the phone does not have it
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val barometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val sideways: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    val hasBarometer: Boolean = barometer != null

    // What the tracker remembers between readings
    private var running = false
    private var groundPressure = 0f // hPa when standing on the ground, 0 until the first reading
    private var climbing = false
    private var startMillis = 0L
    private var maxHeight = 0.0

    // The x y path of the current attempt
    private var x = 0.0             // meters sideways from where the attempt started
    private var speed = 0.0         // meters per second sideways
    private var lastNanos = 0L      // time of the last accelerometer reading
    private val points = mutableListOf<String>()

    // Starts listening, the ground level is learned again from the first reading
    fun start() {
        if (running || barometer == null) return
        running = true
        groundPressure = 0f
        climbing = false
        sensorManager.registerListener(this, barometer, SensorManager.SENSOR_DELAY_NORMAL)
        if (sideways != null) {
            sensorManager.registerListener(this, sideways, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    // An attempt that is still going when this is called is dropped
    fun stop() {
        running = false
        sensorManager.unregisterListener(this)
    }

    // Android calls this every time a sensor has a new reading
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            moveSideways(event)
        } else {
            checkHeight(event.values[0])
        }
    }

    // Adds up the sideways acceleration to guess how far we moved left or right
    // This is rough, it only shows which way we went and about how far
    private fun moveSideways(event: SensorEvent) {
        val seconds = (event.timestamp - lastNanos) / 1_000_000_000.0
        lastNanos = event.timestamp
        if (!climbing || seconds > 0.5) return

        speed = (speed + event.values[0] * seconds) * fade
        x += speed * seconds
    }

    // Uses a new pressure reading to find where an attempt starts and ends
    private fun checkHeight(reading: Float) {
        // The first reading is the ground level
        if (groundPressure == 0f) {
            groundPressure = reading
            return
        }

        // Turn the pressure difference into meters above the ground
        val height = SensorManager.getAltitude(groundPressure, reading).toDouble()

        if (!climbing) {
            // Went high enough, an attempt has started with a fresh x y path
            if (height > startHeight) {
                climbing = true
                startMillis = System.currentTimeMillis()
                maxHeight = height
                x = 0.0
                speed = 0.0
                points.clear()
                addPoint(height)
            }
            return
        }

        // Keep track of the highest point and the path
        if (height > maxHeight) maxHeight = height
        addPoint(height)

        // Back on the ground, the attempt is over
        if (height < groundHeight) {
            climbing = false
            if (maxHeight >= minHeight) {
                val seconds = ((System.currentTimeMillis() - startMillis) / 1000).toInt()
                onAttempt(maxHeight, seconds, points.joinToString(";"))
            }
        }
    }

    // Saves where we are as "x,height" rounded to 2 decimals
    private fun addPoint(height: Double) {
        points.add("${Math.round(x * 100) / 100.0},${Math.round(height * 100) / 100.0}")
    }

    // Required by SensorEventListener, not used
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
