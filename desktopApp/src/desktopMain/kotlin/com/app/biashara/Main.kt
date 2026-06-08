package com.app.biashara

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.app.biashara.di.coreModule
import com.app.biashara.di.platformModule
import com.app.biashara.ui.Biashara360DesktopApp
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.app.biashara.ui.DesktopNavigationViewModel

fun main() {
    // Resolve BASE_URL before Koin starts so all HTTP clients use the correct endpoint
    val savedUrl = runCatching {
        val configFile = java.io.File(System.getProperty("user.home"), ".biashara360/base_url.txt")
        if (configFile.exists()) configFile.readText().trim() else null
    }.getOrNull()
    com.app.biashara.data.remote.BASE_URL = savedUrl
        ?: System.getenv("BASE_URL")
        ?: "http://localhost:8081/v1"

    // Guard against double-initialization (e.g. on hot-restart in dev)
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(coreModule, platformModule, module {
                single { DesktopNavigationViewModel() }
            })
        }
    }

    application {
        val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)

        Window(
            onCloseRequest = ::exitApplication,
            title = "Biashara360 — Business Management",
            state = windowState
        ) {
            Biashara360DesktopApp()
        }
    }
}

