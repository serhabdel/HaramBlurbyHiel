package com.hieltech.haramblur.detection

import com.hieltech.haramblur.utils.UrlUtils
import java.security.MessageDigest

/**
 * Prebuilt database of adult content sites
 * Contains SHA-256 hashes of known adult domains for privacy
 */
object AdultContentDatabase {
    
    /**
     * Generate SHA-256 hash for a domain (for development/testing only)
     * This method should not be used in production builds
     */
    private fun hashDomain(domain: String): String {
        return UrlUtils.hashDomainSha256(domain.lowercase())
    }
    
    /**
     * Major adult content sites - hashed for privacy
     * These domains are blocked at 100% confidence with no user visibility
     */
    val PREBUILT_ADULT_DOMAINS = setOf(
        // Major pornographic sites
        hashDomain("xnxx.com"),
        hashDomain("pornhub.com"),
        hashDomain("xvideos.com"),
        hashDomain("redtube.com"),
        hashDomain("youporn.com"),
        hashDomain("tube8.com"),
        hashDomain("spankbang.com"),
        hashDomain("xhamster.com"),
        hashDomain("beeg.com"),
        hashDomain("tnaflix.com"),
        hashDomain("drtuber.com"),
        hashDomain("nuvid.com"),
        hashDomain("sunporno.com"),
        hashDomain("vporn.com"),
        hashDomain("4tube.com"),
        hashDomain("slutload.com"),
        hashDomain("xtube.com"),
        hashDomain("youjizz.com"),
        hashDomain("keezmovies.com"),
        hashDomain("extremetube.com"),
        
        // Adult cam sites
        hashDomain("chaturbate.com"),
        hashDomain("cam4.com"),
        hashDomain("livejasmin.com"),
        hashDomain("stripchat.com"),
        hashDomain("bongacams.com"),
        hashDomain("camsoda.com"),
        hashDomain("flirt4free.com"),
        hashDomain("imlive.com"),
        hashDomain("streamate.com"),
        hashDomain("myfreecams.com"),
        
        // OnlyFans and similar platforms
        hashDomain("onlyfans.com"),
        hashDomain("fansly.com"),
        hashDomain("justforfans.com"),
        hashDomain("manyvids.com"),
        hashDomain("clips4sale.com"),
        
        // Adult dating/hookup sites
        hashDomain("adultfriendfinder.com"),
        hashDomain("ashley-madison.com"),
        hashDomain("benaughty.com"),
        hashDomain("flirt.com"),
        hashDomain("fuckbook.com"),
        hashDomain("hookup.com"),
        hashDomain("naughtydate.com"),
        hashDomain("sexmessenger.com"),
        hashDomain("together2night.com"),
        hashDomain("xmatch.com"),
        
        // Hentai and anime adult content
        hashDomain("hanime.tv"),
        hashDomain("hentaihaven.org"),
        hashDomain("nhentai.net"),
        hashDomain("fakku.net"),
        hashDomain("tsumino.com"),
        hashDomain("hentai2read.com"),
        hashDomain("doujins.com"),
        
        // Adult image boards and forums
        hashDomain("4chan.org"),
        hashDomain("8kun.top"),
        hashDomain("rule34.xxx"),
        hashDomain("gelbooru.com"),
        hashDomain("danbooru.donmai.us"),
        hashDomain("e621.net"),
        hashDomain("sankakucomplex.com"),
        
        // Note: reddit.com, tumblr.com, twitter.com, discord.gg removed
        // These are general-purpose platforms, not adult-specific
        // Users can still add them manually via custom blocked sites
        
        // Adult shopping and services
        hashDomain("adameve.com"),
        hashDomain("lovehoney.com"),
        hashDomain("spencers.com"),
        hashDomain("pinkcherry.com"),
        hashDomain("edenfantasys.com"),
        
        // Adult news and magazines
        hashDomain("playboy.com"),
        hashDomain("penthouse.com"),
        hashDomain("hustler.com"),
        hashDomain("vivid.com"),
        
        // International adult sites
        hashDomain("javhd.com"),
        hashDomain("javmost.com"),
        hashDomain("jav777.com"),
        hashDomain("caribbeancom.com"),
        hashDomain("1pondo.tv"),
        hashDomain("tokyo-hot.com"),
        
        // Adult torrent and file sharing
        hashDomain("empornium.me"),
        hashDomain("pornbay.org"),
        hashDomain("pornolab.net"),
        
        // Adult VR and interactive content
        hashDomain("vrporn.com"),
        hashDomain("badoinkvr.com"),
        hashDomain("naughtyamerica.com"),
        hashDomain("realitykings.com"),
        hashDomain("brazzers.com"),
        hashDomain("bangbros.com"),
        hashDomain("digitalplayground.com"),
        hashDomain("wickedpictures.com"),
        
        // Adult gaming
        hashDomain("nutaku.net"),
        hashDomain("lewdgamer.com"),
        hashDomain("f95zone.to"),
        
        // Escort and adult services
        hashDomain("eros.com"),
        hashDomain("slixa.com"),
        hashDomain("tryst.link"),
        hashDomain("privatedelights.ch"),
        hashDomain("adultsearch.com"),
        
        // Adult classified ads
        hashDomain("backpage.com"), // Note: Site may be defunct
        // Note: craigslist.org removed — general-purpose classifieds
        hashDomain("doublelist.com"),
        hashDomain("bedpage.com"),
        hashDomain("megapersonals.eu"),
        
        // Adult streaming and live content
        hashDomain("pornhublive.com"),
        hashDomain("camsoda.com"),
        hashDomain("cam4.com"),
        hashDomain("chaturbate.com"),
        hashDomain("xlovecam.com"),
        
        // Adult mobile apps and platforms
        hashDomain("tinder.com"), // Note: Dating app with adult content potential
        hashDomain("grindr.com"), // Note: Dating app with adult content potential
        hashDomain("scruff.com"),
        hashDomain("adam4adam.com"),
        
        // Adult content aggregators
        hashDomain("tblop.com"),
        hashDomain("pornmd.com"),
        hashDomain("fuq.com"),
        hashDomain("porntrex.com"),
        hashDomain("hqporner.com"),
        
        // Adult premium content
        hashDomain("mofos.com"),
        hashDomain("teamskeet.com"),
        hashDomain("girlsway.com"),
        hashDomain("puretaboo.com"),
        hashDomain("adulttime.com"),
        
        // Adult fetish and niche content
        hashDomain("kink.com"),
        hashDomain("fetlife.com"),
        hashDomain("bdsmtest.org"),
        hashDomain("collarspace.com"),
        hashDomain("alt.com"),
        
        // Adult educational (borderline — keeping only explicitly adult sites)
        hashDomain("sexinfo101.com"),
        // Note: scarleteen.com removed — educational resource
        // Note: plannedparenthood.org removed — health organization
        
        // Adult art and photography
        hashDomain("metart.com"),
        hashDomain("hegre.com"),
        hashDomain("femjoy.com"),
        hashDomain("thelifeerotic.com"),
        hashDomain("goddessnudes.com"),
        
        // Adult literature and stories
        hashDomain("literotica.com"),
        hashDomain("asstr.org"),
        hashDomain("sexstories.com"),
        hashDomain("lushstories.com"),
        hashDomain("storiesonline.net"),
        
        // Adult comics and cartoons
        hashDomain("8muses.com"),
        hashDomain("multporn.net"),
        hashDomain("porncomix.info"),
        hashDomain("adultcomicbook.com"),
        hashDomain("allporncomic.com"),
        
        // Adult technology and toys
        hashDomain("lovense.com"),
        hashDomain("kiiroo.com"),
        hashDomain("ohmibod.com"),
        hashDomain("wevibe.com"),
        hashDomain("lelo.com"),
        
        // Adult affiliate and advertising networks
        hashDomain("trafficjunky.com"),
        hashDomain("exoclick.com"),
        hashDomain("juicyads.com"),
        hashDomain("plugrush.com"),
        hashDomain("ero-advertising.com"),
        
        // Adult payment processors
        hashDomain("ccbill.com"),
        hashDomain("epoch.com"),
        hashDomain("segpay.com"),
        hashDomain("verotel.com"),
        hashDomain("zombaio.com"),
        
        // Adult hosting and CDN
        hashDomain("pornhubpremium.com"),
        hashDomain("redtubecdn.com"),
        hashDomain("youporncdn.com"),
        hashDomain("xtube-cdn.com"),
        hashDomain("xhamstercdn.com"),
        
        // Adult mobile and app stores
        hashDomain("mikandi.com"),
        hashDomain("adultappmart.com"),
        hashDomain("sexapps.com"),
        
        // Adult virtual reality
        hashDomain("sexlikereal.com"),
        hashDomain("czechvr.com"),
        hashDomain("virtualrealporn.com"),
        hashDomain("povr.com"),
        hashDomain("vrbangers.com"),
        
        // Adult blockchain and crypto
        hashDomain("spankchain.com"),
        // Note: vice.com removed — news organization
        hashDomain("camsoda.com"),
        
        // Adult social networks
        hashDomain("swinglifestyle.com"),
        hashDomain("kasidie.com"),
        hashDomain("sdc.com"),
        hashDomain("fabswingers.com"),
        hashDomain("adultspace.com"),
        
        // Adult personals and classifieds
        hashDomain("locanto.com"), // Note: Has both adult and non-adult content
        hashDomain("cityxguide.com"),
        hashDomain("skipthegames.com"),
        hashDomain("listcrawler.com"),
        hashDomain("adultsearch.com")
    )
    
