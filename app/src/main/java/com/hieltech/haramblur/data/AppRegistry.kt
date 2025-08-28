package com.hieltech.haramblur.data

/**
 * Registry of popular apps that can be blocked
 * Contains package names, display names, and categories
 */
object AppRegistry {

    // Social Media Apps - Enhanced with regional variants and subcategories
    val SOCIAL_MEDIA_APPS = mapOf(
        // Instagram and variants
        "com.instagram.android" to AppInfo("Instagram", "social_media", "📸 Photo & Video Sharing"),
        "com.instagram.layout" to AppInfo("Instagram Layout", "social_media", "📸 Instagram Layout"),
        "com.instagram.threads" to AppInfo("Threads", "social_media", "🧵 Text-focused Social"),
        "com.instagram.boomerang" to AppInfo("Boomerang", "social_media", "🎬 Video Effects"),
        "com.instagram.reels" to AppInfo("Instagram Reels", "social_media", "🎬 Short Videos"),
        "com.instagram.stories" to AppInfo("Instagram Stories", "social_media", "📖 Story Sharing"),

        // Facebook and variants
        "com.facebook.katana" to AppInfo("Facebook", "social_media", "📘 Social Network"),
        "com.facebook.orca" to AppInfo("Messenger", "social_media", "💬 Facebook Messenger"),
        "com.facebook.lite" to AppInfo("Facebook Lite", "social_media", "📱 Lite Version"),
        "com.facebook.mlite" to AppInfo("Messenger Lite", "social_media", "💬 Lite Messenger"),
        "com.facebook.pages.app" to AppInfo("Facebook Pages", "social_media", "📄 Business Pages"),

        // Twitter/X and variants
        "com.twitter.android" to AppInfo("Twitter/X", "social_media", "🐦 Social Media"),
        "com.twitter.pad" to AppInfo("Twitter Tablet", "social_media", "🐦 Tablet Version"),
        "com.twitter.thirdparty" to AppInfo("Third-party Twitter", "social_media", "🔗 Third-party App"),

        // Snapchat and variants
        "com.snapchat.android" to AppInfo("Snapchat", "social_media", "👻 Photo Messaging"),
        "com.snap.mushroom" to AppInfo("Snapchat Mushroom", "social_media", "🍄 Snapchat Feature"),

        // TikTok and variants
        "com.zhiliaoapp.musically" to AppInfo("TikTok", "social_media", "🎵 Short Videos"),
        "com.ss.android.ugc.tiktok" to AppInfo("TikTok", "social_media", "🎵 Short Videos"),
        "com.ss.android.ugc.tiktok.live" to AppInfo("TikTok Live", "social_media", "🎵 Live Streaming"),
        "com.ss.android.ugc.aweme" to AppInfo("TikTok", "social_media", "🎵 Short Videos"),
        "com.ss.android.ugc.live" to AppInfo("TikTok Live", "social_media", "🎵 Live Streaming"),

        // YouTube and variants
        "com.google.android.youtube" to AppInfo("YouTube", "social_media", "📺 Video Platform"),
        "com.google.android.apps.youtube.music" to AppInfo("YouTube Music", "social_media", "🎵 Music Streaming"),
        "com.google.android.apps.youtube.creator" to AppInfo("YouTube Creator", "social_media", "🎬 Content Creator"),
        "com.google.android.apps.youtube.kids" to AppInfo("YouTube Kids", "social_media", "👶 Family Videos"),

        // LinkedIn and variants
        "com.linkedin.android" to AppInfo("LinkedIn", "social_media", "💼 Professional Network"),
        "com.linkedin.business" to AppInfo("LinkedIn Business", "social_media", "💼 Business Network"),

        // Reddit and variants
        "com.reddit.frontpage" to AppInfo("Reddit", "social_media", "📱 Discussion Forum"),
        "com.reddit.redditlite" to AppInfo("Reddit Lite", "social_media", "📱 Lite Version"),

        // Tumblr
        "com.tumblr" to AppInfo("Tumblr", "social_media", "📝 Microblogging"),

        // Pinterest
        "com.pinterest" to AppInfo("Pinterest", "social_media", "📌 Visual Discovery"),

        // Bluesky
        "org.bluesky.bsky" to AppInfo("Bluesky", "social_media", "🦋 Social Network"),

        // Mastodon
        "org.joinmastodon.android" to AppInfo("Mastodon", "social_media", "🐘 Decentralized Social"),

        // BeReal
        "com.bereal.ft" to AppInfo("BeReal", "social_media", "📸 Authentic Moments"),

        // Clubhouse
        "com.clubhouse.app" to AppInfo("Clubhouse", "social_media", "🎙️ Audio Rooms"),

        // Twitch
        "tv.twitch.android.app" to AppInfo("Twitch", "social_media", "🎮 Live Streaming"),

        // Bilibili
        "tv.danmaku.bili" to AppInfo("Bilibili", "social_media", "📺 Chinese Video"),

        // VK
        "com.vkontakte.android" to AppInfo("VK", "social_media", "🇷🇺 Russian Social"),

        // Odnoklassniki
        "ru.ok.android" to AppInfo("Odnoklassniki", "social_media", "🇷🇺 Russian Network"),

        // WeChat
        "com.tencent.mm" to AppInfo("WeChat", "social_media", "🇨🇳 Chinese Messenger"),

        // LINE
        "jp.naver.line.android" to AppInfo("LINE", "social_media", "🇯🇵 Japanese Messenger"),

        // KakaoTalk
        "com.kakao.talk" to AppInfo("KakaoTalk", "social_media", "🇰🇷 Korean Messenger"),

        // Regional variants and third-party apps
        "com.gbwhatsapp" to AppInfo("GB WhatsApp", "social_media", "💬 Modified WhatsApp"),
        "com.whatsapp.w4b" to AppInfo("WhatsApp Business", "social_media", "💼 Business Messaging"),

        // Social media management tools
        "com.buffer.android" to AppInfo("Buffer", "social_media", "📱 Social Management"),
        "com.hootsuite.android" to AppInfo("Hootsuite", "social_media", "📊 Social Media Mgmt"),
        "com.socialbee.app" to AppInfo("SocialBee", "social_media", "🐝 Social Automation")
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

    /**
     * Get social media subcategory for a package name
     */
    fun getSocialMediaSubcategory(packageName: String): String? {
        return when {
            // Photo sharing apps
            packageName.contains("instagram") ||
            packageName.contains("snapchat") ||
            packageName.contains("pinterest") -> "photo_sharing"

            // Video platforms
            packageName.contains("tiktok") ||
            packageName.contains("youtube") ||
            packageName.contains("twitch") ||
            packageName.contains("bilibili") -> "video_platforms"

            // Messaging apps
            packageName.contains("whatsapp") ||
            packageName.contains("telegram") ||
            packageName.contains("discord") ||
            packageName.contains("skype") ||
            packageName.contains("viber") ||
            packageName.contains("signal") ||
            packageName.contains("wechat") ||
            packageName.contains("line") ||
            packageName.contains("kakaotalk") ||
            packageName.contains("facebook.orca") -> "messaging"

            // Professional networks
            packageName.contains("linkedin") -> "professional_networks"

            // Forums and discussions
            packageName.contains("reddit") -> "forums_discussions"

            // News feeds
            packageName.contains("twitter") ||
            packageName.contains("facebook.katana") ||
            packageName.contains("tumblr") ||
            packageName.contains("bluesky") ||
            packageName.contains("mastodon") -> "news_feeds"

            // Dating apps
            packageName.contains("tinder") ||
            packageName.contains("bumble") ||
            packageName.contains("hinge") ||
            packageName.contains("okcupid") ||
            packageName.contains("match") ||
            packageName.contains("pof") ||
            packageName.contains("grindr") -> "dating"

            // Gaming social
            packageName.contains("steam") ||
            packageName.contains("fortnite") ||
            packageName.contains("riot") ||
            packageName.contains("supercell") -> "gaming_social"

            else -> null
        }
    }

    /**
     * Get all social media package names
     */
    fun getAllSocialMediaPackageNames(): Set<String> {
        return SOCIAL_MEDIA_APPS.keys
    }

    /**
     * Check if app is social media related (broader detection)
     */
    fun isSocialMediaRelated(packageName: String): Boolean {
        return SOCIAL_MEDIA_APPS.containsKey(packageName) ||
               packageName.contains("social") ||
               packageName.contains("chat") ||
               packageName.contains("message") ||
               packageName.contains("dating") ||
               packageName.contains("network") ||
               getSocialMediaSubcategory(packageName) != null
    }

    /**
     * Get social media apps by subcategory
     */
    fun getSocialMediaAppsBySubcategory(subcategory: String): Map<String, AppInfo> {
        return SOCIAL_MEDIA_APPS.filter { (packageName, _) ->
            getSocialMediaSubcategory(packageName) == subcategory
        }
    }

    /**
     * Get all social media subcategories
     */
    fun getSocialMediaSubcategories(): Set<String> {
        return SOCIAL_MEDIA_APPS.keys.mapNotNull { getSocialMediaSubcategory(it) }.toSet()
    }

    /**
     * Get confidence score for social media detection
     */
    fun getSocialMediaConfidence(packageName: String): Double {
        return when {
            SOCIAL_MEDIA_APPS.containsKey(packageName) -> 1.0
            getSocialMediaSubcategory(packageName) != null -> 0.8
            packageName.contains("social") ||
            packageName.contains("chat") ||
            packageName.contains("message") -> 0.6
            packageName.contains("network") ||
            packageName.contains("dating") -> 0.7
            else -> 0.0
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
