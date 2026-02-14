package com.hieltech.haramblur.data

import com.hieltech.haramblur.data.api.*
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranRepository @Inject constructor(
    private val quranApiService: QuranApiService
) {
    // Default translation IDs per language code (popular translations from quran.com)
    private val defaultTranslationIds = mapOf(
        "en" to "131",   // Dr. Mustafa Khattab, The Clear Quran
        "fr" to "136",   // Muhammad Hamidullah
        "ur" to "234",   // Fateh Muhammad Jalandhry
        "tr" to "77",    // Diyanet İşleri
        "id" to "33",    // Indonesian Ministry of Religious Affairs
        "ms" to "39",    // Abdullah Muhammad Basmeih
        "bn" to "161",   // Muhiuddin Khan
        "fa" to "135",   // Ayatollah Makarem Shirazi
        "es" to "140",   // Raúl González Bórnez
        "ar" to ""       // No translation needed for Arabic
    )

    fun getDefaultTranslationId(languageCode: String): String {
        return defaultTranslationIds[languageCode] ?: defaultTranslationIds["en"]!!
    }

    suspend fun getChapters(language: String = "en"): QuranResult<List<QuranChapter>> {
        return try {
            val response = quranApiService.getChapters(language)
            QuranResult.Success(response.chapters.map { it.toDomain() })
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Failed to load chapters")
        }
    }

    suspend fun getVersesByChapter(
        chapterNumber: Int,
        language: String = "en",
        translationId: String? = null,
        page: Int = 1,
        perPage: Int = 50
    ): QuranResult<QuranVersesPage> {
        return try {
            val response = quranApiService.getVersesByChapter(
                chapterNumber = chapterNumber,
                language = language,
                translations = translationId,
                page = page,
                perPage = perPage
            )
            QuranResult.Success(
                QuranVersesPage(
                    verses = response.verses.map { it.toDomain() },
                    currentPage = response.pagination?.currentPage ?: 1,
                    totalPages = response.pagination?.totalPages ?: 1,
                    totalRecords = response.pagination?.totalRecords ?: response.verses.size
                )
            )
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Failed to load verses")
        }
    }

    suspend fun getVersesByJuz(
        juzNumber: Int,
        language: String = "en",
        translationId: String? = null,
        page: Int = 1
    ): QuranResult<QuranVersesPage> {
        return try {
            val response = quranApiService.getVersesByJuz(
                juzNumber = juzNumber,
                language = language,
                translations = translationId,
                page = page
            )
            QuranResult.Success(
                QuranVersesPage(
                    verses = response.verses.map { it.toDomain() },
                    currentPage = response.pagination?.currentPage ?: 1,
                    totalPages = response.pagination?.totalPages ?: 1,
                    totalRecords = response.pagination?.totalRecords ?: response.verses.size
                )
            )
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Failed to load juz verses")
        }
    }

    suspend fun getJuzs(): QuranResult<List<QuranJuz>> {
        return try {
            val response = quranApiService.getJuzs()
            QuranResult.Success(response.juzs.map { it.toDomain() })
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Failed to load juzs")
        }
    }

    suspend fun search(
        query: String,
        language: String = "en",
        page: Int = 0
    ): QuranResult<QuranSearchPage> {
        return try {
            val response = quranApiService.search(query = query, language = language, page = page)
            val results = response.search?.results?.map { it.toDomain() } ?: emptyList()
            QuranResult.Success(
                QuranSearchPage(
                    results = results,
                    totalResults = response.search?.totalResults ?: 0,
                    currentPage = response.search?.currentPage ?: 0,
                    totalPages = response.search?.totalPages ?: 0
                )
            )
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Search failed")
        }
    }

    suspend fun getAvailableTranslations(language: String = "en"): QuranResult<List<TranslationResourceDto>> {
        return try {
            val response = quranApiService.getTranslations(language)
            QuranResult.Success(response.translations)
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Failed to load translations")
        }
    }

    /**
     * Verse of the Day: deterministic daily selection.
     * Total Quran verses = 6236, pick one based on day of year.
     */
    suspend fun getVerseOfDay(
        language: String = "en",
        translationId: String? = null
    ): QuranResult<QuranVerse> {
        return try {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val verseIndex = ((dayOfYear * 7919L) % 6236 + 1).toInt()
            // Map absolute verse index to chapter:verse
            val chapterVerse = absoluteVerseToChapterVerse(verseIndex)
            val response = quranApiService.getVersesByChapter(
                chapterNumber = chapterVerse.first,
                language = language,
                translations = translationId,
                page = 1,
                perPage = chapterVerse.second + 5 // fetch enough to include the verse
            )
            val verse = response.verses.find { it.verseNumber == chapterVerse.second }
                ?: response.verses.firstOrNull()
            if (verse != null) {
                QuranResult.Success(verse.toDomain())
            } else {
                QuranResult.Error("Could not load verse of the day")
            }
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Failed to load verse of the day")
        }
    }

    /** Maps an absolute verse index (1..6236) to (chapter, verseNumber) */
    private fun absoluteVerseToChapterVerse(absoluteIndex: Int): Pair<Int, Int> {
        val verseCounts = intArrayOf(
            7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,
            112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,
            89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,
            30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,
            15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6
        )
        var remaining = absoluteIndex
        for (i in verseCounts.indices) {
            if (remaining <= verseCounts[i]) {
                return Pair(i + 1, remaining)
            }
            remaining -= verseCounts[i]
        }
        return Pair(1, 1) // fallback
    }

    // ==================== Audio / Recitation Methods ====================

    suspend fun getRecitations(language: String = "en"): QuranResult<List<QuranReciter>> {
        return try {
            val response = quranApiService.getRecitations(language)
            QuranResult.Success(response.recitations.map { it.toDomain() })
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Failed to load reciters")
        }
    }

    suspend fun getChapterRecitationUrl(
        reciterId: Int,
        chapterNumber: Int
    ): QuranResult<String> {
        return try {
            val response = quranApiService.getChapterRecitation(reciterId, chapterNumber)
            val url = response.audioFile?.audioUrl
            if (url != null) {
                QuranResult.Success(url)
            } else {
                QuranResult.Error("No audio available for this chapter")
            }
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Failed to load chapter audio")
        }
    }

    suspend fun getVerseRecitations(
        reciterId: Int,
        chapterNumber: Int
    ): QuranResult<List<QuranAudioFile>> {
        return try {
            val response = quranApiService.getVerseRecitations(reciterId, chapterNumber)
            val audioFiles = response.audioFiles?.map { it.toDomain() } ?: emptyList()
            QuranResult.Success(audioFiles)
        } catch (e: Exception) {
            QuranResult.Error(e.message ?: "Failed to load verse audio")
        }
    }
}

// ==================== Result Types ====================

sealed class QuranResult<out T> {
    data class Success<T>(val data: T) : QuranResult<T>()
    data class Error(val message: String) : QuranResult<Nothing>()
}

data class QuranVersesPage(
    val verses: List<QuranVerse>,
    val currentPage: Int,
    val totalPages: Int,
    val totalRecords: Int
)

data class QuranSearchPage(
    val results: List<QuranSearchResult>,
    val totalResults: Int,
    val currentPage: Int,
    val totalPages: Int
)

