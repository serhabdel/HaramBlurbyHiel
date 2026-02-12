package com.hieltech.haramblur.detection

import com.hieltech.haramblur.data.QuranicVerse

/**
 * Interface for site blocking functionality
 */
interface SiteBlockingManager {
    suspend fun checkUrl(url: String): SiteBlockingResult
    suspend fun getQuranicVerse(category: BlockingCategory): QuranicVerse?
    suspend fun updateBlockingDatabase(): Boolean
    suspend fun reportFalsePositive(url: String, reason: String): Boolean
    suspend fun isUrlBlocked(url: String): Boolean
    suspend fun getBlockingCategory(url: String): BlockingCategory?
    suspend fun addCustomBlockedSite(url: String, category: BlockingCategory): Boolean
    suspend fun removeBlockedSite(url: String): Boolean
}

/**
 * Result of site blocking check
 */
data class SiteBlockingResult(
    val isBlocked: Boolean,
    val category: BlockingCategory?,
    val confidence: Float,
    val quranicVerse: QuranicVerse?,
    val reflectionTimeSeconds: Int,
    val matchedPattern: String? = null,
    val blockingReason: String? = null
)