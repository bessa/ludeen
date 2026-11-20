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
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.SubcomposeAsyncImage
import dev.quiblo.core.model.Category
import dev.quiblo.core.model.Channel
import dev.quiblo.core.model.MediaKind
import dev.quiblo.feature.browse.BrowseViewModel
import dev.quiblo.feature.browse.di.browseParams
import dev.quiblo.tv.R
import dev.quiblo.tv.ui.common.insistOnFocus
import org.koin.androidx.compose.koinViewModel

/** Which channels the list level is showing. */
private sealed interface Browsing {
    /** The list the playing channel came from — what Up and Down zap through. */
    data object Start : Browsing

    /** A category picked from the level above; null is "All channels". */
    data class InCategory(val title: String?) : Browsing
}

/**
 * The channel browser over the left of a live channel (Ludeen).
 *
 * Two levels in one panel. **Channels**: Left from the picture opens it on the playing channel;
 * Up and Down walk it, OK plays, Right closes it. **Categories**, its parent: Left from the
 * channels; Up and Down walk them, Right or OK opens the focused one's channels — or, if focus
 * never moved, simply returns to the list it came from. Back steps up the same way (the player
 * owns Back; see [TvPlayerBackState]).
 *
 * It sits over the picture, translucent, rather than shrinking it: the channel keeps playing full
 * size behind it.
 *
 * Categories come from a [BrowseViewModel] of its own, so browsing here never moves the Live
 * screen's selection, and they follow the Live screen's grouping (category, country or type).
 */
