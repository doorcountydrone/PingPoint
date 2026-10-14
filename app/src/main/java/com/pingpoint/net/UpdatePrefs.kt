package com.pingpoint.net

import android.content.Context

class UpdatePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun dismissedVersion(): String? = prefs.getString(KEY, null)

    fun dismiss(versionName: String) {
        prefs.edit().putString(KEY, AppVersion.normalize(versionName)).apply()
    }

    private companion object {
        const val PREFS = "updates"
        const val KEY = "dismissed_version"
    }
}
