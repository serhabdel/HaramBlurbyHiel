package com.hieltech.haramblur.services

import com.hieltech.haramblur.data.models.AppCategory
import com.hieltech.haramblur.detection.BlockingCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps app categories to blocking categories for appropriate Quranic verse selection
 */
@Singleton
class AppCategoryMapper @Inject constructor() {

    /**
     * Map app category to appropriate blocking category for verse selection
     */
    fun mapToBlockingCategory(appCategory: AppCategory): BlockingCategory {
        return when (appCategory) {
            AppCategory.SOCIAL_MEDIA -> BlockingCategory.EXPLICIT_CONTENT
            AppCategory.DATING -> BlockingCategory.DATING_SITES
            AppCategory.ENTERTAINMENT -> BlockingCategory.INAPPROPRIATE_IMAGERY
            AppCategory.BROWSERS -> BlockingCategory.SUSPICIOUS_CONTENT
            AppCategory.MESSAGING -> BlockingCategory.SUSPICIOUS_CONTENT
        }
    }

    /**
     * Map nullable app category to appropriate blocking category with sensible default
     */
    fun mapToBlockingCategoryOrDefault(appCategory: AppCategory?): BlockingCategory {
        return appCategory?.let { mapToBlockingCategory(it) } ?: BlockingCategory.SUSPICIOUS_CONTENT
    }

    /**
     * Get contextual guidance category based on app usage pattern
     */
    fun getContextualCategory(appCategory: AppCategory, isRepeatedOffense: Boolean): BlockingCategory {
        return if (isRepeatedOffense) {
            // For repeated offenses, use more specific guidance
            when (appCategory) {
                AppCategory.SOCIAL_MEDIA -> BlockingCategory.EXPLICIT_CONTENT
                AppCategory.DATING -> BlockingCategory.DATING_SITES
                AppCategory.ENTERTAINMENT -> BlockingCategory.ADULT_ENTERTAINMENT
                AppCategory.BROWSERS -> BlockingCategory.EXPLICIT_CONTENT
                AppCategory.MESSAGING -> BlockingCategory.INAPPROPRIATE_IMAGERY
            }
        } else {
            mapToBlockingCategory(appCategory)
        }
    }

    /**
     * Get appropriate guidance message for app category
     */
    fun getGuidanceMessage(appCategory: AppCategory, timeUsed: Int, timeLimit: Int): String {
        val overageMinutes = timeUsed - timeLimit
        val overageText = if (overageMinutes > 60) {
            "${overageMinutes / 60}h ${overageMinutes % 60}m"
        } else {
            "${overageMinutes}m"
        }

        return when (appCategory) {
            AppCategory.SOCIAL_MEDIA ->
                "You have exceeded your social media time limit by $overageText. " +
                "Remember that excessive social media can distance the heart from Allah's remembrance. " +
                "Consider using this time for dhikr, Quran recitation, or beneficial activities."

            AppCategory.DATING ->
                "You have exceeded your time limit by $overageText. " +
                "Islam encourages seeking marriage through halal means with family involvement. " +
                "Consider focusing on self-improvement and seeking Allah's guidance in finding a righteous spouse."

            AppCategory.ENTERTAINMENT ->
                "You have exceeded your entertainment time limit by $overageText. " +
                "While halal entertainment is permissible, moderation is key in Islam. " +
                "Balance your time between worldly activities and acts of worship."

            AppCategory.BROWSERS ->
                "You have exceeded your browsing time limit by $overageText. " +
                "Use the internet wisely for beneficial knowledge and avoid content that may lead to sin. " +
                "Seek knowledge that brings you closer to Allah."

            AppCategory.MESSAGING ->
                "You have exceeded your messaging time limit by $overageText. " +
                "While maintaining relationships is important in Islam, ensure your conversations are beneficial and appropriate. " +
                "Remember to balance digital communication with real-world connections and worship."
        }
    }

