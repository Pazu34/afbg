package com.afbg.data.bgg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client de l'API XML2 de BoardGameGeek.
 * Depuis fin 2025, BGG exige un jeton d'application (https://boardgamegeek.com/applications),
 * envoyé en `Authorization: Bearer`.
 */
class BggClient(
    private val tokenProvider: () -> String,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val base = "https://boardgamegeek.com/xmlapi2"

    suspend fun search(query: String): List<BggSearchHit> {
        val url = "$base/search".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("type", "boardgame")
            .build()
        return BggParser.parseSearch(fetch(url))
    }

    /** L'API limite à 20 ids par requête. */
    suspend fun things(ids: List<Int>): List<BggGame> =
        ids.chunked(20).flatMap { chunk ->
            val url = "$base/thing".toHttpUrl().newBuilder()
                .addQueryParameter("id", chunk.joinToString(","))
                .addQueryParameter("stats", "1")
                .build()
            BggParser.parseThings(fetch(url))
        }

    /** Jeux possédés par [username] (extensions exclues). BGG répond 202 le temps de préparer la liste. */
    suspend fun collection(username: String): List<BggGame> {
        val url = "$base/collection".toHttpUrl().newBuilder()
            .addQueryParameter("username", username)
            .addQueryParameter("own", "1")
            .addQueryParameter("subtype", "boardgame")
            .addQueryParameter("excludesubtype", "boardgameexpansion")
            .build()
        return BggParser.parseCollection(fetch(url, retryWhileQueued = true))
    }

    private suspend fun fetch(url: HttpUrl, retryWhileQueued: Boolean = false): String {
        var attempt = 0
        while (true) {
            val (code, body) = withContext(Dispatchers.IO) { executeOnce(url) }
            when {
                code == 200 -> return body
                code == 202 && retryWhileQueued && attempt < 10 -> {
                    attempt++
                    delay(3_000)
                }
                code == 202 -> throw BggException("BGG prépare encore la réponse, réessaie dans un instant.", BggException.Kind.SERVER)
                code == 401 || code == 403 -> throw BggException(
                    "BGG a refusé l'accès : renseigne un jeton d'application valide dans les réglages.",
                    BggException.Kind.AUTH,
                )
                code == 429 -> throw BggException("Trop de requêtes vers BGG, patiente quelques secondes.", BggException.Kind.RATE_LIMIT)
                else -> throw BggException("BGG a répondu avec une erreur ($code).", BggException.Kind.SERVER)
            }
        }
    }

    private fun executeOnce(url: HttpUrl): Pair<Int, String> {
        val token = tokenProvider()
        val request = Request.Builder().url(url)
            .header("Accept", "application/xml")
            .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
            .build()
        return try {
            http.newCall(request).execute().use { it.code to (it.body?.string() ?: "") }
        } catch (e: IOException) {
            throw BggException("Impossible de joindre BGG (réseau indisponible ?).", BggException.Kind.NETWORK)
        }
    }
}
