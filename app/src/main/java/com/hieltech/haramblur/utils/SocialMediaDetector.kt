package com.hieltech.haramblur.utils

import com.hieltech.haramblur.detection.AppInfo
import com.hieltech.haramblur.data.AppRegistry

/**
 * Utility class for detecting and managing social media applications
 */
object SocialMediaDetector {

    // Social media subcategories for better organization
    enum class SocialMediaSubcategory(val displayName: String, val description: String) {
        PHOTO_SHARING("Photo & Video Sharing", "Apps focused on sharing photos and videos"),
        VIDEO_PLATFORMS("Video Platforms", "Short-form and long-form video platforms"),
        MESSAGING("Messaging", "Communication and messaging apps"),
        PROFESSIONAL_NETWORKS("Professional Networks", "Business and career-oriented social platforms"),
        FORUMS_DISCUSSIONS("Forums & Discussions", "Community discussion platforms"),
        NEWS_FEEDS("News & Feeds", "Social news and content discovery"),
        DATING("Dating", "Dating and relationship apps"),
        GAMING_SOCIAL("Gaming Social", "Social features within gaming apps")
    }

    // Data class for social media features
    data class SocialMediaFeatures(
        val hasMessaging: Boolean = false,
        val hasVideoSharing: Boolean = false,
        val hasStories: Boolean = false,
        val hasLiveStreaming: Boolean = false,
        val hasPhotoEditing: Boolean = false,
        val isProfessional: Boolean = false,
        val isGamingRelated: Boolean = false
    )

    // Comprehensive social media package names including regional variants
    private val SOCIAL_MEDIA_PACKAGES = setOf(
        // Instagram and variants
        "com.instagram.android",
        "com.instagram.threads",

        // Facebook and variants
        "com.facebook.katana",
        "com.facebook.orca", // Messenger
        "com.facebook.lite",
        "com.facebook.mlite",
        "com.facebook.pages.app",

        // Twitter/X and variants
        "com.twitter.android",

        // Snapchat and variants
        "com.snapchat.android",

        // TikTok and variants
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.tiktok",
        "com.ss.android.ugc.aweme",

        // YouTube and variants
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.google.android.apps.youtube.creator",
        "com.google.android.apps.youtube.kids",

        // LinkedIn and variants
        "com.linkedin.android",

        // Reddit and variants
        "com.reddit.frontpage",

        // Tumblr
        "com.tumblr",

        // Pinterest
        "com.pinterest",

        // WhatsApp and variants
        "com.whatsapp",
        "com.whatsapp.w4b", // WhatsApp Business
        "com.gbwhatsapp",

        // Telegram and variants
        "org.telegram.messenger",

        // Discord
        "com.discord",

        // Skype
        "com.skype.raider",

        // Viber
        "com.viber.voip",

        // Signal
        "org.thoughtcrime.securesms",

        // WeChat
        "com.tencent.mm",

        // LINE
        "jp.naver.line.android",

        // KakaoTalk
        "com.kakao.talk",

        // VK
        "com.vkontakte.android",

        // Odnoklassniki
        "ru.ok.android",

        // Bilibili
        "tv.danmaku.bili",

        // Twitch
        "tv.twitch.android.app",

        // BeReal
        "com.bereal.ft",

        // Bluesky
        "org.bluesky.bsky",

        // Mastodon
        "org.joinmastodon.android",

        // Tinder and dating apps
        "com.tinder",
        "com.bumble.app",
        "com.okcupid.okcupid",
        "com.hinge.app",
        "com.match.android.matchmobile",
        "com.pof.android",
        "com.grindrapp.android",

        // Gaming social
        "com.valvesoftware.android.steam.community",
        "com.epicgames.fortnite",
        "com.riotgames.league.wildrift",
        "com.riotgames.leaguemobile",
        "com.supercell.clashroyale",
        "com.supercell.clashofclans",

        // Regional social media (verified packages only)
        "com.gbwhatsapp", // GBWhatsApp

        // Web-based social media PWAs (browsers commonly used for social media)
        "com.android.chrome",
        "org.mozilla.firefox"
    )

