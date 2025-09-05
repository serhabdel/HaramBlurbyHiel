package com.hieltech.haramblur.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.View
import com.hieltech.haramblur.detection.Language
import java.util.Locale

/**
 * Consolidated locale utility for wrapping context, updating locale at runtime, and queries.
 */
object LocaleUtils {

    private const val TAG = "LocaleUtils"

    /**
     * Wrap the context with the specified language. Use in Application.attachBaseContext().
     */
    fun wrap(context: Context, language: Language): Context {
        val locale = mapLanguageToLocale(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            configuration.setLayoutDirection(locale)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            // Return a configuration-wrapped context
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }

    /**
     * Deprecated: Runtime updates should rely on activity recreation and Application.attachBaseContext.
     * Calling this method will NOT update resources on API >= 25 unless the returned context/configuration is used.
     * Prefer: persist the new language, emit a UI event, and recreate the Activity.
     */
    @Deprecated("Rely on recreation + Application.attachBaseContext(locale-wrapped) instead of runtime updates")
    fun updateLocale(context: Context, language: Language) {
        // Intentionally a no-op to avoid misleading side-effects.
        // Locale changes are applied via wrap() in Application.attachBaseContext after process/activity recreation.
        val locale = mapLanguageToLocale(language)
        Locale.setDefault(locale)
        Log.d(TAG, "updateLocale called for ${language.displayName}; no-op. Changes will apply after recreation.")
    }

    /**
     * Return the current Locale from context resources.
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    /**
     * Whether a given language is RTL. Uses Language.isRTL.
     */
    fun isRTL(language: Language): Boolean = language.isRTL

    /**
     * Whether the current context layout direction is RTL, based on configuration.
     */
    fun isRTL(context: Context): Boolean {
        val direction = context.resources.configuration.layoutDirection
        return direction == View.LAYOUT_DIRECTION_RTL
    }

    private fun mapLanguageToLocale(language: Language): Locale {
        return when (language) {
            Language.ENGLISH -> Locale("en")
            Language.ARABIC -> Locale("ar")
            Language.FRENCH -> Locale("fr")
            Language.URDU -> Locale("ur")
            Language.INDONESIAN -> Locale("id")
            Language.TURKISH -> Locale("tr")
            Language.MALAY -> Locale("ms")
            Language.BENGALI -> Locale("bn")
            Language.PERSIAN -> Locale("fa")
            Language.SPANISH -> Locale("es")
        }
    }
}
