package com.hieltech.haramblur.detection

import com.hieltech.haramblur.data.models.AppCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for resolving user-friendly display names for app packages
 */
@Singleton
class AppNameResolver @Inject constructor() {

    /**
     * Get display name for an app package
     */
    fun getDisplayName(packageName: String, appCategory: AppCategory? = null): String {
        return try {
            // Try to get app category display name first
            if (appCategory != null) {
                // Use a more user-friendly name based on package
                when {
                    packageName.contains("instagram") -> "Instagram"
                    packageName.contains("facebook") -> "Facebook"
                    packageName.contains("tiktok") || packageName.contains("musically") -> "TikTok"
                    packageName.contains("twitter") -> "Twitter"
                    packageName.contains("whatsapp") -> "WhatsApp"
                    packageName.contains("chrome") -> "Chrome"
                    packageName.contains("firefox") -> "Firefox"
                    packageName.contains("youtube") -> "YouTube"
                    packageName.contains("netflix") -> "Netflix"
                    packageName.contains("tinder") -> "Tinder"
                    packageName.contains("bumble") -> "Bumble"
                    packageName.contains("snapchat") -> "Snapchat"
                    packageName.contains("telegram") -> "Telegram"
                    packageName.contains("discord") -> "Discord"
                    packageName.contains("twitch") -> "Twitch"
                    packageName.contains("reddit") -> "Reddit"
                    packageName.contains("pinterest") -> "Pinterest"
                    packageName.contains("linkedin") -> "LinkedIn"
                    packageName.contains("skype") -> "Skype"
                    packageName.contains("zoom") -> "Zoom"
                    packageName.contains("slack") -> "Slack"
                    packageName.contains("teams") -> "Microsoft Teams"
                    packageName.contains("spotify") -> "Spotify"
                    packageName.contains("pandora") -> "Pandora"
                    packageName.contains("soundcloud") -> "SoundCloud"
                    packageName.contains("amazon") -> "Amazon"
                    packageName.contains("ebay") -> "eBay"
                    packageName.contains("aliexpress") -> "AliExpress"
                    packageName.contains("walmart") -> "Walmart"
                    packageName.contains("target") -> "Target"
                    packageName.contains("etsy") -> "Etsy"
                    packageName.contains("gmail") -> "Gmail"
                    packageName.contains("outlook") -> "Outlook"
                    packageName.contains("yahoo") -> "Yahoo Mail"
                    packageName.contains("protonmail") -> "ProtonMail"
                    packageName.contains("drive") || packageName.contains("googledrive") -> "Google Drive"
                    packageName.contains("dropbox") -> "Dropbox"
                    packageName.contains("onedrive") -> "OneDrive"
                    packageName.contains("icloud") -> "iCloud"
                    packageName.contains("photos") -> "Google Photos"
                    packageName.contains("gallery") -> "Gallery"
                    packageName.contains("camera") -> "Camera"
                    packageName.contains("calculator") -> "Calculator"
                    packageName.contains("calendar") -> "Calendar"
                    packageName.contains("clock") -> "Clock"
                    packageName.contains("weather") -> "Weather"
                    packageName.contains("maps") -> "Maps"
                    packageName.contains("playstore") || packageName.contains("vending") -> "Google Play Store"
                    packageName.contains("settings") -> "Settings"
                    packageName.contains("phone") -> "Phone"
                    packageName.contains("contacts") -> "Contacts"
                    packageName.contains("messages") || packageName.contains("sms") -> "Messages"
                    packageName.contains("dialer") -> "Phone Dialer"
                    packageName.contains("browser") -> "Browser"
                    packageName.contains("filemanager") || packageName.contains("files") -> "File Manager"
                    packageName.contains("music") -> "Music Player"
                    packageName.contains("video") -> "Video Player"
                    packageName.contains("gallery") -> "Gallery"
                    else -> appCategory.displayName
                }
            } else {
                // Fallback to package name
                packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
            }
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * Get display names for multiple packages at once
     */
    fun getDisplayNames(packages: List<String>, appCategories: Map<String, AppCategory?> = emptyMap()): Map<String, String> {
        return packages.associateWith { packageName ->
            getDisplayName(packageName, appCategories[packageName])
        }
    }

    /**
     * Check if a package has a custom display name mapping
     */
    fun hasCustomDisplayName(packageName: String): Boolean {
        return packageName.contains(Regex(
            "(instagram|facebook|tiktok|musically|twitter|whatsapp|chrome|firefox|" +
            "youtube|netflix|tinder|bumble|snapchat|telegram|discord|twitch|" +
            "reddit|pinterest|linkedin|skype|zoom|slack|teams|spotify|pandora|" +
            "soundcloud|amazon|ebay|aliexpress|walmart|target|etsy|gmail|outlook|" +
            "yahoo|protonmail|drive|googledrive|dropbox|onedrive|icloud|photos|" +
            "gallery|camera|calculator|calendar|clock|weather|maps|playstore|" +
            "vending|settings|phone|contacts|messages|sms|dialer|browser|" +
            "filemanager|files|music|video)"
        ))
    }
}
