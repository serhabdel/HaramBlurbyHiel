package com.hieltech.haramblur.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for false positive reports operations
 */
@Dao
interface FalsePositiveDao {
    
    @Query("SELECT * FROM false_positives ORDER BY reported_at DESC")
    suspend fun getAllReports(): List<FalsePositiveEntity>
    
    @Query("SELECT * FROM false_positives ORDER BY reported_at DESC")
    fun getAllReportsFlow(): Flow<List<FalsePositiveEntity>>
    
    @Query("SELECT * FROM false_positives WHERE status = :status ORDER BY reported_at DESC")
    suspend fun getReportsByStatus(status: FalsePositiveStatus): List<FalsePositiveEntity>
    
    @Query("SELECT * FROM false_positives WHERE url_hash = :urlHash")
    suspend fun getReportsByUrlHash(urlHash: String): List<FalsePositiveEntity>
    
    @Query("SELECT * FROM false_positives WHERE id = :id")
    suspend fun getReportById(id: Long): FalsePositiveEntity?
    
    @Query("SELECT COUNT(*) FROM false_positives WHERE status = :status")
    suspend fun getReportCountByStatus(status: FalsePositiveStatus): Int
    
    @Query("SELECT COUNT(*) FROM false_positives WHERE url_hash = :urlHash")
    suspend fun getReportCountForUrl(urlHash: String): Int
    
    @Query("""
        SELECT COUNT(*) FROM false_positives 
        WHERE reported_at >= :startTime AND reported_at <= :endTime
    """)
    suspend fun getReportCountInTimeRange(startTime: Long, endTime: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: FalsePositiveEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<FalsePositiveEntity>): List<Long>
    
    @Update
    suspend fun updateReport(report: FalsePositiveEntity)
    
    @Delete
    suspend fun deleteReport(report: FalsePositiveEntity)
    
    @Query("DELETE FROM false_positives WHERE id = :id")
    suspend fun deleteReportById(id: Long)
    
    @Query("UPDATE false_positives SET status = :status WHERE id = :id")
    suspend fun updateReportStatus(id: Long, status: FalsePositiveStatus)
    
    @Query("""
        UPDATE false_positives 
        SET status = :status, resolved_at = :resolvedAt, resolution_notes = :notes 
        WHERE id = :id
    """)
    suspend fun resolveReport(id: Long, status: FalsePositiveStatus, resolvedAt: Long, notes: String?)
    
    @Query("DELETE FROM false_positives WHERE reported_at < :timestamp")
    suspend fun deleteOldReports(timestamp: Long)
    
    @Query("""
        SELECT * FROM false_positives 
        WHERE status = 'PENDING' AND reported_at >= :since 
        ORDER BY reported_at DESC 
        LIMIT :limit
    """)
    suspend fun getRecentPendingReports(since: Long, limit: Int): List<FalsePositiveEntity>
    
    @Query("""
        SELECT url_hash, COUNT(*) as report_count 
        FROM false_positives 
        WHERE reported_at >= :since 
        GROUP BY url_hash 
        HAVING report_count >= :minReports 
        ORDER BY report_count DESC
    """)
    suspend fun getFrequentlyReportedUrls(since: Long, minReports: Int): List<UrlReportCount>
    
    @Query("""
        SELECT status, COUNT(*) as count 
        FROM false_positives 
        GROUP BY status
    """)
    suspend fun getStatusCounts(): List<StatusCount>
}

/**
 * Data class for URL report count results
 */
data class UrlReportCount(
    @ColumnInfo(name = "url_hash") val urlHash: String,
    @ColumnInfo(name = "report_count") val reportCount: Int
)

/**
 * Data class for status count results
 */
data class StatusCount(
    val status: FalsePositiveStatus,
    val count: Int
)