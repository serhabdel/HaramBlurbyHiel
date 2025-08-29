package com.hieltech.haramblur.data

import androidx.compose.runtime.Immutable

/**
 * Represents the time of day for dhikr display
 */
enum class DhikrTime(val displayName: String) {
    MORNING("Morning"),
    EVENING("Evening"),
    ANYTIME("Anytime")
}

/**
 * Individual dhikr remembrance
 */
@Immutable
data class Dhikr(
    val id: String,
    val arabicText: String,
    val transliteration: String,
    val englishTranslation: String,
    val time: DhikrTime,
    val estimatedReadingTimeSeconds: Int = 8,
    val category: String = "General"
)

/**
 * Settings for dhikr display
 */
@Immutable
data class DhikrSettings(
    val enabled: Boolean = true,
    val morningEnabled: Boolean = true,
    val eveningEnabled: Boolean = true,
    val anytimeEnabled: Boolean = true,
    val morningStartTime: Int = 5, // 5 AM in 24-hour format
    val morningEndTime: Int = 10, // 10 AM in 24-hour format
    val eveningStartTime: Int = 17, // 5 PM in 24-hour format
    val eveningEndTime: Int = 22, // 10 PM in 24-hour format
    val intervalMinutes: Int = 60, // Show dhikr every 60 minutes during active times
    val displayDurationSeconds: Int = 10,
    val displayPosition: DhikrPosition = DhikrPosition.TOP_RIGHT,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true,
    val animationEnabled: Boolean = true,
    val soundEnabled: Boolean = false
)

/**
 * Position for dhikr overlay
 */
enum class DhikrPosition(val displayName: String) {
    TOP_RIGHT("Top Right"),
    TOP_LEFT("Top Left"),
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_LEFT("Bottom Left"),
    CENTER("Center")
}

/**
 * Data source for dhikr data
 */
object DhikrDataSource {
    
    val morningDhikr = listOf(
        Dhikr(
            id = "morning_1",
            arabicText = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ للهِ، وَالْحَمْدُ للهِ، لاَ إِلَهَ إِلاَّ اللهُ وَحْدَهُ لاَ شَرِيكَ لَهُ",
            transliteration = "Asbaḥnā wa aṣbaḥa al-mulku lillāh, wa al-ḥamdu lillāh, lā ilāha illā Allāhu waḥdahu lā sharīka lahu",
            englishTranslation = "We have reached the morning and at this very time unto Allah belongs all sovereignty, and all praise is for Allah. None has the right to be worshipped except Allah, alone, without partner.",
            time = DhikrTime.MORNING,
            estimatedReadingTimeSeconds = 12,
            category = "Morning Remembrance"
        ),
        Dhikr(
            id = "morning_2",
            arabicText = "اللَّهُمَّ أَنْتَ رَبِّي لا إِلَهَ إِلا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ",
            transliteration = "Allāhumma anta rabbī lā ilāha illā anta, khalaqtanī wa anā ʿabduka",
            englishTranslation = "O Allah, You are my Lord, none has the right to be worshipped except You, You created me and I am Your servant.",
            time = DhikrTime.MORNING,
            estimatedReadingTimeSeconds = 8,
            category = "Morning Remembrance"
        ),
        Dhikr(
            id = "morning_3",
            arabicText = "رَضِيتُ بِاللهِ رَبًّا، وَبِالإِسْلامِ دِينًا، وَبِمُحَمَّدٍ صلى الله عليه وسلم رَسُولاً",
            transliteration = "Raḍītu billāhi rabban, wa bil-Islāmi dīnan, wa bi Muḥammadin rasūlan",
            englishTranslation = "I am pleased with Allah as a Lord, and Islam as a religion, and Muhammad (ﷺ) as a Messenger.",
            time = DhikrTime.MORNING,
            estimatedReadingTimeSeconds = 9,
            category = "Morning Remembrance"
        ),
        Dhikr(
            id = "morning_4",
            arabicText = "سُبْحَانَ اللهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ",
            transliteration = "Subḥān Allāhi wa biḥamdih, ʿadada khalqih, wa riḍā nafsih, wa zinata ʿarshih, wa midāda kalimātih",
            englishTranslation = "Exalted is Allah and in His praise, by the number of His creation, by His pleasure, by the weight of His throne, and by the ink of His words.",
            time = DhikrTime.MORNING,
            estimatedReadingTimeSeconds = 11,
            category = "Morning Remembrance"
        )
    )
    
