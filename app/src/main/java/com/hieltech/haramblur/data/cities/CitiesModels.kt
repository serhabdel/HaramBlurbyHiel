package com.hieltech.haramblur.data.cities

import com.google.gson.annotations.SerializedName

/**
 * Models and helpers for City Search via OpenStreetMap Nominatim
 */

data class NominatimSearchResponse(
    @SerializedName("place_id") val placeId: Long?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("lat") val lat: String?,
    @SerializedName("lon") val lon: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("importance") val importance: Double?,
    @SerializedName("address") val address: NominatimAddress?
)

data class NominatimAddress(
    @SerializedName("city") val city: String?,
    @SerializedName("town") val town: String?,
    @SerializedName("village") val village: String?,
    @SerializedName("hamlet") val hamlet: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("country_code") val countryCode: String?
)

/** Internal representation for UI list rendering */
data class CitySearchResult(
    val name: String,
    val country: String?,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val importance: Double = 0.0
)

/** Persisted selection from user */
data class CitySelection(
    val name: String,
    val country: String?,
    val countryCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val geonameId: Long? = null
)

sealed class CitySearchState {
    data object Empty : CitySearchState()
    data object Loading : CitySearchState()
    data class Success(val results: List<CitySearchResult>) : CitySearchState()
    data class Error(val message: String) : CitySearchState()
}

/** Constants */
object CitySearchConfig {
    const val MIN_QUERY_LENGTH = 2
    const val DEBOUNCE_MS = 500L
    const val RESULT_LIMIT = 20
}

/** Extensions */
fun NominatimSearchResponse.toCitySearchResult(): CitySearchResult? {
    val latD = lat?.toDoubleOrNull()
    val lonD = lon?.toDoubleOrNull()
    val nameCandidate = address?.city
        ?: address?.town
        ?: address?.village
        ?: address?.hamlet
        ?: displayName
    val country = address?.country
    val code = address?.countryCode?.uppercase()

    if (latD == null || lonD == null || nameCandidate.isNullOrBlank() || displayName.isNullOrBlank()) return null

    return CitySearchResult(
        name = nameCandidate,
        country = country,
        countryCode = code,
        latitude = latD,
        longitude = lonD,
        displayName = displayName,
        importance = importance ?: 0.0
    )
}

fun CitySearchResult.toSelection(): CitySelection = CitySelection(
    name = name,
    country = country,
    countryCode = countryCode,
    latitude = latitude,
    longitude = longitude
)
