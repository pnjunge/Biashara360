package com.app.biashara.ui

import android.content.Context

private const val APP_PREFERENCES = "biashara_app_preferences"
private const val NOTIFICATIONS_ENABLED = "notifications_enabled"
private const val DARK_MODE_ENABLED = "dark_mode_enabled"

fun Context.notificationsEnabled(): Boolean =
    getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(NOTIFICATIONS_ENABLED, true)

fun Context.setNotificationsEnabled(enabled: Boolean) {
    getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .edit().putBoolean(NOTIFICATIONS_ENABLED, enabled).apply()
}

fun Context.darkModeEnabled(): Boolean =
    getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(DARK_MODE_ENABLED, false)

fun Context.setDarkModeEnabled(enabled: Boolean) {
    getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .edit().putBoolean(DARK_MODE_ENABLED, enabled).apply()
}
