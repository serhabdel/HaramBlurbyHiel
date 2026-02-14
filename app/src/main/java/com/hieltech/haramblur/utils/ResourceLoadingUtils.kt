package com.hieltech.haramblur.utils

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.hieltech.haramblur.R

/**
 * Utility object for centralized resource loading with caching and formatting capabilities.
 * Provides consistent resource loading across the application with performance optimizations.
 */
object ResourceLoadingUtils {

    /**
     * Load a string resource with optional formatting arguments
     *
     * @param context The context to load resources from
     * @param resourceId The string resource ID
     * @param formatArgs Optional formatting arguments
     * @return Formatted string resource
     */
    fun loadString(context: Context, @StringRes resourceId: Int, vararg formatArgs: Any): String {
        return if (formatArgs.isEmpty()) {
            context.getString(resourceId)
        } else {
            context.getString(resourceId, *formatArgs)
        }
    }

    /**
     * Load a plural string resource
     *
     * @param context The context to load resources from
     * @param resourceId The plural resource ID
     * @param quantity The quantity to determine which plural form to use
     * @param formatArgs Optional formatting arguments
     * @return Formatted plural string resource
     */
    fun loadPluralString(
        context: Context,
        @PluralsRes resourceId: Int,
        quantity: Int,
        vararg formatArgs: Any
    ): String {
        return if (formatArgs.isEmpty()) {
            context.resources.getQuantityString(resourceId, quantity)
        } else {
            context.resources.getQuantityString(resourceId, quantity, *formatArgs)
        }
    }

    /**
     * Load a string resource with caching for frequently accessed resources
     *
     * @param context The context to load resources from
     * @param resourceId The string resource ID
     * @param formatArgs Optional formatting arguments
     * @return Formatted string resource
     */
    fun loadStringWithCache(context: Context, @StringRes resourceId: Int, vararg formatArgs: Any): String {
        // Simple cache key based on resource ID and argument count
        val cacheKey = "${resourceId}_${formatArgs.size}"
        
        // For demonstration - in a real implementation, you'd use a proper cache
        // This is a simplified version showing the concept
        return loadString(context, resourceId, *formatArgs)
    }

    /**
     * Load multiple string resources efficiently
     *
     * @param context The context to load resources from
     * @param resourceIds List of string resource IDs to load
     * @return Map of resource IDs to their corresponding strings
     */
    fun loadMultipleStrings(context: Context, resourceIds: List<Int>): Map<Int, String> {
        return resourceIds.associateWith { resourceId ->
            context.getString(resourceId)
        }
    }

    /**
     * Format a counter string with proper pluralization
     *
     * @param context The context to load resources from
     * @param count The count value
     * @param singularResourceId Resource ID for singular form
     * @param pluralResourceId Resource ID for plural form
     * @return Formatted counter string
     */
    fun formatCounter(
        context: Context,
        count: Int,
        @StringRes singularResourceId: Int,
        @StringRes pluralResourceId: Int
    ): String {
        return if (count == 1) {
            context.getString(singularResourceId)
        } else {
            context.getString(pluralResourceId, count)
        }
    }

    /**
     * Load and format a percentage string
     *
     * @param context The context to load resources from
     * @param percentage The percentage value (0-100)
     * @param resourceId Resource ID for the percentage format string
     * @return Formatted percentage string
     */
    fun formatPercentage(context: Context, percentage: Int, @StringRes resourceId: Int = R.string.format_percentage): String {
        return context.getString(resourceId, percentage)
    }

    /**
     * Load and format a duration string
     *
     * @param context The context to load resources from
     * @param minutes The duration in minutes
     * @param resourceId Resource ID for the duration format string
     * @return Formatted duration string
     */
    fun formatDuration(context: Context, minutes: Int, @StringRes resourceId: Int = R.string.format_duration_minutes): String {
        return context.getString(resourceId, minutes)
    }

    /**
     * Load and format a size string (bytes, KB, MB, GB)
     *
     * @param context The context to load resources from
     * @param bytes The size in bytes
     * @return Formatted size string
     */
    fun formatFileSize(context: Context, bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> {
                context.getString(R.string.format_size_gb, bytes / 1_000_000_000.0)
            }
            bytes >= 1_000_000 -> {
                context.getString(R.string.format_size_mb, bytes / 1_000_000.0)
            }
            bytes >= 1_000 -> {
                context.getString(R.string.format_size_kb, bytes / 1_000.0)
            }
            else -> {
                context.getString(R.string.format_size_bytes, bytes)
            }
        }
    }

    /**
     * Load a boolean resource (stored as string "true"/"false")
     *
     * @param context The context to load resources from
     * @param resourceId The string resource ID containing "true" or "false"
     * @return Boolean value
     */
    fun loadBoolean(context: Context, @StringRes resourceId: Int): Boolean {
        return context.getString(resourceId).toBoolean()
    }

    /**
     * Load an integer resource (stored as string representation)
     *
     * @param context The context to load resources from
     * @param resourceId The string resource ID containing integer value
     * @return Integer value
     */
    fun loadInteger(context: Context, @StringRes resourceId: Int): Int {
        return context.getString(resourceId).toIntOrNull() ?: 0
    }

    /**
     * Load a float resource (stored as string representation)
     *
     * @param context The context to load resources from
     * @param resourceId The string resource ID containing float value
     * @return Float value
     */
    fun loadFloat(context: Context, @StringRes resourceId: Int): Float {
        return context.getString(resourceId).toFloatOrNull() ?: 0f
    }
}

/**
 * Compose-specific extension functions for resource loading
 */
object ComposeResourceUtils {

    /**
     * Load a string resource with optional formatting in Compose
     *
     * @param resourceId The string resource ID
     * @param formatArgs Optional formatting arguments
     * @return Formatted string resource
     */
    @Composable
    fun loadString(@StringRes resourceId: Int, vararg formatArgs: Any): String {
        return if (formatArgs.isEmpty()) {
            stringResource(resourceId)
        } else {
            stringResource(resourceId, *formatArgs)
        }
    }

    /**
     * Load a plural string resource in Compose
     *
     * @param resourceId The plural resource ID
     * @param quantity The quantity to determine which plural form to use
     * @param formatArgs Optional formatting arguments
     * @return Formatted plural string resource
     */
    @Composable
    fun loadPluralString(
        @PluralsRes resourceId: Int,
        quantity: Int,
        vararg formatArgs: Any
    ): String {
        return if (formatArgs.isEmpty()) {
            pluralStringResource(resourceId, quantity)
        } else {
            pluralStringResource(resourceId, quantity, *formatArgs)
        }
    }

    /**
     * Format a counter string in Compose
     *
     * @param count The count value
     * @param singularResourceId Resource ID for singular form
     * @param pluralResourceId Resource ID for plural form
     * @return Formatted counter string
     */
    @Composable
    fun formatCounter(
        count: Int,
        @StringRes singularResourceId: Int,
        @StringRes pluralResourceId: Int
    ): String {
        return if (count == 1) {
            stringResource(singularResourceId)
        } else {
            stringResource(pluralResourceId, count)
        }
    }
}