package com.app.biashara.di

import com.app.biashara.data.local.DatabaseDriverFactory
import com.app.biashara.data.local.DesktopPreferencesTokenStorage
import com.app.biashara.data.remote.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory() }
    single<TokenStorage> { DesktopPreferencesTokenStorage() }
}
