package com.hieltech.haramblur.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

/**
 * Resilient URL extraction system that doesn't rely on hard-coded resource IDs
 * Uses multiple strategies to extract URLs from browser accessibility trees
 */
class ResilientUrlExtractor {
    
    companion object {
        private const val TAG = "ResilientUrlExtractor"
        private const val MAX_SEARCH_DEPTH = 4
        private const val MAX_CHILDREN_PER_LEVEL = 12
        
        // URL patterns for extraction
        private val URL_PATTERNS = listOf(
            // Full HTTP/HTTPS URLs (most specific first)
            Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE),
            // WWW URLs (add protocol)
            Pattern.compile("www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE),
            // Domain-based URLs
            Pattern.compile("[\\w\\-._~]+\\.[a-zA-Z]{2,}[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*", Pattern.CASE_INSENSITIVE)
        )
        
        // Browser-specific hints for URL location
        private val BROWSER_URL_HINTS = mapOf(
            "chrome" to listOf("url_bar", "location_bar", "omnibox", "address", "search"),
            "firefox" to listOf("url_bar", "location_bar", "toolbar", "address", "search"),
            "edge" to listOf("url_bar", "address_bar", "location", "omnibox", "search"),
            "samsung" to listOf("location_bar", "url_bar", "address", "search"),
            "opera" to listOf("url_field", "address_bar", "location", "search"),
            "brave" to listOf("url_bar", "omnibox", "address", "location", "search"),
            "duckduckgo" to listOf("url_bar", "search_bar", "address", "location"),
            "kiwi" to listOf("url_bar", "address_bar", "location", "search"),
            "via" to listOf("url_bar", "address_bar", "location", "search")
        )
        
        // Content description patterns that might contain URLs
        private val URL_CONTENT_PATTERNS = listOf(
            "address", "url", "location", "site", "website", "link", "navigation",
            "omnibox", "search", "bar", "field", "input", "edit"
        )
        
        // Class name patterns that might indicate URL fields
        private val URL_CLASS_PATTERNS = listOf(
            "url", "address", "location", "omnibox", "search", "edit", "text",
            "input", "field", "bar", "navigation"
        )
    }
    
    /**
     * Extract URL from accessibility event using multiple resilient strategies
     */
    fun extractUrl(rootNode: AccessibilityNodeInfo?, packageName: String?): String? {
        if (rootNode == null) return null
        
        return try {
            // Strategy 1: Browser-specific extraction with hints
            extractUrlWithBrowserHints(rootNode, packageName)
                ?: // Strategy 2: Content-based extraction
                extractUrlByContent(rootNode)
                ?: // Strategy 3: Heuristic-based extraction
                extractUrlByHeuristics(rootNode)
                ?: // Strategy 4: Deep search extraction
                extractUrlByDeepSearch(rootNode)
        } catch (e: Exception) {
            Log.w(TAG, "Error during URL extraction", e)
            null
        }
    }
    
    /**
     * Strategy 1: Browser-specific extraction using hints instead of hard-coded IDs
     */
    private fun extractUrlWithBrowserHints(rootNode: AccessibilityNodeInfo, packageName: String?): String? {
        if (packageName == null) return null
        
        val browserType = identifyBrowserType(packageName)
        val hints = BROWSER_URL_HINTS[browserType] ?: return null
        
        Log.v(TAG, "Using browser hints for $browserType: $hints")
        
        // Search for nodes that match browser-specific hints
        for (hint in hints) {
            val url = searchNodesByHint(rootNode, hint, 0)
            if (url != null) {
                Log.d(TAG, "URL found using hint '$hint' for $browserType: $url")
                return url
            }
        }
        
        return null
    }
    
    /**
     * Strategy 2: Content-based extraction looking for URL-like content
     */
    private fun extractUrlByContent(rootNode: AccessibilityNodeInfo): String? {
        return searchForUrlContent(rootNode, 0)
    }
    
    /**
     * Strategy 3: Heuristic-based extraction using common patterns
     */
    private fun extractUrlByHeuristics(rootNode: AccessibilityNodeInfo): String? {
        // Look for nodes with URL-like class names or content descriptions
        return searchNodesByHeuristics(rootNode, 0)
    }
    
    /**
     * Strategy 4: Deep search extraction as last resort
     */
    private fun extractUrlByDeepSearch(rootNode: AccessibilityNodeInfo): String? {
        return performDeepUrlSearch(rootNode, 0)
    }
    
    /**
     * Identify browser type from package name
     */
    private fun identifyBrowserType(packageName: String): String {
        val lowerPackage = packageName.lowercase()
        return when {
            lowerPackage.contains("chrome") -> "chrome"
            lowerPackage.contains("firefox") -> "firefox"
            lowerPackage.contains("edge") -> "edge"
            lowerPackage.contains("samsung") -> "samsung"
            lowerPackage.contains("opera") -> "opera"
            lowerPackage.contains("brave") -> "brave"
            lowerPackage.contains("duckduckgo") -> "duckduckgo"
            lowerPackage.contains("kiwi") -> "kiwi"
            lowerPackage.contains("via") -> "via"
            else -> "generic"
        }
    }
    
