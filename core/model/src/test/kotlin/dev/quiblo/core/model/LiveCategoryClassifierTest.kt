/*
 * Quiblo — a free, open source IPTV player.
 * Copyright (C) 2026 The Quiblo Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.quiblo.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LiveCategoryClassifierTest {

    private fun country(title: String) = LiveCategoryClassifier.countryOf(title)
    private fun type(title: String) = LiveCategoryClassifier.typeOf(title)

    @Test
    fun `reads a leading country or language code in the usual provider shapes`() {
        assertEquals("United Kingdom", country("UK | Sports"))
        assertEquals("France", country("FR: News HD"))
        assertEquals("Arabic", country("[AR] Kids"))
        assertEquals("Germany", country("DE - Entertainment"))
        assertEquals("United States", country("US/ Movies"))
        assertEquals("Arabic", country("|AR| MBC"))
    }

    @Test
    fun `reads a written-out country or language`() {
        assertEquals("Portugal", country("Portugal Desporto"))
        assertEquals("Turkey", country("Turkish Channels"))
        assertEquals("India", country("Hindi Movies"))
    }

    @Test
    fun `says other when the name carries no country`() {
        assertEquals(LiveCategoryClassifier.OTHER, country("Sports"))
        assertEquals(LiveCategoryClassifier.OTHER, country(""))
        // A short leading word that is not a known code is not mistaken for one.
        assertEquals(LiveCategoryClassifier.OTHER, country("ABC: Comedy"))
    }

    @Test
    fun `reads the type of channel`() {
        assertEquals("Sports", type("UK | Sports"))
        assertEquals("Sports", type("beIN SPORTS"))
        assertEquals("News", type("FR: News HD"))
        assertEquals("Kids", type("[AR] Kids"))
        assertEquals("Movies", type("DE Cinema"))
        assertEquals("Music", type("Music Channels"))
        assertEquals("Documentary", type("Docs & Discovery"))
        assertEquals("Entertainment", type("DE - Entertainment"))
        assertEquals(LiveCategoryClassifier.OTHER, type("UK | Local"))
    }

    @Test
    fun `matches whole words only`() {
        // "info" is a news word, but "Infotainment" must not be read as news.
        assertEquals(LiveCategoryClassifier.OTHER, type("Infotainment"))
        // "sport" inside a longer word is not sport.
        assertEquals(LiveCategoryClassifier.OTHER, type("Transportation"))
    }

    @Test
    fun `adult wins over anything else in the same name`() {
        assertEquals("Adult", type("XXX Movies"))
    }

    @Test
    fun `provider grouping leaves the title as it is`() {
        assertEquals("UK | Sports", LiveCategoryClassifier.groupOf("UK | Sports", LiveGrouping.PROVIDER))
        assertEquals("United Kingdom", LiveCategoryClassifier.groupOf("UK | Sports", LiveGrouping.COUNTRY))
        assertEquals("Sports", LiveCategoryClassifier.groupOf("UK | Sports", LiveGrouping.TYPE))
    }
}
