package com.pingpoint.net

import android.content.Context

enum class ThemePreference {
    System,
    Light,
    Dark,
}

class ThemePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): ThemePreference {
        return when (prefs.getString(KEY, ThemePreference.System.name)) {
            ThemePreference.Light.name -> ThemePreference.Light
            ThemePreference.Dark.name -> ThemePreference.Dark
            else -> ThemePreference.System
        }
    }

    fun write(preference: ThemePreference) {
        prefs.edit().putString(KEY, preference.name).apply()
    }

    fun next(current: ThemePreference): ThemePreference = when (current) {
        ThemePreference.System -> ThemePreference.Light
        ThemePreference.Light -> ThemePreference.Dark
        ThemePreference.Dark -> ThemePreference.System
    }

    private companion object {
        const val PREFS = "theme"
        const val KEY = "preference"
    }
}
