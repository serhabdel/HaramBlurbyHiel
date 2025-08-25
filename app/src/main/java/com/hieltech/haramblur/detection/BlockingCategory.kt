package com.hieltech.haramblur.detection

/**
 * Categories for site blocking and content filtering
 */
enum class BlockingCategory(
    val displayName: String,
    val severity: Int, // 1-5 scale, 5 being most severe
    val defaultReflectionTime: Int // seconds
) {
    EXPLICIT_CONTENT("Explicit Content", 5, 20),
    ADULT_ENTERTAINMENT("Adult Entertainment", 5, 20),
    INAPPROPRIATE_IMAGERY("Inappropriate Imagery", 4, 15),
    GAMBLING("Gambling", 3, 10),
    SUSPICIOUS_CONTENT("Suspicious Content", 2, 10),
    DATING_SITES("Dating Sites", 3, 15),
    SOCIAL_MEDIA_INAPPROPRIATE("Inappropriate Social Media", 2, 10),
    VIOLENCE("Violence", 4, 15),
    HATE_SPEECH("Hate Speech", 4, 15),
    SUBSTANCE_ABUSE("Substance Abuse", 3, 10)
}