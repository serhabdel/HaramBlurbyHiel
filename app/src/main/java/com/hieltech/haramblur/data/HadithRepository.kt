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
