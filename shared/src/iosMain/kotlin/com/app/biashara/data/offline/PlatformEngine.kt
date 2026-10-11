package com.app.biashara.data.offline
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
actual fun nativeNetworkEngine(): HttpClientEngine = Darwin.create()
