package com.hieltech.haramblur.utils

import android.util.Log
import java.net.URL
import java.security.MessageDigest

/**
 * Utility class for URL processing operations.
 * Consolidates duplicate URL manipulation functions from across the codebase.
 */
object UrlUtils {

    private const val TAG = "UrlUtils"

    /**
     * Extract clean domain from URL, removing protocol and www prefix
     *
     * @param url The URL to extract domain from
     * @return The clean domain name, or original URL if extraction fails
     */
    fun extractDomain(url: String): String {
        return try {
            // First try using URL constructor (most reliable)
            val urlObj = URL(url)
            var host = urlObj.host?.lowercase() ?: ""

            // Remove www prefix if present
            if (host.startsWith("www.")) {
                host = host.substring(4)
            }

            // Return host if valid, otherwise fallback
            if (host.isNotEmpty()) host else fallbackDomainExtraction(url)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract domain using URL constructor, using fallback", e)
            fallbackDomainExtraction(url)
        }
    }

    /**
     * Fallback domain extraction using string manipulation
     */
    private fun fallbackDomainExtraction(url: String): String {
        return try {
            // Remove protocol
            val withoutProtocol = url.substringAfter("://", url)

            // Extract domain part (before first slash)
            val domain = withoutProtocol.substringBefore("/")

            // Remove www prefix
            val withoutWww = if (domain.startsWith("www.")) {
                domain.substring(4)
            } else {
                domain
            }

            // Remove port if present
            val finalDomain = withoutWww.substringBefore(":")

            finalDomain.lowercase()

        } catch (e: Exception) {
            Log.w(TAG, "Could not extract domain from URL: $url", e)
            url // Return original URL if all extraction methods fail
        }
    }

    /**
     * Hash domain using SHA-256 for privacy and consistency
     * This is the preferred method for production use
     *
     * @param domain The domain to hash
     * @return SHA-256 hash of the domain in hexadecimal format
     */
    fun hashDomainSha256(domain: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(domain.lowercase().toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to hash domain using SHA-256", e)
            // Fallback to simple hash if SHA-256 fails
            hashDomainSimple(domain)
        }
    }

    /**
     * Simple domain hash using hashCode (for compatibility/fallback)
     * Not recommended for production due to collision potential
     *
     * @param domain The domain to hash
     * @return Simple hash of the domain
     */
    fun hashDomainSimple(domain: String): String {
        return domain.lowercase().hashCode().toString(16).padStart(12, '0').take(12)
    }

    /**
     * Clean and normalize URL by removing fragments and query parameters
     *
     * @param url The URL to clean
     * @return Cleaned URL
     */
    fun cleanUrl(url: String): String {
        var cleanUrl = url.trim()

        // Add protocol if missing
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        // Remove fragments and some query parameters
        cleanUrl = cleanUrl.split("#")[0]

        return cleanUrl
    }

    /**
     * Check if URL is valid
     *
     * @param url The URL to validate
     * @return true if URL is valid, false otherwise
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extract the base URL (protocol + domain + port if non-standard)
     *
     * @param url The URL to process
     * @return Base URL without path or query parameters
     */
    fun extractBaseUrl(url: String): String {
        return try {
            val urlObj = URL(url)
            val protocol = urlObj.protocol
            val host = urlObj.host
            val port = urlObj.port

            if (port != -1 && port != urlObj.defaultPort) {
                "$protocol://$host:$port"
            } else {
                "$protocol://$host"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract base URL", e)
            url
        }
    }
}
