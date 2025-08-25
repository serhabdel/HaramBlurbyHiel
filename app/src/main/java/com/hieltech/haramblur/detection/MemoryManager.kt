package com.hieltech.haramblur.detection

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages memory usage and caching for detection algorithms
 * Provides automatic cache management and memory pressure handling
 */
@Singleton
class MemoryManager @Inject constructor() {
    
    companion object {
        private const val TAG = "MemoryManager"
        private const val DEFAULT_CACHE_SIZE_MB = 50 // 50MB default cache
        private const val MEMORY_PRESSURE_THRESHOLD = 0.8f // 80% memory usage
        private const val CACHE_CLEANUP_INTERVAL_MS = 30000L // 30 seconds
        private const val BITMAP_BYTES_PER_PIXEL = 4 // ARGB_8888
    }
    
    private val memoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Memory tracking
    private val totalAllocatedBytes = AtomicLong(0)
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    
    // Cache management
    private var detectionResultCache: LruCache<String, CachedDetectionResult> = LruCache(50)
    private var bitmapCache: LruCache<String, WeakReference<Bitmap>> = LruCache(20)
    private val activeBitmaps = ConcurrentHashMap<String, BitmapInfo>()
    
    // Memory state
    private val _memoryState = MutableStateFlow(MemoryState.NORMAL)
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()
    
    private val _memoryMetrics = MutableStateFlow(MemoryMetrics.empty())
    val memoryMetrics: StateFlow<MemoryMetrics> = _memoryMetrics.asStateFlow()
    
    private var cleanupJob: Job? = null
    private var maxCacheSizeBytes: Long = DEFAULT_CACHE_SIZE_MB * 1024 * 1024L
    
    init {
        initializeCaches()
        startPeriodicCleanup()
    }
    
    /**
     * Initialize memory caches with appropriate sizes
     */
    private fun initializeCaches() {
        val maxMemory = Runtime.getRuntime().maxMemory()
        val cacheSize = (maxMemory / 8).toInt() // Use 1/8th of available memory for cache
        
        detectionResultCache = object : LruCache<String, CachedDetectionResult>(cacheSize) {
            override fun sizeOf(key: String, value: CachedDetectionResult): Int {
                return value.sizeInBytes
            }
            
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: CachedDetectionResult,
                newValue: CachedDetectionResult?
            ) {
                totalAllocatedBytes.addAndGet(-oldValue.sizeInBytes.toLong())
                Log.d(TAG, "Detection result cache entry removed: $key (${oldValue.sizeInBytes} bytes)")
            }
        }
        