    /**
     * Get recommended actions for app category
     */
    fun getRecommendedActions(appCategory: AppCategory): List<String> {
        return when (appCategory) {
            AppCategory.SOCIAL_MEDIA -> listOf(
                "Recite 'A'udhu billahi min ash-shaytani'r-rajim'",
                "Make du'a for protection from time-wasting activities",
                "Engage in dhikr for the next 10 minutes",
                "Read a page from the Quran",
                "Perform wudu and pray 2 rakah nafl",
                "Call a family member or friend"
            )

            AppCategory.DATING -> listOf(
                "Make du'a for a righteous spouse",
                "Consult with family about marriage",
                "Focus on self-improvement and Islamic knowledge",
                "Engage in community activities",
                "Seek guidance from Islamic counselors",
                "Practice patience and trust in Allah's timing"
            )

            AppCategory.ENTERTAINMENT -> listOf(
                "Listen to Quran recitation",
                "Watch Islamic lectures or documentaries",
                "Engage in physical exercise",
                "Spend time with family",
                "Practice a beneficial skill",
                "Help with household tasks"
            )

            AppCategory.BROWSERS -> listOf(
                "Seek beneficial Islamic knowledge online",
                "Read Islamic articles or books",
                "Learn about Islamic history",
                "Research topics that benefit your dunya and akhirah",
                "Close unnecessary tabs and focus",
                "Set specific goals for internet usage"
            )

            AppCategory.MESSAGING -> listOf(
                "Send Islamic reminders to friends",
                "Share beneficial knowledge",
                "Check on elderly family members",
                "Coordinate beneficial activities",
                "Limit conversations to necessary topics",
                "Use time for face-to-face interactions"
            )
        }
    }

    /**
     * Get time-of-day specific guidance
     */
    fun getTimeSpecificGuidance(appCategory: AppCategory, hour: Int): String? {
        return when (hour) {
            in 5..11 -> "The morning is a blessed time for productivity and worship. Consider starting your day with Quran and beneficial activities."
            in 12..14 -> "This is a time for Dhuhr prayer and midday reflection. Take a break from screens and remember Allah."
            in 15..17 -> "The afternoon is good for productive work and learning. Use your time wisely for beneficial activities."
            in 18..20 -> "Evening is approaching - time for Maghrib prayer and family time. Consider reducing screen time."
            in 21..23 -> "The night is for rest and reflection. Prepare for sleep with dhikr and avoid stimulating content."
            else -> null
        }
    }

    /**
     * Get category-specific Islamic wisdom quotes
     */
    fun getIslamicWisdom(appCategory: AppCategory): List<String> {
        val commonWisdom = listOf(
            "\"By time, indeed, mankind is in loss.\" (Quran 103:1-2)",
            "\"And it is He who has made the night and day in succession for whoever desires to remember or desires gratitude.\" (Quran 25:62)",
            "\"And when you have completed the prayer, remember Allah standing, sitting, or [lying] on your sides.\" (Quran 4:103)"
        )

        val categoryWisdom = when (appCategory) {
            AppCategory.SOCIAL_MEDIA -> listOf(
                "\"O you who have believed, fear Allah and speak words of appropriate justice.\" (Quran 33:70)",
                "\"And do not pursue that of which you have no knowledge.\" (Quran 17:36)"
            )

            AppCategory.DATING -> listOf(
                "\"And marry the unmarried among you and the righteous among your male slaves and female slaves.\" (Quran 24:32)",
                "\"So marry them with the permission of their people and give them their due compensation according to what is acceptable.\" (Quran 4:25)"
            )

            AppCategory.ENTERTAINMENT -> listOf(
                "\"Indeed, the worldly life is only amusement and diversion.\" (Quran 29:64)",
                "\"But the Hereafter is better for those who fear Allah.\" (Quran 29:64)"
            )

            AppCategory.BROWSERS -> listOf(
                "\"Read! And your Lord is the Most Generous.\" (Quran 96:3)",
                "\"Say, 'Are those who know equal to those who do not know?'\" (Quran 39:9)"
            )

            AppCategory.MESSAGING -> listOf(
                "\"And speak to people good [words].\" (Quran 2:83)",
                "\"And let there be [arising] from you a nation inviting to [all that is] good.\" (Quran 3:104)"
            )
        }

        return categoryWisdom + commonWisdom
    }

