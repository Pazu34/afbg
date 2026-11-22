package com.afbg.ui.plays

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.afbg.ui.common.EmptyState
import com.afbg.ui.common.NoInsets
import com.afbg.ui.common.appViewModel
import com.afbg.util.Dates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaysScreen(onAddPlay: () -> Unit) {
    val vm = appViewModel { PlaysViewModel(it.repository) }
    val plays by vm.plays.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<PlayDetails?>(null) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Parties jouées (${plays.size})") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlay) { Icon(Icons.Default.Add, "Enregistrer une partie") }
        },
    ) { padding ->
        if (plays.isEmpty()) {
            EmptyState("Aucune partie enregistrée.\nUtilise le bouton + ou enregistre une partie depuis l'onglet Scores.", Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(plays, key = { it.play.id }) { details -> PlayCard(details, onDelete = { toDelete = details }) }
            }
        }
    }

    toDelete?.let { d ->
        ConfirmDialog(
            message = "Supprimer cette partie de « ${d.game.name} » ?",
            confirmLabel = "Supprimer",
            onConfirm = { vm.delete(d.play); toDelete = null },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun PlayCard(details: PlayDetails, onDelete: () -> Unit) {
    val play = details.play
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(details.game.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            Dates.format(play.playedAt),
                            play.durationMin?.let { "$it min" },
                            play.location.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Supprimer la partie") }
            }
            details.players.sortedByDescending { it.score ?: Int.MIN_VALUE }.forEach { p ->
                Text(
                    (if (p.winner) "🏆 " else "• ") + p.name + (p.score?.let { " — $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (play.notes.isNotBlank()) {
                Text(play.notes, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
