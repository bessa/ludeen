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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import dev.quiblo.core.model.Channel
import dev.quiblo.tv.R
import dev.quiblo.tv.ui.common.insistOnFocus

/**
 * The channel list, over the left of a live channel (Ludeen).
 *
 * Left on a live channel opens it, with the remote on the channel that is playing. Up and down
 * walk the list, OK switches to the focused channel, and Right or Back closes it. It sits over
 * the picture, translucent, rather than shrinking it: the channel keeps playing full size, and
 * a viewer can see what they are leaving while they look for what to watch next.
 *
 * The list is the one the channel was chosen from — the same one Up and Down zap through.
 */
@Composable
internal fun TvChannelListPanel(
    channels: List<Channel>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val current = remember { FocusRequester() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentIndex - ROWS_ABOVE_CURRENT).coerceAtLeast(0),
    )

    // The playing channel has the remote the moment the list appears, or the remote looks dead.
    LaunchedEffect(Unit) { current.insistOnFocus() }

    // One heading for a list that is one category, which is how most lists arrive; "Live" for
    // a mixed one (favourites, search), where any single group's name would be wrong.
    val group = channels.firstOrNull()?.groupTitle
    val heading = if (group != null && channels.all { it.groupTitle == group }) {
        group
    } else {
        stringResource(R.string.tv_tab_live)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(PANEL_WIDTH)
                .background(Color.Black.copy(alpha = PANEL_ALPHA))
                .padding(top = 32.dp, bottom = 16.dp)
                .onPreviewKeyEvent { event ->
                    // Right is the way back to the picture. Back is the player's, which knows
                    // the list is open — see TvPlayerBackState.
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                },
        ) {
            Text(
                text = heading,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(channels, key = { index, channel -> "$index-${channel.id}" }) { index, channel ->
                    ChannelListRow(
                        number = index + 1,
                        channel = channel,
                        isPlaying = index == currentIndex,
                        onClick = { onSelect(index) },
                        modifier = if (index == currentIndex) Modifier.focusRequester(current) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelListRow(
    number: Int,
    channel: Channel,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val textColour = if (isFocused) Color.Black else Color.White.copy(alpha = if (isPlaying) 1f else 0.85f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .background(if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
            .border(
                width = if (isPlaying && !isFocused) 1.dp else 0.dp,
                color = if (isPlaying && !isFocused) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = number.toString(),
            color = textColour.copy(alpha = textColour.alpha * 0.7f),
            fontSize = 15.sp,
            modifier = Modifier.width(44.dp),
        )

        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 30.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val placeholder = @Composable {
                Icon(
                    imageVector = Icons.Filled.LiveTv,
                    contentDescription = null,
                    tint = textColour.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp),
                )
            }
            if (channel.logoUrl.isNullOrBlank()) {
                placeholder()
            } else {
                SubcomposeAsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = { placeholder() },
                    error = { placeholder() },
                )
            }
        }

        Text(
            text = channel.name,
            color = textColour,
            fontSize = 16.sp,
            fontWeight = if (isPlaying || isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val PANEL_WIDTH = 440.dp
private const val PANEL_ALPHA = 0.72f

/** How many channels show above the playing one when the list opens, so it is not on the top edge. */
private const val ROWS_ABOVE_CURRENT = 4
