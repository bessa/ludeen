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

/** How the Live screen groups channels. */
enum class LiveGrouping {
    /** The provider's own categories, as they arrive. */
    PROVIDER,

    /** By country or language, read from the provider's category names. */
    COUNTRY,

    /** By type of channel — sport, news, kids and so on — read from the category names. */
    TYPE,
}

/**
 * Reads a country/language and a channel type out of a provider's category name.
 *
 * Provider category names are where this information lives: `UK | Sports`, `FR: News HD`,
 * `[AR] Kids`, `DE - Entertainment`, `Portugal Desporto`. Playlists rarely carry anything better
 * (`tvg-country` and `tvg-language` are absent from almost every real one), so the name is read.
 *
 * Pure and stateless, so it can be applied to a few hundred category titles each time the rail is
 * drawn without caching.
 */
object LiveCategoryClassifier {

    /** The heading for channels whose category says nothing about the asked-for attribute. */
    const val OTHER = "Other"

    /** A country or language name, or [OTHER]. */
    fun countryOf(categoryTitle: String): String {
        val title = categoryTitle.trim()
        if (title.isEmpty()) return OTHER

        // A leading code: "UK | ...", "FR: ...", "[AR] ...", "DE - ...", "US/...", "|EN| ...".
        val code = LEADING_CODE.find(title)?.groupValues?.get(1)?.uppercase()
        CODES[code]?.let { return it }

        // A country or language written out anywhere in the name.
        val lower = title.lowercase()
        NAMES.firstOrNull { (needle, _) -> wholeWord(needle).containsMatchIn(lower) }?.let { return it.second }

        return OTHER
    }

    /** A channel type, or [OTHER]. */
    fun typeOf(categoryTitle: String): String {
        val lower = categoryTitle.lowercase()
        return TYPES.firstOrNull { (_, words) -> words.any { wholeWord(it).containsMatchIn(lower) } }?.first
            ?: OTHER
    }

    /** The group [categoryTitle] belongs in under [grouping], or the title itself for [LiveGrouping.PROVIDER]. */
    fun groupOf(categoryTitle: String, grouping: LiveGrouping): String = when (grouping) {
        LiveGrouping.PROVIDER -> categoryTitle
        LiveGrouping.COUNTRY -> countryOf(categoryTitle)
        LiveGrouping.TYPE -> typeOf(categoryTitle)
    }

    private val LEADING_CODE = Regex("""^[\[(|]?\s*([A-Za-z]{2,3})\s*[\])|:\-/]""")

    private val boundaryCache = HashMap<String, Regex>()

    /** [word] as a whole word: not preceded or followed by another letter. */
    private fun wholeWord(word: String): Regex = synchronized(boundaryCache) {
        boundaryCache.getOrPut(word) { Regex("(^|[^\\p{L}])" + Regex.escape(word) + "(\$|[^\\p{L}])") }
    }

    private val CODES: Map<String, String> = mapOf(
        "UK" to "United Kingdom", "GB" to "United Kingdom", "EN" to "English",
        "US" to "United States", "USA" to "United States", "CA" to "Canada",
        "FR" to "France", "DE" to "Germany", "GER" to "Germany", "IT" to "Italy", "ES" to "Spain",
        "PT" to "Portugal", "BR" to "Brazil", "NL" to "Netherlands", "BE" to "Belgium",
        "CH" to "Switzerland", "AT" to "Austria", "PL" to "Poland", "RO" to "Romania",
        "GR" to "Greece", "TR" to "Turkey", "RU" to "Russia", "UA" to "Ukraine",
        "SE" to "Sweden", "NO" to "Norway", "DK" to "Denmark", "FI" to "Finland",
        "IE" to "Ireland", "AL" to "Albania", "RS" to "Serbia", "HR" to "Croatia",
        "BG" to "Bulgaria", "HU" to "Hungary", "CZ" to "Czechia", "SK" to "Slovakia",
        "AR" to "Arabic", "ARA" to "Arabic", "MA" to "Morocco", "DZ" to "Algeria", "TN" to "Tunisia",
        "EG" to "Egypt", "IL" to "Israel", "IR" to "Iran", "IN" to "India", "PK" to "Pakistan",
        "AF" to "Afghanistan", "BD" to "Bangladesh", "CN" to "China", "JP" to "Japan",
        "KR" to "Korea", "PH" to "Philippines", "TH" to "Thailand", "VN" to "Vietnam",
        "ID" to "Indonesia", "MY" to "Malaysia", "AU" to "Australia", "NZ" to "New Zealand",
        "MX" to "Mexico", "LAT" to "Latin America", "LATAM" to "Latin America",
        "AFR" to "Africa", "KU" to "Kurdish", "KUR" to "Kurdish", "EX" to "Ex-Yugoslavia",
        "EXYU" to "Ex-Yugoslavia",
    )

