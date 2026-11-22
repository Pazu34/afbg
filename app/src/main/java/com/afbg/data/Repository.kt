package com.afbg.data

import com.afbg.data.bgg.BggClient
import com.afbg.data.bgg.BggException
import com.afbg.data.bgg.BggGame
import com.afbg.data.db.AppDatabase
import com.afbg.data.db.GameEntity
import com.afbg.data.db.LoanEntity
import com.afbg.data.db.PlayEntity
import com.afbg.data.db.PlayerResultEntity
import com.afbg.util.Dates

data class ImportResult(val found: Int, val added: Int)

class Repository(db: AppDatabase, private val bgg: BggClient) {
    private val games = db.gameDao()
    private val plays = db.playDao()
    private val loans = db.loanDao()

    // --- Jeux ---
    fun observeGameList() = games.observeList()
    fun observeAllGames() = games.observeAll()
    fun observeGame(id: Long) = games.observe(id)
    fun observeBggIds() = games.observeBggIds()

    suspend fun searchBgg(query: String) = bgg.search(query)

    suspend fun addManualGame(name: String, year: Int?, min: Int?, max: Int?, time: Int?): Long =
        games.insert(GameEntity(name = name.trim(), year = year, minPlayers = min, maxPlayers = max, playingTime = time))

    /** @return false si le jeu est déjà dans la ludothèque. */
    suspend fun addBggGame(game: BggGame): Boolean = games.insert(game.toEntity()) != -1L

    suspend fun addBggGameById(bggId: Int): Boolean {
        val game = bgg.things(listOf(bggId)).firstOrNull()
            ?: throw BggException("Jeu introuvable sur BGG.", BggException.Kind.PARSE)
        return addBggGame(game)
    }

    suspend fun importCollection(username: String): ImportResult {
        val list = bgg.collection(username)
        var added = 0
        list.forEach { if (addBggGame(it)) added++ }
        return ImportResult(list.size, added)
    }

    /** Recharge les infos (description, image, note…) d'un jeu lié à BGG. */
    suspend fun refreshFromBgg(gameId: Long) {
        val current = games.get(gameId) ?: return
        val bggId = current.bggId ?: return
        val fresh = bgg.things(listOf(bggId)).firstOrNull() ?: return
        games.update(
            current.copy(
                name = fresh.name,
                year = fresh.year ?: current.year,
                minPlayers = fresh.minPlayers ?: current.minPlayers,
                maxPlayers = fresh.maxPlayers ?: current.maxPlayers,
                playingTime = fresh.playingTime ?: current.playingTime,
                imageUrl = fresh.imageUrl ?: current.imageUrl,
                thumbnailUrl = fresh.thumbnailUrl ?: current.thumbnailUrl,
                rating = fresh.rating ?: current.rating,
                description = fresh.description ?: current.description,
            )
        )
    }

    suspend fun updateGame(game: GameEntity) = games.update(game)
    suspend fun deleteGame(game: GameEntity) = games.delete(game)

    // --- Parties ---
    fun observePlays() = plays.observeAll()
    fun observePlaysForGame(gameId: Long) = plays.observeForGame(gameId)

    suspend fun addPlay(play: PlayEntity, players: List<PlayerResultEntity>) = plays.insertWithPlayers(play, players)
    suspend fun deletePlay(play: PlayEntity) = plays.deletePlay(play)

    // --- Prêts ---
    fun observeLoans() = loans.observeAll()
    fun observeLoansForGame(gameId: Long) = loans.observeForGame(gameId)

    /** @return false si le jeu est déjà prêté. */
    suspend fun lend(gameId: Long, borrower: String, note: String): Boolean {
        if (loans.activeCount(gameId) > 0) return false
        loans.insert(LoanEntity(gameId = gameId, borrower = borrower.trim(), lentAt = Dates.today(), note = note.trim()))
        return true
    }

    suspend fun markReturned(loanId: Long) = loans.markReturned(loanId, Dates.today())
    suspend fun deleteLoan(loan: LoanEntity) = loans.delete(loan)
}

private fun BggGame.toEntity() = GameEntity(
    bggId = bggId,
    name = name,
    year = year,
    minPlayers = minPlayers,
    maxPlayers = maxPlayers,
    playingTime = playingTime,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    rating = rating,
    description = description,
)