    /**
     * Search nodes using browser-specific hints
     */
    private fun searchNodesByHint(node: AccessibilityNodeInfo?, hint: String, depth: Int): String? {
        if (node == null || depth > MAX_SEARCH_DEPTH) return null
        
        try {
            // Check if current node matches the hint
            val nodeText = node.text?.toString()?.lowercase()
            val nodeDescription = node.contentDescription?.toString()?.lowercase()
            val nodeClassName = node.className?.toString()?.lowercase()
            val nodeViewId = node.viewIdResourceName?.lowercase()
            
            // Check if any node property contains the hint
            if (nodeText?.contains(hint) == true ||
                nodeDescription?.contains(hint) == true ||
                nodeClassName?.contains(hint) == true ||
                nodeViewId?.contains(hint) == true) {
                
                // Extract URL from this node
                val url = extractUrlFromText(nodeText) 
                    ?: extractUrlFromText(nodeDescription)
                    ?: extractUrlFromText(nodeViewId)
                
                if (url != null) return url
            }
            
            // Search children
            val childCount = minOf(node.childCount, MAX_CHILDREN_PER_LEVEL)
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                val childUrl = searchNodesByHint(child, hint, depth + 1)
                child?.recycle()
                if (childUrl != null) return childUrl
            }
            
            return null
        } catch (e: Exception) {
            Log.w(TAG, "Error searching nodes by hint: $hint", e)
            return null
        }
    }
    
    /**
     * Search for URL content in nodes
     */
    private fun searchForUrlContent(node: AccessibilityNodeInfo?, depth: Int): String? {
        if (node == null || depth > MAX_SEARCH_DEPTH) return null
        
        try {
            // Check current node for URL content
            val nodeText = node.text?.toString()
            val nodeDescription = node.contentDescription?.toString()
            
            val url = extractUrlFromText(nodeText) ?: extractUrlFromText(nodeDescription)
            if (url != null) return url
            
            // Search children
            val childCount = minOf(node.childCount, MAX_CHILDREN_PER_LEVEL)
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                val childUrl = searchForUrlContent(child, depth + 1)
                child?.recycle()
                if (childUrl != null) return childUrl
            }
            
            return null
        } catch (e: Exception) {
            Log.w(TAG, "Error searching for URL content", e)
            return null
        }
    }
    
    /**
     * Search nodes using heuristics
     */
    private fun searchNodesByHeuristics(node: AccessibilityNodeInfo?, depth: Int): String? {
        if (node == null || depth > MAX_SEARCH_DEPTH) return null
        
        try {
            val nodeDescription = node.contentDescription?.toString()?.lowercase()
            val nodeClassName = node.className?.toString()?.lowercase()
            val nodeViewId = node.viewIdResourceName?.lowercase()
            
            // Check if node matches URL-related patterns
            val isUrlRelated = URL_CONTENT_PATTERNS.any { pattern ->
                nodeDescription?.contains(pattern) == true ||
                nodeClassName?.contains(pattern) == true ||
                nodeViewId?.contains(pattern) == true
            }
            
            if (isUrlRelated) {
                val nodeText = node.text?.toString()
                val url = extractUrlFromText(nodeText) ?: extractUrlFromText(nodeDescription)
                if (url != null) return url
            }
            
            // Search children
            val childCount = minOf(node.childCount, MAX_CHILDREN_PER_LEVEL)
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                val childUrl = searchNodesByHeuristics(child, depth + 1)
                child?.recycle()
                if (childUrl != null) return childUrl
            }
            
            return null
        } catch (e: Exception) {
            Log.w(TAG, "Error searching nodes by heuristics", e)
            return null
        }
    }
    
    /**
     * Perform deep search as last resort
     */
    private fun performDeepUrlSearch(node: AccessibilityNodeInfo?, depth: Int): String? {
        if (node == null || depth > MAX_SEARCH_DEPTH) return null
        
        try {
            // Check all text content of current node
            val nodeText = node.text?.toString()
            val nodeDescription = node.contentDescription?.toString()
            val nodeViewId = node.viewIdResourceName
            
            val url = extractUrlFromText(nodeText) 
                ?: extractUrlFromText(nodeDescription)
                ?: extractUrlFromText(nodeViewId)
            
            if (url != null) return url
            
            // Search all children
            val childCount = minOf(node.childCount, MAX_CHILDREN_PER_LEVEL)
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                val childUrl = performDeepUrlSearch(child, depth + 1)
                child?.recycle()
                if (childUrl != null) return childUrl
            }
            
            return null
        } catch (e: Exception) {
            Log.w(TAG, "Error in deep URL search", e)
            return null
        }
    }
    
    /**
     * Extract URL from text using optimized pattern matching
     */
    private fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        
        // Single pass through patterns for better performance
        for (pattern in URL_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                var url = matcher.group().trim()
                
                // Add protocol if missing
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    if (url.startsWith("www.") || url.contains(".")) {
                        url = "https://$url"
                    }
                }
                
                // Basic validation
                if (url.length in 10..2048 && url.contains(".") && isValidUrl(url)) {
                    return url
                }
            }
        }
        
        return null
    }
    
    /**
     * Basic URL validation
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
}
