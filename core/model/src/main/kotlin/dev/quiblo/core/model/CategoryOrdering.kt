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

/** How the Movies and Series shelves are ordered. */
enum class CategorySort {
    /** The provider's own order, with any shelves the viewer moved by hand at the top. */
    PROVIDER,

    /** Alphabetical by the name shown on screen. */
    ALPHABETICAL,

    /** Grouped by language, languages alphabetical, the provider's order kept inside each. */
    LANGUAGE,
}

/**
 * Orders Movies and Series categories for the shelves.
 *
 * [languagesFirst] is the "Arabic first, then English" part: categories in those languages come
 * before everything else, in the order listed, whichever [CategorySort] is chosen. Inside that
 * front block, and in the rest, the chosen sort applies.
 *
 * Pure: the language of a category is read from its provider name — see [languageOf].
 */
object CategoryOrdering {

    /** The language heading for a category that names none. */
    const val OTHER = LiveCategoryClassifier.OTHER

    fun sort(categories: List<Category>, sort: CategorySort, languagesFirst: List<String>): List<Category> {
        if (sort == CategorySort.PROVIDER && languagesFirst.isEmpty()) return categories

        val language = categories.associate { it.title to languageOf(it.title) }
        val priority = languagesFirst.withIndex().associate { (index, name) -> name to index }

        // Stable sorts throughout, so ties keep the order they arrived in — the provider's.
        val within: Comparator<Category> = when (sort) {
            CategorySort.PROVIDER -> Comparator { _, _ -> 0 }
            CategorySort.ALPHABETICAL -> compareBy(String.CASE_INSENSITIVE_ORDER) { alphabeticalKey(it.displayTitle) }
            CategorySort.LANGUAGE -> compareBy<Category> { language.getValue(it.title) == OTHER }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { language.getValue(it.title) }
        }
        return categories.sortedWith(
            compareBy<Category> { priority[language.getValue(it.title)] ?: Int.MAX_VALUE }.then(within),
        )
    }

    /**
     * The language a category is in, read from its name, or [OTHER].
     *
     * Arabic script anywhere in the name means Arabic. Otherwise a code at either end (`AR |`,
     * `| EN`, `[FR]`) or a country or language written out, mapped from country to the language
     * spoken there, so `UK | Drama` and `USA Movies` are both English.
     */
    fun languageOf(categoryTitle: String): String {
        val title = categoryTitle.trim()
        if (title.isEmpty()) return OTHER
        if (title.any { it in '؀'..'ۿ' || it in 'ݐ'..'ݿ' }) return ARABIC

        val lower = title.lowercase()
        LANGUAGE_WORDS.firstOrNull { (word, _) -> wholeWord(word).containsMatchIn(lower) }?.let { return it.second }

        var place = LiveCategoryClassifier.countryOf(title)
        if (place == OTHER) {
            // A trailing code, which the live classifier does not look for: "Action | EN".
            TRAILING_CODE.find(title)?.groupValues?.get(1)?.let { code ->
                place = LiveCategoryClassifier.countryOf("$code |")
            }
        }
        return COUNTRY_LANGUAGE[place] ?: place
    }

    /** Leading symbols and spaces skipped, so "★ Action" sorts under A and not before it. */
    private fun alphabeticalKey(title: String): String = title.trimStart { !it.isLetterOrDigit() }

    private const val ARABIC = "Arabic"
    private const val ENGLISH = "English"

    private val TRAILING_CODE = Regex("""[\[(|:\-/]\s*([A-Za-z]{2,3})\s*[\])|]?\s*$""")

    private val boundaryCache = HashMap<String, Regex>()

    private fun wholeWord(word: String): Regex = synchronized(boundaryCache) {
        boundaryCache.getOrPut(word) { Regex("(^|[^\\p{L}])" + Regex.escape(word) + "(\$|[^\\p{L}])") }
    }

    /** Language names written out, checked before countries so "English" is not read as a place. */
    private val LANGUAGE_WORDS: List<Pair<String, String>> = listOf(
        "arabic" to ARABIC, "arab" to ARABIC, "عربي" to ARABIC,
        "english" to ENGLISH, "anglais" to ENGLISH,
        "french" to "French", "français" to "French", "francais" to "French", "vf" to "French", "vostfr" to "French",
        "german" to "German", "deutsch" to "German",
        "spanish" to "Spanish", "español" to "Spanish", "espanol" to "Spanish", "castellano" to "Spanish",
        "italian" to "Italian", "italiano" to "Italian",
        "portuguese" to "Portuguese", "português" to "Portuguese",
        "turkish" to "Turkish", "türkçe" to "Turkish", "turkce" to "Turkish",
        "hindi" to "Hindi", "bollywood" to "Hindi", "urdu" to "Urdu", "persian" to "Persian", "farsi" to "Persian",
        "kurdish" to "Kurdish", "russian" to "Russian", "dutch" to "Dutch", "polish" to "Polish",
        "korean" to "Korean", "k-drama" to "Korean", "kdrama" to "Korean", "japanese" to "Japanese", "anime" to "Japanese",
        "chinese" to "Chinese", "greek" to "Greek", "albanian" to "Albanian", "romanian" to "Romanian",
    )

    /** From the live classifier's places to the language a film from there is in. */
    private val COUNTRY_LANGUAGE: Map<String, String> = mapOf(
        "United Kingdom" to ENGLISH, "United States" to ENGLISH, "Canada" to ENGLISH, "Ireland" to ENGLISH,
        "Australia" to ENGLISH, "New Zealand" to ENGLISH, "Caribbean" to ENGLISH,
        "Arabic" to ARABIC, "Morocco" to ARABIC, "Algeria" to ARABIC, "Tunisia" to ARABIC, "Egypt" to ARABIC,
        "Lebanon" to ARABIC, "Iraq" to ARABIC, "Syria" to ARABIC, "Jordan" to ARABIC, "Saudi Arabia" to ARABIC,
        "Kuwait" to ARABIC, "Qatar" to ARABIC, "UAE" to ARABIC,
        "France" to "French", "Belgium" to "French", "Switzerland" to "French",
        "Germany" to "German", "Austria" to "German",
        "Spain" to "Spanish", "Mexico" to "Spanish", "Latin America" to "Spanish",
        "Portugal" to "Portuguese", "Brazil" to "Portuguese",
        "Italy" to "Italian", "Turkey" to "Turkish", "Netherlands" to "Dutch", "Poland" to "Polish",
        "Russia" to "Russian", "Ukraine" to "Ukrainian", "Greece" to "Greek", "Romania" to "Romanian",
        "Albania" to "Albanian", "India" to "Hindi", "Pakistan" to "Urdu", "Iran" to "Persian",
        "Afghanistan" to "Persian", "Bangladesh" to "Bengali", "China" to "Chinese", "Japan" to "Japanese",
        "Korea" to "Korean", "Israel" to "Hebrew", "Sweden" to "Swedish", "Norway" to "Norwegian",
        "Denmark" to "Danish", "Finland" to "Finnish",
    )
}
