package com.app.biashara.data.local

import com.app.biashara.data.remote.TokenStorage
import com.app.biashara.data.remote.SESSION_IDLE_TIMEOUT_MILLIS
import java.util.prefs.Preferences

class DesktopPreferencesTokenStorage : TokenStorage {
    private val prefs: Preferences = Preferences.userRoot().node("com/app/biashara/auth")
    @Volatile private var inMemoryLastActivity: Long = 0L
    @Volatile private var lastSavedActivity: Long = 0L
    @Volatile private var lastTouchThrottle: Long = 0L

    init {
        inMemoryLastActivity = prefs.get(KEY_LAST_ACTIVITY, "0").toLongOrNull() ?: 0L
        lastSavedActivity = inMemoryLastActivity
    }

    override suspend fun getAccessToken(): String? {
        return activeToken(KEY_ACCESS_TOKEN)
    }

    override suspend fun getRefreshToken(): String? {
        return activeToken(KEY_REFRESH_TOKEN)
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        val now = System.currentTimeMillis()
        inMemoryLastActivity = now
        lastSavedActivity = now
        prefs.put(KEY_ACCESS_TOKEN, accessToken)
        prefs.put(KEY_REFRESH_TOKEN, refreshToken)
        prefs.put(KEY_LAST_ACTIVITY, now.toString())
        prefs.flush()
    }

    override suspend fun clearTokens() {
        clearTokensSync()
    }

    private fun clearTokensSync() {
        inMemoryLastActivity = 0L
        lastSavedActivity = 0L
        prefs.remove(KEY_ACCESS_TOKEN)
        prefs.remove(KEY_REFRESH_TOKEN)
        prefs.remove(KEY_LAST_ACTIVITY)
        prefs.flush()
    }

    override suspend fun saveSessionIdleTimeoutSeconds(seconds: Long) {
        prefs.putLong(KEY_SESSION_TIMEOUT_SECONDS, seconds.coerceIn(60L, 86_400L))
        prefs.flush()
    }

    override suspend fun getSessionRemainingMillis(): Long? {
        if (prefs.get(KEY_ACCESS_TOKEN, null) == null) return null
        val lastActivity = if (inMemoryLastActivity > 0L) inMemoryLastActivity else (prefs.get(KEY_LAST_ACTIVITY, "0").toLongOrNull() ?: 0L)
        if (lastActivity == 0L) return effectiveTimeoutMillis()
        val elapsed = System.currentTimeMillis() - lastActivity
        val remaining = effectiveTimeoutMillis() - elapsed
        return remaining.coerceAtLeast(0L)
    }

    fun touchSessionSync() {
        if (prefs.get(KEY_ACCESS_TOKEN, null) == null) return
        val now = System.currentTimeMillis()
        if (now - lastTouchThrottle < 1000L) return
        lastTouchThrottle = now
        inMemoryLastActivity = now
        if (now - lastSavedActivity >= 5000L) {
            lastSavedActivity = now
            prefs.put(KEY_LAST_ACTIVITY, now.toString())
            prefs.flush()
        }
    }

    override suspend fun touchSession() {
        touchSessionSync()
    }

    private fun activeToken(key: String): String? {
        val access = prefs.get(KEY_ACCESS_TOKEN, null)
        val last = if (inMemoryLastActivity > 0L) inMemoryLastActivity else (prefs.get(KEY_LAST_ACTIVITY, "0").toLongOrNull() ?: 0L)
        if (access != null && last == 0L) {
            val now = System.currentTimeMillis()
            inMemoryLastActivity = now
            lastSavedActivity = now
            prefs.put(KEY_LAST_ACTIVITY, now.toString())
            prefs.flush()
        } else if (access != null && System.currentTimeMillis() - last >= effectiveTimeoutMillis()) {
            clearTokensSync()
            return null
        }
        return prefs.get(key, null)
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_LAST_ACTIVITY = "last_activity"
        private const val KEY_SESSION_TIMEOUT_SECONDS = "session_timeout_seconds"
    }

    private fun effectiveTimeoutMillis() =
        prefs.getLong(KEY_SESSION_TIMEOUT_SECONDS, SESSION_IDLE_TIMEOUT_MILLIS / 1000L)
            .coerceIn(60L, 86_400L) * 1000L
}
