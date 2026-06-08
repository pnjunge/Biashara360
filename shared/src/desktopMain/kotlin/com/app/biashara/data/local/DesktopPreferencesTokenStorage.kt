package com.app.biashara.data.local

import com.app.biashara.data.remote.TokenStorage
import java.util.prefs.Preferences

class DesktopPreferencesTokenStorage : TokenStorage {
    private val prefs: Preferences = Preferences.userRoot().node("com/app/biashara/auth")

    override suspend fun getAccessToken(): String? {
        return prefs.get(KEY_ACCESS_TOKEN, null)
    }

    override suspend fun getRefreshToken(): String? {
        return prefs.get(KEY_REFRESH_TOKEN, null)
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.put(KEY_ACCESS_TOKEN, accessToken)
        prefs.put(KEY_REFRESH_TOKEN, refreshToken)
        prefs.flush()
    }

    override suspend fun clearTokens() {
        prefs.remove(KEY_ACCESS_TOKEN)
        prefs.remove(KEY_REFRESH_TOKEN)
        prefs.flush()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
