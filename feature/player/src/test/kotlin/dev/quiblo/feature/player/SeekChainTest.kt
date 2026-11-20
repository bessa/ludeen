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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Quick presses of a seek key add up, instead of each restarting from a stale position. */
class SeekChainTest {

    private val chain = SeekChain(windowMillis = 1_500L)
    private val tenSeconds = 10_000L
    private val film = 6_000_000L

    private fun press(at: Long, position: Long, delta: Long = tenSeconds) =
        chain.next(nowMillis = at, positionMillis = position, deltaMillis = delta, durationMillis = film)

    @Test
    fun `three quick presses move thirty seconds although the position has not caught up`() {
        // The engine still reports 60 s for all three: it updates twice a second.
        assertEquals(70_000L, press(0, 60_000))
        assertEquals(80_000L, press(200, 60_000))
        assertEquals(90_000L, press(400, 60_000))
    }

    @Test
    fun `a press after a pause starts from where the film really is`() {
        press(0, 60_000)

        assertEquals(105_000L, press(5_000, 95_000))
    }

    @Test
    fun `back and forth inside a run cancel out`() {
        press(0, 60_000)
        assertEquals(60_000L, press(300, 60_000, -tenSeconds))
    }

    @Test
    fun `the target stays inside the film`() {
        assertEquals(0L, press(0, 4_000, -tenSeconds))
        assertEquals(film, press(10_000, film - 1_000))
        // And a run that hit the end comes straight back with one press.
        assertEquals(film - tenSeconds, press(10_300, film - 1_000, -tenSeconds))
    }

    @Test
    fun `reset drops the run`() {
        press(0, 60_000)
        chain.reset()
        assertEquals(30_000L, press(100, 20_000))
    }
}
