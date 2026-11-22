package com.afbg.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "games", indices = [Index(value = ["bggId"], unique = true)])
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bggId: Int? = null,
    val name: String,
    val year: Int? = null,
    val minPlayers: Int? = null,
    val maxPlayers: Int? = null,
    val playingTime: Int? = null,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
    val rating: Double? = null,
    val description: String? = null,
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "plays",
    foreignKeys = [ForeignKey(GameEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameId")],
)
data class PlayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    /** Minuit UTC du jour de la partie (voir [com.afbg.util.Dates]). */
    val playedAt: Long,
    val durationMin: Int? = null,
    val location: String = "",
    val notes: String = "",
)

@Entity(
    tableName = "play_players",
    foreignKeys = [ForeignKey(PlayEntity::class, ["id"], ["playId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playId")],
)
data class PlayerResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playId: Long = 0,
    val name: String,
    val score: Int? = null,
    val winner: Boolean = false,
)

@Entity(
    tableName = "loans",
    foreignKeys = [ForeignKey(GameEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameId")],
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val borrower: String,
    val lentAt: Long,
    /** null = toujours prêté. */
    val returnedAt: Long? = null,
    val note: String = "",
)

/** Ligne de la ludothèque : le jeu + quelques agrégats. */
data class GameListItem(
    @Embedded val game: GameEntity,
    @ColumnInfo(name = "playCount") val playCount: Int,
    @ColumnInfo(name = "borrower") val borrower: String?,
)

data class PlayDetails(
    @Embedded val play: PlayEntity,
    @Relation(parentColumn = "gameId", entityColumn = "id") val game: GameEntity,
    @Relation(parentColumn = "id", entityColumn = "playId") val players: List<PlayerResultEntity>,
)

data class LoanWithGame(
    @Embedded val loan: LoanEntity,
    @Relation(parentColumn = "gameId", entityColumn = "id") val game: GameEntity,
)