    // Package to subcategory mapping
    private val PACKAGE_SUBCATEGORY_MAP = mapOf(
        // Photo & Video Sharing
        "com.instagram.android" to SocialMediaSubcategory.PHOTO_SHARING,
        "com.snapchat.android" to SocialMediaSubcategory.PHOTO_SHARING,
        "com.pinterest" to SocialMediaSubcategory.PHOTO_SHARING,
        "com.instagram.threads" to SocialMediaSubcategory.PHOTO_SHARING,

        // Video Platforms
        "com.zhiliaoapp.musically" to SocialMediaSubcategory.VIDEO_PLATFORMS,
        "com.ss.android.ugc.tiktok" to SocialMediaSubcategory.VIDEO_PLATFORMS,
        "com.google.android.youtube" to SocialMediaSubcategory.VIDEO_PLATFORMS,
        "com.google.android.apps.youtube.music" to SocialMediaSubcategory.VIDEO_PLATFORMS,
        "tv.twitch.android.app" to SocialMediaSubcategory.VIDEO_PLATFORMS,
        "tv.danmaku.bili" to SocialMediaSubcategory.VIDEO_PLATFORMS,
        "com.google.android.apps.youtube.creator" to SocialMediaSubcategory.VIDEO_PLATFORMS,
        "com.google.android.apps.youtube.kids" to SocialMediaSubcategory.VIDEO_PLATFORMS,

        // Messaging
        "com.whatsapp" to SocialMediaSubcategory.MESSAGING,
        "com.facebook.orca" to SocialMediaSubcategory.MESSAGING,
        "org.telegram.messenger" to SocialMediaSubcategory.MESSAGING,
        "com.discord" to SocialMediaSubcategory.MESSAGING,
        "com.skype.raider" to SocialMediaSubcategory.MESSAGING,
        "com.viber.voip" to SocialMediaSubcategory.MESSAGING,
        "org.thoughtcrime.securesms" to SocialMediaSubcategory.MESSAGING,
        "com.tencent.mm" to SocialMediaSubcategory.MESSAGING,
        "jp.naver.line.android" to SocialMediaSubcategory.MESSAGING,
        "com.kakao.talk" to SocialMediaSubcategory.MESSAGING,
        "com.whatsapp.w4b" to SocialMediaSubcategory.MESSAGING,
        "com.gbwhatsapp" to SocialMediaSubcategory.MESSAGING,

        // Professional Networks
        "com.linkedin.android" to SocialMediaSubcategory.PROFESSIONAL_NETWORKS,
        "com.linkedin.business" to SocialMediaSubcategory.PROFESSIONAL_NETWORKS,

        // Forums & Discussions
        "com.reddit.frontpage" to SocialMediaSubcategory.FORUMS_DISCUSSIONS,
        "com.reddit.redditlite" to SocialMediaSubcategory.FORUMS_DISCUSSIONS,

        // News & Feeds
        "com.twitter.android" to SocialMediaSubcategory.NEWS_FEEDS,
        "com.facebook.katana" to SocialMediaSubcategory.NEWS_FEEDS,
        "com.tumblr" to SocialMediaSubcategory.NEWS_FEEDS,
        "org.bluesky.bsky" to SocialMediaSubcategory.NEWS_FEEDS,
        "org.joinmastodon.android" to SocialMediaSubcategory.NEWS_FEEDS,

        // Dating
        "com.tinder" to SocialMediaSubcategory.DATING,
        "com.bumble.app" to SocialMediaSubcategory.DATING,
        "com.okcupid.okcupid" to SocialMediaSubcategory.DATING,
        "com.hinge.app" to SocialMediaSubcategory.DATING,
        "com.match.android.matchmobile" to SocialMediaSubcategory.DATING,
        "com.pof.android" to SocialMediaSubcategory.DATING,
        "com.grindrapp.android" to SocialMediaSubcategory.DATING,

        // Gaming Social
        "com.valvesoftware.android.steam.community" to SocialMediaSubcategory.GAMING_SOCIAL,
        "com.epicgames.fortnite" to SocialMediaSubcategory.GAMING_SOCIAL,
        "com.riotgames.league.wildrift" to SocialMediaSubcategory.GAMING_SOCIAL,
        "com.riotgames.leaguemobile" to SocialMediaSubcategory.GAMING_SOCIAL,
        "com.supercell.clashroyale" to SocialMediaSubcategory.GAMING_SOCIAL,
        "com.supercell.clashofclans" to SocialMediaSubcategory.GAMING_SOCIAL
    )

