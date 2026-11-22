package com.afbg.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query(
        """
        SELECT g.*,
          (SELECT COUNT(*) FROM plays p WHERE p.gameId = g.id) AS playCount,
          (SELECT l.borrower FROM loans l WHERE l.gameId = g.id AND l.returnedAt IS NULL
             ORDER BY l.lentAt DESC LIMIT 1) AS borrower
        FROM games g ORDER BY g.name COLLATE NOCASE
        """
    )
    fun observeList(): Flow<List<GameListItem>>

    @Query("SELECT * FROM games ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    fun observe(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun get(id: Long): GameEntity?

    @Query("SELECT bggId FROM games WHERE bggId IS NOT NULL")
    fun observeBggIds(): Flow<List<Int>>

    /** Retourne -1 si un jeu avec le même bggId existe déjà. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Delete
    suspend fun delete(game: GameEntity)
}

@Dao
abstract class PlayDao {
    @Transaction
    @Query("SELECT * FROM plays ORDER BY playedAt DESC, id DESC")
    abstract fun observeAll(): Flow<List<PlayDetails>>

    @Transaction
    @Query("SELECT * FROM plays WHERE gameId = :gameId ORDER BY playedAt DESC, id DESC")
    abstract fun observeForGame(gameId: Long): Flow<List<PlayDetails>>

    @Insert
    abstract suspend fun insertPlay(play: PlayEntity): Long

    @Insert
    abstract suspend fun insertPlayers(players: List<PlayerResultEntity>)

    @Delete
    abstract suspend fun deletePlay(play: PlayEntity)

    @Transaction
    open suspend fun insertWithPlayers(play: PlayEntity, players: List<PlayerResultEntity>): Long {
        val id = insertPlay(play)
        insertPlayers(players.map { it.copy(playId = id) })
        return id
    }
}

@Dao
interface LoanDao {
    @Transaction
    @Query("SELECT * FROM loans ORDER BY lentAt DESC, id DESC")
    fun observeAll(): Flow<List<LoanWithGame>>

    @Query("SELECT * FROM loans WHERE gameId = :gameId ORDER BY lentAt DESC, id DESC")
    fun observeForGame(gameId: Long): Flow<List<LoanEntity>>

    @Query("SELECT COUNT(*) FROM loans WHERE gameId = :gameId AND returnedAt IS NULL")
    suspend fun activeCount(gameId: Long): Int

    @Insert
    suspend fun insert(loan: LoanEntity): Long

    @Query("UPDATE loans SET returnedAt = :day WHERE id = :id")
    suspend fun markReturned(id: Long, day: Long)

    @Delete
    suspend fun delete(loan: LoanEntity)
}
