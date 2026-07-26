package com.app.biashara.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK

private const val BIOMETRIC_PREFERENCES = "biashara_biometric_preferences"
private const val BIOMETRIC_LOGIN_ENABLED = "biometric_login_enabled"

fun biometricEnrollmentIntent(): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BIOMETRIC_STRONG or BIOMETRIC_WEAK
            )
        }
    } else {
        Intent(Settings.ACTION_SECURITY_SETTINGS)
    }

fun Context.isBiometricLoginEnabled(): Boolean =
    getSharedPreferences(BIOMETRIC_PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(BIOMETRIC_LOGIN_ENABLED, false)

fun Context.setBiometricLoginEnabled(enabled: Boolean) {
    getSharedPreferences(BIOMETRIC_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(BIOMETRIC_LOGIN_ENABLED, enabled)
        .apply()
}
