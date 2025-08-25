package com.hieltech.haramblur.ui.components

import com.hieltech.haramblur.data.QuranicRepository
import com.hieltech.haramblur.data.SettingsRepository
import com.hieltech.haramblur.data.WarningDialogAction
import com.hieltech.haramblur.data.WarningDialogResult
import com.hieltech.haramblur.detection.BlockingCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling warning dialog creation and lifecycle
 */
@Singleton
class WarningDialogManager @Inject constructor(
    private val quranicRepository: QuranicRepository,
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Create warning dialog for full-screen blur scenario
     */
    suspend fun createFullScreenWarning(
        category: BlockingCategory,
        customMessage: String? = null
    ): Triple<String, String, com.hieltech.haramblur.data.QuranicVerse?> {
        val settings = settingsRepository.getCurrentSettings()
        
        val title = when (category) {
            BlockingCategory.EXPLICIT_CONTENT -> "Inappropriate Content Detected"
            BlockingCategory.ADULT_ENTERTAINMENT -> "Adult Content Blocked"
            BlockingCategory.INAPPROPRIATE_IMAGERY -> "Inappropriate Images Found"
            BlockingCategory.GAMBLING -> "Gambling Content Detected"
            BlockingCategory.SUSPICIOUS_CONTENT -> "Questionable Content Found"
            BlockingCategory.DATING_SITES -> "Dating Site Blocked"
            BlockingCategory.SOCIAL_MEDIA_INAPPROPRIATE -> "Inappropriate Social Content"
            BlockingCategory.VIOLENCE -> "Violent Content Detected"
            BlockingCategory.HATE_SPEECH -> "Harmful Content Blocked"
            BlockingCategory.SUBSTANCE_ABUSE -> "Harmful Content Found"
        }
        
        val message = customMessage ?: when (category) {
            BlockingCategory.EXPLICIT_CONTENT -> 
                "This page contains explicit content that may be harmful to your spiritual well-being. " +
                "Take a moment to reflect on your intentions and seek Allah's guidance."
            
            BlockingCategory.ADULT_ENTERTAINMENT -> 
                "Adult entertainment content has been detected. Remember that protecting your gaze " +
                "is a command from Allah that purifies the heart and strengthens faith."
            
            BlockingCategory.INAPPROPRIATE_IMAGERY -> 
                "Inappropriate images have been found on this page. Consider whether viewing this " +
                "content aligns with your Islamic values and brings you closer to Allah."
            
            BlockingCategory.GAMBLING -> 
                "This content promotes gambling, which is prohibited in Islam. True success and " +
                "provision come from Allah through lawful means."
            
            BlockingCategory.SUSPICIOUS_CONTENT -> 
                "This content may not be suitable for viewing. When in doubt, it's better to " +
                "choose content that benefits your spiritual growth."
            
            BlockingCategory.DATING_SITES -> 
                "This dating site has been blocked. Islam encourages proper channels for marriage " +
                "through family and community involvement."
            
            BlockingCategory.SOCIAL_MEDIA_INAPPROPRIATE -> 
                "Inappropriate social media content detected. Consider whether this content " +
                "benefits your spiritual growth and relationship with Allah."
            
            BlockingCategory.VIOLENCE -> 
                "Violent content has been detected. Islam promotes peace and discourages " +
                "exposure to unnecessary violence."
            
            BlockingCategory.HATE_SPEECH -> 
                "Harmful content containing hate speech has been blocked. Islam teaches us " +
                "to speak with kindness and avoid harmful speech."
            
            BlockingCategory.SUBSTANCE_ABUSE -> 
                "Content promoting substance abuse has been detected. Islam prohibits " +
                "intoxicants and encourages maintaining a clear mind for worship."
        }
        
        val verse = if (settings.enableQuranicGuidance) {
            quranicRepository.getVerseForCategory(category)
        } else null
        
        return Triple(title, message, verse)
    }
    
    /**
     * Create warning dialog for site blocking
     */
    suspend fun createSiteBlockingWarning(
        siteName: String,
        category: BlockingCategory
    ): Triple<String, String, com.hieltech.haramblur.data.QuranicVerse?> {
        val settings = settingsRepository.getCurrentSettings()
        
        val title = "Site Blocked: $siteName"
        
        val message = when (category) {
            BlockingCategory.EXPLICIT_CONTENT, BlockingCategory.ADULT_ENTERTAINMENT -> 
                "This website contains adult content that is harmful to your spiritual well-being. " +
                "Allah has commanded us to lower our gaze and protect our hearts from such content."
            
            BlockingCategory.GAMBLING -> 
                "This gambling website has been blocked as gambling is prohibited in Islam. " +
                "Trust in Allah's provision through lawful means."
            
            BlockingCategory.INAPPROPRIATE_IMAGERY -> 
                "This website contains inappropriate imagery. Consider seeking beneficial content that brings you closer to Allah."
            
            BlockingCategory.SUSPICIOUS_CONTENT -> 
                "This website has been blocked as it may contain content that conflicts with Islamic values. " +
                "Consider seeking beneficial content that brings you closer to Allah."
            
            BlockingCategory.DATING_SITES -> 
                "This dating site has been blocked. Islam encourages proper channels for marriage " +
                "through family and community involvement."
            
            BlockingCategory.SOCIAL_MEDIA_INAPPROPRIATE -> 
                "This social media content has been blocked due to inappropriate material. " +
                "Consider content that benefits your spiritual growth."
            
            BlockingCategory.VIOLENCE -> 
                "This website contains violent content. Islam promotes peace and discourages " +
                "exposure to unnecessary violence."
            
            BlockingCategory.HATE_SPEECH -> 
                "This website contains hate speech. Islam teaches us to speak with kindness " +
                "and avoid harmful speech."
            
            BlockingCategory.SUBSTANCE_ABUSE -> 
                "This website promotes substance abuse. Islam prohibits intoxicants and " +
                "encourages maintaining a clear mind for worship."
        }
        
        val verse = if (settings.enableQuranicGuidance) {
            quranicRepository.getVerseForCategory(category)
        } else null
        
        return Triple(title, message, verse)
    }
    
    /**
     * Handle warning dialog result and determine next action
     */
    fun handleWarningResult(result: WarningDialogResult): WarningAction {
        return when (result.action) {
            is WarningDialogAction.Close -> WarningAction.CloseContent
            is WarningDialogAction.Continue -> {
                if (result.reflectionTimeCompleted) {
                    WarningAction.AllowContent
                } else {
                    WarningAction.WaitForReflection
                }
            }
            is WarningDialogAction.Dismiss -> WarningAction.HideDialog
            is WarningDialogAction.ChangeLanguage -> WarningAction.UpdateLanguage
        }
    }
    
    /**
     * Get reflection time for category
     */
    fun getReflectionTimeForCategory(category: BlockingCategory): Int {
        val settings = settingsRepository.getCurrentSettings()
        return when {
            category.severity >= 5 -> maxOf(category.defaultReflectionTime, settings.mandatoryReflectionTime)
            category.severity >= 4 -> category.defaultReflectionTime
            else -> minOf(category.defaultReflectionTime, settings.mandatoryReflectionTime)
        }
    }
    
    /**
     * Check if warning should be shown based on settings
     */
    fun shouldShowWarning(category: BlockingCategory): Boolean {
        val settings = settingsRepository.getCurrentSettings()
        return settings.fullScreenWarningEnabled && 
               (category.severity >= 3 || settings.enableQuranicGuidance)
    }
    
    /**
     * Get appropriate Islamic guidance for the situation
     */
    fun getIslamicGuidance(category: BlockingCategory): String {
        return when (category) {
            BlockingCategory.EXPLICIT_CONTENT, BlockingCategory.ADULT_ENTERTAINMENT -> 
                "Remember the words of Prophet Muhammad (ﷺ): 'The eyes commit adultery, the hands commit adultery, the feet commit adultery, and then the private parts confirm or deny it.' Protect your gaze to protect your heart."
            
            BlockingCategory.GAMBLING -> 
                "The Prophet (ﷺ) said: 'Whoever says to his companion when gambling, \"Come let us gamble,\" should give charity.' Avoid all forms of gambling and trust in Allah's provision."
            
            BlockingCategory.INAPPROPRIATE_IMAGERY -> 
                "Allah says in the Quran: 'Tell the believing men to lower their gaze and guard their private parts. That is purer for them.' (24:30)"
            
            BlockingCategory.SUSPICIOUS_CONTENT -> 
                "When faced with doubtful matters, remember the hadith: 'Leave what makes you doubt for what does not make you doubt.' Choose the path of certainty in righteousness."
            
            BlockingCategory.DATING_SITES -> 
                "The Prophet (ﷺ) said: 'When someone whose religion and character you are pleased with proposes to you, then marry him.' Seek marriage through proper Islamic channels."
            
            BlockingCategory.SOCIAL_MEDIA_INAPPROPRIATE -> 
                "The Prophet (ﷺ) said: 'Whoever believes in Allah and the Last Day should speak good or remain silent.' Choose content that elevates your soul."
            
            BlockingCategory.VIOLENCE -> 
                "Allah says in the Quran: 'And whoever kills a soul unless for a soul or for corruption in the land - it is as if he had slain mankind entirely.' (5:32) Avoid content that glorifies violence."
            
            BlockingCategory.HATE_SPEECH -> 
                "Allah says: 'O you who believe! Let not some men among you laugh at others; it may be that the latter are better than the former.' (49:11) Avoid content that promotes hatred."
            
            BlockingCategory.SUBSTANCE_ABUSE -> 
                "Allah says in the Quran: 'O you who believe! Intoxicants and gambling are abominations of Satan's handiwork. So avoid them that you may be successful.' (5:90)"
        }
    }
}

/**
 * Actions to take based on warning dialog results
 */
sealed class WarningAction {
    object CloseContent : WarningAction()
    object AllowContent : WarningAction()
    object WaitForReflection : WarningAction()
    object HideDialog : WarningAction()
    object UpdateLanguage : WarningAction()
}