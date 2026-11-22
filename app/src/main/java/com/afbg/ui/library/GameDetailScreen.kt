package com.afbg.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afbg.data.db.PlayDetails
import com.afbg.ui.common.ConfirmDialog
import com.afbg.ui.common.GameThumbnail
import com.afbg.ui.common.NoInsets
import com.afbg.ui.common.appViewModel
import com.afbg.ui.common.subtitle
import com.afbg.util.Dates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(gameId: Long, onBack: () -> Unit, onAddPlay: () -> Unit) {
    val vm = appViewModel(key = "game_$gameId") { GameDetailViewModel(it.repository, gameId) }
    val game by vm.game.collectAsStateWithLifecycle()
    val plays by vm.plays.collectAsStateWithLifecycle()
    val loans by vm.loans.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var lending by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editNotes by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.message.value = null
        }
    }

    val g = game
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text(g?.name ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
                actions = {
                    if (g?.bggId != null) {
                        if (refreshing) {
                            CircularProgressIndicator(Modifier.padding(12.dp))
                        } else {
                            IconButton(onClick = vm::refresh) { Icon(Icons.Default.Refresh, "Actualiser depuis BGG") }
                        }
                    }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Supprimer") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (g == null) return@Scaffold
        val activeLoan = loans.firstOrNull { it.returnedAt == null }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameThumbnail(g, size = 96.dp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(g.name, style = MaterialTheme.typography.titleLarge)
                        Text(g.subtitle(), style = MaterialTheme.typography.bodyMedium)
                        g.rating?.let { Text("Note BGG : ${"%.1f".format(it)}/10", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onAddPlay, modifier = Modifier.weight(1f)) { Text("Enregistrer une partie") }
                    if (activeLoan == null) {
                        OutlinedButton(onClick = { lending = true }, modifier = Modifier.weight(1f)) { Text("Prêter") }
                    } else {
                        OutlinedButton(onClick = { vm.markReturned(activeLoan.id) }, modifier = Modifier.weight(1f)) { Text("Marquer rendu") }
                    }
                }
            }
            if (activeLoan != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text(
                            "Prêté à ${activeLoan.borrower} depuis le ${Dates.format(activeLoan.lentAt)} (${Dates.daysSince(activeLoan.lentAt)} j)" +
                                if (activeLoan.note.isNotBlank()) "\n${activeLoan.note}" else "",
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editNotes = true }) { Text("Modifier") }
                    }
                    Text(g.notes.ifBlank { "Aucune note." }, style = MaterialTheme.typography.bodyMedium)
                }
            }
            g.description?.let { desc ->
                item {
                    Column {
                        Text("Description", style = MaterialTheme.typography.titleMedium)
                        var expanded by remember { mutableStateOf(false) }
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (expanded) Int.MAX_VALUE else 6,
                        )
                        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Réduire" else "Lire la suite") }
                    }
                }
            }
            item {
                Text("Parties (${plays.size})", style = MaterialTheme.typography.titleMedium)
            }
            if (plays.isEmpty()) {
                item { Text("Aucune partie enregistrée.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(plays, key = { "p${it.play.id}" }) { PlayLine(it) }
            if (loans.isNotEmpty()) {
                item { HorizontalDivider() }
                item { Text("Historique des prêts", style = MaterialTheme.typography.titleMedium) }
                items(loans, key = { "l${it.id}" }) { loan ->
                    Text(
                        "${loan.borrower} · ${Dates.format(loan.lentAt)} → " +
                            (loan.returnedAt?.let { Dates.format(it) } ?: "en cours"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (lending) LendDialog(onConfirm = { who, note -> vm.lend(who, note); lending = false }, onDismiss = { lending = false })
    if (editNotes && g != null) {
        NotesDialog(initial = g.notes, onConfirm = { vm.saveNotes(it); editNotes = false }, onDismiss = { editNotes = false })
    }
    if (confirmDelete) {
        ConfirmDialog(
            message = "Supprimer ce jeu ainsi que ses parties et ses prêts ?",
            confirmLabel = "Supprimer",
            onConfirm = { confirmDelete = false; vm.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun PlayLine(details: PlayDetails) {
    val winners = details.players.filter { it.winner }.joinToString(", ") { it.name }
    Column {
        Text(Dates.format(details.play.playedAt), style = MaterialTheme.typography.labelLarge)
        Text(
            details.players.joinToString(" · ") { p -> p.name + (p.score?.let { " ($it)" } ?: "") }.ifEmpty { "Sans joueurs" } +
                if (winners.isNotEmpty()) "\nVainqueur : $winners" else "",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LendDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var who by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prêter ce jeu") },
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

@Composable
private fun NotesDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notes") },
        text = { OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
