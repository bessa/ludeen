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

class CategoryOrderingTest {

    private fun cats(vararg titles: String) = titles.mapIndexed { i, t -> Category(id = i.toLong(), sourceId = 1, title = t) }

    private fun List<Category>.titles() = map { it.title }

    @Test
    fun `languages are read from codes, names and script`() {
        assertEquals("Arabic", CategoryOrdering.languageOf("AR | Action"))
        assertEquals("Arabic", CategoryOrdering.languageOf("أفلام عربية"))
        assertEquals("Arabic", CategoryOrdering.languageOf("Netflix Arabic"))
        assertEquals("English", CategoryOrdering.languageOf("EN | Drama"))
        assertEquals("English", CategoryOrdering.languageOf("UK | Comedy"))
        assertEquals("English", CategoryOrdering.languageOf("Action | EN"))
        assertEquals("French", CategoryOrdering.languageOf("[FR] Comédie"))
        assertEquals("Turkish", CategoryOrdering.languageOf("TR: Diziler"))
        assertEquals(CategoryOrdering.OTHER, CategoryOrdering.languageOf("Action"))
    }

    @Test
    fun `provider order with no languages first is left alone`() {
        val input = cats("b", "a", "c")
        assertEquals(input, CategoryOrdering.sort(input, CategorySort.PROVIDER, emptyList()))
    }

    @Test
    fun `alphabetical ignores case and leading symbols`() {
        val sorted = CategoryOrdering.sort(cats("zeta", "★ Action", "beta"), CategorySort.ALPHABETICAL, emptyList())
        assertEquals(listOf("★ Action", "beta", "zeta"), sorted.titles())
    }

    @Test
    fun `by language groups languages alphabetically with Other last and keeps provider order inside`() {
        val sorted = CategoryOrdering.sort(
            cats("Action", "TR | Drama", "EN | Zed", "AR | Kids", "EN | Alpha"),
            CategorySort.LANGUAGE,
            emptyList(),
        )
        assertEquals(listOf("AR | Kids", "EN | Zed", "EN | Alpha", "TR | Drama", "Action"), sorted.titles())
    }

    @Test
    fun `languages first come first in the order given, whatever the sort`() {
        val input = cats("FR | Films", "EN | Drama", "Action", "AR | Comedy", "EN | Action", "AR | Action")
        val provider = CategoryOrdering.sort(input, CategorySort.PROVIDER, listOf("Arabic", "English"))
        assertEquals(
            listOf("AR | Comedy", "AR | Action", "EN | Drama", "EN | Action", "FR | Films", "Action"),
            provider.titles(),
        )
        val alpha = CategoryOrdering.sort(input, CategorySort.ALPHABETICAL, listOf("Arabic", "English"))
        assertEquals(
            listOf("AR | Action", "AR | Comedy", "EN | Action", "EN | Drama", "Action", "FR | Films"),
            alpha.titles(),
        )
    }
}
