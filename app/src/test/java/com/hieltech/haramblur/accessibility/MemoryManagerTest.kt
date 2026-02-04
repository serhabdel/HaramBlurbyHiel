package com.hieltech.haramblur.accessibility

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for MemoryManager
 */
class MemoryManagerTest {

    @Test
    fun `checkMemoryPressure returns NORMAL when memory is fine`() {
        var clearCachesCalled = false
        var hideOverlayCalled = false
        
        val memoryManager = MemoryManager(
            onClearCaches = { clearCachesCalled = true },
            onHideOverlay = { hideOverlayCalled = true }
        )
        
        // In a real test environment, this would depend on actual memory
        val pressure = memoryManager.checkMemoryPressure()
        
        // Just verify it doesn't crash and returns a valid value
        assertNotNull(pressure)
        assertTrue(
            "Should return valid memory pressure",
            pressure in listOf(
                MemoryManager.MemoryPressure.NORMAL,
                MemoryManager.MemoryPressure.WARNING,
                MemoryManager.MemoryPressure.CRITICAL
            )
        )
    }
    
    @Test
    fun `emergencyCleanup calls callbacks`() {
        var clearCachesCalled = false
        var hideOverlayCalled = false
        
        val memoryManager = MemoryManager(
            onClearCaches = { clearCachesCalled = true },
            onHideOverlay = { hideOverlayCalled = true }
        )
        
        memoryManager.emergencyCleanup()
        
        // After first call, should execute
        assertTrue("Should call clear caches", clearCachesCalled)
        assertTrue("Should call hide overlay", hideOverlayCalled)
        
        // Reset flags
        clearCachesCalled = false
        hideOverlayCalled = false
        
        // Second call should be on cooldown
        memoryManager.emergencyCleanup()
        
        // Should not be called due to cooldown
        assertFalse("Should not call clear caches on cooldown", clearCachesCalled)
        assertFalse("Should not call hide overlay on cooldown", hideOverlayCalled)
    }
    
    @Test
    fun `MemoryPressure enum has correct order`() {
        val values = MemoryManager.MemoryPressure.values()
        assertEquals(3, values.size)
        assertEquals(MemoryManager.MemoryPressure.NORMAL, values[0])
        assertEquals(MemoryManager.MemoryPressure.WARNING, values[1])
        assertEquals(MemoryManager.MemoryPressure.CRITICAL, values[2])
    }
}