        bitmapCache = object : LruCache<String, WeakReference<Bitmap>>(cacheSize / 4) {
            override fun sizeOf(key: String, value: WeakReference<Bitmap>): Int {
                val bitmap = value.get()
                return bitmap?.let { calculateBitmapSize(it) } ?: 0
            }
            
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: WeakReference<Bitmap>,
                newValue: WeakReference<Bitmap>?
            ) {
                oldValue.get()?.let { bitmap ->
                    val size = calculateBitmapSize(bitmap)
                    totalAllocatedBytes.addAndGet(-size.toLong())
                    Log.d(TAG, "Bitmap cache entry removed: $key (${size} bytes)")
                }
            }
        }
        
        Log.d(TAG, "Memory caches initialized with size: ${cacheSize / 1024}KB")
    }
    
    /**
     * Cache detection result for future use
     */
    fun cacheDetectionResult(
        key: String,
        result: Any,
        operationType: OperationType,
        ttlMs: Long = 60000L // 1 minute default TTL
    ) {
        try {
            val cachedResult = CachedDetectionResult(
                result = result,
                operationType = operationType,
                timestamp = System.currentTimeMillis(),
                ttlMs = ttlMs,
                sizeInBytes = estimateObjectSize(result)
            )
            
            detectionResultCache.put(key, cachedResult)
            totalAllocatedBytes.addAndGet(cachedResult.sizeInBytes.toLong())
            
            Log.d(TAG, "Cached detection result: $key (${cachedResult.sizeInBytes} bytes, TTL: ${ttlMs}ms)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache detection result: $key", e)
        }
    }
    
    /**
     * Retrieve cached detection result
     */
    fun getCachedDetectionResult(key: String): Any? {
        return try {
            val cached = detectionResultCache.get(key)
            if (cached != null) {
                // Check TTL
                if (System.currentTimeMillis() - cached.timestamp <= cached.ttlMs) {
                    cacheHits.incrementAndGet()
                    Log.d(TAG, "Cache hit: $key")
                    cached.result
                } else {
                    // Expired, remove from cache
                    detectionResultCache.remove(key)
                    cacheMisses.incrementAndGet()
                    Log.d(TAG, "Cache miss (expired): $key")
                    null
                }
            } else {
                cacheMisses.incrementAndGet()
                Log.d(TAG, "Cache miss: $key")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve cached result: $key", e)
            cacheMisses.incrementAndGet()
            null
        }
    }
    
    /**
     * Cache bitmap with weak reference
     */
    fun cacheBitmap(key: String, bitmap: Bitmap) {
        try {
            val size = calculateBitmapSize(bitmap)
            bitmapCache.put(key, WeakReference(bitmap))
            activeBitmaps[key] = BitmapInfo(
                width = bitmap.width,
                height = bitmap.height,
                sizeBytes = size,
                timestamp = System.currentTimeMillis()
            )
            totalAllocatedBytes.addAndGet(size.toLong())
            
            Log.d(TAG, "Cached bitmap: $key (${bitmap.width}x${bitmap.height}, ${size} bytes)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache bitmap: $key", e)
        }
    }
    
    /**
     * Retrieve cached bitmap
     */
    fun getCachedBitmap(key: String): Bitmap? {
        return try {
            val weakRef = bitmapCache.get(key)
            val bitmap = weakRef?.get()
            if (bitmap != null && !bitmap.isRecycled) {
                cacheHits.incrementAndGet()
                Log.d(TAG, "Bitmap cache hit: $key")
                bitmap
            } else {
                // Bitmap was garbage collected or recycled
                if (weakRef != null) {
                    bitmapCache.remove(key)
                    activeBitmaps.remove(key)
                }
                cacheMisses.incrementAndGet()
                Log.d(TAG, "Bitmap cache miss: $key")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve cached bitmap: $key", e)
            cacheMisses.incrementAndGet()
            null
        }
    }
    
    /**
     * Handle memory pressure by clearing caches
     */
    suspend fun handleMemoryPressure(level: MemoryPressureLevel) {
        Log.w(TAG, "Handling memory pressure: $level")
        
        when (level) {
            MemoryPressureLevel.LOW -> {
                // Clear expired entries
                clearExpiredEntries()
            }
            MemoryPressureLevel.MODERATE -> {
                // Clear half of the cache
                clearCachePercentage(0.5f)
            }
            MemoryPressureLevel.HIGH -> {
                // Clear most of the cache
                clearCachePercentage(0.8f)
            }
            MemoryPressureLevel.CRITICAL -> {
                // Clear all caches
                clearAllCaches()
                // Force garbage collection
                System.gc()
            }
        }
        
        updateMemoryMetrics()
    }
    
    /**
     * Get current memory usage statistics
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        val cacheHitRate = if (cacheHits.get() + cacheMisses.get() > 0) {
            cacheHits.get().toFloat() / (cacheHits.get() + cacheMisses.get())
        } else {
            0f
        }
        
        return MemoryStats(
            maxMemoryMB = maxMemory / (1024 * 1024),
            totalMemoryMB = totalMemory / (1024 * 1024),
            usedMemoryMB = usedMemory / (1024 * 1024),
            freeMemoryMB = freeMemory / (1024 * 1024),
            cacheAllocatedMB = totalAllocatedBytes.get() / (1024 * 1024),
            detectionCacheSize = detectionResultCache.size(),
            bitmapCacheSize = bitmapCache.size(),
            cacheHitRate = cacheHitRate,
            activeBitmapCount = activeBitmaps.size
        )
    }
    
    /**
     * Configure cache sizes based on device capabilities
     */
    fun configureCacheSize(sizeMB: Int) {
        maxCacheSizeBytes = sizeMB * 1024 * 1024L
        
        // Resize caches
        val newCacheSize = (maxCacheSizeBytes / 1024).toInt() // Convert to KB for LruCache
        detectionResultCache.resize(newCacheSize)
        bitmapCache.resize(newCacheSize / 4)
        
        Log.d(TAG, "Cache size configured to: ${sizeMB}MB")
    }
    
    /**
     * Clear all caches
     */
    fun clearAllCaches() {
        detectionResultCache.evictAll()
        bitmapCache.evictAll()
        activeBitmaps.clear()
        totalAllocatedBytes.set(0)
        Log.d(TAG, "All caches cleared")
    }
    
    private fun startPeriodicCleanup() {
        cleanupJob = memoryScope.launch {
            while (isActive) {
                try {
                    delay(CACHE_CLEANUP_INTERVAL_MS)
                    performPeriodicCleanup()
                } catch (e: Exception) {
                    Log.w(TAG, "Error during periodic cleanup", e)
                }
            }
        }
    }
    
    private suspend fun performPeriodicCleanup() {
        // Clear expired entries
        clearExpiredEntries()
        
        // Update memory metrics
        updateMemoryMetrics()
        
        // Check memory pressure
        val stats = getMemoryStats()
        val memoryUsageRatio = stats.usedMemoryMB.toFloat() / stats.maxMemoryMB
        
        val newMemoryState = when {
            memoryUsageRatio > 0.9f -> MemoryState.CRITICAL
            memoryUsageRatio > 0.8f -> MemoryState.HIGH_PRESSURE
            memoryUsageRatio > 0.6f -> MemoryState.MODERATE_PRESSURE
            else -> MemoryState.NORMAL
        }
        
        if (newMemoryState != _memoryState.value) {
            _memoryState.value = newMemoryState
            Log.d(TAG, "Memory state changed to: $newMemoryState")
            
            // Handle memory pressure automatically
            if (newMemoryState != MemoryState.NORMAL) {
                val pressureLevel = when (newMemoryState) {
                    MemoryState.MODERATE_PRESSURE -> MemoryPressureLevel.LOW
                    MemoryState.HIGH_PRESSURE -> MemoryPressureLevel.MODERATE
                    MemoryState.CRITICAL -> MemoryPressureLevel.HIGH
                    else -> MemoryPressureLevel.LOW
                }
                handleMemoryPressure(pressureLevel)
            }
        }
    }
    
    private fun clearExpiredEntries() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = mutableListOf<String>()
        
        // Find expired detection results
        detectionResultCache.snapshot().forEach { (key, cached) ->
            if (currentTime - cached.timestamp > cached.ttlMs) {
                expiredKeys.add(key)
            }
        }
        
        // Remove expired entries
        expiredKeys.forEach { key ->
            detectionResultCache.remove(key)
        }
        
        if (expiredKeys.isNotEmpty()) {
            Log.d(TAG, "Cleared ${expiredKeys.size} expired cache entries")
        }
    }
    
    private fun clearCachePercentage(percentage: Float) {
        val detectionEntriesToRemove = (detectionResultCache.size() * percentage).toInt()
        val bitmapEntriesToRemove = (bitmapCache.size() * percentage).toInt()
        
        // Remove oldest entries from detection cache
        val detectionSnapshot = detectionResultCache.snapshot()
        detectionSnapshot.entries
            .sortedBy { it.value.timestamp }
            .take(detectionEntriesToRemove)
            .forEach { detectionResultCache.remove(it.key) }
        
        // Remove oldest entries from bitmap cache
        val bitmapSnapshot = bitmapCache.snapshot()
        bitmapSnapshot.keys
            .take(bitmapEntriesToRemove)
            .forEach { bitmapCache.remove(it) }
        
        Log.d(TAG, "Cleared ${percentage * 100}% of cache entries")
    }
    
    private fun updateMemoryMetrics() {
        val stats = getMemoryStats()
        _memoryMetrics.value = MemoryMetrics(
            usedMemoryMB = stats.usedMemoryMB,
            maxMemoryMB = stats.maxMemoryMB,
            cacheAllocatedMB = stats.cacheAllocatedMB,
            cacheHitRate = stats.cacheHitRate,
            memoryPressure = stats.usedMemoryMB.toFloat() / stats.maxMemoryMB,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun calculateBitmapSize(bitmap: Bitmap): Int {
        return bitmap.width * bitmap.height * BITMAP_BYTES_PER_PIXEL
    }
    
    private fun estimateObjectSize(obj: Any): Int {
        // Simplified object size estimation
        return when (obj) {
            is String -> obj.length * 2 // 2 bytes per char
            is List<*> -> obj.size * 100 // Rough estimate
            is ByteArray -> obj.size
            else -> 1024 // Default 1KB estimate
        }
    }
    
    fun cleanup() {
        cleanupJob?.cancel()
        clearAllCaches()
        memoryScope.cancel()
        Log.d(TAG, "Memory manager cleaned up")
    }
}

/**
 * Cached detection result with metadata
 */
private data class CachedDetectionResult(
    val result: Any,
    val operationType: OperationType,
    val timestamp: Long,
    val ttlMs: Long,
    val sizeInBytes: Int
)

/**
 * Bitmap information for tracking
 */
private data class BitmapInfo(
    val width: Int,
    val height: Int,
    val sizeBytes: Int,
    val timestamp: Long
)

/**
 * Memory pressure levels
 */
enum class MemoryPressureLevel {
    LOW, MODERATE, HIGH, CRITICAL
}

/**
 * Memory state
 */
enum class MemoryState(val displayName: String) {
    NORMAL("Normal memory usage"),
    MODERATE_PRESSURE("Moderate memory pressure"),
    HIGH_PRESSURE("High memory pressure"),
    CRITICAL("Critical memory usage")
}

/**
 * Memory usage statistics
 */
data class MemoryStats(
    val maxMemoryMB: Long,
    val totalMemoryMB: Long,
    val usedMemoryMB: Long,
    val freeMemoryMB: Long,
    val cacheAllocatedMB: Long,
    val detectionCacheSize: Int,
    val bitmapCacheSize: Int,
    val cacheHitRate: Float,
    val activeBitmapCount: Int
)

/**
 * Real-time memory metrics
 */
data class MemoryMetrics(
    val usedMemoryMB: Long,
    val maxMemoryMB: Long,
    val cacheAllocatedMB: Long,
    val cacheHitRate: Float,
    val memoryPressure: Float,
    val timestamp: Long
) {
    companion object {
        fun empty() = MemoryMetrics(0L, 0L, 0L, 0f, 0f, 0L)
    }
}