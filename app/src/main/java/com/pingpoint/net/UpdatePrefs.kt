package com.pingpoint.net

import android.content.Context

class UpdatePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun dismissedVersion(): String? = prefs.getString(KEY_VERSION, null)

    fun opensSinceDismiss(): Int = prefs.getInt(KEY_OPENS, 0)

    fun dismiss(versionName: String) {
        prefs.edit()
            .putString(KEY_VERSION, AppVersion.normalize(versionName))
            .putInt(KEY_OPENS, 0)
            .apply()
    }

    /** Counts this launch while the same dismissed update is still current. */
    fun recordOpen(): Int {
        val opens = opensSinceDismiss() + 1
        prefs.edit().putInt(KEY_OPENS, opens).apply()
        return opens
    }

    fun clearDismiss() {
        prefs.edit().remove(KEY_VERSION).remove(KEY_OPENS).apply()
    }

    private companion object {
        const val PREFS = "updates"
        const val KEY_VERSION = "dismissed_version"
        const val KEY_OPENS = "opens_since_dismiss"
    }
}
