package com.hieltech.haramblur.data.cities

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for searching cities via OpenStreetMap Nominatim with simple in-memory caching
 * and offline fallback from bundled assets.
 */
@Singleton
class CitiesRepository @Inject constructor(
    private val context: Context,
    private val api: CitiesApiService,
    private val gson: Gson
) {
    // In-memory LRU cache for recent queries
    private val cache = object : LinkedHashMap<String, List<CitySearchResult>>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<CitySearchResult>>?): Boolean {
            return size > 50
        }
    }

    // Lazy load offline fallback list
    @Volatile
    private var popularFallback: List<CitySearchResult>? = null

    suspend fun search(query: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return@withContext emptyList()

        cache[normalized]?.let { return@withContext it }

        val online = runCatching {
            api.searchCities(query = normalized, limit = CitySearchConfig.RESULT_LIMIT)
                .mapNotNull { it.toCitySearchResult() }
                .filter { it.name.isNotBlank() }
                .sortedByDescending { it.importance }
        }.getOrElse { emptyList() }

        val results = if (online.isNotEmpty()) online else getOfflineFallback(normalized)
        cache[normalized] = results
        return@withContext results
    }

    private fun getOfflineFallback(normalizedQuery: String): List<CitySearchResult> {
        val list = loadPopularCities()
        if (list.isEmpty()) return emptyList()
        return list.filter {
            val n = it.name.lowercase()
            val dn = it.displayName.lowercase()
            n.contains(normalizedQuery) || dn.contains(normalizedQuery)
        }.take(CitySearchConfig.RESULT_LIMIT)
    }

    private fun loadPopularCities(): List<CitySearchResult> {
        popularFallback?.let { return it }
        return try {
            val assetManager = context.assets
            val input = assetManager.open("popular_cities.json")
            val reader = BufferedReader(InputStreamReader(input))
            val content = reader.readText()
            reader.close()

            val parsed = gson.fromJson(content, PopularCitiesWrapper::class.java)
            val results = parsed.cities.map { pc ->
                CitySearchResult(
                    name = pc.name,
                    country = pc.country,
                    countryCode = pc.countryCode,
                    latitude = pc.latitude,
                    longitude = pc.longitude,
                    displayName = pc.displayName ?: listOfNotNull(pc.name, pc.country).joinToString(", "),
                    importance = pc.importance ?: 0.0
                )
            }
            popularFallback = results
            results
        } catch (e: Exception) {
            popularFallback = emptyList()
            emptyList()
        }
    }
}

// Models used to parse offline fallback asset
private data class PopularCitiesWrapper(
    @SerializedName("cities") val cities: List<PopularCity>
)

private data class PopularCity(
    @SerializedName("name") val name: String,
    @SerializedName("country") val country: String?,
    @SerializedName("countryCode") val countryCode: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("importance") val importance: Double?
)
