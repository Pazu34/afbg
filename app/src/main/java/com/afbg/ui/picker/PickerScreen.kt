package com.afbg.ui.picker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afbg.ui.common.NoInsets
import com.afbg.ui.theme.PlayerPalette
import kotlin.math.roundToInt

private enum class PickerMode(val label: String) {
    FIRST("Premier joueur"),
    ORDER("Ordre"),
    TEAMS("Équipes"),
}

/** Un doigt dans le résultat final (position figée au moment du tirage). */
private data class Mark(val position: Offset, val color: Color, val label: String?, val highlighted: Boolean)

private const val COUNTDOWN_MS = 2_500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen() {
    var mode by rememberSaveable { mutableStateOf(PickerMode.FIRST) }
    var teams by rememberSaveable { mutableIntStateOf(2) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Premier joueur") }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PickerMode.entries.forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                }
            }
            if (mode == PickerMode.TEAMS) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Nombre d'équipes : $teams")
                    TextButton(onClick = { if (teams > 2) teams-- }) { Text("−") }
                    TextButton(onClick = { if (teams < 5) teams++ }) { Text("+") }
                }
            }
            FingerArea(mode = mode, teams = teams, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun FingerArea(mode: PickerMode, teams: Int, modifier: Modifier = Modifier) {
    val positions = remember { mutableStateMapOf<Long, Offset>() }
    val colors = remember { mutableStateMapOf<Long, Color>() }
    var result by remember { mutableStateOf<List<Mark>?>(null) }
    val progress = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val ids by remember { derivedStateOf { positions.keys.toSet() } }
    val needed = if (mode == PickerMode.TEAMS) maxOf(teams, 2) else 2

    LaunchedEffect(ids, mode, teams) {
        if (result != null) {
            // Résultat affiché : on attend que tous les doigts soient levés pour recommencer.
            if (ids.isEmpty()) {
                result = null
                colors.clear()
                reveal.snapTo(0f)
                progress.snapTo(0f)
            }
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        if (ids.size < needed) return@LaunchedEffect
        progress.animateTo(1f, tween(COUNTDOWN_MS, easing = LinearEasing))
        result = computeResult(mode, teams, ids.toList(), positions, colors)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(600))
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulseValue",
    )

    val hint = when {
        result != null -> "Lève tous les doigts pour recommencer"
        ids.isEmpty() -> "Chacun pose un doigt sur l'écran"
        ids.size < needed -> "Il faut au moins $needed doigts"
        else -> "Ne bougez plus…"
    }
    val background = result?.firstOrNull { it.highlighted && mode == PickerMode.FIRST }
        ?.color?.copy(alpha = 0.25f * reveal.value) ?: Color.Transparent

    Box(
        modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            val id = change.id.value
                            if (change.pressed) {
                                if (result == null) {
                                    positions[id] = change.position
                                    if (id !in colors) colors[id] = nextColor(colors)
                                }
                            } else {
                                positions.remove(id)
                                if (result == null) colors.remove(id)
                            }
                            change.consume()
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(background)
            val base = 44.dp.toPx()
            val res = result
            if (res == null) {
                positions.forEach { (id, pos) ->
                    val color = colors[id] ?: Color.Gray
                    drawCircle(color.copy(alpha = 0.9f), radius = base * 0.7f, center = pos)
                    drawCircle(
                        color, radius = base * (1.15f + 0.15f * pulse), center = pos,
                        style = Stroke(width = 6.dp.toPx()),
                    )
                    val ring = base * 1.6f
                    drawArc(
                        color = Color.White.copy(alpha = 0.9f),
                        startAngle = -90f,
                        sweepAngle = 360f * progress.value,
                        useCenter = false,
                        topLeft = pos - Offset(ring, ring),
                        size = Size(ring * 2, ring * 2),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            } else {
                res.forEach { mark ->
                    when {
                        mode == PickerMode.FIRST && mark.highlighted -> {
                            drawCircle(mark.color, radius = base * (1f + 2.5f * reveal.value), center = mark.position)
                        }
                        mode == PickerMode.FIRST ->
                            drawCircle(mark.color.copy(alpha = 0.25f), radius = base * 0.6f, center = mark.position)
                        else -> drawCircle(mark.color, radius = base * (0.6f + 0.5f * reveal.value), center = mark.position)
                    }
                }
            }
        }

        // Libellés (rang, équipe) posés au-dessus du Canvas.
        result?.forEach { mark ->
            val label = mark.label ?: return@forEach
            val half = with(density) { 36.dp.toPx() }
            Box(
                Modifier
                    .offset { IntOffset((mark.position.x - half).roundToInt(), (mark.position.y - half).roundToInt()) }
                    .size(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            hint,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun nextColor(used: SnapshotStateMap<Long, Color>): Color =
    PlayerPalette.firstOrNull { it !in used.values } ?: PlayerPalette[used.size % PlayerPalette.size]

private fun computeResult(
    mode: PickerMode,
    teams: Int,
    ids: List<Long>,
    positions: Map<Long, Offset>,
    colors: Map<Long, Color>,
): List<Mark> {
    val live = ids.filter { it in positions }
    return when (mode) {
        PickerMode.FIRST -> {
            val winner = live.random()
            live.map { id -> Mark(positions.getValue(id), colors[id] ?: Color.Gray, null, id == winner) }
        }
        PickerMode.ORDER -> live.shuffled().mapIndexed { index, id ->
            Mark(positions.getValue(id), colors[id] ?: Color.Gray, (index + 1).toString(), index == 0)
        }
        PickerMode.TEAMS -> live.shuffled().mapIndexed { index, id ->
            val team = index % maxOf(teams, 2)
            Mark(positions.getValue(id), PlayerPalette[team % PlayerPalette.size], ('A' + team).toString(), false)
        }
    }
}
