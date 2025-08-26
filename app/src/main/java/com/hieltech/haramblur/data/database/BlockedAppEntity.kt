package com.hieltech.haramblur.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a blocked app in the database
 */
@Entity(
    tableName = "blocked_apps",
    indices = [
        Index(value = ["package_name"], unique = true),
        Index(value = ["is_blocked"]),
        Index(value = ["block_type"])
    ]
)
data class BlockedAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "is_blocked")
    val isBlocked: Boolean = false,

    @ColumnInfo(name = "block_type")
    val blockType: String = "simple", // 'simple', 'time_based', 'scheduled'

    @ColumnInfo(name = "icon_path")
    val iconPath: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null, // 'social', 'entertainment', 'gaming', etc.

    @ColumnInfo(name = "is_system_app")
    val isSystemApp: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "last_blocked_at")
    val lastBlockedAt: Long? = null,

    @ColumnInfo(name = "block_count")
    val blockCount: Int = 0
)
