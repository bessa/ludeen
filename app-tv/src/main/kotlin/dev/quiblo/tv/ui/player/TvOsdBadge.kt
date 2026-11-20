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

package dev.quiblo.tv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** What a press with the controls hidden did, for the badge. */
internal enum class TvOsdKind { REWIND, FORWARD, PLAY }

/**
 * One press worth showing: what it did and the position it was aimed at.
 *
 * A class rather than a data class on purpose: two taps of Right to the same clamped end are
 * two flashes, and equal values would let the second one leave the first one's timeout running.
 */
internal class TvOsdFlash(val kind: TvOsdKind, val positionMillis: Long)

/**
 * The small badge at the top while the controls are hidden on a film or an episode.
 *
 * With the controls down, Left and Right seek and OK pauses without drawing anything over the
 * picture — which leaves a viewer seeking blind. This is the least that answers "where am I":
 * the direction and the time, for a moment after each seek, and a pause sign for as long as the
 * film is paused.
 */
@Composable
internal fun TvOsdBadge(
    flash: TvOsdFlash?,
    isPaused: Boolean,
    positionMillis: Long,
    durationMillis: Long,
) {
    val icon: ImageVector? = when {
        flash != null -> when (flash.kind) {
            TvOsdKind.REWIND -> Icons.Filled.FastRewind
            TvOsdKind.FORWARD -> Icons.Filled.FastForward
            TvOsdKind.PLAY -> Icons.Filled.PlayArrow
        }
        isPaused -> Icons.Filled.Pause
        else -> null
    }
    val shownMillis = flash?.positionMillis ?: positionMillis
    // Kept through the fade-out, which runs after the icon has already gone to null.
    var lastIcon by remember { mutableStateOf(icon) }
    if (icon != null) lastIcon = icon

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(visible = icon != null, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                lastIcon?.let {
                    Icon(it, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                val time = if (durationMillis > 0L) {
                    "${shownMillis.asClock()} / ${durationMillis.asClock()}"
                } else {
                    shownMillis.asClock()
                }
                Text(text = time, color = Color.White, fontSize = 22.sp)
            }
        }
    }
}
