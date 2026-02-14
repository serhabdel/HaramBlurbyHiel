package com.hieltech.haramblur.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Hadith API Service Interface
 * Provides access to prophetic traditions from hadithapi.com
 */
interface HadithApiService {

    companion object {
        const val BASE_URL = "https://hadithapi.com/"
    }

    @GET("api/hadiths")
    suspend fun getHadiths(
        @Query("apiKey") apiKey: String,
        @Query("book") book: String? = null,
        @Query("chapter") chapter: String? = null,
        @Query("hadithNumber") hadithNumber: String? = null,
        @Query("page") page: Int = 1
    ): HadithApiResponse
}

// --- API Response Models ---

data class HadithApiResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("hadiths") val hadiths: HadithPagination
)

data class HadithPagination(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("data") val data: List<HadithDto>,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("total") val total: Int
)

data class HadithDto(
    @SerializedName("id") val id: Int,
    @SerializedName("hadithNumber") val hadithNumber: String,
    @SerializedName("englishNarrator") val englishNarrator: String?,
    @SerializedName("hadithEnglish") val hadithEnglish: String?,
    @SerializedName("hadithArabic") val hadithArabic: String?,
    @SerializedName("hadithUrdu") val hadithUrdu: String?,
    @SerializedName("headingArabic") val headingArabic: String?,
    @SerializedName("headingEnglish") val headingEnglish: String?,
    @SerializedName("headingUrdu") val headingUrdu: String?,
    @SerializedName("chapterNumber") val chapterNumber: String?,
    @SerializedName("bookSlug") val bookSlug: String?,
    @SerializedName("volume") val volume: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("book") val book: HadithBookInfo?
)

data class HadithBookInfo(
    @SerializedName("id") val id: Int?,
    @SerializedName("bookName") val bookName: String?,
    @SerializedName("writerName") val writerName: String?,
    @SerializedName("aboutWriter") val aboutWriter: String?,
    @SerializedName("writerDeath") val writerDeath: String?,
    @SerializedName("bookSlug") val bookSlug: String?
)

// --- Domain Models ---

data class Hadith(
    val id: Int,
    val hadithNumber: String,
    val arabicText: String,
    val englishText: String,
    val narrator: String,
    val chapterNumber: String,
    val bookName: String,
    val bookSlug: String,
    val grade: String,
    val headingArabic: String,
    val headingEnglish: String
)

fun HadithDto.toDomain(): Hadith = Hadith(
    id = id,
    hadithNumber = hadithNumber,
    arabicText = hadithArabic ?: "",
    englishText = hadithEnglish ?: "",
    narrator = englishNarrator ?: "",
    chapterNumber = chapterNumber ?: "",
    bookName = book?.bookName ?: bookSlug ?: "",
    bookSlug = bookSlug ?: "",
    grade = status ?: "",
    headingArabic = headingArabic ?: "",
    headingEnglish = headingEnglish ?: ""
)