@Composable
// Two levels, two kinds of list and the rule for when Right is a step back rather than a choice:
// splitting it would scatter the one piece of state, [Browsing], across three functions.
@Suppress("CyclomaticComplexMethod")
internal fun TvChannelListPanel(
    startQueue: List<Channel>,
    startIndex: Int,
    playingId: Long,
    showCategories: Boolean,
    onShowCategories: (Boolean) -> Unit,
    onPlay: (List<Channel>, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val browser: BrowseViewModel = koinViewModel(
        key = "tv-player-channel-list",
        parameters = { browseParams(MediaKind.LIVE) },
    )
    val browseState by browser.uiState.collectAsStateWithLifecycle()
    val categories = browseState.categories

    var browsing: Browsing by remember { mutableStateOf(Browsing.Start) }

    // One heading for a list that is one category, which is how most lists arrive.
    val startGroup = startQueue.firstOrNull()?.groupTitle?.takeIf { group -> startQueue.all { it.groupTitle == group } }

    // The category the level above opens on: the one being shown, or the start list's own.
    val shownTitle: String? = when (val b = browsing) {
        is Browsing.InCategory -> b.title
        Browsing.Start -> startGroup?.takeIf { group -> categories.any { it.title == group } }
    }

    // Back down to the channels. [title] is the category to show, or null for "what was showing"
    // — Right with the focus unmoved, which is a step back rather than a choice.
    fun openCategory(title: String?, isChoice: Boolean) {
        val alreadyShowing = browsing == Browsing.InCategory(title) ||
            (browsing == Browsing.Start && startGroup != null && title == startGroup)
        if ((isChoice || title != shownTitle) && !alreadyShowing) {
            browsing = Browsing.InCategory(title)
            browser.selectCategory(title)
        }
        onShowCategories(false)
    }

    val allLabel = stringResource(R.string.tv_category_all)
    val ungroupedLabel = stringResource(R.string.tv_category_ungrouped)
    fun labelOf(title: String?): String = when (title) {
        null -> allLabel
        Category.UNGROUPED_TITLE -> ungroupedLabel
        else -> categories.firstOrNull { it.title == title }?.displayTitle ?: title
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        if (showCategories) {
            // Re-keyed once the categories arrive, so the landing row is worked out against them
            // rather than against the empty list of the first frame.
            key(categories.isEmpty()) {
                CategoryLevel(
                    categories = categories,
                    labelOf = ::labelOf,
                    focusTitle = shownTitle,
                    onOpen = ::openCategory,
                )
            }
        } else {
            when (val b = browsing) {
                Browsing.Start -> ChannelLevel(
                    heading = startGroup?.let(::labelOf) ?: stringResource(R.string.tv_tab_live),
                    count = startQueue.size,
                    channelAt = { startQueue[it] },
                    keyAt = { "$it-${startQueue[it].id}" },
                    focusIndex = startIndex,
                    playingId = playingId,
                    onSelect = { onPlay(startQueue, it) },
                    onLeft = { onShowCategories(true) },
                    onRight = onDismiss,
                )

                is Browsing.InCategory -> {
                    val paged = browser.pagedItems.collectAsLazyPagingItems()
                    val loaded = paged.itemSnapshotList.items
                    ChannelLevel(
                        heading = labelOf(b.title),
                        count = paged.itemCount,
                        channelAt = { paged[it] },
                        keyAt = paged.itemKey { it.id },
                        focusIndex = loaded.indexOfFirst { it.id == playingId }.coerceAtLeast(0),
                        playingId = playingId,
                        // The pages loaded so far are what zapping can honestly walk — the same
                        // queue the Live screen hands over.
                        onSelect = { onPlay(paged.itemSnapshotList.items, it) },
                        onLeft = { onShowCategories(true) },
                        onRight = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelColumn(
    heading: String,
    onKey: (Key) -> Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(PANEL_WIDTH)
            .background(Color.Black.copy(alpha = PANEL_ALPHA))
            .padding(top = 32.dp, bottom = 16.dp)
            .onPreviewKeyEvent { event -> event.type == KeyEventType.KeyDown && onKey(event.key) },
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
        content()
    }
}

@Composable
@Suppress("LongParameterList")
private fun ChannelLevel(
    heading: String,
    count: Int,
    channelAt: (Int) -> Channel?,
    keyAt: (Int) -> Any,
    focusIndex: Int,
    playingId: Long,
    onSelect: (Int) -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    val rowFocus = remember { FocusRequester() }
    val emptyFocus = remember { FocusRequester() }
    val listState = remember { LazyListState() }
    val hasRows = count > 0
    val landing = focusIndex.coerceIn(0, (count - 1).coerceAtLeast(0))

    // The remote must land somewhere the moment this level appears, or it looks dead. A
    // category still loading, or empty, has no row yet: the panel holds it until one arrives.
    LaunchedEffect(hasRows) {
        if (hasRows) {
            listState.scrollToItem((landing - ROWS_ABOVE_CURRENT).coerceAtLeast(0))
            rowFocus.insistOnFocus()
        } else {
            emptyFocus.insistOnFocus()
        }
    }

    PanelColumn(
        heading = heading,
        onKey = { key ->
            when (key) {
                Key.DirectionLeft -> {
                    onLeft()
                    true
                }
                Key.DirectionRight -> {
                    onRight()
                    true
                }
                else -> false
            }
        },
    ) {
        if (!hasRows) {
            Box(modifier = Modifier.focusRequester(emptyFocus).focusable().padding(horizontal = 24.dp)) {
                Text(text = "…", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().focusGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(count = count, key = keyAt) { index ->
                val channel = channelAt(index) ?: return@items
                ChannelListRow(
                    number = index + 1,
                    channel = channel,
                    isPlaying = channel.id == playingId,
                    onClick = { onSelect(index) },
                    modifier = if (index == landing) Modifier.focusRequester(rowFocus) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun CategoryLevel(
    categories: List<Category>,
    labelOf: (String?) -> String,
    focusTitle: String?,
    onOpen: (title: String?, isChoice: Boolean) -> Unit,
) {
    val landingFocus = remember { FocusRequester() }
    // Index in the column: "All" is row 0, the categories follow.
    val landing = if (focusTitle == null) 0 else categories.indexOfFirst { it.title == focusTitle } + 1
    val listState = remember { LazyListState((landing - ROWS_ABOVE_CURRENT).coerceAtLeast(0)) }
    var focused: String? by remember { mutableStateOf(focusTitle) }

    LaunchedEffect(Unit) { landingFocus.insistOnFocus() }

    PanelColumn(
        heading = stringResource(R.string.tv_tab_live),
        onKey = { key ->
            when (key) {
                // Right opens what the remote is on: the same list back if it never moved.
                Key.DirectionRight -> {
                    onOpen(focused, false)
                    true
                }
                // Nothing further left; held here rather than leaking to the player.
                Key.DirectionLeft -> true
                else -> false
            }
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().focusGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val rows = listOf<String?>(null) + categories.map { it.title }
            itemsIndexed(rows, key = { _, title -> title ?: ALL_KEY }) { index, title ->
                CategoryRow(
                    label = labelOf(title),
                    count = if (title == null) {
                        categories.sumOf { it.itemCount }
                    } else {
                        categories.first { it.title == title }.itemCount
                    },
                    isShown = title == focusTitle,
                    onFocused = { focused = title },
                    onClick = { onOpen(title, true) },
                    modifier = if (index == landing) Modifier.focusRequester(landingFocus) else Modifier,
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun CategoryRow(
    label: String,
    count: Int,
    isShown: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val textColour = if (isFocused) Color.Black else Color.White.copy(alpha = if (isShown) 1f else 0.85f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .background(if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
            .border(
                width = if (isShown && !isFocused) 1.dp else 0.dp,
                color = if (isShown && !isFocused) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            color = textColour,
            fontSize = 16.sp,
            fontWeight = if (isShown || isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(text = count.toString(), color = textColour.copy(alpha = textColour.alpha * 0.6f), fontSize = 14.sp)
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
private const val ALL_KEY = "all"
