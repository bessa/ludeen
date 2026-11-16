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

package dev.quiblo.tv.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * The language the television app is shown in: English (the default), French or Arabic.
 *
 * **Chosen in the app, not taken from the television.** English is the default whatever the set
 * is configured for, and the viewer switches from Settings. The choice is for the whole device
 * rather than per profile, because it has to be known before anybody has picked a profile — the
 * profile chooser itself is drawn in it.
 *
 * Applied by wrapping the activity's context in [wrap], so every `stringResource` and the
 * layout direction follow it. Arabic is right-to-left; the manifest has `supportsRtl`, and
 * Compose mirrors the layout from the configuration this sets.
 */
enum class TvLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    FRENCH("fr", "Français"),
    ARABIC("ar", "العربية"),
    ;

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        val DEFAULT = ENGLISH

        fun fromTag(tag: String?): TvLanguage = entries.firstOrNull { it.tag == tag } ?: DEFAULT

        private const val PREFS = "ludeen_language"
        private const val KEY = "language"

        /**
         * The stored choice, read synchronously.
         *
         * Plain `SharedPreferences` rather than the app's DataStore: this is read in
         * `attachBaseContext`, before anything else exists, and must not suspend or block on a
         * coroutine. It is one short string.
         */
        fun current(context: Context): TvLanguage =
            fromTag(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))

        fun save(context: Context, language: TvLanguage) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.tag).apply()
        }

        /** [base] with its resources and layout direction set to the stored language. */
        fun wrap(base: Context): Context {
            val language = current(base)
            val locale = language.locale
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            return base.createConfigurationContext(config)
        }
    }
}
