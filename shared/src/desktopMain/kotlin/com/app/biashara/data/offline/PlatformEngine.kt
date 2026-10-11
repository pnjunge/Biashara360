package com.app.biashara.data.offline
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java
actual fun nativeNetworkEngine(): HttpClientEngine = Java.create()
