package com.hieltech.haramblur.data

import androidx.compose.runtime.Immutable
import androidx.annotation.StringRes
import com.hieltech.haramblur.R
import android.content.Context

/**
 * Represents the time of day for dhikr display
 */
enum class DhikrTime(
    @StringRes val displayNameResId: Int
) {
    MORNING(R.string.dhikr_time_morning),
    EVENING(R.string.dhikr_time_evening),
    ANYTIME(R.string.dhikr_time_anytime),
    AFTER_PRAYER(R.string.dhikr_time_after_prayer);
    
    companion object {
        fun getByDisplayName(displayName: String): DhikrTime? {
            return values().find { it.name == displayName }
        }
    }
}

/**
 * Category for dhikr grouping
 */
enum class DhikrCategory(
    @StringRes val displayNameResId: Int
) {
    MORNING_REMEMBRANCE(R.string.dhikr_category_morning_remembrance),
    EVENING_REMEMBRANCE(R.string.dhikr_category_evening_remembrance),
    AFTER_PRAYER(R.string.dhikr_category_after_prayer),
    GENERAL(R.string.dhikr_category_general),
    TASBIH(R.string.dhikr_category_tasbih),
    ISTIGHFAR(R.string.dhikr_category_istighfar),
    SALAWAT(R.string.dhikr_category_salawat),
    DUA(R.string.dhikr_category_dua);
    
    companion object {
        fun getByDisplayName(displayName: String): DhikrCategory? {
            return values().find { it.name == displayName }
        }
    }
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
    val category: DhikrCategory = DhikrCategory.GENERAL
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
    val intervalMinutes: Int = 5, // Show dhikr every 5 minutes during active times
    val displayDurationSeconds: Int = 30,
    val displayPosition: DhikrPosition = DhikrPosition.TOP_RIGHT,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true,
    val animationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val sleepStartMinutes: Int = 1350, // 22:30 PM in minutes-of-day format
    val sleepEndMinutes: Int = 390     // 6:30 AM in minutes-of-day format
)

/**
 * Position for dhikr overlay
 */