    /**
     * Adult content TLDs and domains patterns
     */
    val ADULT_TLDS = setOf(
        ".porn", ".sex", ".xxx", ".adult"
        // Note: .tube, .video, .live removed — legitimate gTLDs used by non-adult sites
    )
    
    /**
     * Adult content keywords for pattern matching
     */
    /**
     * Unambiguous explicit keywords only.
     * Context-dependent words (teen, gay, adult, mature, escort, dating, etc.)
     * were removed to prevent false positives on legitimate sites.
     */
    val ADULT_KEYWORDS = setOf(
        "porn", "xxx", "nude", "naked", "fuck", "pussy", "dick", "cock",
        "tits", "boobs", "blowjob", "handjob", "milf",
        "hentai", "onlyfans", "chaturbate", "cam4",
        "nsfw", "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn"
    )
    
    /**
     * Check if a domain hash is in the prebuilt adult content database
     */
    fun isAdultContentDomain(domainHash: String): Boolean {
        return PREBUILT_ADULT_DOMAINS.contains(domainHash)
    }
    
    /**
     * Check if a URL contains adult content patterns
     */
    fun containsAdultContentPattern(url: String): Boolean {
        val lowercaseUrl = url.lowercase()
        
        // Check TLDs
        for (tld in ADULT_TLDS) {
            if (lowercaseUrl.contains(tld)) {
                return true
            }
        }
        
        // Check keywords
        for (keyword in ADULT_KEYWORDS) {
            if (lowercaseUrl.contains(keyword)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Get the total number of prebuilt adult domains
     */
    fun getPrebuiltDomainCount(): Int {
        return PREBUILT_ADULT_DOMAINS.size
    }
}
