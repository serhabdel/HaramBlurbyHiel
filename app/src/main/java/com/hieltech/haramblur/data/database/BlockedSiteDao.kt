package com.hieltech.haramblur.data.database

import androidx.room.*
import com.hieltech.haramblur.detection.BlockingCategory
import kotlinx.coroutines.flow.Flow

/**
 * DAO for blocked sites operations
 */
@Dao
interface BlockedSiteDao {
    
    @Query("SELECT * FROM blocked_sites WHERE is_active = 1")
    suspend fun getAllActiveSites(): List<BlockedSiteEntity>
    
    @Query("SELECT * FROM blocked_sites WHERE is_active = 1")
    fun getAllActiveSitesFlow(): Flow<List<BlockedSiteEntity>>
    
    @Query("SELECT * FROM blocked_sites WHERE category = :category AND is_active = 1")
    suspend fun getSitesByCategory(category: BlockingCategory): List<BlockedSiteEntity>
    
    @Query("SELECT * FROM blocked_sites WHERE domain_hash = :domainHash AND is_active = 1")
    suspend fun getSiteByDomainHash(domainHash: String): BlockedSiteEntity?
    
    @Query("SELECT * FROM blocked_sites WHERE pattern LIKE :pattern AND is_active = 1")
    suspend fun getSitesByPattern(pattern: String): List<BlockedSiteEntity>
    
    @Query("SELECT * FROM blocked_sites WHERE is_regex = 1 AND is_active = 1")
    suspend fun getRegexSites(): List<BlockedSiteEntity>
    
    @Query("SELECT COUNT(*) FROM blocked_sites WHERE is_active = 1")
    suspend fun getActiveSiteCount(): Int
    
    @Query("SELECT COUNT(*) FROM blocked_sites WHERE category = :category AND is_active = 1")
    suspend fun getSiteCountByCategory(category: BlockingCategory): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: BlockedSiteEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<BlockedSiteEntity>): List<Long>
    
    @Update
    suspend fun updateSite(site: BlockedSiteEntity)
    
    @Delete
    suspend fun deleteSite(site: BlockedSiteEntity)
    
    @Query("DELETE FROM blocked_sites WHERE domain_hash = :domainHash")
    suspend fun deleteSiteByDomainHash(domainHash: String)
    
    @Query("UPDATE blocked_sites SET is_active = 0 WHERE domain_hash = :domainHash")
    suspend fun deactivateSite(domainHash: String)
    
    @Query("UPDATE blocked_sites SET is_active = 1 WHERE domain_hash = :domainHash")
    suspend fun activateSite(domainHash: String)
    
    @Query("DELETE FROM blocked_sites WHERE last_updated < :timestamp")
    suspend fun deleteOldSites(timestamp: Long)
    
    @Query("UPDATE blocked_sites SET last_updated = :timestamp WHERE domain_hash IN (:domainHashes)")
    suspend fun updateLastUpdated(domainHashes: List<String>, timestamp: Long)
    
    @Query("""
        SELECT * FROM blocked_sites 
        WHERE is_active = 1 AND confidence >= :minConfidence 
        ORDER BY confidence DESC, last_updated DESC
    """)
    suspend fun getHighConfidenceSites(minConfidence: Float): List<BlockedSiteEntity>
    
    @Query("""
        SELECT DISTINCT category, COUNT(*) as count 
        FROM blocked_sites 
        WHERE is_active = 1 
        GROUP BY category
    """)
    suspend fun getCategoryCounts(): List<CategoryCount>
    
    @Query("SELECT * FROM blocked_sites WHERE source = :source AND is_active = 1")
    suspend fun getSitesBySource(source: String): List<BlockedSiteEntity>

    // Custom blocking methods
    @Query("SELECT * FROM blocked_sites WHERE added_by_user = 1 AND is_active = 1 ORDER BY date_added DESC")
    suspend fun getUserAddedSites(): List<BlockedSiteEntity>

    @Query("SELECT * FROM blocked_sites WHERE is_custom = 1 AND is_active = 1")
    suspend fun getCustomSites(): List<BlockedSiteEntity>

    @Query("SELECT COUNT(*) FROM blocked_sites WHERE added_by_user = 1 AND is_active = 1")
    suspend fun getUserAddedSiteCount(): Int

    @Query("INSERT OR REPLACE INTO blocked_sites (domain_hash, pattern, category, confidence, last_updated, is_regex, source, description, is_custom, added_by_user, custom_category, date_added, block_count, is_active) VALUES (:domainHash, :pattern, :category, :confidence, :lastUpdated, :isRegex, :source, :description, 1, 1, :customCategory, :dateAdded, 0, 1)")
    suspend fun insertCustomSite(
        domainHash: String,
        pattern: String,
        category: BlockingCategory,
        confidence: Float,
        lastUpdated: Long,
        isRegex: Boolean = false,
        source: String = "user_added",
        description: String? = null,
        customCategory: String? = null,
        dateAdded: Long
    ): Long

    @Query("UPDATE blocked_sites SET block_count = block_count + 1 WHERE domain_hash = :domainHash")
    suspend fun incrementBlockCount(domainHash: String)

    @Query("SELECT * FROM blocked_sites WHERE pattern LIKE '%' || :query || '%' AND is_active = 1 ORDER BY added_by_user DESC, confidence DESC")
    suspend fun searchSites(query: String): List<BlockedSiteEntity>

    // Flow versions for custom sites
    @Query("SELECT * FROM blocked_sites WHERE added_by_user = 1 AND is_active = 1 ORDER BY date_added DESC")
    fun getUserAddedSitesFlow(): Flow<List<BlockedSiteEntity>>

    @Query("SELECT COUNT(*) FROM blocked_sites WHERE added_by_user = 1 AND is_active = 1")
    fun getUserAddedSiteCountFlow(): Flow<Int>
}

/**
 * Data class for category count results
 */
data class CategoryCount(
    val category: BlockingCategory,
    val count: Int
)