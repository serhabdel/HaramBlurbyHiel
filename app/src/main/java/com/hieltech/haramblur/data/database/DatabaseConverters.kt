package com.hieltech.haramblur.data.database

import androidx.room.TypeConverter
import com.hieltech.haramblur.detection.BlockingCategory
import com.hieltech.haramblur.detection.Language

/**
 * Type converters for Room database
 */
class DatabaseConverters {
    
    @TypeConverter
    fun fromBlockingCategory(category: BlockingCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toBlockingCategory(categoryName: String): BlockingCategory {
        return try {
            BlockingCategory.valueOf(categoryName)
        } catch (e: IllegalArgumentException) {
            BlockingCategory.SUSPICIOUS_CONTENT // Default fallback
        }
    }
    
    @TypeConverter
    fun fromLanguage(language: Language): String {
        return language.name
    }
    
    @TypeConverter
    fun toLanguage(languageName: String): Language {
        return try {
            Language.valueOf(languageName)
        } catch (e: IllegalArgumentException) {
            Language.ENGLISH // Default fallback
        }
    }
    
    @TypeConverter
    fun fromFalsePositiveStatus(status: FalsePositiveStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toFalsePositiveStatus(statusName: String): FalsePositiveStatus {
        return try {
            FalsePositiveStatus.valueOf(statusName)
        } catch (e: IllegalArgumentException) {
            FalsePositiveStatus.PENDING // Default fallback
        }
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}