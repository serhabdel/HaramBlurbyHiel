package com.hieltech.haramblur.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hieltech.haramblur.detection.BlockingCategory

/**
 * Entity representing a Quranic verse in the database
 */
@Entity(
    tableName = "quranic_verses",
    indices = [
        Index(value = ["surah_number", "verse_number"], unique = true),
        Index(value = ["category"]),
        Index(value = ["surah_name"])
    ]
)
data class QuranicVerseEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "surah_name")
    val surahName: String,
    
    @ColumnInfo(name = "surah_number")
    val surahNumber: Int,
    
    @ColumnInfo(name = "verse_number")
    val verseNumber: Int,
    
    @ColumnInfo(name = "arabic_text")
    val arabicText: String,
    
    @ColumnInfo(name = "transliteration")
    val transliteration: String,
    
    @ColumnInfo(name = "english_translation")
    val englishTranslation: String,
    
    @ColumnInfo(name = "arabic_translation")
    val arabicTranslation: String? = null,
    
    @ColumnInfo(name = "urdu_translation")
    val urduTranslation: String? = null,
    
    @ColumnInfo(name = "french_translation")
    val frenchTranslation: String? = null,
    
    @ColumnInfo(name = "indonesian_translation")
    val indonesianTranslation: String? = null,
    
    @ColumnInfo(name = "category")
    val category: BlockingCategory,
    
    @ColumnInfo(name = "context")
    val context: String,
    
    @ColumnInfo(name = "reflection")
    val reflection: String,
    
    @ColumnInfo(name = "audio_url")
    val audioUrl: String? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "priority")
    val priority: Int = 1, // Higher priority verses shown more often
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)