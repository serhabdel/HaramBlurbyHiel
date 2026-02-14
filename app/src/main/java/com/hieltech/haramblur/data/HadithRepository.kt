package com.hieltech.haramblur.data

import com.hieltech.haramblur.data.api.Hadith
import com.hieltech.haramblur.data.api.HadithApiService
import com.hieltech.haramblur.data.api.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HadithRepository @Inject constructor(
    private val hadithApiService: HadithApiService
) {
    suspend fun getHadiths(
        apiKey: String,
        book: String? = null,
        page: Int = 1
    ): HadithResult {
        return try {
            val response = hadithApiService.getHadiths(
                apiKey = apiKey,
                book = book,
                page = page
            )
            if (response.status == 200) {
                HadithResult.Success(
                    hadiths = response.hadiths.data.map { it.toDomain() },
                    currentPage = response.hadiths.currentPage,
                    lastPage = response.hadiths.lastPage,
                    total = response.hadiths.total
                )
            } else {
                HadithResult.Error("API error: ${response.message}")
            }
        } catch (e: Exception) {
            HadithResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun getHadithsByChapter(
        apiKey: String,
        book: String,
        chapter: String,
        page: Int = 1
    ): HadithResult {
        return try {
            val response = hadithApiService.getHadiths(
                apiKey = apiKey,
                book = book,
                chapter = chapter,
                page = page
            )
            if (response.status == 200) {
                HadithResult.Success(
                    hadiths = response.hadiths.data.map { it.toDomain() },
                    currentPage = response.hadiths.currentPage,
                    lastPage = response.hadiths.lastPage,
                    total = response.hadiths.total
                )
            } else {
                HadithResult.Error("API error: ${response.message}")
            }
        } catch (e: Exception) {
            HadithResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun getHadithByNumber(
        apiKey: String,
        book: String,
        hadithNumber: String
    ): HadithSingleResult {
        return try {
            val response = hadithApiService.getHadiths(
                apiKey = apiKey,
                book = book,
                hadithNumber = hadithNumber
            )
            if (response.status == 200 && response.hadiths.data.isNotEmpty()) {
                HadithSingleResult.Success(response.hadiths.data.first().toDomain())
            } else {
                HadithSingleResult.Error("Hadith not found")
            }
        } catch (e: Exception) {
            HadithSingleResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Fetch the Hadith of the Day using a deterministic daily selection.
     * Uses day-of-year as seed to pick a consistent hadith number per day.
     */
    suspend fun getHadithOfDay(
        apiKey: String,
        book: String,
        dayOfYear: Int
    ): HadithSingleResult {
        // Use a prime multiplier to spread selections across the book
        val hadithNumber = ((dayOfYear * 7919L) % 7000 + 1).toString()
        return try {
            val response = hadithApiService.getHadiths(
                apiKey = apiKey,
                book = book,
                hadithNumber = hadithNumber
            )
            if (response.status == 200 && response.hadiths.data.isNotEmpty()) {
                HadithSingleResult.Success(response.hadiths.data.first().toDomain())
            } else {
                // Fallback: fetch first hadith from the book if the calculated number doesn't exist
                val fallback = hadithApiService.getHadiths(
                    apiKey = apiKey,
                    book = book,
                    page = 1
                )
                if (fallback.status == 200 && fallback.hadiths.data.isNotEmpty()) {
                    HadithSingleResult.Success(fallback.hadiths.data.first().toDomain())
                } else {
                    HadithSingleResult.Error("No hadith found for today")
                }
            }
        } catch (e: Exception) {
            HadithSingleResult.Error(e.message ?: "Unknown error occurred")
        }
    }
}

sealed class HadithResult {
    data class Success(
        val hadiths: List<Hadith>,
        val currentPage: Int,
        val lastPage: Int,
        val total: Int
    ) : HadithResult()

    data class Error(val message: String) : HadithResult()
}

sealed class HadithSingleResult {
    data class Success(val hadith: Hadith) : HadithSingleResult()
    data class Error(val message: String) : HadithSingleResult()
}
