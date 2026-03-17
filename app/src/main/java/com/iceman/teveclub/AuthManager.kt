package com.iceman.teveclub

import android.content.Context

object AuthManager {
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USERNAME = "auth_username"
    private const val KEY_LOGGED_IN = "logged_in"

    fun saveAuthToken(context: Context, token: String) {
        val prefs = SecurePrefs.getEncryptedPrefs(context)
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(context: Context): String? {
        val prefs = SecurePrefs.getEncryptedPrefs(context)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUsername(context: Context, usernameEncrypted: String) {
        val prefs = SecurePrefs.getEncryptedPrefs(context)
        prefs.edit().putString(KEY_USERNAME, usernameEncrypted).apply()
    }

    fun setLoggedIn(context: Context, value: Boolean) {
        val prefs = SecurePrefs.getEncryptedPrefs(context)
        prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()
    }

    fun isLoggedFlag(context: Context): Boolean {
        val prefs = SecurePrefs.getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_LOGGED_IN, false)
    }

    fun clear(context: Context) {
        val prefs = SecurePrefs.getEncryptedPrefs(context)
        prefs.edit().remove(KEY_AUTH_TOKEN).remove(KEY_USERNAME).apply()
    }

    fun isLoggedIn(context: Context): Boolean = !getAuthToken(context).isNullOrEmpty()
}