    // Package to features mapping
    private val PACKAGE_FEATURES_MAP = mapOf(
        "com.instagram.android" to SocialMediaFeatures(hasMessaging = true, hasVideoSharing = true, hasStories = true, hasPhotoEditing = true),
        "com.snapchat.android" to SocialMediaFeatures(hasMessaging = true, hasVideoSharing = true, hasStories = true, hasPhotoEditing = true),
        "com.zhiliaoapp.musically" to SocialMediaFeatures(hasVideoSharing = true, hasLiveStreaming = true),
        "com.google.android.youtube" to SocialMediaFeatures(hasVideoSharing = true, hasLiveStreaming = true),
        "com.facebook.katana" to SocialMediaFeatures(hasMessaging = true, hasVideoSharing = true),
        "com.twitter.android" to SocialMediaFeatures(hasMessaging = true),
        "com.whatsapp" to SocialMediaFeatures(hasMessaging = true),
        "org.telegram.messenger" to SocialMediaFeatures(hasMessaging = true),
        "com.discord" to SocialMediaFeatures(hasMessaging = true, hasVideoSharing = true, isGamingRelated = true),
        "com.linkedin.android" to SocialMediaFeatures(isProfessional = true),
        "tv.twitch.android.app" to SocialMediaFeatures(hasVideoSharing = true, hasLiveStreaming = true, isGamingRelated = true),
        "com.valvesoftware.android.steam.community" to SocialMediaFeatures(isGamingRelated = true),
        "com.epicgames.fortnite" to SocialMediaFeatures(hasMessaging = true, isGamingRelated = true)
    )

    /**
     * Check if a package name belongs to a social media app
     * Primarily uses AppRegistry for consistent detection, with fallback to known packages
     */
    fun isSocialMediaApp(packageName: String): Boolean {
        // Primary check: Use AppRegistry for social media apps
        if (AppRegistry.SOCIAL_MEDIA_APPS.containsKey(packageName)) {
            return true
        }

        // Secondary check: Known social media packages (excluding browsers/utilities)
        return SOCIAL_MEDIA_PACKAGES.contains(packageName) &&
               !isBrowserOrUtility(packageName)
    }

