package com.pingpoint.net

import android.content.Context
import com.pingpoint.model.SavedDevice

class SavedDeviceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): List<SavedDevice> = SavedDeviceCodec.decode(prefs.getString(KEY, "").orEmpty())

    fun write(devices: List<SavedDevice>) {
        prefs.edit().putString(KEY, SavedDeviceCodec.encode(devices)).apply()
    }

    private companion object {
        const val PREFS = "saved_devices"
        const val KEY = "devices"
    }
}
