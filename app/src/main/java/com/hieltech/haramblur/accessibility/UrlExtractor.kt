package com.hieltech.haramblur.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.hieltech.haramblur.utils.AppConstants.Tags
import java.util.regex.Pattern

/**
 * Utility class for extracting URLs from browser apps via accessibility nodes
 * Supports multiple browser types with specific and generic extraction strategies
 */
object UrlExtractor {
    
    private const val TAG = Tags.ACCESSIBILITY_SERVICE
    
    // URL pattern for validation
    private val URL_PATTERN = Pattern.compile(
        "(?:https?://)?(?:www\\.)?([a-zA-Z0-9][-a-zA-Z0-9]*[a-zA-Z0-9]\\.)+[a-zA-Z]{2,}(/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )
    
    /**
     * Extract URL from a browser app based on package name
     */
    fun extractUrlFromBrowser(packageName: String?, rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null
        
        val normalizedPackage = packageName?.lowercase() ?: ""
        
        return when {
            normalizedPackage.contains("chrome") -> extractUrlFromChrome(rootNode)
            normalizedPackage.contains("firefox") -> extractUrlFromFirefox(rootNode)
            normalizedPackage.contains("edge") -> extractUrlFromEdge(rootNode)
            normalizedPackage.contains("samsung") || normalizedPackage.contains("sbrowser") -> 
                extractUrlFromSamsungBrowser(rootNode)
            else -> extractUrlFromGenericBrowser(rootNode)
        }
    }
    
    /**
     * Extract URL from Chrome browser
     */
    private fun extractUrlFromChrome(rootNode: AccessibilityNodeInfo): String? {
        // Chrome stores URL in specific node structures
        val urlBars = rootNode.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
        if (urlBars.isNotEmpty()) {
            val url = urlBars[0].text?.toString()
            urlBars.forEach { it.recycle() }
            if (!url.isNullOrBlank()) return normalizeUrl(url)
        }
        
        // Fallback: search in edit text fields
        return findUrlInNodeHierarchy(rootNode)
    }
    
    /**
     * Extract URL from Firefox browser
     */
    private fun extractUrlFromFirefox(rootNode: AccessibilityNodeInfo): String? {
        // Firefox URL bar
        val urlBars = rootNode.findAccessibilityNodeInfosByViewId("org.mozilla.firefox:id/url_bar_title")
        if (urlBars.isNotEmpty()) {
            val url = urlBars[0].text?.toString()
            urlBars.forEach { it.recycle() }
            if (!url.isNullOrBlank()) return normalizeUrl(url)
        }
        
        // Alternative Firefox URL bar ID
        val urlBars2 = rootNode.findAccessibilityNodeInfosByViewId("org.mozilla.firefox:id/mozac_browser_toolbar_url_view")
        if (urlBars2.isNotEmpty()) {
            val url = urlBars2[0].text?.toString()
            urlBars2.forEach { it.recycle() }
            if (!url.isNullOrBlank()) return normalizeUrl(url)
        }
        
        return findUrlInNodeHierarchy(rootNode)
    }
    
    /**
     * Extract URL from Edge browser
     */
    private fun extractUrlFromEdge(rootNode: AccessibilityNodeInfo): String? {
        val urlBars = rootNode.findAccessibilityNodeInfosByViewId("com.microsoft.emmx:id/url_bar")
        if (urlBars.isNotEmpty()) {
            val url = urlBars[0].text?.toString()
            urlBars.forEach { it.recycle() }
            if (!url.isNullOrBlank()) return normalizeUrl(url)
        }
        
        return findUrlInNodeHierarchy(rootNode)
    }
    
    /**
     * Extract URL from Samsung browser
     */
    private fun extractUrlFromSamsungBrowser(rootNode: AccessibilityNodeInfo): String? {
        val urlBars = rootNode.findAccessibilityNodeInfosByViewId("com.sec.android.app.sbrowser:id/location_bar_edit_text")
        if (urlBars.isNotEmpty()) {
            val url = urlBars[0].text?.toString()
            urlBars.forEach { it.recycle() }
            if (!url.isNullOrBlank()) return normalizeUrl(url)
        }
        
        return findUrlInNodeHierarchy(rootNode)
    }
    
    /**
     * Generic URL extraction from any browser
     */
    private fun extractUrlFromGenericBrowser(rootNode: AccessibilityNodeInfo): String? {
        return findUrlInNodeHierarchy(rootNode)
    }
    
    /**
     * Recursively search for URL in node hierarchy
     */
    fun findUrlInNodeHierarchy(node: AccessibilityNodeInfo?, depth: Int = 0): String? {
        if (node == null || depth > 10) return null
        
        // Check if this node contains a URL
        val text = node.text?.toString()
        val description = node.contentDescription?.toString()
        
        listOf(text, description).forEach { content ->
            if (!content.isNullOrBlank()) {
                extractUrlFromText(content)?.let { return it }
            }
        }
        
        // Check if node is editable (likely URL bar)
        if (node.isEditable && !text.isNullOrBlank()) {
            extractUrlFromText(text)?.let { return it }
        }
        
        // Recursively check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val url = findUrlInNodeHierarchy(child, depth + 1)
            child.recycle()
            if (url != null) return url
        }
        
        return null
    }
    
    /**
     * Extract URL from text content using regex
     */
    fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        
        val matcher = URL_PATTERN.matcher(text)
        return if (matcher.find()) {
            normalizeUrl(matcher.group())
        } else {
            null
        }
    }
    
    /**
     * Extract URL from content description
     */
    fun extractUrlFromContentDescription(description: String?): String? {
        return extractUrlFromText(description)
    }
    
    /**
     * Extract URL from node info using multiple strategies
     */
    fun extractUrlFromNodeInfo(nodeInfo: AccessibilityNodeInfo?): String? {
        if (nodeInfo == null) return null
        
        // Try text
        nodeInfo.text?.toString()?.let { text ->
            extractUrlFromText(text)?.let { return it }
        }
        
        // Try content description
        nodeInfo.contentDescription?.toString()?.let { desc ->
            extractUrlFromContentDescription(desc)?.let { return it }
        }
        
        return null
    }
    
    /**
     * Normalize URL to consistent format
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        
        // Add https:// if no protocol
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        
        return normalized
    }
    
    /**
     * Check if text is a valid URL
     */
    fun isValidUrl(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return URL_PATTERN.matcher(text).matches()
    }
}
