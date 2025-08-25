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
}

/**
 * Data class for category count results
 */
data class CategoryCount(
    val category: BlockingCategory,
    val count: Int
)