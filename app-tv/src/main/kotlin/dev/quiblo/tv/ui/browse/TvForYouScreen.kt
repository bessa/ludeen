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

package dev.quiblo.tv.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.quiblo.core.model.Channel
import dev.quiblo.core.model.HistoryEntry
import dev.quiblo.core.model.MediaKind
import dev.quiblo.feature.browse.BrowseViewModel
import dev.quiblo.feature.browse.di.forYouParams
import dev.quiblo.tv.R
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Home: what the viewer was in the middle of, and nothing else.
 *
 * Two rows — series and films — each built from this profile's watch history and showing only
 * what was started and not finished. No hero slider, no "recently added", no popularity or
 * suggestion rows: Home is a place to pick up where you left off, not a shop window.
 */
@Composable
fun TvForYouScreen(
    /** Hands over the whole list, not just the item: the player needs it to zap. */
    onPlay: (List<Channel>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: BrowseViewModel = koinViewModel(
        // Not display text: a Koin scope key, never rendered.
        key = "tv-for-you",
        parameters = { forYouParams() },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val (series, films) = remember(state.history) { splitUnfinished(state.history) }

    val play: (HistoryEntry) -> Unit = { entry ->
        scope.launch {
            viewModel.channelForHistory(entry)?.let { channel -> onPlay(listOf(channel), 0) }
        }
    }

    when {
        state.isLoading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }

        !state.hasSource -> Message(stringResource(R.string.tv_no_source))

        series.isEmpty() && films.isEmpty() -> Message(stringResource(R.string.tv_home_nothing_to_continue))

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (series.isNotEmpty()) {
                item(key = "continue-series") {
                    TvContinueWatchingRow(
                        entries = series,
                        posters = state.posters,
                        onClick = play,
                        onRemove = viewModel::removeFromHistory,
                        title = stringResource(R.string.tv_continue_watching_series),
                    )
                }
            }
            if (films.isNotEmpty()) {
                item(key = "continue-films") {
                    TvContinueWatchingRow(
                        entries = films,
                        posters = state.posters,
                        onClick = play,
                        onRemove = viewModel::removeFromHistory,
                        title = stringResource(R.string.tv_continue_watching_movies),
                    )
                }
            }
        }
    }
}

/**
 * Splits history into series and films, keeping only what is still worth resuming.
 *
 * An entry counts as finished once it is past [FINISHED_FRACTION] of its length — the credits
 * are not something anybody resumes. An entry with no known length is kept: nothing says it was
 * finished. A series is never dropped for its last-watched episode being finished, because the
 * next episode is what "continue" means for a series; only films are filtered that way.
 */
internal fun splitUnfinished(history: List<HistoryEntry>): Pair<List<HistoryEntry>, List<HistoryEntry>> {
    val series = history.filter { it.kind == MediaKind.SERIES }
    val films = history.filter { it.kind == MediaKind.VOD && (it.progress ?: 0f) < FINISHED_FRACTION }
    return series to films
}

private const val FINISHED_FRACTION = 0.95f

/** Whatever this screen has to say when it has no row to draw. */
@Composable
private fun Message(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.65f),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
    }
}
