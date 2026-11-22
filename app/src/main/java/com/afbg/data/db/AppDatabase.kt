package com.afbg.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameEntity::class, PlayEntity::class, PlayerResultEntity::class, LoanEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun playDao(): PlayDao
    abstract fun loanDao(): LoanDao
}
