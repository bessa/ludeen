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

import androidx.media3.common.MimeTypes
import java.util.Locale

/**
 * What an audio track is called in the menu.
 *
 * The engine's own label is usually absent and the language is an ISO code, so a film with an
 * English stereo track and an English 5.1 track listed "eng" twice, and one with no tags at all
 * listed "Track" for every entry. This names the language in the viewer's own language and adds
 * what tells two tracks of one language apart: the channel layout and the codec.
 *
 * "English · 5.1 · E-AC3", or "Director's commentary · English · Stereo · AAC" when the container
 * carries a title of its own. Codec names are trade names and the layout is digits, so neither
 * needs translating; the language name comes from [displayLocale].
 */
internal fun audioTrackLabel(
    label: String?,
    language: String?,
    channelCount: Int,
    sampleMimeType: String?,
    displayLocale: Locale = Locale.getDefault(),
): String {
    val languageName = languageName(language, displayLocale)
    val parts = buildList {
        label?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        // A container label is often just the language again ("English"); don't say it twice.
        if (languageName != null && none { it.equals(languageName, ignoreCase = true) }) add(languageName)
        channelLayout(channelCount)?.let { add(it) }
        codecName(sampleMimeType)?.let { add(it) }
    }
    return parts.joinToString(SEPARATOR).ifEmpty { FALLBACK }
}

/** "eng", "en" or "en-US" as "English"; null for "und", unknown codes, or nothing. */
private fun languageName(language: String?, displayLocale: Locale): String? {
    val tag = language?.trim()?.takeIf { it.isNotEmpty() && !it.equals("und", ignoreCase = true) } ?: return null
    val locale = Locale.forLanguageTag(tag.replace('_', '-'))
    val name = locale.getDisplayName(displayLocale)
    // The JDK echoes back a code it does not know; showing "xyz" is no better than the code.
    return name.takeIf { it.isNotBlank() && !it.equals(tag, ignoreCase = true) }
        ?.replaceFirstChar { it.titlecase(displayLocale) }
        ?: tag
}

private fun channelLayout(channelCount: Int): String? = when {
    channelCount == MONO -> "Mono"
    channelCount == STEREO -> "Stereo"
    channelCount == SURROUND_5_1 -> "5.1"
    channelCount == SURROUND_7_1 -> "7.1"
    channelCount > STEREO -> "${channelCount}ch"
    else -> null
}

private fun codecName(sampleMimeType: String?): String? = when (sampleMimeType) {
    MimeTypes.AUDIO_AAC -> "AAC"
    MimeTypes.AUDIO_AC3 -> "AC3"
    MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC3"
    MimeTypes.AUDIO_AC4 -> "AC4"
    MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_EXPRESS -> "DTS"
    MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
    MimeTypes.AUDIO_TRUEHD -> "TrueHD"
    MimeTypes.AUDIO_MPEG -> "MP3"
    MimeTypes.AUDIO_MPEG_L2 -> "MP2"
    MimeTypes.AUDIO_OPUS -> "Opus"
    MimeTypes.AUDIO_VORBIS -> "Vorbis"
    MimeTypes.AUDIO_FLAC -> "FLAC"
    else -> null
}

private const val MONO = 1
private const val STEREO = 2
private const val SURROUND_5_1 = 6
private const val SURROUND_7_1 = 8
private const val SEPARATOR = " · "
private const val FALLBACK = "Track"
