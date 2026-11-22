package com.afbg.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afbg.data.Repository
import com.afbg.data.bgg.BggException
import com.afbg.data.bgg.BggSearchHit
import com.afbg.data.db.GameEntity
import com.afbg.data.db.GameListItem
import com.afbg.data.db.LoanEntity
import com.afbg.data.db.PlayDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun Throwable.userMessage(): String = when (this) {
    is BggException -> message ?: "Erreur BGG"
    else -> "Erreur inattendue : ${message ?: javaClass.simpleName}"
}

class LibraryViewModel(repo: Repository) : ViewModel() {
    val query = MutableStateFlow("")
    val onlyLoaned = MutableStateFlow(false)

    val games: StateFlow<List<GameListItem>> =
        combine(repo.observeGameList(), query, onlyLoaned) { list, q, loaned ->
            list.filter { (!loaned || it.borrower != null) && it.game.name.contains(q.trim(), ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val total: StateFlow<Int> = repo.observeGameList().map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

data class AddGameUiState(
    val query: String = "",
    val results: List<BggSearchHit> = emptyList(),
    val loading: Boolean = false,
    val searched: Boolean = false,
    val addingId: Int? = null,
    val message: String? = null,
)

class AddGameViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(AddGameUiState())
    val state: StateFlow<AddGameUiState> = _state.asStateFlow()

    val ownedBggIds: StateFlow<Set<Int>> = combine(repo.observeBggIds(), _state) { ids, _ -> ids.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun onQuery(q: String) = _state.update { it.copy(query = q) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun search() {
        val q = _state.value.query.trim()
        if (q.length < 2) return
        _state.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            try {
                val hits = repo.searchBgg(q)
                _state.update { it.copy(results = hits, loading = false, searched = true) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, message = e.userMessage()) }
            }
        }
    }

    fun add(hit: BggSearchHit) {
        if (_state.value.addingId != null) return
        _state.update { it.copy(addingId = hit.bggId) }
        viewModelScope.launch {
            try {
                val added = repo.addBggGameById(hit.bggId)
                val msg = if (added) "« ${hit.name} » ajouté à la ludothèque" else "« ${hit.name} » est déjà dans la ludothèque"
                _state.update { it.copy(addingId = null, message = msg) }
            } catch (e: Exception) {
                _state.update { it.copy(addingId = null, message = e.userMessage()) }
            }
        }
    }

    fun addManual(name: String, year: Int?, min: Int?, max: Int?, time: Int?) {
        viewModelScope.launch {
            repo.addManualGame(name, year, min, max, time)
            _state.update { it.copy(message = "« ${name.trim()} » ajouté à la ludothèque") }
        }
    }
}

class GameDetailViewModel(private val repo: Repository, private val gameId: Long) : ViewModel() {
    val game: StateFlow<GameEntity?> = repo.observeGame(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val plays: StateFlow<List<PlayDetails>> = repo.observePlaysForGame(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val loans: StateFlow<List<LoanEntity>> = repo.observeLoansForGame(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val message = MutableStateFlow<String?>(null)
    val refreshing = MutableStateFlow(false)

    fun lend(borrower: String, note: String) {
        viewModelScope.launch {
            if (!repo.lend(gameId, borrower, note)) message.value = "Ce jeu est déjà prêté."
        }
    }

    fun markReturned(loanId: Long) {
        viewModelScope.launch { repo.markReturned(loanId) }
    }

    fun refresh() {
        refreshing.value = true
        viewModelScope.launch {
            try {
                repo.refreshFromBgg(gameId)
                message.value = "Infos mises à jour depuis BGG"
            } catch (e: Exception) {
                message.value = e.userMessage()
            } finally {
                refreshing.value = false
            }
        }
    }

    fun saveNotes(notes: String) {
        val g = game.value ?: return
        viewModelScope.launch { repo.updateGame(g.copy(notes = notes)) }
    }

    fun delete(onDone: () -> Unit) {
        val g = game.value ?: return
        viewModelScope.launch {
            repo.deleteGame(g)
            onDone()
        }
    }
}
