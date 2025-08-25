package com.hieltech.haramblur.data

import com.hieltech.haramblur.data.database.DatabaseInitializer
import com.hieltech.haramblur.data.database.SiteBlockingDatabase
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Quranic verses and Islamic guidance
 */
@Singleton
class QuranicRepository @Inject constructor(
    private val database: SiteBlockingDatabase,
    private val databaseInitializer: DatabaseInitializer
) {
    
    // Initialize database on first access
    private var isInitialized = false
    
    private suspend fun ensureInitialized() {
        if (!isInitialized) {
            databaseInitializer.initializeDatabase()
            isInitialized = true
        }
    }
    

    
    /**
     * Get a Quranic verse for a specific blocking category
     */
    suspend fun getVerseForCategory(category: BlockingCategory): QuranicVerse? = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val verseEntity = database.quranicVerseDao().getRandomVerseByCategory(category)
            verseEntity?.let { convertEntityToVerse(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get Islamic guidance for a specific category
     */
    suspend fun getGuidanceForCategory(category: BlockingCategory): IslamicGuidance? = withContext(Dispatchers.IO) {
        try {
            val verse = getVerseForCategory(category)
            if (verse != null) {
                IslamicGuidance(
                    verse = verse,
                    guidance = getGuidanceText(category),
                    actionRecommendations = getActionRecommendations(category),
                    duaText = getDuaForCategory(category),
                    additionalResources = getAdditionalResources(category)
                )
            } else {
                getDefaultGuidance(category)
            }
        } catch (e: Exception) {
            getDefaultGuidance(category)
        }
    }
    
    /**
     * Get all available verses for a category
     */
    suspend fun getAllVersesForCategory(category: BlockingCategory): List<QuranicVerse> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val verseEntities = database.quranicVerseDao().getVersesByCategoryOrdered(category)
            verseEntities.map { convertEntityToVerse(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get a random verse from any category
     */
    suspend fun getRandomVerse(): QuranicVerse = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val verseEntity = database.quranicVerseDao().getRandomVerse()
            verseEntity?.let { convertEntityToVerse(it) } ?: getDefaultVerse()
        } catch (e: Exception) {
            getDefaultVerse()
        }
    }
    
    /**
     * Check if a verse has translation in the specified language
     */
    fun hasTranslation(verse: QuranicVerse, language: Language): Boolean {
        return verse.translations.containsKey(language)
    }
    
    /**
     * Get verse by ID
     */
    suspend fun getVerseById(id: String): QuranicVerse? = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val verseEntity = database.quranicVerseDao().getVerseById(id)
            verseEntity?.let { convertEntityToVerse(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Search verses by text
     */
    suspend fun searchVerses(searchTerm: String): List<QuranicVerse> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val verseEntities = database.quranicVerseDao().searchVerses("%$searchTerm%")
            verseEntities.map { convertEntityToVerse(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get all verses as Flow for reactive updates
     */
    fun getAllVersesFlow(): Flow<List<QuranicVerse>> {
        return database.quranicVerseDao().getAllActiveVersesFlow()
            .map { entities -> entities.map { convertEntityToVerse(it) } }
    }
    
    /**
     * Get verses by category as Flow
     */
    fun getVersesByCategoryFlow(category: BlockingCategory): Flow<List<QuranicVerse>> {
        return database.quranicVerseDao().getAllActiveVersesFlow()
            .map { entities -> 
                entities.filter { it.category == category }
                    .map { convertEntityToVerse(it) }
            }
    }
    
    /**
     * Get all available categories
     */
    suspend fun getAllCategories(): List<BlockingCategory> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            database.quranicVerseDao().getAllCategories()
        } catch (e: Exception) {
            BlockingCategory.values().toList()
        }
    }
    
    /**
     * Get verse count by category
     */
    suspend fun getVerseCountByCategory(category: BlockingCategory): Int = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            database.quranicVerseDao().getVerseCountByCategory(category)
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Validate Islamic content for theological correctness
     */
    suspend fun validateIslamicContent(verse: QuranicVerse): ContentValidationResult = withContext(Dispatchers.IO) {
        val validationErrors = mutableListOf<String>()
        val validationWarnings = mutableListOf<String>()
        
        // Basic validation checks
        if (verse.arabicText.isBlank()) {
            validationErrors.add("Arabic text is required")
        }
        
        if (verse.surahNumber < 1 || verse.surahNumber > 114) {
            validationErrors.add("Invalid Surah number: ${verse.surahNumber}")
        }
        
        if (verse.verseNumber < 1) {
            validationErrors.add("Invalid verse number: ${verse.verseNumber}")
        }
        
        if (!verse.translations.containsKey(Language.ENGLISH)) {
            validationWarnings.add("English translation is recommended")
        }
        
        if (verse.context.isBlank()) {
            validationWarnings.add("Context explanation is recommended")
        }
        
        if (verse.reflection.isBlank()) {
            validationWarnings.add("Reflection guidance is recommended")
        }
        
        // Check for common transliteration patterns
        if (verse.transliteration.isNotBlank() && !verse.transliteration.matches(Regex("^[a-zA-Z\\s'-]+$"))) {
            validationWarnings.add("Transliteration contains unusual characters")
        }
        
        ContentValidationResult(
            isValid = validationErrors.isEmpty(),
            errors = validationErrors,
            warnings = validationWarnings,
            suggestions = generateContentSuggestions(verse)
        )
    }
    
    /**
     * Generate contextual guidance based on blocking scenario
     */
    suspend fun generateContextualGuidance(
        category: BlockingCategory,
        userLanguage: Language,
        timeOfDay: String? = null,
        isRepeatedOffense: Boolean = false
    ): IslamicGuidance? = withContext(Dispatchers.IO) {
        try {
            val verse = getVerseForCategory(category)
            if (verse != null) {
                val contextualGuidance = buildContextualGuidance(category, timeOfDay, isRepeatedOffense)
                val contextualRecommendations = buildContextualRecommendations(category, isRepeatedOffense)
                
                IslamicGuidance(
                    verse = verse,
                    guidance = contextualGuidance,
                    actionRecommendations = contextualRecommendations,
                    duaText = getDuaForCategory(category),
                    additionalResources = getAdditionalResources(category)
                )
            } else {
                getDefaultGuidance(category)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get available languages for a verse
     */
    fun getAvailableLanguages(verse: QuranicVerse): List<Language> {
        return verse.translations.keys.toList()
    }
    
    /**
     * Convert database entity to domain model
     */
    private fun convertEntityToVerse(entity: com.hieltech.haramblur.data.database.QuranicVerseEntity): QuranicVerse {
        val translations = mutableMapOf<Language, String>()
        translations[Language.ENGLISH] = entity.englishTranslation
        entity.arabicTranslation?.let { translations[Language.ARABIC] = it }
        entity.urduTranslation?.let { translations[Language.URDU] = it }
        entity.frenchTranslation?.let { translations[Language.FRENCH] = it }
        entity.indonesianTranslation?.let { translations[Language.INDONESIAN] = it }
        
        return QuranicVerse(
            id = entity.id,
            surahName = entity.surahName,
            surahNumber = entity.surahNumber,
            verseNumber = entity.verseNumber,
            arabicText = entity.arabicText,
            transliteration = entity.transliteration,
            translations = translations,
            category = entity.category,
            context = entity.context,
            reflection = entity.reflection,
            audioUrl = entity.audioUrl
        )
    }
    
    private fun getDefaultVerse(): QuranicVerse {
        return QuranicVerse(
            id = "2_286",
            surahName = "Al-Baqarah",
            surahNumber = 2,
            verseNumber = 286,
            arabicText = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
            transliteration = "La yukallifu Allahu nafsan illa wus'aha",
            translations = mapOf(
                Language.ENGLISH to "Allah does not burden a soul beyond that it can bear.",
                Language.ARABIC to "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
                Language.URDU to "اللہ کسی جان پر اس کی طاقت سے زیادہ بوجھ نہیں ڈالتا۔"
            ),
            category = BlockingCategory.SUSPICIOUS_CONTENT,
            context = "Allah's mercy and understanding of human limitations.",
            reflection = "Remember that Allah is Most Merciful and helps those who strive to do right."
        )
    }
    
    private fun getDefaultGuidance(category: BlockingCategory): IslamicGuidance {
        return IslamicGuidance(
            verse = getDefaultVerse(),
            guidance = getGuidanceText(category),
            actionRecommendations = getActionRecommendations(category),
            duaText = getDuaForCategory(category),
            additionalResources = getAdditionalResources(category)
        )
    }
    
    private fun getGuidanceText(category: BlockingCategory): String {
        return when (category) {
            BlockingCategory.EXPLICIT_CONTENT -> "Protecting your gaze is a command from Allah that purifies the heart and strengthens your faith."
            BlockingCategory.ADULT_ENTERTAINMENT -> "Avoiding paths that lead to sin is wisdom. Choose content that elevates your soul."
            BlockingCategory.GAMBLING -> "True success comes from Allah's blessings, not from games of chance."
            BlockingCategory.INAPPROPRIATE_IMAGERY -> "Guard your eyes as you would guard your prayers - both are acts of worship."
            else -> "When in doubt, choose the path that brings you closer to Allah."
        }
    }
    
    private fun getActionRecommendations(category: BlockingCategory): List<String> {
        return when (category) {
            BlockingCategory.EXPLICIT_CONTENT, BlockingCategory.ADULT_ENTERTAINMENT -> listOf(
                "Recite 'A'udhu billahi min ash-shaytani'r-rajim'",
                "Make du'a for protection from temptation",
                "Engage in dhikr or Quran recitation",
                "Seek beneficial Islamic content instead"
            )
            BlockingCategory.GAMBLING -> listOf(
                "Remember that Allah is the Provider of all sustenance",
                "Make du'a for halal rizq (lawful provision)",
                "Consider charitable giving instead",
                "Seek knowledge about Islamic finance"
            )
            else -> listOf(
                "Make istighfar (seek forgiveness)",
                "Turn to beneficial activities",
                "Remember Allah's guidance"
            )
        }
    }
    
    private fun getDuaForCategory(category: BlockingCategory): String? {
        return when (category) {
            BlockingCategory.EXPLICIT_CONTENT, BlockingCategory.ADULT_ENTERTAINMENT -> 
                "اللَّهُمَّ طَهِّرْ قَلْبِي وَأَعِنِّي عَلَى غَضِّ بَصَرِي" // O Allah, purify my heart and help me lower my gaze
            BlockingCategory.GAMBLING -> 
                "اللَّهُمَّ اكْفِنِي بِحَلَالِكَ عَنْ حَرَامِكَ" // O Allah, suffice me with what You have made lawful over what You have made unlawful
            else -> 
                "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ" // I seek forgiveness from Allah
        }
    }
    
    private fun getAdditionalResources(category: BlockingCategory): List<String> {
        return when (category) {
            BlockingCategory.EXPLICIT_CONTENT, BlockingCategory.ADULT_ENTERTAINMENT -> listOf(
                "Read about the benefits of lowering the gaze in Islamic literature",
                "Listen to lectures on purification of the soul",
                "Join Islamic study circles focused on self-improvement",
                "Practice regular dhikr and Quran recitation"
            )
            BlockingCategory.GAMBLING -> listOf(
                "Learn about Islamic finance and halal earning",
                "Study the concept of rizq (divine provision) in Islam",
                "Engage in charitable giving (sadaqah) instead",
                "Seek guidance from Islamic financial advisors"
            )
            BlockingCategory.DATING_SITES -> listOf(
                "Learn about Islamic guidelines for marriage",
                "Consult with family and Islamic counselors",
                "Focus on self-improvement and spiritual growth",
                "Engage in community activities and Islamic events"
            )
            else -> listOf(
                "Increase Islamic knowledge through study",
                "Seek guidance from knowledgeable scholars",
                "Practice regular prayer and remembrance of Allah",
                "Engage in beneficial activities for the community"
            )
        }
    }
    
    private fun buildContextualGuidance(
        category: BlockingCategory,
        timeOfDay: String?,
        isRepeatedOffense: Boolean
    ): String {
        val baseGuidance = getGuidanceText(category)
        
        val contextualAddition = when {
            isRepeatedOffense -> " Remember that Allah is Most Forgiving, and each moment is a new opportunity to turn back to Him. Seek His help in overcoming this challenge."
            timeOfDay == "night" -> " The night is a time for reflection and seeking Allah's forgiveness. Use this moment to strengthen your resolve."
            timeOfDay == "morning" -> " Start your day with the remembrance of Allah and set positive intentions for the hours ahead."
            else -> " Take this moment to reflect on your relationship with Allah and choose the path that brings you closer to Him."
        }
        
        return baseGuidance + contextualAddition
    }
    
    private fun buildContextualRecommendations(
        category: BlockingCategory,
        isRepeatedOffense: Boolean
    ): List<String> {
        val baseRecommendations = getActionRecommendations(category)
        
        val additionalRecommendations = if (isRepeatedOffense) {
            listOf(
                "Consider setting up additional accountability measures",
                "Seek support from trusted family members or friends",
                "Increase your daily dhikr and Quran recitation",
                "Make du'a for strength during your five daily prayers"
            )
        } else {
            listOf(
                "Use this as a reminder to strengthen your spiritual practices",
                "Reflect on your daily routine and make positive changes"
            )
        }
        
        return baseRecommendations + additionalRecommendations
    }
    
    private fun generateContentSuggestions(verse: QuranicVerse): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (verse.translations.size < 3) {
            suggestions.add("Consider adding more language translations")
        }
        
        if (verse.context.length < 50) {
            suggestions.add("Expand the context explanation for better understanding")
        }
        
        if (verse.reflection.length < 30) {
            suggestions.add("Provide more detailed reflection guidance")
        }
        
        if (verse.audioUrl == null) {
            suggestions.add("Consider adding audio recitation URL")
        }
        
        return suggestions
    }
}

/**
 * Result of Islamic content validation
 */
data class ContentValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val suggestions: List<String>
)