    /**
     * Get dua recommendations for different situations
     */
    fun getDuaRecommendations(appCategory: AppCategory, situation: String): List<String> {
        val generalDuas = listOf(
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعِلْمَ النَّافِعَ وَالرِّزْقَ الطَّيِّبَ وَالْعَمَلَ الْمُتَقَبَّلَ",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عِلْمٍ لَا يَنْفَعُ وَقَلْبٍ لَا يَخْشَعُ وَدُعَاءٍ لَا يُسْمَعُ وَنَفْسٍ لَا تَشْبَعُ"
        )

        return when (situation) {
            "time_wasting" -> when (appCategory) {
                AppCategory.SOCIAL_MEDIA -> listOf(
                    "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَدْيِ مِنْ بَعْدِ الْهِدَايَةِ",
                    "اللَّهُمَّ أَصْلِحْ لِي دِينِي الَّذِي هُوَ عِصْمَةُ أَمْرِي"
                )
                else -> generalDuas
            }

            "reflection" -> listOf(
                "اللَّهُمَّ أَرِنَا الْحَقَّ حَقًّا وَارْزُقْنَا اتِّبَاعَهُ وَأَرِنَا الْبَاطِلَ بَاطِلًا وَارْزُقْنَا اجْتِنَابَهُ",
                "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ خَلَقْتَنِي وَأَنَا عَبْدُكَ وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ"
            )

            else -> generalDuas
        }
    }

    /**
     * Get reflection questions based on app category and usage pattern
     */
    fun getReflectionQuestions(appCategory: AppCategory, isRepeated: Boolean): List<String> {
        val commonQuestions = listOf(
            "How does this activity align with my Islamic values?",
            "What could I do instead that would be more beneficial?",
            "How will this activity affect my relationship with Allah?"
        )

        val categoryQuestions = when (appCategory) {
            AppCategory.SOCIAL_MEDIA -> if (isRepeated) {
                listOf(
                    "Why do I keep returning to social media despite knowing the harm?",
                    "What am I really seeking through these platforms?",
                    "How can I build real relationships instead of virtual ones?"
                )
            } else {
                listOf(
                    "What benefit am I gaining from this social media time?",
                    "Could this time be better spent on Islamic knowledge or worship?",
                    "How does social media affect my spiritual state?"
                )
            }

            AppCategory.DATING -> listOf(
                "Am I seeking marriage through halal means?",
                "How does this activity align with Islamic guidelines on relationships?",
                "What qualities am I looking for in a spouse according to Islamic teachings?"
            )

            AppCategory.ENTERTAINMENT -> listOf(
                "Is this entertainment halal and beneficial?",
                "How does this activity affect my worship and remembrance of Allah?",
                "Could I replace this with more spiritually enriching entertainment?"
            )

            AppCategory.BROWSERS -> listOf(
                "Am I seeking beneficial knowledge or just wasting time?",
                "How does my internet usage affect my character and behavior?",
                "What Islamic knowledge could I seek instead?"
            )

            AppCategory.MESSAGING -> listOf(
                "Are my conversations beneficial and appropriate?",
                "How do my digital interactions affect my real-world relationships?",
                "Could I use messaging for dawah and spreading good?"
            )
        }

        return categoryQuestions + commonQuestions
    }

    /**
     * Get encouragement messages for positive behavior change
     */
    fun getEncouragementMessage(appCategory: AppCategory, improvement: String): String {
        return when (improvement) {
            "reduced_usage" -> "Excellent! Reducing your ${appCategory.name.lowercase()} usage shows wisdom and self-discipline. May Allah reward you abundantly."

            "increased_worship" -> "Beautiful! Increasing your worship time demonstrates true success in this life and the next."

            "beneficial_activities" -> "Wonderful! Engaging in beneficial activities instead shows your priority for what truly matters."

            else -> "Alhamdulillah! Your positive changes are a sign of Allah's guidance in your life."
        }
    }
}
