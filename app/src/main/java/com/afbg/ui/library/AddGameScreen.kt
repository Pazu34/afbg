package com.afbg.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afbg.ui.common.NoInsets
import com.afbg.ui.common.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(onBack: () -> Unit) {
    val vm = appViewModel { AddGameViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val owned by vm.ownedBggIds.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var manual by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un jeu") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQuery,
                    label = { Text("Rechercher sur BoardGameGeek") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = vm::search) { Icon(Icons.Default.Search, "Rechercher") }
            }
            OutlinedButton(onClick = { manual = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Ajouter manuellement")
            }
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else if (state.searched && state.results.isEmpty()) {
                Text("Aucun résultat.")
            }
            LazyColumn {
                items(state.results, key = { it.bggId }) { hit ->
                    val isOwned = hit.bggId in owned
                    ListItem(
                        headlineContent = { Text(hit.name) },
                        supportingContent = { hit.year?.let { Text("$it") } },
                        trailingContent = {
                            when {
                                state.addingId == hit.bggId -> CircularProgressIndicator(Modifier.padding(4.dp))
                                isOwned -> Icon(Icons.Default.Check, "Déjà dans la ludothèque")
                                else -> TextButton(onClick = { vm.add(hit) }) { Text("Ajouter") }
                            }
                        },
                        modifier = Modifier.clickable(enabled = !isOwned && state.addingId == null) { vm.add(hit) },
                    )
                }
            }
        }
    }

    if (manual) {
        ManualGameDialog(
            onConfirm = { name, year, min, max, time ->
                vm.addManual(name, year, min, max, time)
                manual = false
            },
            onDismiss = { manual = false },
        )
    }
}

@Composable
private fun ManualGameDialog(
    onConfirm: (String, Int?, Int?, Int?, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("") }
    var max by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau jeu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true)
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Année (facultatif)") }, singleLine = true, keyboardOptions = numeric)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(min, { min = it.filter(Char::isDigit).take(2) }, label = { Text("Joueurs min") }, singleLine = true, keyboardOptions = numeric, modifier = Modifier.weight(1f))
                    OutlinedTextField(max, { max = it.filter(Char::isDigit).take(2) }, label = { Text("Joueurs max") }, singleLine = true, keyboardOptions = numeric, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(time, { time = it.filter(Char::isDigit).take(4) }, label = { Text("Durée en min (facultatif)") }, singleLine = true, keyboardOptions = numeric)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, year.toIntOrNull(), min.toIntOrNull(), max.toIntOrNull(), time.toIntOrNull()) },
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
