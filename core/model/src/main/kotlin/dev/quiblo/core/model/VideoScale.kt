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
 * Scale factors that turn the stretched-to-fill surface into the requested framing.
 *
 * Returns 1:1 until the first frame has been decoded, because the correct correction is
 * unknowable without the frame size — and guessing produces a visible jump when the real
 * size arrives.
 */
fun videoScale(
    videoAspectRatio: Float?,
    containerAspectRatio: Float,
    mode: AspectRatioMode,
): Pair<Float, Float> {
    if (mode == AspectRatioMode.STRETCH) return 1f to 1f
    if (mode == AspectRatioMode.EXPAND_4_3) return expandFourThree(videoAspectRatio)
    if (containerAspectRatio <= 0f) return 1f to 1f

    // A forced shape: the surface fills the screen, so draw the forced ratio letterboxed inside
    // it, whatever the stream says its own shape is.
    mode.forcedRatio?.let { forced ->
        val r = forced / containerAspectRatio
        return if (r > 1f) 1f to (1f / r) else r to 1f
    }

    val video = videoAspectRatio ?: return 1f to 1f
    if (video <= 0f) return 1f to 1f

    val ratio = video / containerAspectRatio
    val isWiderThanContainer = ratio > 1f

    val base = when (mode) {
        // Contain: shrink whichever axis is overfilled, leaving bars on the other.
        AspectRatioMode.FIT ->
            if (isWiderThanContainer) 1f to (1f / ratio) else ratio to 1f

        // Cover: grow whichever axis is underfilled, cropping the overflow.
        AspectRatioMode.FILL, AspectRatioMode.ZOOM ->
            if (isWiderThanContainer) ratio to 1f else 1f to (1f / ratio)

        else -> 1f to 1f
    }

    val extra = mode.extraScale
    return (base.first * extra) to (base.second * extra)
}

private const val FOUR_THREE = 4f / 3f

/**
 * [AspectRatioMode.EXPAND_4_3]: the surface already fills the screen, so the frame is stretched
 * to it. Widening it by `frame / (4:3)` pushes everything outside the centred 4:3 picture — the
 * burned-in side bars — past the clipped edges, and leaves that picture filling the screen.
 *
 * A frame that is 4:3 or narrower has no side bars to lose and is simply stretched. Before the
 * first frame nothing is known, and 1:1 avoids a jump, as everywhere else here.
 */
private fun expandFourThree(videoAspectRatio: Float?): Pair<Float, Float> {
    val video = videoAspectRatio?.takeIf { it > 0f } ?: return 1f to 1f
    return maxOf(1f, video / FOUR_THREE) to 1f
}
