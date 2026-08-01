package com.app.biashara

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.unit.dp
import com.app.biashara.di.coreModule
import com.app.biashara.di.platformModule
import com.app.biashara.ui.Biashara360DesktopApp
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.app.biashara.ui.DesktopNavigationViewModel

fun main() {
    com.app.biashara.data.remote.CLIENT_PLATFORM = "desktop"
    System.getenv("SESSION_IDLE_TIMEOUT_SECONDS")?.toLongOrNull()?.let {
        com.app.biashara.data.remote.SESSION_IDLE_TIMEOUT_SECONDS = it
    }
    // Endpoint overrides are deployment configuration, not an end-user setting.
    com.app.biashara.data.remote.BASE_URL = System.getenv("BASE_URL")
        ?: "https://sddgmezqj2.us-east-1.awsapprunner.com/v1"

    // Guard against double-initialization (e.g. on hot-restart in dev)
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(coreModule, platformModule, module {
                single { DesktopNavigationViewModel() }
            })
        }
    }

    application {
        val windowState = rememberWindowState(
            placement = WindowPlacement.Maximized,
            width = 1280.dp,
            height = 800.dp
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "Biashara360 — Business Management",
            state = windowState
        ) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                window.minimumSize = java.awt.Dimension(1024, 680)
            }
            Biashara360DesktopApp()
        }
    }
}
