package com.afbg.ui.plays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afbg.AfbgApp
import com.afbg.data.db.PlayEntity
import com.afbg.data.db.PlayerResultEntity
import com.afbg.ui.common.GamePickerDialog
import com.afbg.ui.common.NoInsets
import com.afbg.ui.common.appViewModel
import com.afbg.util.Dates

private data class PlayerRow(val name: String = "", val score: String = "", val winner: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayScreen(initialGameId: Long?, fromScore: Boolean, onDone: () -> Unit) {
    val vm = appViewModel { AddPlayViewModel(it.repository) }
    val games by vm.games.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as AfbgApp

    var gameId by remember { mutableStateOf(initialGameId) }
    var day by remember { mutableStateOf(Dates.today()) }
    var duration by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var pickingGame by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }

    // Joueurs : préremplis depuis le compteur de points si on vient de l'onglet Scores.
    val players = remember {
        val initial = if (fromScore) {
            val board = app.scoreStore.load()
            val top = board.players.maxOfOrNull { it.score }
            val anyPoints = board.players.any { it.score != 0 }
            board.players.map { PlayerRow(it.name, it.score.toString(), winner = anyPoints && it.score == top) }
        } else {
            listOf(PlayerRow(), PlayerRow())
        }
        mutableStateListOf<PlayerRow>().apply { addAll(initial) }
    }

    val game = games.firstOrNull { it.id == gameId }
    val canSave = game != null && players.any { it.name.isNotBlank() }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle partie") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            vm.save(
                                PlayEntity(
                                    gameId = game!!.id,
                                    playedAt = day,
                                    durationMin = duration.toIntOrNull(),
                                    location = location.trim(),
                                    notes = notes.trim(),
                                ),
                                players.filter { it.name.isNotBlank() }.map {
                                    PlayerResultEntity(name = it.name.trim(), score = it.score.toIntOrNull(), winner = it.winner)
                                },
                                onDone,
                            )
                        },
                    ) { Text("Enregistrer") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedButton(onClick = { pickingGame = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(game?.name ?: "Choisir le jeu")
                }
            }
            item {
                OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Date : ${Dates.format(day)}")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        duration, { duration = it.filter(Char::isDigit).take(4) },
                        label = { Text("Durée (min)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        location, { location = it },
                        label = { Text("Lieu") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item { Text("Joueurs", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(players) { index, row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        row.name, { players[index] = row.copy(name = it) },
                        label = { Text("Nom") }, singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                    OutlinedTextField(
                        row.score, { players[index] = row.copy(score = it.filter { c -> c.isDigit() || c == '-' }.take(6)) },
                        label = { Text("Score") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(checked = row.winner, onCheckedChange = { players[index] = row.copy(winner = it) })
                    IconButton(onClick = { players.removeAt(index) }) { Icon(Icons.Default.Close, "Retirer") }
                }
            }
            item {
                Row {
                    TextButton(onClick = { players.add(PlayerRow()) }) { Text("+ Ajouter un joueur") }
                    Text(
                        "Case cochée = vainqueur",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterVertically),
                    )
                }
            }
            item {
                OutlinedTextField(
                    notes, { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (pickingGame) {
        GamePickerDialog(
            games = games,
            onPick = { gameId = it.id; pickingGame = false },
            onDismiss = { pickingGame = false },
        )
    }
    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { day = it }; pickingDate = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Annuler") } },
        ) { DatePicker(state = state) }
    }
}
