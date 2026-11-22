package com.afbg.ui.score

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afbg.ui.common.ConfirmDialog
import com.afbg.ui.common.NoInsets
import com.afbg.ui.common.TextInputDialog
import com.afbg.ui.common.appViewModel
import com.afbg.ui.theme.PlayerPalette

private fun signed(n: Int) = if (n > 0) "+$n" else if (n < 0) "−${-n}" else "0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreScreen(onSaveGame: () -> Unit) {
    val vm = appViewModel { ScoreViewModel(it.scoreStore) }
    val board by vm.board.collectAsStateWithLifecycle()

    var renaming by remember { mutableStateOf<ScorePlayer?>(null) }
    var customStep by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    val topScore = board.players.maxOf { it.score }
    val hasLeader = board.players.any { it.score != 0 } && board.players.count { it.score == topScore } == 1

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Scores") },
                actions = {
                    IconButton(onClick = vm::addPlayer) { Icon(Icons.Default.Add, "Ajouter un joueur") }
                    IconButton(onClick = { confirmReset = true }) { Icon(Icons.Default.Refresh, "Remettre à zéro") }
                    IconButton(onClick = onSaveGame) { Icon(Icons.Default.Check, "Enregistrer la partie") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item { Text("Pas :", style = MaterialTheme.typography.labelLarge) }
                items(board.steps) { s ->
                    FilterChip(selected = board.step == s, onClick = { vm.setStep(s) }, label = { Text("$s") })
                }
                item { TextButton(onClick = { customStep = true }) { Text("Autre…") } }
            }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(board.players, key = { it.id }) { player ->
                    PlayerCard(
                        player = player,
                        step = board.step,
                        leading = hasLeader && player.score == topScore,
                        canRemove = board.players.size > 1,
                        onAdd = { vm.add(player.id, it) },
                        onUndo = { vm.undo(player.id) },
                        onRename = { renaming = player },
                        onRemove = { vm.removePlayer(player.id) },
                    )
                }
            }
        }
    }

    renaming?.let { p ->
        TextInputDialog(
            title = "Nom du joueur",
            initial = p.name,
            label = "Nom",
            onConfirm = { vm.rename(p.id, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    if (customStep) CustomStepDialog(onConfirm = { vm.setStep(it); customStep = false }, onDismiss = { customStep = false })
    if (confirmReset) {
        ConfirmDialog(
            message = "Remettre tous les scores à zéro ?",
            confirmLabel = "Remettre à zéro",
            onConfirm = { vm.resetScores(); confirmReset = false },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun PlayerCard(
    player: ScorePlayer,
    step: Int,
    leading: Boolean,
    canRemove: Boolean,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    val color = PlayerPalette[player.colorIndex % PlayerPalette.size]
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (leading) "★ " else "") + player.name,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).clickable(onClick = onRename),
            )
            if (canRemove) {
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "Retirer le joueur", tint = Color.White) }
            }
        }
        Text(
            "${player.score}",
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            val buttonColors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f), contentColor = Color.White)
            Button(onClick = { onAdd(-step) }, colors = buttonColors, modifier = Modifier.weight(1f)) {
                Text("− $step", fontSize = 20.sp)
            }
            Button(onClick = { onAdd(step) }, colors = buttonColors, modifier = Modifier.weight(1f)) {
                Text("+ $step", fontSize = 20.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                player.history.takeLast(8).joinToString("  ") { signed(it) }.ifEmpty { " " },
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (player.history.isNotEmpty()) {
                TextButton(onClick = onUndo, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text("Annuler")
                }
            }
        }
    }
}

@Composable
private fun CustomStepDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val value = text.toIntOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pas personnalisé") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(4) },
                label = { Text("Points par appui") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
