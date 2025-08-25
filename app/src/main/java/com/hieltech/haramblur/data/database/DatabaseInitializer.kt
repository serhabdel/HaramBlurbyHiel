package com.hieltech.haramblur.data.database

import com.hieltech.haramblur.detection.BlockingCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes the site blocking database with default data
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val database: SiteBlockingDatabase
) {
    
    /**
     * Initialize database with default blocked sites and Quranic verses
     */
    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        val blockedSiteDao = database.blockedSiteDao()
        val quranicVerseDao = database.quranicVerseDao()
        
        // Check if database is already initialized
        val existingSiteCount = blockedSiteDao.getActiveSiteCount()
        val existingVerseCount = quranicVerseDao.getActiveVerseCount()
        
        if (existingSiteCount == 0) {
            initializeBlockedSites()
        }
        
        if (existingVerseCount == 0) {
            initializeQuranicVerses()
        }
    }
    
    /**
     * Initialize blocked sites with common inappropriate domains
     */
    private suspend fun initializeBlockedSites() {
        val blockedSiteDao = database.blockedSiteDao()
        val currentTime = System.currentTimeMillis()
        
        val defaultBlockedSites = listOf(
            // Explicit Content Sites
            createBlockedSite("pornhub.com", BlockingCategory.EXPLICIT_CONTENT, 1.0f, currentTime),
            createBlockedSite("xvideos.com", BlockingCategory.EXPLICIT_CONTENT, 1.0f, currentTime),
            createBlockedSite("xnxx.com", BlockingCategory.EXPLICIT_CONTENT, 1.0f, currentTime),
            createBlockedSite("redtube.com", BlockingCategory.EXPLICIT_CONTENT, 1.0f, currentTime),
            createBlockedSite("youporn.com", BlockingCategory.EXPLICIT_CONTENT, 1.0f, currentTime),
            createBlockedSite("tube8.com", BlockingCategory.EXPLICIT_CONTENT, 1.0f, currentTime),
            createBlockedSite("spankbang.com", BlockingCategory.EXPLICIT_CONTENT, 1.0f, currentTime),
            createBlockedSite("xhamster.com", BlockingCategory.EXPLICIT_CONTENT, 1.0f, currentTime),
            
            // Adult Entertainment
            createBlockedSite("onlyfans.com", BlockingCategory.ADULT_ENTERTAINMENT, 0.95f, currentTime),
            createBlockedSite("chaturbate.com", BlockingCategory.ADULT_ENTERTAINMENT, 1.0f, currentTime),
            createBlockedSite("cam4.com", BlockingCategory.ADULT_ENTERTAINMENT, 1.0f, currentTime),
            createBlockedSite("livejasmin.com", BlockingCategory.ADULT_ENTERTAINMENT, 1.0f, currentTime),
            createBlockedSite("stripchat.com", BlockingCategory.ADULT_ENTERTAINMENT, 1.0f, currentTime),
            
            // Gambling Sites
            createBlockedSite("bet365.com", BlockingCategory.GAMBLING, 0.9f, currentTime),
            createBlockedSite("pokerstars.com", BlockingCategory.GAMBLING, 0.9f, currentTime),
            createBlockedSite("888casino.com", BlockingCategory.GAMBLING, 0.9f, currentTime),
            createBlockedSite("betfair.com", BlockingCategory.GAMBLING, 0.9f, currentTime),
            createBlockedSite("williamhill.com", BlockingCategory.GAMBLING, 0.9f, currentTime),
            
            // Dating Sites (can be inappropriate)
            createBlockedSite("tinder.com", BlockingCategory.DATING_SITES, 0.7f, currentTime),
            createBlockedSite("adultfriendfinder.com", BlockingCategory.DATING_SITES, 0.95f, currentTime),
            createBlockedSite("ashley-madison.com", BlockingCategory.DATING_SITES, 0.9f, currentTime),
            
            // Regex patterns for dynamic content
            createRegexBlockedSite(".*\\.xxx$", BlockingCategory.EXPLICIT_CONTENT, 0.95f, currentTime),
            createRegexBlockedSite(".*porn.*", BlockingCategory.EXPLICIT_CONTENT, 0.8f, currentTime),
            createRegexBlockedSite(".*sex.*", BlockingCategory.EXPLICIT_CONTENT, 0.7f, currentTime),
            createRegexBlockedSite(".*adult.*", BlockingCategory.ADULT_ENTERTAINMENT, 0.6f, currentTime),
            createRegexBlockedSite(".*casino.*", BlockingCategory.GAMBLING, 0.7f, currentTime),
            createRegexBlockedSite(".*bet.*", BlockingCategory.GAMBLING, 0.6f, currentTime)
        )
        
        blockedSiteDao.insertSites(defaultBlockedSites)
    }
    
    /**
     * Initialize Quranic verses for different blocking categories
     */
    private suspend fun initializeQuranicVerses() {
        val quranicVerseDao = database.quranicVerseDao()
        val currentTime = System.currentTimeMillis()
        
        val defaultVerses = listOf(
            // Explicit Content - Lowering Gaze
            QuranicVerseEntity(
                id = "24_30",
                surahName = "An-Nur",
                surahNumber = 24,
                verseNumber = 30,
                arabicText = "قُل لِّلْمُؤْمِنِينَ يَغُضُّوا مِنْ أَبْصَارِهِمْ وَيَحْفَظُوا فُرُوجَهُمْ ۚ ذَٰلِكَ أَزْكَىٰ لَهُمْ ۗ إِنَّ اللَّهَ خَبِيرٌ بِمَا يَصْنَعُونَ",
                transliteration = "Qul lil-mu'mineena yaghuddu min absarihim wa yahfazu furujahum; dhalika azka lahum; inna Allaha khabeerun bima yasna'un",
                englishTranslation = "Tell the believing men to lower their gaze and guard their private parts. That is purer for them. Indeed, Allah is Acquainted with what they do.",
                arabicTranslation = "قُل لِّلْمُؤْمِنِينَ يَغُضُّوا مِنْ أَبْصَارِهِمْ وَيَحْفَظُوا فُرُوجَهُمْ ۚ ذَٰلِكَ أَزْكَىٰ لَهُمْ ۗ إِنَّ اللَّهَ خَبِيرٌ بِمَا يَصْنَعُونَ",
                urduTranslation = "مومن مردوں سے کہہ دو کہ اپنی نظریں نیچی رکھیں اور اپنی شرمگاہوں کی حفاظت کریں۔ یہ ان کے لیے زیادہ پاک ہے۔ بے شک اللہ ان کے اعمال سے باخبر ہے۔",
                frenchTranslation = "Dis aux croyants de baisser leurs regards et de garder leur chasteté. C'est plus pur pour eux. Allah est, certes, Parfaitement Connaisseur de ce qu'ils font.",
                indonesianTranslation = "Katakanlah kepada orang laki-laki yang beriman: \"Hendaklah mereka menahan pandangannya, dan memelihara kemaluannya; yang demikian itu adalah lebih suci bagi mereka, sesungguhnya Allah Maha Mengetahui apa yang mereka perbuat.\"",
                category = BlockingCategory.EXPLICIT_CONTENT,
                context = "This verse emphasizes the importance of lowering one's gaze and maintaining modesty as a means of spiritual purification.",
                reflection = "Reflect on how protecting your gaze protects your heart and brings you closer to Allah's pleasure.",
                priority = 5,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            
            // Adult Entertainment - Avoiding Zina
            QuranicVerseEntity(
                id = "17_32",
                surahName = "Al-Isra",
                surahNumber = 17,
                verseNumber = 32,
                arabicText = "وَلَا تَقْرَبُوا الزِّنَا ۖ إِنَّهُ كَانَ فَاحِشَةً وَسَاءَ سَبِيلًا",
                transliteration = "Wa la taqrabu az-zina; innahu kana fahishatan wa saa'a sabeela",
                englishTranslation = "And do not approach unlawful sexual intercourse. Indeed, it is ever an immorality and is evil as a way.",
                arabicTranslation = "وَلَا تَقْرَبُوا الزِّنَا ۖ إِنَّهُ كَانَ فَاحِشَةً وَسَاءَ سَبِيلًا",
                urduTranslation = "اور زنا کے قریب بھی نہ جاؤ۔ بے شک یہ بے حیائی ہے اور برا راستہ ہے۔",
                frenchTranslation = "Et n'approchez point la fornication. En vérité, c'est une turpitude et quel mauvais chemin!",
                indonesianTranslation = "Dan janganlah kamu mendekati zina; sesungguhnya zina itu adalah suatu perbuatan yang keji dan suatu jalan yang buruk.",
                category = BlockingCategory.ADULT_ENTERTAINMENT,
                context = "This verse warns against even approaching situations that may lead to unlawful acts.",
                reflection = "Consider how avoiding the paths that lead to sin protects your soul and maintains your relationship with Allah.",
                priority = 5,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            
            // Gambling - Avoiding Games of Chance
            QuranicVerseEntity(
                id = "5_90",
                surahName = "Al-Ma'idah",
                surahNumber = 5,
                verseNumber = 90,
                arabicText = "يَا أَيُّهَا الَّذِينَ آمَنُوا إِنَّمَا الْخَمْرُ وَالْمَيْسِرُ وَالْأَنصَابُ وَالْأَزْلَامُ رِجْسٌ مِّنْ عَمَلِ الشَّيْطَانِ فَاجْتَنِبُوهُ لَعَلَّكُمْ تُفْلِحُونَ",
                transliteration = "Ya ayyuha alladheena amanu innama al-khamru wa al-maysiru wa al-ansabu wa al-azlamu rijsun min 'amali ash-shaytani fajtanibuh la'allakum tuflihun",
                englishTranslation = "O you who believe! Intoxicants, gambling, stone altars and divining arrows are abominations devised by Satan. Avoid them, so that you may be successful.",
                arabicTranslation = "يَا أَيُّهَا الَّذِينَ آمَنُوا إِنَّمَا الْخَمْرُ وَالْمَيْسِرُ وَالْأَنصَابُ وَالْأَزْلَامُ رِجْسٌ مِّنْ عَمَلِ الشَّيْطَانِ فَاجْتَنِبُوهُ لَعَلَّكُمْ تُفْلِحُونَ",
                urduTranslation = "اے ایمان والو! شراب اور جوا اور بت اور پانسے یہ سب گندے شیطانی کام ہیں، سو ان سے بچو تاکہ تم فلاح پاؤ۔",
                frenchTranslation = "Ô les croyants! Le vin, le jeu de hasard, les pierres dressées, les flèches de divination ne sont qu'une abomination, œuvre du Diable. Écartez-vous en, afin que vous réussissiez.",
                indonesianTranslation = "Hai orang-orang yang beriman, sesungguhnya (meminum) khamar, berjudi, (berkorban untuk) berhala, mengundi nasib dengan panah, adalah termasuk perbuatan syaitan. Maka jauhilah perbuatan-perbuatan itu agar kamu mendapat keberuntungan.",
                category = BlockingCategory.GAMBLING,
                context = "This verse clearly prohibits gambling as a work of Satan that should be avoided.",
                reflection = "Think about how avoiding these harmful activities leads to true success and Allah's blessings.",
                priority = 5,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            
            // General Guidance - Taqwa
            QuranicVerseEntity(
                id = "2_197",
                surahName = "Al-Baqarah",
                surahNumber = 2,
                verseNumber = 197,
                arabicText = "وَتَزَوَّدُوا فَإِنَّ خَيْرَ الزَّادِ التَّقْوَىٰ ۚ وَاتَّقُونِ يَا أُولِي الْأَلْبَابِ",
                transliteration = "Wa tazawwadu fa inna khayra az-zadi at-taqwa; wa attaquni ya uli al-albab",
                englishTranslation = "And take provisions, but indeed, the best provision is fear of Allah. So fear Me, O you of understanding.",
                arabicTranslation = "وَتَزَوَّدُوا فَإِنَّ خَيْرَ الزَّادِ التَّقْوَىٰ ۚ وَاتَّقُونِ يَا أُولِي الْأَلْبَابِ",
                urduTranslation = "اور سفر کا سامان لے لو، اور بہترین سامان تقویٰ ہے۔ اور اے عقل والو! میرا تقویٰ اختیار کرو۔",
                frenchTranslation = "Et prenez vos provisions; mais vraiment la meilleure provision est la piété. Et redoutez-Moi, ô doués d'intelligence!",
                indonesianTranslation = "Berbekallah, dan sesungguhnya sebaik-baik bekal adalah takwa dan bertakwalah kepada-Ku hai orang-orang yang berakal.",
                category = BlockingCategory.SUSPICIOUS_CONTENT,
                context = "This verse reminds us that taqwa (God-consciousness) is the best provision for any journey.",
                reflection = "Consider how developing taqwa helps you make better choices in all aspects of life.",
                priority = 3,
                createdAt = currentTime,
                updatedAt = currentTime
            ),
            
            // Inappropriate Imagery - Purification
            QuranicVerseEntity(
                id = "91_9",
                surahName = "Ash-Shams",
                surahNumber = 91,
                verseNumber = 9,
                arabicText = "قَدْ أَفْلَحَ مَن زَكَّاهَا",
                transliteration = "Qad aflaha man zakkaha",
                englishTranslation = "He has succeeded who purifies it [his soul].",
                arabicTranslation = "قَدْ أَفْلَحَ مَن زَكَّاهَا",
                urduTranslation = "بے شک اس نے فلاح پائی جس نے اپنے نفس کو پاک کیا۔",
                frenchTranslation = "A réussi, certes, celui qui la purifie.",
                indonesianTranslation = "Sesungguhnya beruntunglah orang yang mensucikan jiwa itu.",
                category = BlockingCategory.INAPPROPRIATE_IMAGERY,
                context = "This verse emphasizes the success that comes from purifying one's soul.",
                reflection = "Reflect on how purifying your soul through avoiding harmful content brings true success.",
                priority = 4,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )
        
        quranicVerseDao.insertVerses(defaultVerses)
    }
    
    private fun createBlockedSite(
        domain: String,
        category: BlockingCategory,
        confidence: Float,
        timestamp: Long
    ): BlockedSiteEntity {
        return BlockedSiteEntity(
            domainHash = hashDomain(domain),
            pattern = domain,
            category = category,
            confidence = confidence,
            lastUpdated = timestamp,
            isRegex = false,
            source = "default"
        )
    }
    
    private fun createRegexBlockedSite(
        pattern: String,
        category: BlockingCategory,
        confidence: Float,
        timestamp: Long
    ): BlockedSiteEntity {
        return BlockedSiteEntity(
            domainHash = hashDomain(pattern),
            pattern = pattern,
            category = category,
            confidence = confidence,
            lastUpdated = timestamp,
            isRegex = true,
            source = "default"
        )
    }
    
    private fun hashDomain(domain: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(domain.lowercase().toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}