    /**
     * Check if package is a browser or utility that should be excluded from bulk operations
     */
    private fun isBrowserOrUtility(packageName: String): Boolean {
        return packageName in setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.google.android.apps.chrome"
        )
    }

    /**
     * Get the social media subcategory for a package
     * Primarily delegates to AppRegistry for categorization
     */
    fun getSocialMediaCategory(packageName: String): SocialMediaSubcategory? {
        // First check if it's in AppRegistry and map the category
        val appRegistryInfo = AppRegistry.ALL_POPULAR_APPS[packageName]
        if (appRegistryInfo != null) {
            return mapAppRegistryCategoryToSubcategory(appRegistryInfo.category, packageName)
        }

        // Fallback to local mapping for packages not in AppRegistry
        return PACKAGE_SUBCATEGORY_MAP[packageName]
    }

    /**
     * Map AppRegistry category strings to SocialMediaDetector subcategories
     */
    private fun mapAppRegistryCategoryToSubcategory(category: String, packageName: String): SocialMediaSubcategory? {
        return when (category) {
            "social_media" -> {
                // Use description or package name to determine more specific subcategory
                val appInfo = AppRegistry.ALL_POPULAR_APPS[packageName]
                when {
                    appInfo?.description?.contains("Photo") == true ||
                    appInfo?.description?.contains("📸") == true -> SocialMediaSubcategory.PHOTO_SHARING
                    appInfo?.description?.contains("Video") == true ||
                    appInfo?.description?.contains("🎬") == true -> SocialMediaSubcategory.VIDEO_PLATFORMS
                    appInfo?.description?.contains("Messaging") == true ||
                    appInfo?.description?.contains("💬") == true -> SocialMediaSubcategory.MESSAGING
                    appInfo?.description?.contains("Professional") == true ||
                    appInfo?.description?.contains("💼") == true -> SocialMediaSubcategory.PROFESSIONAL_NETWORKS
                    else -> SocialMediaSubcategory.NEWS_FEEDS
                }
            }
            "messaging" -> SocialMediaSubcategory.MESSAGING
            "dating" -> SocialMediaSubcategory.DATING
            "entertainment" -> {
                if (packageName.contains("gaming") || packageName.contains("game")) {
                    SocialMediaSubcategory.GAMING_SOCIAL
                } else {
                    SocialMediaSubcategory.VIDEO_PLATFORMS
                }
            }
            else -> null
        }
    }

    /**
     * Filter a list of apps to only include social media apps
     */
    fun getInstalledSocialMediaApps(allInstalledApps: List<AppInfo>): List<AppInfo> {
        return allInstalledApps.filter { isSocialMediaApp(it.packageName) }
    }

    /**
     * Detect social media features for a package
     */
    fun detectSocialMediaFeatures(packageName: String): SocialMediaFeatures {
        return PACKAGE_FEATURES_MAP[packageName] ?: SocialMediaFeatures()
    }

    /**
     * Check if an app is social media related (broader detection)
     */
    fun isSocialMediaRelated(packageName: String): Boolean {
        return isSocialMediaApp(packageName) ||
               packageName.contains("social") ||
               packageName.contains("chat") ||
               packageName.contains("message") ||
               packageName.contains("dating") ||
               packageName.contains("network")
    }

    /**
     * Get all social media package names
     */
    fun getAllSocialMediaPackageNames(): Set<String> {
        return SOCIAL_MEDIA_PACKAGES + AppRegistry.SOCIAL_MEDIA_APPS.keys
    }

    /**
     * Get apps by social media subcategory
     */
    fun getAppsBySubcategory(apps: List<AppInfo>, subcategory: SocialMediaSubcategory): List<AppInfo> {
        return apps.filter { getSocialMediaCategory(it.packageName) == subcategory }
    }

    /**
     * Get confidence score for social media detection (0.0 to 1.0)
     * Primarily uses AppRegistry for consistent confidence scoring
     */
    fun getSocialMediaConfidence(packageName: String): Double {
        return when {
            // Highest confidence: Apps in AppRegistry social_media category
            AppRegistry.SOCIAL_MEDIA_APPS.containsKey(packageName) -> 1.0
            // High confidence: Known social media packages not in AppRegistry
            SOCIAL_MEDIA_PACKAGES.contains(packageName) -> 0.9
            // Medium confidence: Contains well-known social media keywords
            packageName.contains("instagram") ||
            packageName.contains("facebook") ||
            packageName.contains("twitter") ||
            packageName.contains("snapchat") ||
            packageName.contains("tiktok") ||
            packageName.contains("youtube") -> 0.8
            // Lower confidence: Generic social/chat keywords
            packageName.contains("social") ||
            packageName.contains("chat") ||
            packageName.contains("message") -> 0.6
            else -> 0.0
        }
    }

    /**
     * Get all available social media subcategories
     */
    fun getAllSubcategories(): List<SocialMediaSubcategory> {
        return SocialMediaSubcategory.values().toList()
    }
}
