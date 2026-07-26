package com.app.biashara

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.app.biashara.ui.Biashara360App
import com.app.biashara.ui.theme.Biashara360Theme
import com.app.biashara.ui.theme.ThemeState
import com.app.biashara.ui.darkModeEnabled

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeState.setDarkMode(darkModeEnabled())
        enableEdgeToEdge()
        setContent {
            val isDarkMode by ThemeState.isDarkMode.collectAsState()
            Biashara360Theme(darkTheme = isDarkMode) {
                Biashara360App()
            }
        }
    }
}
