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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecentLanguagesTest {

    @Test
    fun `arabic and english categories are read from their names`() {
        assertEquals("Arabic", RecentLanguages.groupOf("AR | Action"))
        assertEquals("Arabic", RecentLanguages.groupOf("أفلام عربية"))
        assertEquals("English", RecentLanguages.groupOf("EN - Drama"))
        assertEquals("English", RecentLanguages.groupOf("UK | Comedy"))
        assertEquals("French", RecentLanguages.groupOf("FR | Films"))
    }

    @Test
    fun `east and south-east asian categories fold into asian`() {
        assertEquals(RecentLanguages.ASIAN, RecentLanguages.groupOf("KR | Drama"))
        assertEquals(RecentLanguages.ASIAN, RecentLanguages.groupOf("Japanese Anime"))
        assertEquals(RecentLanguages.ASIAN, RecentLanguages.groupOf("Chinese Movies"))
        assertEquals(RecentLanguages.ASIAN, RecentLanguages.groupOf("TH | Movies"))
        assertEquals(RecentLanguages.ASIAN, RecentLanguages.groupOf("Asian Series"))
    }

    @Test
    fun `indian titles keep their own language`() {
        assertEquals("Hindi", RecentLanguages.groupOf("Bollywood"))
    }

    @Test
    fun `a category naming no language is allowed only when nothing is chosen`() {
        assertTrue(RecentLanguages.allows(emptyList(), "Action"))
        assertFalse(RecentLanguages.allows(RecentLanguages.DEFAULT, "Action"))
    }

    @Test
    fun `the default lets arabic and english through and nothing else`() {
        assertTrue(RecentLanguages.allows(RecentLanguages.DEFAULT, "AR | Action"))
        assertTrue(RecentLanguages.allows(RecentLanguages.DEFAULT, "EN | Action"))
        assertFalse(RecentLanguages.allows(RecentLanguages.DEFAULT, "FR | Action"))
        assertFalse(RecentLanguages.allows(RecentLanguages.DEFAULT, "KR | Drama"))
    }

    @Test
    fun `options put the choice first, then the defaults, and fold what the playlist has`() {
        assertEquals(
            listOf("French", "Arabic", "English", "Asian", "Turkish"),
            RecentLanguages.options(
                chosen = listOf("French"),
                detected = listOf("Korean", "Turkish", "French", CategoryOrdering.OTHER, "Japanese"),
            ),
        )
    }
}
