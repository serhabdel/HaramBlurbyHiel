package com.hieltech.haramblur.ui

/**
 * Centralized navigation routes to avoid duplication and typos
 */
object NavRoutes {
    const val PERMISSION_WIZARD = "permission_wizard"
    const val HOME = "home"
    const val BLOCK_APPS_SITES = "block_apps_sites"
    const val SETTINGS = "settings"
    const val LOGS = "logs"
    const val DEBUG = "debug"
    const val SUPPORT = "support"

    /**
     * Primary routes that show bottom navigation
     */
    val PRIMARY_ROUTES = listOf(HOME, BLOCK_APPS_SITES, SETTINGS)
}
