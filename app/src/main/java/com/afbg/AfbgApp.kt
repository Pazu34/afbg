package com.afbg

import android.app.Application
import androidx.room.Room
import com.afbg.data.AppSettings
import com.afbg.data.Repository
import com.afbg.data.bgg.BggClient
import com.afbg.data.db.AppDatabase
import com.afbg.ui.score.ScoreStore

/** Conteneur de dépendances minimaliste (pas de Hilt pour garder le projet léger). */
class AfbgApp : Application() {
    val settings by lazy { AppSettings(this) }
    private val database by lazy { Room.databaseBuilder(this, AppDatabase::class.java, "afbg.db").build() }
    val repository by lazy { Repository(database, BggClient(tokenProvider = { settings.bggToken })) }
    val scoreStore by lazy { ScoreStore(this) }
}
