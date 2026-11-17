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

/**
 * Which languages the Recently Added row on Movies and Series may show.
 *
 * A title has no language of its own here: the provider files it under a category, and the
 * category's name says the language — `AR | Action`, `EN - Drama`, `Asian Movies`. So the
 * filter works on category names, through the same [CategoryOrdering.languageOf] the shelves
 * are sorted by, and a title whose category names no language is in none of them.
 *
 * One step coarser than [CategoryOrdering.languageOf]: East and South-East Asian languages
 * collapse into [ASIAN], because that is how panels file them ("Asian Series", "KR | Drama",
 * "Thai Movies") and how a viewer asks for them. Hindi, Urdu and Bengali keep their own names —
 * panels give Indian and Pakistani films categories of their own, not an "Asian" one.
 */
object RecentLanguages {

    const val ASIAN = "Asian"

    /** What the row shows until the viewer chooses: Arabic and English titles. */
    val DEFAULT: List<String> = listOf("Arabic", "English")

    /**
     * The language group a category is in, or [CategoryOrdering.OTHER].
     *
     * Places the language reader knows but has no language for (Thailand, Vietnam) come back
     * as the place; the Asian ones are folded here.
     */
    fun groupOf(categoryTitle: String): String {
        val language = CategoryOrdering.languageOf(categoryTitle)
        return when {
            language in ASIAN_MEMBERS -> ASIAN
            language == CategoryOrdering.OTHER && ASIAN_WORD.containsMatchIn(categoryTitle) -> ASIAN
            else -> language
        }
    }

    /** Whether a title in [categoryTitle] belongs in the row. No choice at all means every language. */
    fun allows(chosen: Collection<String>, categoryTitle: String): Boolean =
        chosen.isEmpty() || groupOf(categoryTitle) in chosen

    /** [language] as a chip offers it: an Asian language becomes [ASIAN]. */
    fun fold(language: String): String = if (language in ASIAN_MEMBERS) ASIAN else language

    /**
     * The chips to offer: the chosen ones first, then Arabic and English, then whatever else this
     * playlist's categories are in ([detected], most shelves first), each once.
     */
    fun options(chosen: List<String>, detected: List<String>): List<String> =
        (chosen + DEFAULT + ASIAN + detected.map(::fold))
            .filter { it != CategoryOrdering.OTHER }
            .distinct()

    private val ASIAN_MEMBERS = setOf(
        "Korean",
        "Japanese",
        "Chinese",
        // Places the language reader returns as places, having no language mapped for them.
        "Thailand",
        "Vietnam",
        "Philippines",
        "Indonesia",
        "Malaysia",
    )

    private val ASIAN_WORD = Regex("""(^|[^\p{L}])(asian?|asie|asiatique|آسيوي)($|[^\p{L}])""", RegexOption.IGNORE_CASE)
}
