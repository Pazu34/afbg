package com.afbg.ui.score

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ScorePlayer(
    val id: Long,
    val name: String,
    val colorIndex: Int,
    val score: Int = 0,
    /** Historique des variations (la plus récente en dernier). */
    val history: List<Int> = emptyList(),
)

data class ScoreBoard(
    val players: List<ScorePlayer> = listOf(
        ScorePlayer(1, "Joueur 1", 0),
        ScorePlayer(2, "Joueur 2", 1),
    ),
    val step: Int = 1,
    val steps: List<Int> = listOf(1, 5, 10),
    val nextId: Long = 3,
)

/** Sauvegarde du tableau de scores en cours (JSON dans les SharedPreferences). */
class ScoreStore(context: Context) {
    private val prefs = context.getSharedPreferences("afbg_score", Context.MODE_PRIVATE)

    fun load(): ScoreBoard {
        val raw = prefs.getString(KEY, null) ?: return ScoreBoard()
        return try {
            val o = JSONObject(raw)
            val players = o.getJSONArray("players").let { arr ->
                (0 until arr.length()).map { i ->
                    val p = arr.getJSONObject(i)
                    ScorePlayer(
                        id = p.getLong("id"),
                        name = p.getString("name"),
                        colorIndex = p.getInt("color"),
                        score = p.getInt("score"),
                        history = p.getJSONArray("history").let { h -> (0 until h.length()).map { h.getInt(it) } },
                    )
                }
            }
            ScoreBoard(
                players = players,
                step = o.getInt("step"),
                steps = o.getJSONArray("steps").let { s -> (0 until s.length()).map { s.getInt(it) } },
                nextId = o.getLong("nextId"),
            )
        } catch (e: Exception) {
            ScoreBoard()
        }
    }

    fun save(board: ScoreBoard) {
        val o = JSONObject()
        o.put("step", board.step)
        o.put("nextId", board.nextId)
        o.put("steps", JSONArray(board.steps))
        o.put(
            "players",
            JSONArray().also { arr ->
                board.players.forEach { p ->
                    arr.put(
                        JSONObject()
                            .put("id", p.id)
                            .put("name", p.name)
                            .put("color", p.colorIndex)
                            .put("score", p.score)
                            .put("history", JSONArray(p.history))
                    )
                }
            },
        )
        prefs.edit().putString(KEY, o.toString()).apply()
    }

    private companion object {
        const val KEY = "board"
    }
}
