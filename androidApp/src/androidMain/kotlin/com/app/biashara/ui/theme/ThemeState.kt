package com.app.biashara.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped singleton for dark mode preference.
 * MainActivity observes this; SettingsScreen writes to it.
 * Survives across recompositions — lives for the process lifetime.
 */
object ThemeState {
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }
}
