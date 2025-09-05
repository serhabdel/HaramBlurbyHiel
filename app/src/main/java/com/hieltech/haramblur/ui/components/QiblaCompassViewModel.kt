package com.hieltech.haramblur.ui.components

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieltech.haramblur.data.*
import com.hieltech.haramblur.data.compass.*
import com.hieltech.haramblur.utils.LocationHelper
import com.hieltech.haramblur.utils.QiblaCalculator
import com.hieltech.haramblur.utils.CompassSensorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QiblaCompassViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compassSensorManager: CompassSensorManager,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _state = MutableStateFlow(QiblaCompassData())
    val state: StateFlow<QiblaCompassData> = _state.asStateFlow()

    private var cachedBearing: Double? = null
    private var cachedDeclination: Float? = null

    init {
        observeSensors()
        observeSettings()
    }

    private fun observeSensors() {
        viewModelScope.launch {
            combine(
                compassSensorManager.azimuthDeg,
                compassSensorManager.accuracy
            ) { azimuth, acc -> azimuth to acc }
                .collect { (azimuth, acc) ->
                    val current = _state.value
                    val bearing = cachedBearing ?: current.compassState.qiblaBearing
                    val decl = cachedDeclination ?: current.compassState.magneticDeclination
                    
                    // Use the helper function to calculate angle to Qibla with declination
                    val angleTo = calculateAngleToQiblaWithDeclination(azimuth, bearing, decl)
                    
                    // Calculate true azimuth for completeness
                    val trueAzimuth = QiblaCalculator.calculateTrueAzimuth(azimuth.toDouble(), decl).toFloat()
                    
                    _state.value = current.copy(
                        compassState = current.compassState.copy(
                            // Keep deviceAzimuth as RAW magnetic azimuth (no declination applied)
                            deviceAzimuth = azimuth,
                            trueAzimuth = trueAzimuth,
                            angleToQibla = angleTo,
                            sensorAccuracy = acc
                        ),
                        isLoading = false
                    )
                }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                // respond to update rate or declination preference
                if (s.qiblaCompassEnabled) {
                    compassSensorManager.startListening(s.compassUpdateRate)
                } else {
                    compassSensorManager.stopListening()
                }
                
                // Handle runtime declination setting changes
                val currentState = _state.value
                if (currentState.compassState.qiblaBearing != 0.0) {
                    // Recompute declination when setting changes
                    val loc = locationHelper.getBestLocation()
                    if (loc != null) {
                        val newDecl = if (s.enableMagneticDeclination) {
                            QiblaCalculator.getMagneticDeclination(context, loc.latitude, loc.longitude, null)
                        } else 0f
                        cachedDeclination = newDecl
                        
                        // Update state with new declination
                        _state.value = currentState.copy(
                            compassState = currentState.compassState.copy(
                                magneticDeclination = newDecl
                            )
                        )
                    }
                }
            }
        }
    }

    fun startCompass() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val loc = locationHelper.getBestLocation()
            if (loc == null) {
                _state.value = _state.value.copy(
                    error = CompassError.LOCATION_UNAVAILABLE,
                    isLoading = false
                )
                return@launch
            }
            val lat = loc.latitude
            val lon = loc.longitude
            val bearing = try {
                QiblaCalculator.calculateQiblaBearing(lat, lon)
            } catch (e: Exception) { 0.0 }
            cachedBearing = bearing

            val settings = settingsRepository.getCurrentSettings()
            val decl = if (settings.enableMagneticDeclination) {
                QiblaCalculator.getMagneticDeclination(context, lat, lon, null)
            } else 0f
            cachedDeclination = decl

            _state.value = _state.value.copy(
                compassState = _state.value.compassState.copy(
                    qiblaBearing = bearing,
                    magneticDeclination = decl,
                    // Do NOT apply declination here; keep raw magnetic azimuth
                    deviceAzimuth = _state.value.compassState.deviceAzimuth
                ),
                locationAccuracy = locationHelper.classifyAccuracy(settings.locationAccuracy),
                lastLocationUpdate = settings.locationLastUpdated,
                error = null,
                isLoading = false
            )
        }
    }

    fun stopCompass() {
        compassSensorManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        compassSensorManager.stopListening()
    }
}