enum class DhikrPosition(
    @StringRes val displayNameResId: Int
) {
    TOP_RIGHT(R.string.position_top_right),
    TOP_LEFT(R.string.position_top_left),
    BOTTOM_RIGHT(R.string.position_bottom_right),
    BOTTOM_LEFT(R.string.position_bottom_left),
    CENTER(R.string.position_center);
    
    companion object {
        fun getByDisplayName(displayName: String): DhikrPosition? {
            return values().find { it.name == displayName }
        }
    }
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
            category = DhikrCategory.MORNING_REMEMBRANCE
        ),
        Dhikr(
            id = "morning_2",
            arabicText = "اللَّهُمَّ أَنْتَ رَبِّي لا إِلَهَ إِلا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ",
            transliteration = "Allāhumma anta rabbī lā ilāha illā anta, khalaqtanī wa anā ʿabduka",
            englishTranslation = "O Allah, You are my Lord, none has the right to be worshipped except You, You created me and I am Your servant.",
            time = DhikrTime.MORNING,
            estimatedReadingTimeSeconds = 8,
            category = DhikrCategory.MORNING_REMEMBRANCE
        ),
        Dhikr(
            id = "morning_3",
            arabicText = "رَضِيتُ بِاللهِ رَبًّا، وَبِالإِسْلامِ دِينًا، وَبِمُحَمَّدٍ صلى الله عليه وسلم رَسُولاً",
            transliteration = "Raḍītu billāhi rabban, wa bil-Islāmi dīnan, wa bi Muḥammadin rasūlan",
            englishTranslation = "I am pleased with Allah as a Lord, and Islam as a religion, and Muhammad (ﷺ) as a Messenger.",
            time = DhikrTime.MORNING,
            estimatedReadingTimeSeconds = 9,
            category = DhikrCategory.MORNING_REMEMBRANCE
        ),
        Dhikr(
            id = "morning_4",
            arabicText = "سُبْحَانَ اللهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ",
            transliteration = "Subḥān Allāhi wa biḥamdih, ʿadada khalqih, wa riḍā nafsih, wa zinata ʿarshih, wa midāda kalimātih",
            englishTranslation = "Exalted is Allah and in His praise, by the number of His creation, by His pleasure, by the weight of His throne, and by the ink of His words.",
            time = DhikrTime.MORNING,
            estimatedReadingTimeSeconds = 11,
            category = DhikrCategory.MORNING_REMEMBRANCE
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
            category = DhikrCategory.EVENING_REMEMBRANCE
        ),
        Dhikr(
            id = "evening_2",
            arabicText = "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ",
            transliteration = "Allāhumma bika amsaynā, wa bika aṣbaḥnā, wa bika naḥyā, wa bika namūtu, wa ilayka an-nushūr",
            englishTranslation = "O Allah, by Your leave we have reached the evening and by Your leave we have reached the morning, by Your leave we live and die, and unto You is the resurrection.",
            time = DhikrTime.EVENING,
            estimatedReadingTimeSeconds = 10,
            category = DhikrCategory.EVENING_REMEMBRANCE
        ),
        Dhikr(
            id = "evening_3",
            arabicText = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ عِبَادَتِكَ",
            transliteration = "Allāhumma aʿinnī ʿalā dhikrika, wa shukrika, wa ḥusni ʿibādatika",
            englishTranslation = "O Allah, help me remember You, to be grateful to You, and to worship You in an excellent manner.",
            time = DhikrTime.EVENING,
            estimatedReadingTimeSeconds = 8,
            category = DhikrCategory.EVENING_REMEMBRANCE
        ),
        Dhikr(
            id = "evening_4",
            arabicText = "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي",
            transliteration = "Allāhumma ʿāfinī fī badanī, Allāhumma ʿāfinī fī samʿī, Allāhumma ʿāfinī fī baṣarī",
            englishTranslation = "O Allah, grant me health in my body. O Allah, grant me health in my hearing. O Allah, grant me health in my sight.",
            time = DhikrTime.EVENING,
            estimatedReadingTimeSeconds = 9,
            category = DhikrCategory.EVENING_REMEMBRANCE
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
            category = DhikrCategory.GENERAL
        ),
        Dhikr(
            id = "anytime_2",
            arabicText = "لاَ إِلَهَ إِلاَّ اللهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            transliteration = "Lā ilāha illā Allāhu waḥdahu lā sharīka lahu, lahu al-mulku wa lahu al-ḥamdu wa huwa ʿalā kulli shay'in qadīr",
            englishTranslation = "None has the right to be worshipped except Allah, alone, without partner, to Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            time = DhikrTime.ANYTIME,
            estimatedReadingTimeSeconds = 10,
            category = DhikrCategory.GENERAL
        ),
        Dhikr(
            id = "anytime_3",
            arabicText = "الْحَمْدُ للهِ رَبِّ الْعَالَمِينَ",
            transliteration = "Al-ḥamdu lillāhi rabbi al-ʿālamīn",
            englishTranslation = "All praise is for Allah, Lord of the worlds.",
            time = DhikrTime.ANYTIME,
            estimatedReadingTimeSeconds = 6,
            category = DhikrCategory.GENERAL
        ),
        Dhikr(
            id = "anytime_4",
            arabicText = "أَسْتَغْفِرُ اللهَ الَّذِي لاَ إِلَهَ إِلاَّ هُوَ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ",
            transliteration = "Astaghfiru Allāha alladhī lā ilāha illā huwa al-ḥayyu al-qayyūmu wa atūbu ilayh",
            englishTranslation = "I seek forgiveness of Allah, besides whom, none has the right to be worshipped except He, The Ever Living, The Self-Subsisting and Supporter of all, and I turn to Him in repentance.",
            time = DhikrTime.ANYTIME,
            estimatedReadingTimeSeconds = 11,
            category = DhikrCategory.ISTIGHFAR
        ),
        Dhikr(
            id = "anytime_5",
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            transliteration = "Rabbanā ātinā fī ad-dunyā ḥasanatan wa fī al-ākhirati ḥasanatan wa qinā ʿadhāb an-nār",
            englishTranslation = "Our Lord, give us in this world [that which is] good and in the next world [that which is] good and protect us from the punishment of the Fire.",
            time = DhikrTime.ANYTIME,
            estimatedReadingTimeSeconds = 9,
            category = DhikrCategory.DUA
        )
    )
    
    /**
     * After-prayer dhikr (Adhkar after Salah)
     * These are the recommended dhikr to recite after each prayer
     */
    val afterPrayerDhikr = listOf(
        Dhikr(
            id = "after_prayer_1",
            arabicText = "أَسْتَغْفِرُ اللهَ",
            transliteration = "Astaghfiru Allah",
            englishTranslation = "I seek forgiveness from Allah.",
            time = DhikrTime.AFTER_PRAYER,
            estimatedReadingTimeSeconds = 3,
            category = DhikrCategory.AFTER_PRAYER
        ),
        Dhikr(
            id = "after_prayer_2",
            arabicText = "اللَّهُمَّ أَنْتَ السَّلاَمُ وَمِنْكَ السَّلاَمُ، تَبَارَكْتَ يَا ذَا الْجَلاَلِ وَالإِكْرَامِ",
            transliteration = "Allāhumma anta as-salām wa minka as-salām, tabārakta yā dhal-jalāli wal-ikrām",
            englishTranslation = "O Allah, You are Peace and from You comes peace. Blessed are You, O Owner of majesty and honor.",
            time = DhikrTime.AFTER_PRAYER,
            estimatedReadingTimeSeconds = 10,
            category = DhikrCategory.AFTER_PRAYER
        ),
        Dhikr(
            id = "after_prayer_3",
            arabicText = "لاَ إِلَهَ إِلاَّ اللهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            transliteration = "Lā ilāha illā Allāhu waḥdahu lā sharīka lahu, lahu al-mulku wa lahu al-ḥamdu wa huwa ʿalā kulli shay'in qadīr",
            englishTranslation = "None has the right to be worshipped except Allah, alone, without partner, to Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            time = DhikrTime.AFTER_PRAYER,
            estimatedReadingTimeSeconds = 12,
            category = DhikrCategory.AFTER_PRAYER
        ),
        Dhikr(
            id = "after_prayer_4",
            arabicText = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
            transliteration = "Allāhumma aʿinnī ʿalā dhikrika wa shukrika wa ḥusni ʿibādatika",
            englishTranslation = "O Allah, help me to remember You, to thank You, and to worship You in the best manner.",
            time = DhikrTime.AFTER_PRAYER,
            estimatedReadingTimeSeconds = 8,
            category = DhikrCategory.AFTER_PRAYER
        )
    )
    
    /**
     * Tasbih dhikr for counter (33 times each)
     */
    val tasbihDhikr = listOf(
        Dhikr(
            id = "tasbih_subhanallah",
            arabicText = "سُبْحَانَ اللهِ",
            transliteration = "Subḥān Allāh",
            englishTranslation = "Glory be to Allah",
            time = DhikrTime.AFTER_PRAYER,
            estimatedReadingTimeSeconds = 2,
            category = DhikrCategory.TASBIH
        ),
        Dhikr(
            id = "tasbih_alhamdulillah",
            arabicText = "الْحَمْدُ للهِ",
            transliteration = "Al-ḥamdu lillāh",
            englishTranslation = "All praise is for Allah",
            time = DhikrTime.AFTER_PRAYER,
            estimatedReadingTimeSeconds = 2,
            category = DhikrCategory.TASBIH
        ),
        Dhikr(
            id = "tasbih_allahuakbar",
            arabicText = "اللهُ أَكْبَرُ",
            transliteration = "Allāhu Akbar",
            englishTranslation = "Allah is the Greatest",
            time = DhikrTime.AFTER_PRAYER,
            estimatedReadingTimeSeconds = 2,
            category = DhikrCategory.TASBIH
        )
    )
    
    fun getAllDhikr(): List<Dhikr> {
        return morningDhikr + eveningDhikr + anytimeDhikr + afterPrayerDhikr + tasbihDhikr
    }
    
    fun getDhikrByTime(time: DhikrTime): List<Dhikr> {
        return when (time) {
            DhikrTime.MORNING -> morningDhikr
            DhikrTime.EVENING -> eveningDhikr
            DhikrTime.ANYTIME -> anytimeDhikr
            DhikrTime.AFTER_PRAYER -> afterPrayerDhikr + tasbihDhikr
        }
    }
    
    // afterPrayerDhikr and tasbihDhikr are already accessible as properties
    
    fun getDhikrById(id: String): Dhikr? {
        return getAllDhikr().find { it.id == id }
    }
    
    fun getRandomDhikr(time: DhikrTime): Dhikr? {
        val dhikrList = getDhikrByTime(time)
        return if (dhikrList.isNotEmpty()) dhikrList.random() else null
    }
}

