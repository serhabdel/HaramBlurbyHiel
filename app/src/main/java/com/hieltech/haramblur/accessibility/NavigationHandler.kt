package com.hieltech.haramblur.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.hieltech.haramblur.utils.AppConstants.Tags

/**
 * Handles navigation actions within the accessibility service
 * Provides safe ways to navigate away from inappropriate content
 */
class NavigationHandler(
    private val accessibilityService: AccessibilityService
) {
    
    private val TAG = Tags.ACCESSIBILITY_SERVICE
    
    companion object {
        // Islamic websites for redirection
        private val ISLAMIC_WEBSITES = listOf(
            "https://quran.com",
            "https://sunnah.com",
            "https://islamqa.info",
            "https://muslimpro.com"
        )
    }
    
    /**
     * Navigate away from current content using back action
     */
    fun navigateBack(): Boolean {
        return try {
            val success = accessibilityService.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK
            )
            if (success) {
                Log.d(TAG, "Navigated back successfully")
            } else {
                Log.w(TAG, "Failed to navigate back")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating back", e)
            false
        }
    }
    
    /**
     * Navigate to home screen
     */
    fun navigateHome(): Boolean {
        return try {
            val success = accessibilityService.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_HOME
            )
            if (success) {
                Log.d(TAG, "Navigated to home successfully")
            } else {
                Log.w(TAG, "Failed to navigate home")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating home", e)
            false
        }
    }
    
    /**
     * Navigate to an Islamic website as positive redirection
     */
    fun navigateToIslamicWebsite(): Boolean {
        val url = ISLAMIC_WEBSITES.random()
        return openUrlWithIntent(url)
    }
    
    /**
     * Navigate to a specific URL using intent
     */
    fun openUrlWithIntent(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            accessibilityService.startActivity(intent)
            Log.d(TAG, "Opened URL with intent: $url")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL with intent: $url", e)
            false
        }
    }
    
    /**
     * Perform scroll gesture to move away from content
     */
    fun performScrollAway(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        
        val scrollableNode = findScrollableNode(rootNode)
        if (scrollableNode == null) {
            Log.w(TAG, "No scrollable node found for scroll away")
            return false
        }
        
        return try {
            // Scroll down to move away
            val success = scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (success) {
                Log.d(TAG, "Scrolled away from content")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error performing scroll away", e)
            false
        } finally {
            try {
                scrollableNode.recycle()
            } catch (_: Exception) {}
        }
    }
    
    /**
     * Find a scrollable node in the hierarchy
     */
    private fun findScrollableNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check if root is scrollable
        if (rootNode.isScrollable) {
            return AccessibilityNodeInfo.obtain(rootNode)
        }
        
        // Search children
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            
            if (child.isScrollable) {
                return child
            }
            
            // Recursively check grandchildren
            val scrollable = findScrollableNode(child)
            if (scrollable != null) {
                return scrollable
            }
            
            child.recycle()
        }
        
        return null
    }
    
    /**
     * Execute navigation with safety wrapper
     */
    fun <T> safeNavigate(
        operationName: String,
        defaultValue: T,
        operation: () -> T
    ): T {
        return try {
            operation()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed: $operationName", e)
            defaultValue
        }
    }
    
    /**
     * Execute navigation with boolean return
     */
    fun safeNavigate(operationName: String, operation: () -> Boolean): Boolean {
        return safeNavigate(operationName, false, operation)
    }
}