    val eveningDhikr = listOf(
        Dhikr(
            id = "evening_1",
            arabicText = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ للهِ، وَالْحَمْدُ للهِ، لاَ إِلَهَ إِلاَّ اللهُ وَحْدَهُ لاَ شَرِيكَ لَهُ",
            transliteration = "Amsaynā wa amsā al-mulku lillāh, wa al-ḥamdu lillāh, lā ilāha illā Allāhu waḥdahu lā sharīka lahu",
            englishTranslation = "We have reached the evening and at this very time unto Allah belongs all sovereignty, and all praise is for Allah. None has the right to be worshipped except Allah, alone, without partner.",
            time = DhikrTime.EVENING,
            estimatedReadingTimeSeconds = 12,
            category = "Evening Remembrance"
        ),
        Dhikr(
            id = "evening_2",
            arabicText = "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ",
            transliteration = "Allāhumma bika amsaynā, wa bika aṣbaḥnā, wa bika naḥyā, wa bika namūtu, wa ilayka an-nushūr",
            englishTranslation = "O Allah, by Your leave we have reached the evening and by Your leave we have reached the morning, by Your leave we live and die, and unto You is the resurrection.",
            time = DhikrTime.EVENING,
            estimatedReadingTimeSeconds = 10,
            category = "Evening Remembrance"
        ),
        Dhikr(
            id = "evening_3",
            arabicText = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ عِبَادَتِكَ",
            transliteration = "Allāhumma aʿinnī ʿalā dhikrika, wa shukrika, wa ḥusni ʿibādatika",
            englishTranslation = "O Allah, help me remember You, to be grateful to You, and to worship You in an excellent manner.",
            time = DhikrTime.EVENING,
            estimatedReadingTimeSeconds = 8,
            category = "Evening Remembrance"
        ),
        Dhikr(
            id = "evening_4",
            arabicText = "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي",
            transliteration = "Allāhumma ʿāfinī fī badanī, Allāhumma ʿāfinī fī samʿī, Allāhumma ʿāfinī fī baṣarī",
            englishTranslation = "O Allah, grant me health in my body. O Allah, grant me health in my hearing. O Allah, grant me health in my sight.",
            time = DhikrTime.EVENING,
            estimatedReadingTimeSeconds = 9,
            category = "Evening Remembrance"
        )
    )
    
    val anytimeDhikr = listOf(
        Dhikr(
            id = "anytime_1",
            arabicText = "سُبْحَانَ اللهِ وَبِحَمْدِهِ",
            transliteration = "Subḥān Allāhi wa biḥamdih",
            englishTranslation = "Exalted is Allah and in His praise.",
            time = DhikrTime.ANYTIME,
            estimatedReadingTimeSeconds = 5,
            category = "General Remembrance"
        ),
        Dhikr(
            id = "anytime_2",
            arabicText = "لاَ إِلَهَ إِلاَّ اللهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            transliteration = "Lā ilāha illā Allāhu waḥdahu lā sharīka lahu, lahu al-mulku wa lahu al-ḥamdu wa huwa ʿalā kulli shay'in qadīr",
            englishTranslation = "None has the right to be worshipped except Allah, alone, without partner, to Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            time = DhikrTime.ANYTIME,
            estimatedReadingTimeSeconds = 10,
            category = "General Remembrance"
        ),
        Dhikr(
            id = "anytime_3",
            arabicText = "الْحَمْدُ للهِ رَبِّ الْعَالَمِينَ",
            transliteration = "Al-ḥamdu lillāhi rabbi al-ʿālamīn",
            englishTranslation = "All praise is for Allah, Lord of the worlds.",
            time = DhikrTime.ANYTIME,
            estimatedReadingTimeSeconds = 6,
            category = "General Remembrance"
        ),
        Dhikr(
            id = "anytime_4",
            arabicText = "أَسْتَغْفِرُ اللهَ الَّذِي لاَ إِلَهَ إِلاَّ هُوَ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ",
            transliteration = "Astaghfiru Allāha alladhī lā ilāha illā huwa al-ḥayyu al-qayyūmu wa atūbu ilayh",
            englishTranslation = "I seek forgiveness of Allah, besides whom, none has the right to be worshipped except He, The Ever Living, The Self-Subsisting and Supporter of all, and I turn to Him in repentance.",
            time = DhikrTime.ANYTIME,
            estimatedReadingTimeSeconds = 11,
            category = "Seeking Forgiveness"
        ),
        Dhikr(
            id = "anytime_5",
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            transliteration = "Rabbanā ātinā fī ad-dunyā ḥasanatan wa fī al-ākhirati ḥasanatan wa qinā ʿadhāb an-nār",
            englishTranslation = "Our Lord, give us in this world [that which is] good and in the next world [that which is] good and protect us from the punishment of the Fire.",
            time = DhikrTime.ANYTIME,
            estimatedReadingTimeSeconds = 9,
            category = "Du'a"
        )
    )
    
    fun getAllDhikr(): List<Dhikr> {
        return morningDhikr + eveningDhikr + anytimeDhikr
    }
    
    fun getDhikrByTime(time: DhikrTime): List<Dhikr> {
        return when (time) {
            DhikrTime.MORNING -> morningDhikr
            DhikrTime.EVENING -> eveningDhikr
            DhikrTime.ANYTIME -> anytimeDhikr
        }
    }
    
    fun getDhikrById(id: String): Dhikr? {
        return getAllDhikr().find { it.id == id }
    }
    
    fun getRandomDhikr(time: DhikrTime): Dhikr? {
        val dhikrList = getDhikrByTime(time)
        return if (dhikrList.isNotEmpty()) dhikrList.random() else null
    }
}