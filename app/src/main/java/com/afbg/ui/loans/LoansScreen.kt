package com.afbg.ui.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.afbg.data.Repository
import com.afbg.data.db.GameEntity
import com.afbg.data.db.LoanWithGame
import com.afbg.ui.common.EmptyState
import com.afbg.ui.common.GamePickerDialog
import com.afbg.ui.common.GameThumbnail
import com.afbg.ui.common.NoInsets
import com.afbg.ui.common.appViewModel
import com.afbg.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoansViewModel(private val repo: Repository) : ViewModel() {
    val loans: StateFlow<List<LoanWithGame>> = repo.observeLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val games: StateFlow<List<GameEntity>> = repo.observeAllGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun lend(gameId: Long, borrower: String, note: String) {
        viewModelScope.launch { repo.lend(gameId, borrower, note) }
    }

    fun markReturned(loanId: Long) {
        viewModelScope.launch { repo.markReturned(loanId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(onOpenGame: (Long) -> Unit) {
    val vm = appViewModel { LoansViewModel(it.repository) }
    val loans by vm.loans.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var pickingGame by remember { mutableStateOf(false) }
    var lendingGame by remember { mutableStateOf<GameEntity?>(null) }

    val active = loans.filter { it.loan.returnedAt == null }
    val history = loans.filter { it.loan.returnedAt != null }
    val shown = if (tab == 0) active else history
    val availableGames = games.filter { g -> active.none { it.game.id == g.id } }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Prêts") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickingGame = true }) { Icon(Icons.Default.Add, "Nouveau prêt") }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("En cours (${active.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Historique (${history.size})") })
            }
            if (shown.isEmpty()) {
                EmptyState(if (tab == 0) "Aucun jeu prêté en ce moment." else "Aucun prêt terminé.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown, key = { it.loan.id }) { item ->
                        LoanCard(item, onOpen = { onOpenGame(item.game.id) }, onReturn = { vm.markReturned(item.loan.id) })
                    }
                }
            }
        }
    }

    if (pickingGame) {
        GamePickerDialog(
            games = availableGames,
            onPick = { lendingGame = it; pickingGame = false },
            onDismiss = { pickingGame = false },
        )
    }
    lendingGame?.let { game ->
        BorrowerDialog(
            gameName = game.name,
            onConfirm = { who, note -> vm.lend(game.id, who, note); lendingGame = null; tab = 0 },
            onDismiss = { lendingGame = null },
        )
    }
}

@Composable
private fun LoanCard(item: LoanWithGame, onOpen: () -> Unit, onReturn: () -> Unit) {
    val loan = item.loan
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            GameThumbnail(item.game, size = 56.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.game.name, style = MaterialTheme.typography.titleMedium)
                Text("À ${loan.borrower}", style = MaterialTheme.typography.bodyMedium)
                val dates = if (loan.returnedAt == null) {
                    "Depuis le ${Dates.format(loan.lentAt)} (${Dates.daysSince(loan.lentAt)} j)"
                } else {
                    "${Dates.format(loan.lentAt)} → ${Dates.format(loan.returnedAt)}"
                }
                Text(dates, style = MaterialTheme.typography.bodySmall)
                if (loan.note.isNotBlank()) Text(loan.note, style = MaterialTheme.typography.bodySmall)
            }
            if (loan.returnedAt == null) {
                OutlinedButton(onClick = onReturn) { Text("Rendu") }
            }
        }
    }
}

@Composable
private fun BorrowerDialog(gameName: String, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var who by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prêter « $gameName »") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(who, { who = it }, label = { Text("À qui ?") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Note (facultatif)") })
            }
        },
        confirmButton = { TextButton(enabled = who.isNotBlank(), onClick = { onConfirm(who, note) }) { Text("Prêter") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
