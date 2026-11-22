package com.afbg.data.bgg

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/** Parseur des réponses XML de BGG (XML API2). Pur JVM : testable sans Android. */
object BggParser {

    fun parseSearch(xml: String): List<BggSearchHit> =
        items(parse(xml)).mapNotNull { item ->
            val id = item.getAttribute("id").toIntOrNull() ?: return@mapNotNull null
            val names = item.children("name")
            val name = (names.firstOrNull { it.getAttribute("type") == "primary" } ?: names.firstOrNull())
                ?.getAttribute("value")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            BggSearchHit(id, name, item.child("yearpublished")?.getAttribute("value")?.toIntOrNull())
        }

    /** Réponse de `thing?id=…&stats=1`. */
    fun parseThings(xml: String): List<BggGame> =
        items(parse(xml)).mapNotNull { item ->
            val id = item.getAttribute("id").toIntOrNull() ?: return@mapNotNull null
            val names = item.children("name")
            val name = (names.firstOrNull { it.getAttribute("type") == "primary" } ?: names.firstOrNull())
                ?.getAttribute("value")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            BggGame(
                bggId = id,
                name = name,
                year = item.child("yearpublished")?.getAttribute("value")?.toIntOrNull()?.takeIf { it > 0 },
                minPlayers = item.child("minplayers")?.getAttribute("value")?.positiveInt(),
                maxPlayers = item.child("maxplayers")?.getAttribute("value")?.positiveInt(),
                playingTime = item.child("playingtime")?.getAttribute("value")?.positiveInt(),
                imageUrl = item.child("image")?.textContent?.trim()?.takeIf { it.isNotEmpty() },
                thumbnailUrl = item.child("thumbnail")?.textContent?.trim()?.takeIf { it.isNotEmpty() },
                rating = item.child("statistics")?.child("ratings")?.child("average")
                    ?.getAttribute("value")?.toDoubleOrNull()?.takeIf { it > 0 },
                description = item.child("description")?.textContent?.let(::cleanDescription)?.takeIf { it.isNotBlank() },
            )
        }

    /** Réponse de `collection?username=…` (stats incluses par défaut). */
    fun parseCollection(xml: String): List<BggGame> =
        items(parse(xml)).mapNotNull { item ->
            val id = item.getAttribute("objectid").toIntOrNull() ?: return@mapNotNull null
            val name = item.child("name")?.textContent?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val stats = item.child("stats")
            BggGame(
                bggId = id,
                name = name,
                year = item.child("yearpublished")?.textContent?.trim()?.toIntOrNull()?.takeIf { it > 0 },
                minPlayers = stats?.getAttribute("minplayers")?.positiveInt(),
                maxPlayers = stats?.getAttribute("maxplayers")?.positiveInt(),
                playingTime = stats?.getAttribute("playingtime")?.positiveInt(),
                imageUrl = item.child("image")?.textContent?.trim()?.takeIf { it.isNotEmpty() },
                thumbnailUrl = item.child("thumbnail")?.textContent?.trim()?.takeIf { it.isNotEmpty() },
                rating = stats?.child("rating")?.child("average")
                    ?.getAttribute("value")?.toDoubleOrNull()?.takeIf { it > 0 },
                description = null,
            )
        }

    private val namedEntities = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "mdash" to "—", "ndash" to "–", "rsquo" to "’", "lsquo" to "‘", "ldquo" to "“", "rdquo" to "”",
        "hellip" to "…", "times" to "×", "bull" to "•",
    )

    /** Les descriptions BGG sont encodées deux fois (entités HTML dans le texte XML). */
    fun cleanDescription(raw: String): String {
        var s = raw.replace(Regex("<[^>]+>"), "")
        s = Regex("&#(\\d+);").replace(s) { m ->
            m.groupValues[1].toIntOrNull()?.let { cp -> String(Character.toChars(cp)) } ?: m.value
        }
        s = Regex("&([a-zA-Z]+);").replace(s) { m -> namedEntities[m.groupValues[1]] ?: m.value }
        return s.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun String.positiveInt(): Int? = toIntOrNull()?.takeIf { it > 0 }

    private fun parse(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        return try {
            factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        } catch (e: Exception) {
            throw BggException("Réponse BGG illisible", BggException.Kind.PARSE)
        }
    }

    private fun items(doc: Document): List<Element> {
        val nodes = doc.getElementsByTagName("item")
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun Element.children(tag: String): List<Element> {
        val out = ArrayList<Element>()
        val nodes = childNodes
        for (i in 0 until nodes.length) {
            val n = nodes.item(i)
            if (n is Element && n.tagName == tag) out.add(n)
        }
        return out
    }

    private fun Element.child(tag: String): Element? = children(tag).firstOrNull()
}