/**
 * Tasbih counter state for tracking dhikr counts
 */
@Immutable
data class TasbihCounter(
    val dhikrId: String,
    val currentCount: Int = 0,
    val targetCount: Int = 33,
    val totalCompleted: Int = 0
) {
    val isComplete: Boolean get() = currentCount >= targetCount
    val progress: Float get() = if (targetCount > 0) currentCount.toFloat() / targetCount else 0f
}

/**
 * Daily dhikr progress tracking
 */
@Immutable
data class DailyDhikrProgress(
    val date: String,
    val morningCompleted: Boolean = false,
    val eveningCompleted: Boolean = false,
    val afterPrayerCount: Int = 0,
    val tasbihSets: Int = 0, // Number of complete 33x3 sets
    val totalDhikrCount: Int = 0
)

/**
 * Extension function to get localized display name for a DhikrCategory
 */
fun DhikrCategory.getDisplayName(context: android.content.Context): String {
    return context.getString(this.displayNameResId)
}

/**
 * Extension property to get localized display name (requires Context)
 */
val DhikrCategory.localizedName: String
    get() = throw UnsupportedOperationException("Use getDisplayName(Context) instead")

/**
 * Extension function to get localized display name for a DhikrTime
 */
fun DhikrTime.getDisplayName(context: Context): String {
    return context.getString(this.displayNameResId)
}

/**
 * Extension property to get localized display name for DhikrTime (requires Context)
 */
val DhikrTime.localizedName: String
    get() = throw UnsupportedOperationException("Use getDisplayName(Context) instead")

/**
 * Extension function to get localized display name for a DhikrPosition
 */
fun DhikrPosition.getDisplayName(context: Context): String {
    return context.getString(this.displayNameResId)
}

/**
 * Extension property to get localized display name for DhikrPosition (requires Context)
 */
val DhikrPosition.localizedName: String
    get() = throw UnsupportedOperationException("Use getDisplayName(Context) instead")