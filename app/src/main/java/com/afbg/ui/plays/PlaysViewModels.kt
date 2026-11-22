package com.afbg.ui.plays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afbg.data.Repository
import com.afbg.data.db.GameEntity
import com.afbg.data.db.PlayDetails
import com.afbg.data.db.PlayEntity
import com.afbg.data.db.PlayerResultEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaysViewModel(private val repo: Repository) : ViewModel() {
    val plays: StateFlow<List<PlayDetails>> = repo.observePlays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(play: PlayEntity) {
        viewModelScope.launch { repo.deletePlay(play) }
    }
}

class AddPlayViewModel(private val repo: Repository) : ViewModel() {
    val games: StateFlow<List<GameEntity>> = repo.observeAllGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(play: PlayEntity, players: List<PlayerResultEntity>, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.addPlay(play, players)
            onDone()
        }
    }
}
