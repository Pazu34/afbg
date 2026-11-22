package com.afbg.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.afbg.ui.common.NoInsets
import com.afbg.ui.library.AddGameScreen
import com.afbg.ui.library.GameDetailScreen
import com.afbg.ui.library.LibraryScreen
import com.afbg.ui.loans.LoansScreen
import com.afbg.ui.picker.PickerScreen
import com.afbg.ui.plays.AddPlayScreen
import com.afbg.ui.plays.PlaysScreen
import com.afbg.ui.score.ScoreScreen
import com.afbg.ui.settings.SettingsScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Picker("picker", "Premier joueur", Icons.Default.Face),
    Score("score", "Scores", Icons.Default.Add),
    Library("library", "Ludothèque", Icons.AutoMirrored.Filled.List),
    Plays("plays", "Parties", Icons.Default.DateRange),
    Loans("loans", "Prêts", Icons.Default.Share),
}

private const val ROUTE_ADD_GAME = "library/add"
private const val ROUTE_GAME = "game/{id}"
private const val ROUTE_NEW_PLAY = "plays/new?gameId={gameId}&fromScore={fromScore}"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = Tab.entries.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

    Scaffold(
        contentWindowInsets = NoInsets,
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        val contentModifier = Modifier
            .padding(bottom = inner.calculateBottomPadding())
            .then(if (showBar) Modifier else Modifier.windowInsetsPadding(WindowInsets.navigationBars))

        NavHost(nav, startDestination = Tab.Picker.route, modifier = contentModifier) {
            composable(Tab.Picker.route) { PickerScreen() }
            composable(Tab.Score.route) {
                ScoreScreen(onSaveGame = { nav.navigate("plays/new?fromScore=true") })
            }
            composable(Tab.Library.route) {
                LibraryScreen(
                    onOpenGame = { nav.navigate("game/$it") },
                    onAddGame = { nav.navigate(ROUTE_ADD_GAME) },
                    onSettings = { nav.navigate(ROUTE_SETTINGS) },
                )
            }
            composable(Tab.Plays.route) { PlaysScreen(onAddPlay = { nav.navigate("plays/new") }) }
            composable(Tab.Loans.route) { LoansScreen(onOpenGame = { nav.navigate("game/$it") }) }

            composable(ROUTE_ADD_GAME) { AddGameScreen(onBack = { nav.popBackStack() }) }
            composable(ROUTE_GAME, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                GameDetailScreen(
                    gameId = id,
                    onBack = { nav.popBackStack() },
                    onAddPlay = { nav.navigate("plays/new?gameId=$id") },
                )
            }
            composable(
                ROUTE_NEW_PLAY,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("fromScore") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                val gameId = entry.arguments?.getLong("gameId") ?: -1L
                val fromScore = entry.arguments?.getBoolean("fromScore") ?: false
                AddPlayScreen(
                    initialGameId = gameId.takeIf { it > 0 },
                    fromScore = fromScore,
                    onDone = { nav.popBackStack() },
                )
            }
            composable(ROUTE_SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
