package com.hieltech.haramblur.data

import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.Language

/**
 * Data models for Quranic verses and Islamic guidance
 */

/**
 * Represents a Quranic verse with translations and context
 */
data class QuranicVerse(
    val id: String,
    val surahName: String,
    val surahNumber: Int,
    val verseNumber: Int,
    val arabicText: String,
    val transliteration: String,
    val translations: Map<Language, String>,
    val category: BlockingCategory,
    val context: String,
    val reflection: String,
    val audioUrl: String? = null
)

/**
 * Islamic guidance for specific situations
 */
data class IslamicGuidance(
    val verse: QuranicVerse,
    val guidance: String,
    val actionRecommendations: List<String>,
    val duaText: String? = null,
    val additionalResources: List<String> = emptyList()
)

/**
 * Warning dialog state for full-screen blur scenarios
 */
data class WarningDialogState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val quranicVerse: QuranicVerse? = null,
    val reflectionTimeSeconds: Int = 15,
    val remainingTimeSeconds: Int = 15,
    val canContinue: Boolean = false,
    val showCloseOption: Boolean = true,
    val showContinueOption: Boolean = true,
    val category: BlockingCategory? = null,
    val language: Language = Language.ENGLISH
)

/**
 * Action to take when warning dialog is dismissed
 */
sealed class WarningDialogAction {
    object Close : WarningDialogAction()
    object Continue : WarningDialogAction()
    object Dismiss : WarningDialogAction()
    data class ChangeLanguage(val language: Language) : WarningDialogAction()
}

/**
 * Result of warning dialog interaction
 */
data class WarningDialogResult(
    val action: WarningDialogAction,
    val reflectionTimeCompleted: Boolean,
    val verseRead: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)