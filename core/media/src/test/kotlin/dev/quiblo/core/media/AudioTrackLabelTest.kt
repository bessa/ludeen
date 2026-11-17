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

package dev.quiblo.core.media

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/** What the audio menu calls a track, so two tracks of one language can be told apart. */
class AudioTrackLabelTest {

    private fun label(
        label: String? = null,
        language: String? = null,
        channels: Int = Format.NO_VALUE,
        mime: String? = null,
        locale: Locale = Locale.ENGLISH,
    ) = audioTrackLabel(label, language, channels, mime, locale)

    @Test
    fun `an ISO code becomes the language's name with layout and codec`() {
        assertEquals("English · 5.1 · E-AC3", label(language = "eng", channels = 6, mime = MimeTypes.AUDIO_E_AC3))
        assertEquals("French · Stereo · AAC", label(language = "fr", channels = 2, mime = MimeTypes.AUDIO_AAC))
    }

    @Test
    fun `two tracks in the same language no longer read the same`() {
        val stereo = label(language = "eng", channels = 2, mime = MimeTypes.AUDIO_AAC)
        val surround = label(language = "eng", channels = 6, mime = MimeTypes.AUDIO_AC3)
        assertNotEquals(stereo, surround)
    }

    @Test
    fun `the container's own title leads, and a title that is just the language is not repeated`() {
        assertEquals(
            "Commentary · English · Stereo",
            label(label = "Commentary", language = "en", channels = 2),
        )
        assertEquals("English · Stereo", label(label = "English", language = "eng", channels = 2))
    }

    @Test
    fun `the language is named in the viewer's language`() {
        assertEquals("Anglais", label(language = "eng", locale = Locale.FRENCH))
    }

    @Test
    fun `an undetermined or unknown language is not shown as a name`() {
        assertEquals("Stereo", label(language = "und", channels = 2))
        assertEquals("Track", label())
    }
}
