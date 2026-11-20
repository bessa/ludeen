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

package dev.quiblo.feature.player

/**
 * Where a run of skip presses is heading.
 *
 * The engine reports its position twice a second, so three quick presses of Right each added ten
 * seconds to the *same* stale position and moved the film ten seconds, not thirty. With the
 * controls hidden the D-pad seeks directly and a viewer holds or taps the key, so the presses are
 * chained: one inside [windowMillis] of the last starts from where the last one was sent, not
 * from where the engine last said it was.
 *
 * The target is kept inside the film. Past the end used to be the engine's problem; a chain that
 * keeps counting past it would need as many presses back before Left did anything.
 */
internal class SeekChain(private val windowMillis: Long = DEFAULT_WINDOW_MILLIS) {

    private var target: Long? = null
    private var sentAt = 0L

    fun next(nowMillis: Long, positionMillis: Long, deltaMillis: Long, durationMillis: Long): Long {
        val base = target?.takeIf { nowMillis - sentAt in 0 until windowMillis } ?: positionMillis
        val next = (base + deltaMillis).coerceAtLeast(0L)
            .let { if (durationMillis > 0L) it.coerceAtMost(durationMillis) else it }
        target = next
        sentAt = nowMillis
        return next
    }

    /** Forgets the run, for a seek that did not come from here or a new item. */
    fun reset() {
        target = null
    }

    private companion object {
        const val DEFAULT_WINDOW_MILLIS = 1_500L
    }
}
