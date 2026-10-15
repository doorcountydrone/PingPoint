package com.pingpoint.net

import android.content.Context
import com.pingpoint.model.HistoryEntry

class HistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): List<HistoryEntry> = HistoryCodec.decode(prefs.getString(KEY, "").orEmpty())

    fun write(entries: List<HistoryEntry>) {
        prefs.edit().putString(KEY, HistoryCodec.encode(entries)).apply()
    }

    private companion object {
        const val PREFS = "history"
        const val KEY = "entries"
    }
}
