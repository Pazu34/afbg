package com.afbg.ui.score

import androidx.lifecycle.ViewModel
import com.afbg.ui.theme.PlayerPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScoreViewModel(private val store: ScoreStore) : ViewModel() {
    private val _board = MutableStateFlow(store.load())
    val board: StateFlow<ScoreBoard> = _board.asStateFlow()

    private fun change(transform: (ScoreBoard) -> ScoreBoard) {
        _board.update(transform)
        store.save(_board.value)
    }

    private fun ScoreBoard.mapPlayer(id: Long, f: (ScorePlayer) -> ScorePlayer) =
        copy(players = players.map { if (it.id == id) f(it) else it })

    fun add(playerId: Long, delta: Int) = change { b ->
        b.mapPlayer(playerId) { it.copy(score = it.score + delta, history = it.history + delta) }
    }

    fun undo(playerId: Long) = change { b ->
        b.mapPlayer(playerId) { p ->
            val last = p.history.lastOrNull() ?: return@mapPlayer p
            p.copy(score = p.score - last, history = p.history.dropLast(1))
        }
    }

    fun rename(playerId: Long, name: String) = change { b -> b.mapPlayer(playerId) { it.copy(name = name) } }

    fun addPlayer() = change { b ->
        val used = b.players.map { it.colorIndex }.toSet()
        val color = PlayerPalette.indices.firstOrNull { it !in used } ?: (b.players.size % PlayerPalette.size)
        b.copy(
            players = b.players + ScorePlayer(b.nextId, "Joueur ${b.players.size + 1}", color),
            nextId = b.nextId + 1,
        )
    }

    fun removePlayer(playerId: Long) = change { b ->
        if (b.players.size <= 1) b else b.copy(players = b.players.filterNot { it.id == playerId })
    }

    fun setStep(step: Int) = change { b ->
        b.copy(step = step, steps = if (step in b.steps) b.steps else (b.steps + step).sorted())
    }

    fun resetScores() = change { b ->
        b.copy(players = b.players.map { it.copy(score = 0, history = emptyList()) })
    }
}
