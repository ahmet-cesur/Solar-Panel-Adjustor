package com.acesur.solarpvtracker.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.sqrt

data class TiltData(
    val pitch: Float,  // Forward/backward tilt (rotation around X-axis)
    val roll: Float,   // Left/right tilt (rotation around Y-axis)
    val azimuth: Float // Compass heading (rotation around Z-axis)
)

class TiltSensorManager(context: Context) {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    val tiltDataFlow: Flow<TiltData> = callbackFlow {
        var lastUpdateTimestamp: Long = 0
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        accelerometerReading[0] = event.values[0]
                        accelerometerReading[1] = event.values[1]
                        // Force Z positive to always simulate "face up" orientation
                        // This prevents 180-degree azimuth flips near vertical as requested
                        accelerometerReading[2] = kotlin.math.abs(event.values[2])
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
                    }
                }
                
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTimestamp >= 300) { // 0.3 seconds interval
                    lastUpdateTimestamp = currentTime
                    
                    updateOrientationAngles()
                    
                    val azimuthDegree = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val pitchDegree = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val rollDegree = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                    
                    // Calculate azimuth and normalize to 0-360
                    val azimuth = (azimuthDegree + 360) % 360
                    
                    val tiltData = TiltData(
                        pitch = pitchDegree,
                        roll = rollDegree,
                        azimuth = azimuth
                    )
                    
                    trySend(tiltData)
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed for this implementation
            }
        }
        
        accelerometer?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        
        magnetometer?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    private fun updateOrientationAngles() {
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }
    
    /**
     * Calculate the tilt angle for solar panel from accelerometer reading.
     * Returns angle from horizontal (0° = flat, 90° = vertical)
     */
    fun calculatePanelTiltAngle(accelerometerValues: FloatArray): Float {
        val x = accelerometerValues[0]
        val y = accelerometerValues[1]
        val z = accelerometerValues[2]
        
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude == 0f) return 0f
        
        // Calculate angle from horizontal plane
        val angle = Math.toDegrees(atan2(y.toDouble(), sqrt((x * x + z * z).toDouble())))
        return angle.toFloat()
    }
    
    companion object {
        /**
         * Checks if the device has the required sensors
         */
        fun hasSensors(context: Context): Boolean {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        }
    }
}
