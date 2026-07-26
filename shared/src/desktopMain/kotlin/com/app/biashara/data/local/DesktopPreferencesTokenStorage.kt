package com.app.biashara.data.local

import com.app.biashara.data.remote.TokenStorage
import com.app.biashara.data.remote.SESSION_IDLE_TIMEOUT_MILLIS
import java.util.prefs.Preferences

class DesktopPreferencesTokenStorage : TokenStorage {
    private val prefs: Preferences = Preferences.userRoot().node("com/app/biashara/auth")

    override suspend fun getAccessToken(): String? {
        return activeToken(KEY_ACCESS_TOKEN)
    }

    override suspend fun getRefreshToken(): String? {
        return activeToken(KEY_REFRESH_TOKEN)
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.put(KEY_ACCESS_TOKEN, accessToken)
        prefs.put(KEY_REFRESH_TOKEN, refreshToken)
        prefs.put(KEY_LAST_ACTIVITY, System.currentTimeMillis().toString())
        prefs.flush()
    }

    override suspend fun clearTokens() {
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
        val lastActivity = prefs.get(KEY_LAST_ACTIVITY, "0").toLongOrNull() ?: 0L
        return (effectiveTimeoutMillis() - (System.currentTimeMillis() - lastActivity)).coerceAtLeast(0L)
    }

    override suspend fun touchSession() {
        if (prefs.get(KEY_ACCESS_TOKEN, null) != null) {
            prefs.put(KEY_LAST_ACTIVITY, System.currentTimeMillis().toString())
            prefs.flush()
        }
    }

    private fun activeToken(key: String): String? {
        val access = prefs.get(KEY_ACCESS_TOKEN, null)
        val last = prefs.get(KEY_LAST_ACTIVITY, "0").toLongOrNull() ?: 0L
        if (access != null && last == 0L) {
            prefs.put(KEY_LAST_ACTIVITY, System.currentTimeMillis().toString()); prefs.flush()
        } else if (access != null && System.currentTimeMillis() - last >= effectiveTimeoutMillis()) {
            prefs.remove(KEY_ACCESS_TOKEN); prefs.remove(KEY_REFRESH_TOKEN); prefs.remove(KEY_LAST_ACTIVITY); prefs.flush()
            return null
        }
        if (access != null) { prefs.put(KEY_LAST_ACTIVITY, System.currentTimeMillis().toString()); prefs.flush() }
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
