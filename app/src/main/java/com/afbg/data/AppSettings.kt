package com.afbg.data

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("afbg_settings", Context.MODE_PRIVATE)

    /** Jeton d'application BoardGameGeek (obligatoire pour l'API XML depuis fin 2025). */
    var bggToken: String
        get() = prefs.getString("bgg_token", "") ?: ""
        set(value) = prefs.edit().putString("bgg_token", value.trim()).apply()

    var bggUsername: String
        get() = prefs.getString("bgg_username", "") ?: ""
        set(value) = prefs.edit().putString("bgg_username", value.trim()).apply()
}
