package com.afbg.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.afbg.data.AppSettings
import com.afbg.data.Repository
import com.afbg.ui.common.NoInsets
import com.afbg.ui.common.appViewModel
import com.afbg.ui.library.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImportUiState(val running: Boolean = false, val message: String? = null)

class SettingsViewModel(private val repo: Repository, val settings: AppSettings) : ViewModel() {
    private val _import = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _import.asStateFlow()

    fun importCollection(username: String) {
        if (_import.value.running || username.isBlank()) return
        settings.bggUsername = username
        _import.value = ImportUiState(running = true)
        viewModelScope.launch {
            _import.value = try {
                val r = repo.importCollection(username.trim())
                ImportUiState(message = "${r.found} jeu(x) trouvé(s) sur BGG, ${r.added} ajouté(s).")
            } catch (e: Exception) {
                ImportUiState(message = e.userMessage())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it.repository, it.settings) }
    val importState by vm.importState.collectAsStateWithLifecycle()
    var token by remember { mutableStateOf(vm.settings.bggToken) }
    var username by remember { mutableStateOf(vm.settings.bggUsername) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("BoardGameGeek", style = MaterialTheme.typography.titleLarge)
            Text(
                "BGG exige un jeton d'application pour son API. Crée une application sur " +
                    "boardgamegeek.com/applications puis colle ici le jeton obtenu. " +
                    "Il reste stocké sur ton téléphone uniquement.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it; vm.settings.bggToken = it },
                label = { Text("Jeton d'application BGG") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Importer ma collection", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Pseudo BGG") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.importCollection(username) },
                    enabled = !importState.running && username.isNotBlank(),
                ) { Text("Importer les jeux possédés") }
                if (importState.running) CircularProgressIndicator(Modifier.padding(4.dp))
            }
            importState.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (importState.running) {
                Text("BGG prépare la liste, cela peut prendre une trentaine de secondes…", style = MaterialTheme.typography.bodySmall)
            }

            Text("Données fournies par BoardGameGeek.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
