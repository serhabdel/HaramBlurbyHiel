package com.hieltech.haramblur.data.database

import androidx.room.*
import com.hieltech.haramblur.detection.BlockingCategory
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Quranic verses operations
 */
@Dao
interface QuranicVerseDao {
    
    @Query("SELECT * FROM quranic_verses WHERE is_active = 1")
    suspend fun getAllActiveVerses(): List<QuranicVerseEntity>
    
    @Query("SELECT * FROM quranic_verses WHERE is_active = 1")
    fun getAllActiveVersesFlow(): Flow<List<QuranicVerseEntity>>
    
    @Query("SELECT * FROM quranic_verses WHERE category = :category AND is_active = 1")
    suspend fun getVersesByCategory(category: BlockingCategory): List<QuranicVerseEntity>
    
    @Query("""
        SELECT * FROM quranic_verses 
        WHERE category = :category AND is_active = 1 
        ORDER BY priority DESC, RANDOM() 
        LIMIT 1
    """)
    suspend fun getRandomVerseByCategory(category: BlockingCategory): QuranicVerseEntity?
    
    @Query("SELECT * FROM quranic_verses WHERE id = :id")
    suspend fun getVerseById(id: String): QuranicVerseEntity?
    
    @Query("SELECT * FROM quranic_verses WHERE surah_number = :surahNumber AND verse_number = :verseNumber")
    suspend fun getVerseByReference(surahNumber: Int, verseNumber: Int): QuranicVerseEntity?
    
    @Query("SELECT * FROM quranic_verses WHERE surah_name = :surahName AND is_active = 1")
    suspend fun getVersesBySurah(surahName: String): List<QuranicVerseEntity>
    
    @Query("""
        SELECT * FROM quranic_verses 
        WHERE is_active = 1 
        ORDER BY RANDOM() 
        LIMIT 1
    """)
    suspend fun getRandomVerse(): QuranicVerseEntity?
    
    @Query("SELECT COUNT(*) FROM quranic_verses WHERE is_active = 1")
    suspend fun getActiveVerseCount(): Int
    
    @Query("SELECT COUNT(*) FROM quranic_verses WHERE category = :category AND is_active = 1")
    suspend fun getVerseCountByCategory(category: BlockingCategory): Int
    
    @Query("SELECT DISTINCT surah_name FROM quranic_verses WHERE is_active = 1 ORDER BY surah_number")
    suspend fun getAllSurahNames(): List<String>
    
    @Query("SELECT DISTINCT category FROM quranic_verses WHERE is_active = 1")
    suspend fun getAllCategories(): List<BlockingCategory>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerse(verse: QuranicVerseEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<QuranicVerseEntity>): List<Long>
    
    @Update
    suspend fun updateVerse(verse: QuranicVerseEntity)
    
    @Delete
    suspend fun deleteVerse(verse: QuranicVerseEntity)
    
    @Query("DELETE FROM quranic_verses WHERE id = :id")
    suspend fun deleteVerseById(id: String)
    
    @Query("UPDATE quranic_verses SET is_active = 0 WHERE id = :id")
    suspend fun deactivateVerse(id: String)
    
    @Query("UPDATE quranic_verses SET is_active = 1 WHERE id = :id")
    suspend fun activateVerse(id: String)
    
    @Query("UPDATE quranic_verses SET updated_at = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: String, timestamp: Long)
    
    @Query("""
        SELECT * FROM quranic_verses 
        WHERE (arabic_text LIKE :searchTerm 
               OR english_translation LIKE :searchTerm 
               OR urdu_translation LIKE :searchTerm 
               OR transliteration LIKE :searchTerm 
               OR surah_name LIKE :searchTerm) 
        AND is_active = 1
    """)
    suspend fun searchVerses(searchTerm: String): List<QuranicVerseEntity>
    
    @Query("""
        SELECT * FROM quranic_verses 
        WHERE category = :category AND is_active = 1 
        ORDER BY priority DESC, surah_number ASC, verse_number ASC
    """)
    suspend fun getVersesByCategoryOrdered(category: BlockingCategory): List<QuranicVerseEntity>
    
    @Query("""
        SELECT DISTINCT category, COUNT(*) as count 
        FROM quranic_verses 
        WHERE is_active = 1 
        GROUP BY category
    """)
    suspend fun getCategoryCounts(): List<VerseCategoryCount>
}

/**
 * Data class for verse category count results
 */
data class VerseCategoryCount(
    val category: BlockingCategory,
    val count: Int
)