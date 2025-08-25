package com.hieltech.haramblur.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hieltech.haramblur.detection.BlockingCategory

/**
 * Entity representing a blocked site in the database
 */
@Entity(
    tableName = "blocked_sites",
    indices = [
        Index(value = ["domain_hash"], unique = true),
        Index(value = ["category"]),
        Index(value = ["confidence"])
    ]
)
data class BlockedSiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "domain_hash")
    val domainHash: String,
    
    @ColumnInfo(name = "pattern")
    val pattern: String,
    
    @ColumnInfo(name = "category")
    val category: BlockingCategory,
    
    @ColumnInfo(name = "confidence")
    val confidence: Float,
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long,
    
    @ColumnInfo(name = "is_regex")
    val isRegex: Boolean = false,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "source")
    val source: String = "default", // default, user_added, community, etc.
    
    @ColumnInfo(name = "description")
    val description: String? = null
)