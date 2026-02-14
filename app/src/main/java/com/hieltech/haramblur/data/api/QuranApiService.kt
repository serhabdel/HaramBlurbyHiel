package com.hieltech.haramblur.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Quran.com Public API v4 Service Interface.
 * Uses the public API endpoint which does not require authentication.
 */
interface QuranApiService {

    companion object {
        const val API_BASE_URL = "https://api.quran.com/api/v4/"
    }

    @GET("chapters")
    suspend fun getChapters(
        @Query("language") language: String = "en"
    ): ChaptersResponse

    @GET("chapters/{id}")
    suspend fun getChapter(
        @Path("id") chapterId: Int,
        @Query("language") language: String = "en"
    ): ChapterDetailResponse

    @GET("verses/by_chapter/{chapter_number}")
    suspend fun getVersesByChapter(
        @Path("chapter_number") chapterNumber: Int,
        @Query("language") language: String = "en",
        @Query("words") words: Boolean = false,
        @Query("translations") translations: String? = null,
        @Query("fields") fields: String = "text_uthmani",
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): VersesResponse

    @GET("verses/by_juz/{juz_number}")
    suspend fun getVersesByJuz(
        @Path("juz_number") juzNumber: Int,
        @Query("language") language: String = "en",
        @Query("translations") translations: String? = null,
        @Query("fields") fields: String = "text_uthmani",
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1
    ): VersesResponse

    @GET("juzs")
    suspend fun getJuzs(): JuzsResponse

    @GET("resources/translations")
    suspend fun getTranslations(
        @Query("language") language: String = "en"
    ): TranslationsResponse

    @GET("resources/tafsirs")
    suspend fun getTafsirs(
        @Query("language") language: String = "en"
    ): TafsirsResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("language") language: String = "en",
        @Query("size") size: Int = 20,
        @Query("page") page: Int = 0
    ): SearchResponse

    // ==================== Audio / Recitation Endpoints ====================

    @GET("resources/recitations")
    suspend fun getRecitations(
        @Query("language") language: String = "en"
    ): RecitationsResponse

    @GET("chapter_recitations/{recitation_id}/{chapter_number}")
    suspend fun getChapterRecitation(
        @Path("recitation_id") recitationId: Int,
        @Path("chapter_number") chapterNumber: Int
    ): ChapterRecitationResponse

    @GET("recitations/{recitation_id}/by_chapter/{chapter_number}")
    suspend fun getVerseRecitations(
        @Path("recitation_id") recitationId: Int,
        @Path("chapter_number") chapterNumber: Int
    ): VerseRecitationsResponse
}

// ==================== API Response DTOs ====================

data class ChaptersResponse(
    @SerializedName("chapters") val chapters: List<ChapterDto>
)

data class ChapterDetailResponse(
    @SerializedName("chapter") val chapter: ChapterDto
)

data class ChapterDto(
    @SerializedName("id") val id: Int,
    @SerializedName("revelation_place") val revelationPlace: String,
    @SerializedName("revelation_order") val revelationOrder: Int,
    @SerializedName("bismillah_pre") val bismillahPre: Boolean,
    @SerializedName("name_simple") val nameSimple: String,
    @SerializedName("name_complex") val nameComplex: String,
    @SerializedName("name_arabic") val nameArabic: String,
    @SerializedName("verses_count") val versesCount: Int,
    @SerializedName("pages") val pages: List<Int>,
    @SerializedName("translated_name") val translatedName: TranslatedNameDto?
)

data class TranslatedNameDto(
    @SerializedName("language_name") val languageName: String?,
    @SerializedName("name") val name: String?
)

data class VersesResponse(
    @SerializedName("verses") val verses: List<VerseDto>,
    @SerializedName("pagination") val pagination: PaginationDto?
)

data class PaginationDto(
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("next_page") val nextPage: Int?,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_records") val totalRecords: Int
)

data class VerseDto(
    @SerializedName("id") val id: Int,
    @SerializedName("chapter_id") val chapterId: Int?,
    @SerializedName("verse_number") val verseNumber: Int,
    @SerializedName("verse_key") val verseKey: String,
    @SerializedName("text_uthmani") val textUthmani: String?,
    @SerializedName("text_imlaei") val textImlaei: String?,
    @SerializedName("juz_number") val juzNumber: Int,
    @SerializedName("hizb_number") val hizbNumber: Int,
    @SerializedName("page_number") val pageNumber: Int,
    @SerializedName("translations") val translations: List<TranslationDto>?,
    @SerializedName("words") val words: List<WordDto>?
)

data class TranslationDto(
    @SerializedName("resource_id") val resourceId: Int,
    @SerializedName("resource_name") val resourceName: String?,
    @SerializedName("id") val id: Int?,
    @SerializedName("text") val text: String,
    @SerializedName("language_name") val languageName: String?,
    @SerializedName("verse_key") val verseKey: String?
)

data class WordDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("position") val position: Int,
    @SerializedName("text_uthmani") val textUthmani: String?,
    @SerializedName("char_type_name") val charTypeName: String?,
    @SerializedName("translation") val translation: WordTranslationDto?,
    @SerializedName("transliteration") val transliteration: WordTranslationDto?
)

data class WordTranslationDto(
    @SerializedName("text") val text: String?,
    @SerializedName("language_name") val languageName: String?
)

data class JuzsResponse(
    @SerializedName("juzs") val juzs: List<JuzDto>
)

