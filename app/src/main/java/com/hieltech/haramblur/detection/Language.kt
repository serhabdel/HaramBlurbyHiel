package com.hieltech.haramblur.detection

/**
 * Supported languages for Islamic guidance and Quranic verses
 */
enum class Language(
    val displayName: String,
    val code: String,
    val isRTL: Boolean = false
) {
    ENGLISH("English", "en"),
    ARABIC("العربية", "ar", isRTL = true),
    URDU("اردو", "ur", isRTL = true),
    FRENCH("Français", "fr"),
    INDONESIAN("Bahasa Indonesia", "id"),
    TURKISH("Türkçe", "tr"),
    MALAY("Bahasa Melayu", "ms"),
    BENGALI("বাংলা", "bn"),
    PERSIAN("فارسی", "fa", isRTL = true),
    SPANISH("Español", "es")
}