package com.app.biashara.data.offline
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
actual fun nativeNetworkEngine(): HttpClientEngine = Android.create()
