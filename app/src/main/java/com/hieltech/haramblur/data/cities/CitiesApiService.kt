package com.hieltech.haramblur.data.cities

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * Retrofit service for OpenStreetMap Nominatim Search API.
 * We use the public endpoint and must include a valid User-Agent via OkHttp (added in NetworkModule).
 * See: https://nominatim.org/release-docs/develop/api/Search/
 */
interface CitiesApiService {

    /**
     * Search for places (cities/towns/villages) by free-text query.
     *
     * Notes:
     * - We request JSON v2 with address details to parse city/country reliably.
     * - We pass a language hint via the `accept-language` header for localized names where possible.
     * - We filter to administrative places typically considered cities by using `featuretype`.
     * - The Nominatim usage policy recommends identifying the app via User-Agent and being rate-friendly.
     */
    @Headers(
        // Ask for localized results where possible; concrete language can also be set on OkHttp dynamically.
        "Accept-Language: en",
        // As per Nominatim docs, JSON v2 is recommended. (We also encode via query, but header is fine.)
        "Accept: application/json"
    )
    @GET("search")
    suspend fun searchCities(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("namedetails") nameDetails: Int = 1,
        @Query("extratags") extraTags: Int = 0,
        // Limit results to be UI-friendly
        @Query("limit") limit: Int = 15,
        // Restrict to place types that are commonly cities. Nominatim uses `featuretype` for a rough filter.
        // Valid values include: city, town, village, state, country, etc.
        // We will prefer city/town/village in repository filtering as well.
        @Query("featuretype") featureType: String = "city"
    ): List<NominatimSearchResponse>

    companion object {
        const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/"
    }
}
