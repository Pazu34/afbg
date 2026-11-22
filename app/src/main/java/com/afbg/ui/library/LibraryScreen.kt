package com.afbg.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afbg.data.db.GameListItem
import com.afbg.ui.common.EmptyState
import com.afbg.ui.common.GameThumbnail
import com.afbg.ui.common.NoInsets
import com.afbg.ui.common.appViewModel
import com.afbg.ui.common.subtitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpenGame: (Long) -> Unit, onAddGame: () -> Unit, onSettings: () -> Unit) {
    val vm = appViewModel { LibraryViewModel(it.repository) }
    val games by vm.games.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val onlyLoaned by vm.onlyLoaned.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Ludothèque ($total)") },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Réglages") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGame) { Icon(Icons.Default.Add, "Ajouter un jeu") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                label = { Text("Rechercher un jeu") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            Row(Modifier.padding(horizontal = 12.dp)) {
                FilterChip(
                    selected = onlyLoaned,
                    onClick = { vm.onlyLoaned.value = !onlyLoaned },
                    label = { Text("Prêtés") },
                )
            }
            if (games.isEmpty()) {
                EmptyState(
                    if (total == 0) "Ta ludothèque est vide.\nAjoute un jeu avec le bouton +, ou importe ta collection BGG depuis les réglages."
                    else "Aucun jeu ne correspond."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(games, key = { it.game.id }) { item -> GameRow(item) { onOpenGame(item.game.id) } }
                }
            }
        }
    }
}

@Composable
private fun GameRow(item: GameListItem, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            GameThumbnail(item.game)
            Column(Modifier.padding(start = 12.dp)) {
                Text(item.game.name, style = MaterialTheme.typography.titleMedium)
                val sub = item.game.subtitle()
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall)
                val extra = listOfNotNull(
                    item.playCount.takeIf { it > 0 }?.let { "$it partie" + if (it > 1) "s" else "" },
                    item.borrower?.let { "Prêté à $it" },
                ).joinToString(" · ")
                if (extra.isNotEmpty()) {
                    Text(
                        extra,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.borrower != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
