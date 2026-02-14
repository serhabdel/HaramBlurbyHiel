package com.hieltech.haramblur.detection

import android.content.Context
import androidx.annotation.StringRes
import com.hieltech.haramblur.R

/**
 * Supported languages for Islamic guidance and Quranic verses
 */
enum class Language(
    @StringRes val displayNameResId: Int,
    val code: String,
    val isRTL: Boolean = false
) {
    ENGLISH(R.string.language_name_english, "en"),
    ARABIC(R.string.language_name_arabic, "ar", isRTL = true),
    URDU(R.string.language_name_urdu, "ur", isRTL = true),
    FRENCH(R.string.language_name_french, "fr"),
    INDONESIAN(R.string.language_name_indonesian, "id"),
    TURKISH(R.string.language_name_turkish, "tr"),
    MALAY(R.string.language_name_malay, "ms"),
    BENGALI(R.string.language_name_bengali, "bn"),
    PERSIAN(R.string.language_name_persian, "fa", isRTL = true),
    SPANISH(R.string.language_name_spanish, "es");
    
    companion object {
        fun getByCode(code: String): Language? {
            return values().find { it.code == code }
        }
    }
}

/**
 * Extension function to get localized display name for a Language
 */
fun Language.getDisplayName(context: Context): String {
    return context.getString(this.displayNameResId)
}

/**
 * Extension property to get localized display name (requires Context)
 */
val Language.localizedName: String
    get() = throw UnsupportedOperationException("Use getDisplayName(Context) instead")