    /** Written-out names, checked in order; the first match wins. */
    private val NAMES: List<Pair<String, String>> = listOf(
        "united kingdom" to "United Kingdom", "england" to "United Kingdom", "british" to "United Kingdom",
        "usa" to "United States", "united states" to "United States", "america" to "United States",
        "canada" to "Canada", "france" to "France", "french" to "France", "germany" to "Germany",
        "deutsch" to "Germany", "german" to "Germany", "italy" to "Italy", "italia" to "Italy",
        "spain" to "Spain", "españa" to "Spain", "espana" to "Spain", "portugal" to "Portugal",
        "brazil" to "Brazil", "brasil" to "Brazil", "netherlands" to "Netherlands", "dutch" to "Netherlands",
        "belgium" to "Belgium", "switzerland" to "Switzerland", "austria" to "Austria",
        "poland" to "Poland", "polska" to "Poland", "romania" to "Romania", "greece" to "Greece",
        "turkey" to "Turkey", "türkiye" to "Turkey", "turkish" to "Turkey", "russia" to "Russia",
        "ukraine" to "Ukraine", "sweden" to "Sweden", "norway" to "Norway", "denmark" to "Denmark",
        "finland" to "Finland", "ireland" to "Ireland", "albania" to "Albania", "serbia" to "Serbia",
        "croatia" to "Croatia", "bulgaria" to "Bulgaria", "hungary" to "Hungary",
        "arabic" to "Arabic", "arab" to "Arabic", "morocco" to "Morocco", "algeria" to "Algeria",
        "tunisia" to "Tunisia", "egypt" to "Egypt", "lebanon" to "Lebanon", "iraq" to "Iraq",
        "syria" to "Syria", "jordan" to "Jordan", "saudi" to "Saudi Arabia", "kuwait" to "Kuwait",
        "qatar" to "Qatar", "emirates" to "UAE", "israel" to "Israel", "iran" to "Iran", "persian" to "Iran",
        "india" to "India", "hindi" to "India", "pakistan" to "Pakistan", "urdu" to "Pakistan",
        "afghan" to "Afghanistan", "bangla" to "Bangladesh", "china" to "China", "chinese" to "China",
        "japan" to "Japan", "korea" to "Korea", "philippines" to "Philippines", "filipino" to "Philippines",
        "thailand" to "Thailand", "vietnam" to "Vietnam", "indonesia" to "Indonesia",
        "malaysia" to "Malaysia", "australia" to "Australia", "new zealand" to "New Zealand",
        "mexico" to "Mexico", "latino" to "Latin America", "latin" to "Latin America",
        "africa" to "Africa", "kurd" to "Kurdish", "kurdish" to "Kurdish", "ex-yu" to "Ex-Yugoslavia",
        "balkan" to "Ex-Yugoslavia", "caribbean" to "Caribbean",
    )

    /** Types, checked in order; the first with a matching word wins. Adult is first so it is never mislabelled. */
    private val TYPES: List<Pair<String, List<String>>> = listOf(
        "Adult" to listOf("adult", "xxx", "18+"),
        "Kids" to listOf("kids", "kid", "children", "cartoon", "cartoons", "enfants", "kinder", "infantil", "junior"),
        "Sports" to listOf(
            "sport", "sports", "football", "soccer", "f1", "formula", "nba", "nfl", "nhl", "mlb", "ufc",
            "boxing", "tennis", "golf", "cricket", "rugby", "ppv", "bein", "dazn", "espn", "desporto", "deportes",
            "calcio", "fussball", "racing", "motorsport", "wrestling", "wwe",
        ),
        "News" to listOf("news", "info", "actualités", "noticias", "nachrichten", "notizie", "haber", "akhbar"),
        "Movies" to listOf("movie", "movies", "cinema", "cine", "film", "films", "kino", "filmes", "peliculas", "vod"),
        "Music" to listOf("music", "musique", "musik", "musica", "mtv", "radio"),
        "Documentary" to listOf("documentary", "documentaries", "docs", "doc", "discovery", "nat geo", "history", "science", "nature"),
        "Religious" to listOf("religious", "religion", "islam", "islamic", "quran", "christian", "church", "gospel"),
        "Entertainment" to listOf(
            "entertainment", "general", "generalist", "variety", "series", "drama", "comedy", "lifestyle",
            "reality", "divertissement", "unterhaltung", "intrattenimento",
        ),
    )
}
