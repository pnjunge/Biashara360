package com.app.biashara.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.app.biashara.data.remote.TokenStorage
import com.app.biashara.data.remote.SESSION_IDLE_TIMEOUT_MILLIS

class SharedPreferencesTokenStorage(context: Context) : TokenStorage {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        SECURE_PREFERENCES_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val accessToken = legacyPrefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = legacyPrefs.getString(KEY_REFRESH_TOKEN, null)
        if (!prefs.contains(KEY_ACCESS_TOKEN) && (accessToken != null || refreshToken != null)) {
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply()
            legacyPrefs.edit().clear().apply()
        }
        if (prefs.contains(KEY_ACCESS_TOKEN) && !prefs.contains(KEY_LAST_ACTIVITY)) {
            prefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
        }
    }

    override suspend fun getAccessToken(): String? = activeToken(KEY_ACCESS_TOKEN)

    override suspend fun getRefreshToken(): String? = activeToken(KEY_REFRESH_TOKEN)

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
    }

    override suspend fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_LAST_ACTIVITY)
            .apply()
    }

    override suspend fun saveSessionIdleTimeoutSeconds(seconds: Long) {
        prefs.edit().putLong(KEY_SESSION_TIMEOUT_SECONDS, seconds.coerceIn(60L, 86_400L)).apply()
    }

    override suspend fun getSessionRemainingMillis(): Long? {
        if (!prefs.contains(KEY_ACCESS_TOKEN)) return null
        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
        return (effectiveTimeoutMillis() - (System.currentTimeMillis() - lastActivity)).coerceAtLeast(0L)
    }

    override suspend fun touchSession() {
        if (prefs.contains(KEY_ACCESS_TOKEN)) {
            prefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
        }
    }

    private fun activeToken(key: String): String? {
        val access = prefs.getString(KEY_ACCESS_TOKEN, null)
        val last = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
        if (access != null && last == 0L) {
            prefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
        } else if (access != null && System.currentTimeMillis() - last >= effectiveTimeoutMillis()) {
            clearTokensSync()
            return null
        }
        if (access != null) prefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
        return prefs.getString(key, null)
    }

    private fun clearTokensSync() { prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).remove(KEY_LAST_ACTIVITY).apply() }

    private fun effectiveTimeoutMillis() =
        prefs.getLong(KEY_SESSION_TIMEOUT_SECONDS, SESSION_IDLE_TIMEOUT_MILLIS / 1000L)
            .coerceIn(60L, 86_400L) * 1000L

    companion object {
        private const val LEGACY_PREFERENCES_NAME = "b360_auth_prefs"
        private const val SECURE_PREFERENCES_NAME = "b360_secure_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_LAST_ACTIVITY = "last_activity"
        private const val KEY_SESSION_TIMEOUT_SECONDS = "session_timeout_seconds"
    }
}
