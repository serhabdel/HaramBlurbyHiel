package com.hieltech.haramblur.data

/**
 * Registry of popular apps that can be blocked
 * Contains package names, display names, and categories
 */
object AppRegistry {

    // Social Media Apps
    val SOCIAL_MEDIA_APPS = mapOf(
        "com.instagram.android" to AppInfo("Instagram", "social_media", "📸 Photo & Video Sharing"),
        "com.facebook.katana" to AppInfo("Facebook", "social_media", "📘 Social Network"),
        "com.twitter.android" to AppInfo("Twitter/X", "social_media", "🐦 Social Media"),
        "com.snapchat.android" to AppInfo("Snapchat", "social_media", "👻 Photo Messaging"),
        "com.zhiliaoapp.musically" to AppInfo("TikTok", "social_media", "🎵 Short Videos"),
        "com.linkedin.android" to AppInfo("LinkedIn", "social_media", "💼 Professional Network"),
        "com.reddit.frontpage" to AppInfo("Reddit", "social_media", "📱 Discussion Forum"),
        "com.tumblr" to AppInfo("Tumblr", "social_media", "📝 Microblogging"),
        "com.pinterest" to AppInfo("Pinterest", "social_media", "📌 Visual Discovery"),
        "org.bluesky.bsky" to AppInfo("Bluesky", "social_media", "🦋 Social Network")
    )

    // Messaging Apps
    val MESSAGING_APPS = mapOf(
        "com.whatsapp" to AppInfo("WhatsApp", "messaging", "💬 Messaging"),
        "org.telegram.messenger" to AppInfo("Telegram", "messaging", "✈️ Messaging"),
        "com.discord" to AppInfo("Discord", "messaging", "🎮 Gaming Chat"),
        "com.facebook.orca" to AppInfo("Messenger", "messaging", "💬 Facebook Messenger"),
        "com.skype.raider" to AppInfo("Skype", "messaging", "📞 Video Calls"),
        "com.viber.voip" to AppInfo("Viber", "messaging", "📱 VoIP"),
        "com.google.android.apps.messaging" to AppInfo("Messages", "messaging", "💬 SMS"),
        "com.android.mms" to AppInfo("Messaging", "messaging", "💬 SMS"),
        "com.signal" to AppInfo("Signal", "messaging", "🔒 Secure Messaging"),
        "com.wire" to AppInfo("Wire", "messaging", "🔒 Team Communication")
    )

    // Dating Apps
    val DATING_APPS = mapOf(
        "com.tinder" to AppInfo("Tinder", "dating", "💕 Dating"),
        "com.bumble.app" to AppInfo("Bumble", "dating", "💕 Dating"),
        "com.okcupid.okcupid" to AppInfo("OkCupid", "dating", "💕 Dating"),
        "com.hinge.app" to AppInfo("Hinge", "dating", "💕 Dating"),
        "com.grindrapp.android" to AppInfo("Grindr", "dating", "💕 Dating"),
        "com.match.android.matchmobile" to AppInfo("Match", "dating", "💕 Dating"),
        "com.pof.android" to AppInfo("Plenty of Fish", "dating", "💕 Dating")
    )

    // Entertainment Apps
    val ENTERTAINMENT_APPS = mapOf(
        "com.netflix.mediaclient" to AppInfo("Netflix", "entertainment", "🎬 Streaming"),
        "com.amazon.avod.thirdpartyclient" to AppInfo("Prime Video", "entertainment", "🎬 Streaming"),
        "com.google.android.youtube" to AppInfo("YouTube", "entertainment", "📺 Video Platform"),
        "com.google.android.apps.youtube.music" to AppInfo("YouTube Music", "entertainment", "🎵 Music"),
        "com.spotify.music" to AppInfo("Spotify", "entertainment", "🎵 Music Streaming"),
        "com.apple.android.music" to AppInfo("Apple Music", "entertainment", "🎵 Music"),
        "com.soundcloud.android" to AppInfo("SoundCloud", "entertainment", "🎵 Music Platform"),
        "tv.twitch.android.app" to AppInfo("Twitch", "entertainment", "🎮 Streaming"),
        "com.valvesoftware.android.steam.community" to AppInfo("Steam", "entertainment", "🎮 Gaming"),
        "com.epicgames.fortnite" to AppInfo("Fortnite", "entertainment", "🎮 Gaming")
    )

