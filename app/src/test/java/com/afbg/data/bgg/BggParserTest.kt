package com.afbg.data.bgg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BggParserTest {

    @Test
    fun parseSearch_prefersPrimaryName() {
        val xml = """
            <items total="1">
              <item type="boardgame" id="13">
                <name type="alternate" value="Die Siedler von Catan"/>
                <name type="primary" value="CATAN"/>
                <yearpublished value="1995"/>
              </item>
              <item type="boardgame" id="14"><name type="primary" value="Sans année"/></item>
            </items>
        """.trimIndent()
        val hits = BggParser.parseSearch(xml)
        assertEquals(2, hits.size)
        assertEquals(BggSearchHit(13, "CATAN", 1995), hits[0])
        assertNull(hits[1].year)
    }

    @Test
    fun parseThings_readsAllFields() {
        val xml = """
            <items>
              <item type="boardgame" id="13">
                <thumbnail>https://img/thumb.jpg</thumbnail>
                <image>https://img/full.jpg</image>
                <name type="primary" sortindex="1" value="CATAN"/>
                <description>Trade &amp;mdash; build.&amp;#10;&amp;#10;&amp;#10;&amp;#10;Settle &amp;amp; win.</description>
                <yearpublished value="1995"/>
                <minplayers value="3"/>
                <maxplayers value="4"/>
                <playingtime value="120"/>
                <statistics page="1"><ratings><average value="7.1"/></ratings></statistics>
              </item>
            </items>
        """.trimIndent()
        val g = BggParser.parseThings(xml).single()
        assertEquals(13, g.bggId)
        assertEquals("CATAN", g.name)
        assertEquals(3, g.minPlayers)
        assertEquals(4, g.maxPlayers)
        assertEquals(120, g.playingTime)
        assertEquals(7.1, g.rating!!, 0.0001)
        assertEquals("https://img/thumb.jpg", g.thumbnailUrl)
        assertEquals("Trade — build.\n\nSettle & win.", g.description)
    }

    @Test
    fun parseThings_zeroValuesBecomeNull() {
        val xml = """
            <items><item type="boardgame" id="1">
              <name type="primary" value="X"/>
              <yearpublished value="0"/><minplayers value="0"/><playingtime value="0"/>
              <statistics><ratings><average value="0"/></ratings></statistics>
            </item></items>
        """.trimIndent()
        val g = BggParser.parseThings(xml).single()
        assertNull(g.year); assertNull(g.minPlayers); assertNull(g.playingTime); assertNull(g.rating)
    }

    @Test
    fun parseCollection_readsStats() {
        val xml = """
            <items totalitems="1">
              <item objecttype="thing" objectid="822" subtype="boardgame">
                <name sortindex="1">Carcassonne</name>
                <yearpublished>2000</yearpublished>
                <image>https://img/c.jpg</image>
                <thumbnail>https://img/ct.jpg</thumbnail>
                <stats minplayers="2" maxplayers="5" playingtime="45">
                  <rating value="N/A"><average value="7.4"/></rating>
                </stats>
              </item>
            </items>
        """.trimIndent()
        val g = BggParser.parseCollection(xml).single()
        assertEquals(822, g.bggId)
        assertEquals("Carcassonne", g.name)
        assertEquals(2000, g.year)
        assertEquals(2, g.minPlayers)
        assertEquals(5, g.maxPlayers)
        assertEquals(45, g.playingTime)
        assertEquals(7.4, g.rating!!, 0.0001)
    }

    @Test(expected = BggException::class)
    fun invalidXml_throwsParseException() {
        BggParser.parseSearch("not xml at all")
    }
}
