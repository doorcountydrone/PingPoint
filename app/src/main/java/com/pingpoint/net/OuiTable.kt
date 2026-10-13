package com.pingpoint.net

import android.content.Context

object OuiTable {
    fun load(context: Context): Map<String, String> {
        val vendors = HashMap<String, String>(45_000)
        context.assets.open("oui.txt").bufferedReader().useLines { lines ->
            for (line in lines) {
                val tab = line.indexOf('\t')
                if (tab == 6) {
                    vendors[line.substring(0, 6)] = line.substring(tab + 1)
                }
            }
        }
        return vendors
    }
}