    // Shopping Apps
    val SHOPPING_APPS = mapOf(
        "com.amazon.mShop.android.shopping" to AppInfo("Amazon Shopping", "shopping", "🛒 E-commerce"),
        "com.ebay.mobile" to AppInfo("eBay", "shopping", "🛒 Auction"),
        "com.etsy.android" to AppInfo("Etsy", "shopping", "🛍️ Handmade"),
        "com.walmart.android" to AppInfo("Walmart", "shopping", "🛒 Retail"),
        "com.target" to AppInfo("Target", "shopping", "🛒 Retail"),
        "com.shopify.mobile" to AppInfo("Shopify", "shopping", "🛒 E-commerce"),
        "com.alibaba.aliexpresshd" to AppInfo("AliExpress", "shopping", "🛒 Online Shopping")
    )

    // News Apps
    val NEWS_APPS = mapOf(
        "flipboard.app" to AppInfo("Flipboard", "news", "📰 News"),
        "com.google.android.apps.magazines" to AppInfo("Google News", "news", "📰 News"),
        "com.cnn.mobile.android.phone" to AppInfo("CNN", "news", "📰 News"),
        "com.nytimes.android" to AppInfo("NY Times", "news", "📰 News"),
        "com.bbc.news" to AppInfo("BBC News", "news", "📰 News"),
        "com.reuters" to AppInfo("Reuters", "news", "📰 News"),
        "com.aljazeera.english" to AppInfo("Al Jazeera", "news", "📰 News")
    )

    // All apps combined
    val ALL_POPULAR_APPS = SOCIAL_MEDIA_APPS + MESSAGING_APPS + DATING_APPS +
                           ENTERTAINMENT_APPS + SHOPPING_APPS + NEWS_APPS

    // Apps that should be blocked by default (high distraction potential)
    val DEFAULT_BLOCKED_APPS = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.snapchat.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.tinder",
        "com.bumble.app",
        "com.discord",
        "com.netflix.mediaclient",
        "com.google.android.youtube"
    )

    // Apps that should be suggested for blocking (moderate distraction)
    val SUGGESTED_BLOCKED_APPS = setOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "tv.twitch.android.app",
        "com.reddit.frontpage"
    )

    /**
     * Get app info by package name
     */
    fun getAppInfo(packageName: String): AppInfo? {
        return ALL_POPULAR_APPS[packageName]
    }

    /**
     * Check if an app should be blocked by default
     */
    fun shouldBlockByDefault(packageName: String): Boolean {
        return DEFAULT_BLOCKED_APPS.contains(packageName)
    }

    /**
     * Check if an app should be suggested for blocking
     */
    fun shouldSuggestBlocking(packageName: String): Boolean {
        return SUGGESTED_BLOCKED_APPS.contains(packageName)
    }

    /**
     * Get apps by category
     */
    fun getAppsByCategory(category: String): Map<String, AppInfo> {
        return ALL_POPULAR_APPS.filter { it.value.category == category }
    }

    /**
     * Get all categories
     */
    fun getCategories(): Set<String> {
        return ALL_POPULAR_APPS.values.map { it.category }.toSet()
    }

    /**
     * Search apps by name
     */
    fun searchApps(query: String): Map<String, AppInfo> {
        val lowercaseQuery = query.lowercase()
        return ALL_POPULAR_APPS.filter { (_, appInfo) ->
            appInfo.name.lowercase().contains(lowercaseQuery) ||
            appInfo.description.lowercase().contains(lowercaseQuery)
        }
    }
}

/**
 * Data class for app information
 */
data class AppInfo(
    val name: String,
    val category: String,
    val description: String,
    val isSystemApp: Boolean = false
)
