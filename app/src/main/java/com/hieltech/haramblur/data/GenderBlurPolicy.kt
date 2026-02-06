package com.hieltech.haramblur.data

import android.util.Log

/**
 * Centralized policy for deriving the *effective* face-blur targets.
 *
 * Goal: when [AppSettings.allowCustomGenderBlur] is false, blur targets are locked to [AppSettings.userGender]
 * (MALE -> blur female only, FEMALE -> blur male only, NOT_SPECIFIED -> blur both).
 */
data class EffectiveBlurTargets(
    val blurMaleFaces: Boolean,
    val blurFemaleFaces: Boolean,
    /** True when we are overriding stored blur toggles due to gender lock. */
    val lockedToGender: Boolean
)

fun AppSettings.effectiveBlurTargets(logTag: String? = null): EffectiveBlurTargets {
    if (allowCustomGenderBlur) {
        return EffectiveBlurTargets(
            blurMaleFaces = blurMaleFaces,
            blurFemaleFaces = blurFemaleFaces,
            lockedToGender = false
        )
    }

    val expected = when (userGender) {
        UserGender.MALE -> false to true
        UserGender.FEMALE -> true to false
        UserGender.NOT_SPECIFIED -> true to true
    }

    val expectedBlurMale = expected.first
    val expectedBlurFemale = expected.second

    if (logTag != null && (blurMaleFaces != expectedBlurMale || blurFemaleFaces != expectedBlurFemale)) {
        // Loud log: something tried to blur the "wrong" gender in locked mode.
        Log.w(
            logTag,
            "🔒 Gender blur lock active (userGender=$userGender, allowCustomGenderBlur=false). " +
                "Forcing blurMaleFaces=$expectedBlurMale, blurFemaleFaces=$expectedBlurFemale " +
                "(stored blurMaleFaces=$blurMaleFaces, blurFemaleFaces=$blurFemaleFaces)"
        )
    }

    return EffectiveBlurTargets(
        blurMaleFaces = expectedBlurMale,
        blurFemaleFaces = expectedBlurFemale,
        lockedToGender = true
    )
}

