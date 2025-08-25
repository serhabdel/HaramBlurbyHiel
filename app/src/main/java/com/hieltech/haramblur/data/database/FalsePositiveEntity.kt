package com.hieltech.haramblur.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a false positive report for blocked sites
 */
@Entity(
    tableName = "false_positives",
    indices = [
        Index(value = ["url_hash"]),
        Index(value = ["status"]),
        Index(value = ["reported_at"])
    ]
)
data class FalsePositiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "url_hash")
    val urlHash: String,
    
    @ColumnInfo(name = "original_url")
    val originalUrl: String, // Store for debugging, but hash for privacy
    
    @ColumnInfo(name = "reported_at")
    val reportedAt: Long,
    
    @ColumnInfo(name = "reason")
    val reason: String,
    
    @ColumnInfo(name = "status")
    val status: FalsePositiveStatus = FalsePositiveStatus.PENDING,
    
    @ColumnInfo(name = "user_agent")
    val userAgent: String? = null,
    
    @ColumnInfo(name = "app_package")
    val appPackage: String? = null,
    
    @ColumnInfo(name = "resolved_at")
    val resolvedAt: Long? = null,
    
    @ColumnInfo(name = "resolution_notes")
    val resolutionNotes: String? = null
)

/**
 * Status of false positive reports
 */
enum class FalsePositiveStatus {
    PENDING,
    REVIEWED,
    APPROVED,
    REJECTED,
    RESOLVED
}