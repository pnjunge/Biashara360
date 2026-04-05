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
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@Biashara360Application)
            modules(coreModule, platformModule)
        }
    }
}
