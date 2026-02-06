package com.hieltech.haramblur.ui

/**
 * Centralized navigation routes to avoid duplication and typos
 */
object NavRoutes {
    const val PERMISSION_WIZARD = "permission_wizard"
    const val HOME = "home"
    const val BLOCK_APPS_SITES = "block_apps_sites"
    const val DHIKR = "dhikr"
    const val PRAYER = "prayer"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val LOGS = "logs"
    const val DEBUG = "debug"
    const val SUPPORT = "support"
    const val DIAGNOSTICS = "diagnostics"

    /**
     * Primary routes that show bottom navigation
     */
    val PRIMARY_ROUTES = listOf(HOME, BLOCK_APPS_SITES, PRAYER, DHIKR, SETTINGS)
}
