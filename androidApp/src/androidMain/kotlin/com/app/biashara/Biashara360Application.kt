package com.app.biashara

import android.app.Application
import com.app.biashara.di.coreModule
import com.app.biashara.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class Biashara360Application : Application() {
    override fun onCreate() {
        super.onCreate()
        com.app.biashara.data.remote.CLIENT_PLATFORM = "android"
        com.app.biashara.data.remote.SESSION_IDLE_TIMEOUT_SECONDS = BuildConfig.SESSION_IDLE_TIMEOUT_SECONDS
        // BASE_URL defaults to "https://api.biashara360.co.ke/v1" in shared ApiClient
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@Biashara360Application)
            modules(coreModule, platformModule)
        }
    }
}
