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
        val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            com.app.biashara.data.remote.BASE_URL = "http://10.0.3.2:8081/v1"
        }
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@Biashara360Application)
            modules(coreModule, platformModule)
        }
    }
}
