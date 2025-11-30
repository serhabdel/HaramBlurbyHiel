package com.hieltech.haramblur.data.models

/**
 * Categories of apps that can be monitored for content detection
 */
enum class AppCategory(val displayName: String, val description: String, val defaultApps: Set<String>) {
    SOCIAL_MEDIA(
        "Social Media",
        "Instagram, Facebook, TikTok, LinkedIn, Twitter, Snapchat",
        setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.zhiliaoapp.musically", // TikTok
            "com.ss.android.ugc.trill", // TikTok variant
            "com.twitter.android",
            "com.snapchat.android",
            "com.linkedin.android", // LinkedIn
            "com.pinterest",
            "com.reddit.frontpage",
            "com.tumblr"
        )
    ),
    BROWSERS(
        "Web Browsers",
        "Chrome, Firefox, Safari, Edge, Opera",
        setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "com.samsung.android.app.sbrowser",
            "com.UCMobile.intl",
            "com.kiwibrowser.browser",
            "org.mozilla.focus",
            "com.android.browser"
        )
    ),
    DATING(
        "Dating Apps",
        "Tinder, Bumble, Hinge, OkCupid",
        setOf(
            "com.tinder",
            "com.bumble.app",
            "com.hinge.app",
            "com.okcupid.okcupid",
            "com.grindrapp.android",
            "com.match.android.matchmobile",
            "com.pof.android"
        )
    ),
    MESSAGING(
        "Messaging Apps",
        "WhatsApp, Telegram, Discord, Messenger",
        setOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.discord",
            "com.facebook.orca",
            "com.skype.raider",
            "com.viber.voip",
            "com.signal",
            "com.wire"
        )
    ),
    ENTERTAINMENT(
        "Entertainment",
        "YouTube, Netflix, Twitch, Spotify",
        setOf(
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "tv.twitch.android.app",
            "com.spotify.music",
            "com.amazon.avod.thirdpartyclient"
        )
    )
}
