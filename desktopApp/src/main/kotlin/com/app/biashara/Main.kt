package com.app.biashara

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.app.biashara.di.coreModule
import com.app.biashara.di.platformModule
import com.app.biashara.ui.Biashara360DesktopApp
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(coreModule, platformModule)
    }

    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Biashara360 — Business Management",
        state = windowState
    ) {
        Biashara360DesktopApp()
    }
}
