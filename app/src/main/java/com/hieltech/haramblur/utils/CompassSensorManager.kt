package com.hieltech.haramblur.utils

import android.content.Context
import android.hardware.*
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.roundToInt

import com.hieltech.haramblur.data.compass.SensorAccuracy
import com.hieltech.haramblur.data.compass.CompassConstants
import com.hieltech.haramblur.utils.normalizeDegrees

class CompassSensorManager(
    private val context: Context
) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _azimuthDeg = MutableStateFlow(0f)
    val azimuthDeg: StateFlow<Float> = _azimuthDeg

    private val _accuracy = MutableStateFlow(SensorAccuracy.UNAVAILABLE)
    val accuracy: StateFlow<SensorAccuracy> = _accuracy

    private var scope: CoroutineScope? = null
    private var throttleJob: Job? = null

    private val accelValues = FloatArray(3)
    private val magnetValues = FloatArray(3)
    private var haveAccel = false
    private var haveMagnet = false

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var lastEmitMs = 0L

    fun isSensorAvailable(): Boolean {
        return rotationVector != null || (accelerometer != null && magnetometer != null)
    }

    fun startListening(updateRateHz: Int = CompassConstants.SENSOR_UPDATE_RATE_HZ_DEFAULT) {
        if (!isSensorAvailable()) {
            _accuracy.value = SensorAccuracy.UNAVAILABLE
            return
        }
        stopListening()
        scope = CoroutineScope(Dispatchers.Default)
        val delayMicros = (1_000_000f / updateRateHz).roundToInt()
        rotationVector?.let {
            sensorManager.registerListener(this, it, delayMicros)
        } ?: run {
            accelerometer?.let { sensorManager.registerListener(this, it, delayMicros) }
            magnetometer?.let { sensorManager.registerListener(this, it, delayMicros) }
        }
        _accuracy.value = SensorAccuracy.MEDIUM
    }

    fun stopListening() {
        try { sensorManager.unregisterListener(this) } catch (_: Exception) {}
        throttleJob?.cancel()
        throttleJob = null
        scope?.cancel()
        scope = null
        haveAccel = false
        haveMagnet = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> handleRotationVector(event)
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelValues, 0, 3)
                haveAccel = true
                computeOrientationIfPossible()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetValues, 0, 3)
                haveMagnet = true
                computeOrientationIfPossible()
            }
        }
    }

    private fun handleRotationVector(event: SensorEvent) {
        try {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthRad = orientation[0]
            val deg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            publishAzimuth(deg)
        } catch (_: Exception) {}
    }

    private fun computeOrientationIfPossible() {
        if (!(haveAccel && haveMagnet)) return
        try {
            SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magnetValues)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthRad = orientation[0]
            val deg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            publishAzimuth(deg)
        } catch (_: Exception) {}
    }

    private fun publishAzimuth(rawDeg: Float) {
        val now = System.currentTimeMillis()
        if (now - lastEmitMs < 1000L / CompassConstants.SENSOR_UPDATE_RATE_HZ_DEFAULT) return
        lastEmitMs = now
        val current = _azimuthDeg.value
        val normalized = normalizeDegrees(rawDeg)
        val alpha = CompassConstants.LOW_PASS_ALPHA
        val filtered = (alpha * normalized + (1 - alpha) * current)
        _azimuthDeg.value = normalizeDegrees(filtered)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        _accuracy.value = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SensorAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SensorAccuracy.LOW
            SensorManager.SENSOR_STATUS_UNRELIABLE -> SensorAccuracy.UNRELIABLE
            else -> SensorAccuracy.UNAVAILABLE
        }
    }
}