data class JuzDto(
    @SerializedName("id") val id: Int,
    @SerializedName("juz_number") val juzNumber: Int,
    @SerializedName("verse_mapping") val verseMapping: Map<String, String>?,
    @SerializedName("first_verse_id") val firstVerseId: Int?,
    @SerializedName("last_verse_id") val lastVerseId: Int?,
    @SerializedName("verses_count") val versesCount: Int?
)

data class TranslationsResponse(
    @SerializedName("translations") val translations: List<TranslationResourceDto>
)

data class TranslationResourceDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("author_name") val authorName: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("language_name") val languageName: String?,
    @SerializedName("translated_name") val translatedName: TranslatedNameDto?
)

data class TafsirsResponse(
    @SerializedName("tafsirs") val tafsirs: List<TafsirResourceDto>
)

data class TafsirResourceDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("author_name") val authorName: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("language_name") val languageName: String?,
    @SerializedName("translated_name") val translatedName: TranslatedNameDto?
)

data class SearchResponse(
    @SerializedName("search") val search: SearchResultsDto?
)

data class SearchResultsDto(
    @SerializedName("query") val query: String?,
    @SerializedName("total_results") val totalResults: Int?,
    @SerializedName("current_page") val currentPage: Int?,
    @SerializedName("total_pages") val totalPages: Int?,
    @SerializedName("results") val results: List<SearchResultDto>?
)

data class SearchResultDto(
    @SerializedName("verse_key") val verseKey: String,
    @SerializedName("verse_id") val verseId: Int?,
    @SerializedName("text") val text: String?,
    @SerializedName("highlighted") val highlighted: String?,
    @SerializedName("translations") val translations: List<TranslationDto>?
)

// ==================== Audio Response DTOs ====================

data class RecitationsResponse(
    @SerializedName("recitations") val recitations: List<ReciterDto>
)

data class ReciterDto(
    @SerializedName("id") val id: Int,
    @SerializedName("reciter_name") val reciterName: String,
    @SerializedName("style") val style: String?,
    @SerializedName("translated_name") val translatedName: TranslatedNameDto?
)

data class ChapterRecitationResponse(
    @SerializedName("audio_file") val audioFile: ChapterAudioFileDto?
)

data class ChapterAudioFileDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("chapter_id") val chapterId: Int?,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("format") val format: String?,
    @SerializedName("audio_url") val audioUrl: String?
)

data class VerseRecitationsResponse(
    @SerializedName("audio_files") val audioFiles: List<VerseAudioFileDto>?,
    @SerializedName("pagination") val pagination: PaginationDto?
)

data class VerseAudioFileDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("chapter_id") val chapterId: Int?,
    @SerializedName("verse_key") val verseKey: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("format") val format: String?
)

// ==================== Domain Models ====================

data class QuranChapter(
    val id: Int,
    val revelationPlace: String,
    val revelationOrder: Int,
    val bismillahPre: Boolean,
    val nameSimple: String,
    val nameComplex: String,
    val nameArabic: String,
    val versesCount: Int,
    val pages: List<Int>,
    val translatedName: String
)

data class QuranVerse(
    val id: Int,
    val chapterId: Int,
    val verseNumber: Int,
    val verseKey: String,
    val textUthmani: String,
    val juzNumber: Int,
    val hizbNumber: Int,
    val pageNumber: Int,
    val translationText: String,
    val translationResourceName: String
)

data class QuranJuz(
    val id: Int,
    val juzNumber: Int,
    val verseMapping: Map<String, String>,
    val versesCount: Int
)

data class QuranSearchResult(
    val verseKey: String,
    val verseId: Int,
    val text: String,
    val highlighted: String
)

data class QuranReciter(
    val id: Int,
    val name: String,
    val style: String?
)

data class QuranAudioFile(
    val verseKey: String,
    val url: String
)

// ==================== DTO -> Domain Mappers ====================

fun ChapterDto.toDomain(): QuranChapter = QuranChapter(
    id = id,
    revelationPlace = revelationPlace,
    revelationOrder = revelationOrder,
    bismillahPre = bismillahPre,
    nameSimple = nameSimple,
    nameComplex = nameComplex,
    nameArabic = nameArabic,
    versesCount = versesCount,
    pages = pages,
    translatedName = translatedName?.name ?: nameSimple
)

fun VerseDto.toDomain(): QuranVerse = QuranVerse(
    id = id,
    chapterId = chapterId ?: 0,
    verseNumber = verseNumber,
    verseKey = verseKey,
    textUthmani = textUthmani ?: textImlaei ?: "",
    juzNumber = juzNumber,
    hizbNumber = hizbNumber,
    pageNumber = pageNumber,
    translationText = translations?.firstOrNull()?.text ?: "",
    translationResourceName = translations?.firstOrNull()?.resourceName ?: ""
)

fun JuzDto.toDomain(): QuranJuz = QuranJuz(
    id = id,
    juzNumber = juzNumber,
    verseMapping = verseMapping ?: emptyMap(),
    versesCount = versesCount ?: 0
)

fun SearchResultDto.toDomain(): QuranSearchResult = QuranSearchResult(
    verseKey = verseKey,
    verseId = verseId ?: 0,
    text = text ?: "",
    highlighted = highlighted ?: text ?: ""
)

fun ReciterDto.toDomain(): QuranReciter = QuranReciter(
    id = id,
    name = translatedName?.name ?: reciterName,
    style = style
)

fun VerseAudioFileDto.toDomain(): QuranAudioFile = QuranAudioFile(
    verseKey = verseKey ?: "",
    url = url?.let { rawUrl ->
        if (rawUrl.startsWith("http")) rawUrl else "https://verses.quran.com/$rawUrl"
    } ?: ""
)
