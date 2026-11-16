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

package dev.quiblo.tv.ui.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.quiblo.core.data.ChannelRepository
import dev.quiblo.core.data.SourceRepository
import dev.quiblo.core.data.WatchHistoryRepository
import dev.quiblo.core.model.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the Recent tab shows. */
internal sealed interface TvRecentState {
    data object Loading : TvRecentState

    /** In the order they were last watched, newest first, and only those the provider still carries. */
    data class Loaded(val channels: List<Channel>) : TvRecentState
}

/**
 * The live channels most recently tuned to, newest first (up to 100).
 *
 * Read from the watch history the player writes on tune-in, then joined back to the current
 * channel rows in one query, so a channel the provider has since dropped simply disappears.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TvRecentViewModel(
    sourceRepository: SourceRepository,
    private val historyRepository: WatchHistoryRepository,
    private val channelRepository: ChannelRepository,
) : ViewModel() {

    val state: StateFlow<TvRecentState> = sourceRepository.observeSources()
        .map { sources -> sources.firstOrNull()?.id }
        .flatMapLatest { sourceId ->
            if (sourceId == null) {
                flowOf(TvRecentState.Loaded(emptyList()))
            } else {
                historyRepository.observeRecentLive(sourceId).map { entries ->
                    val byKey = channelRepository
                        .channelsByStableKeys(sourceId, entries.map { it.stableKey })
                        .associateBy { it.stableKey }
                    TvRecentState.Loaded(entries.mapNotNull { byKey[it.stableKey] })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TvRecentState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
