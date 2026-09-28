package com.example.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.components.TrackListItem
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NotifyBackground
import com.example.ui.theme.NotifySurface
import com.example.ui.theme.NotifySurfaceElevated
import com.example.ui.theme.NotifySurfaceVariant
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VibrantCoral
import com.example.ui.viewmodel.SearchFilter

/**
 * Visual model for search category discovery tiles.
 */
data class GenreCardItem(
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>
)

/**
 * SearchScreen
 *
 * Provides a persistent search text input bar, quick filter chips (All, Local Storage,
 * By Artist, By Album, Streaming), and a reactive, filterable list of audio tracks
 * backed by the [AudioRepository].
 *
 * Features:
 * - Persistent high-contrast dark theme search bar with Spotify green accents.
 * - Clear button with animated reveal.
 * - Real-time filtering across titles, artists, albums, genres, and audio formats.
 * - Direct highlights for device audio scanned from [AudioRepository].
 * - Dark-mode category grid and empty state feedback.
 */
@Composable
fun SearchScreen(
    query: String,
    filter: SearchFilter,
    searchResults: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    isFavorite: (String) -> Boolean,
    modifier: Modifier = Modifier,
    localTracks: List<Track> = emptyList(),
    onScanLocalClick: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    val genreCards = remember {
        listOf(
            GenreCardItem("Synthwave", "Retro Electronic", listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))),
            GenreCardItem("Lo-Fi & Chill", "Relax & Focus", listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))),
            GenreCardItem("Electronic", "High Energy Beats", listOf(Color(0xFF00C6FF), Color(0xFF0072FF))),
            GenreCardItem("Future Bass", "Melodic Drops", listOf(Color(0xFFA8FF78), Color(0xFF78FFD6))),
            GenreCardItem("Cyberpunk", "Dark Synth Bass", listOf(Color(0xFFFF512F), Color(0xFFDD2476))),
            GenreCardItem("Ambient Flow", "Deep Atmospheric", listOf(Color(0xFF134E5E), Color(0xFF71B280))),
            GenreCardItem("Hip-Hop & Trap", "Heavy 808s", listOf(Color(0xFF654EA3), Color(0xFFEAAFC8))),
            GenreCardItem("Rock & Indie", "Guitars & Vocals", listOf(Color(0xFFEB3349), Color(0xFFF45C43)))
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NotifyBackground)
            .testTag("search_screen")
    ) {
        // Persistent Top Bar & Search Header
        Surface(
            color = NotifyBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Screen Title with storage badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = TextPrimary
                    )

                    if (localTracks.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(NotifySurfaceVariant)
                                .clickable { onFilterChange(SearchFilter.LOCAL) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${localTracks.size} Local",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Persistent Text Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = "Search songs, artists, albums...",
                            color = TextTertiary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = if (query.isNotBlank()) SpotifyGreen else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = query.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NotifySurfaceVariant,
                        unfocusedContainerColor = NotifySurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = SpotifyGreen,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.08f),
                        cursorColor = SpotifyGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("search_text_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SearchFilter.values()) { f ->
                        val selected = f == filter
                        val label = when (f) {
                            SearchFilter.ALL -> "All Tracks"
                            SearchFilter.LOCAL -> "Local Audio"
                            SearchFilter.ONLINE -> "Streaming"
                            SearchFilter.ARTISTS -> "Artists"
                            SearchFilter.ALBUMS -> "Albums"
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(f) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpotifyGreen,
                                selectedLabelColor = NotifyBackground,
                                containerColor = NotifySurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = if (selected) SpotifyGreen else Color.White.copy(alpha = 0.08f),
                                selectedBorderColor = SpotifyGreen
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("filter_chip_${f.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Search Content or Category Discovery
        if (query.isBlank()) {
            // Display Explore & Local Audio Repository overview
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Local Audio Repository Banner
                item {
                    LocalAudioRepositoryCard(
                        localTracksCount = localTracks.size,
                        onScanClick = onScanLocalClick,
                        onViewLocalClick = { onFilterChange(SearchFilter.LOCAL) }
                    )
                }

                // Discovery Grid Title
                item {
                    Text(
                        text = "Browse Categories",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Grid of Genre Cards
                item {
                    val chunkedGenres = genreCards.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (row in chunkedGenres) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (genre in row) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(96.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(colors = genre.gradientColors)
                                            )
                                            .clickable { onQueryChange(genre.title) }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = genre.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                ),
                                                color = Color.White
                                            )
                                            Text(
                                                text = genre.subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp
                                                ),
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.25f),
                                            modifier = Modifier
                                                .size(42.dp)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Local Preview if storage files exist
                if (localTracks.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = "Device Audio Tracks",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "See all",
                                color = SpotifyGreenBright,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { onFilterChange(SearchFilter.LOCAL) }
                                    .padding(4.dp)
                            )
                        }
                    }

                    items(localTracks.take(4)) { track ->
                        TrackListItem(
                            track = track,
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying,
                            isFavorite = isFavorite(track.id),
                            onClick = { onTrackClick(track, localTracks) },
                            onFavoriteToggle = { onFavoriteToggle(track) },
                            onAddToPlaylistClick = { onAddToPlaylistClick(track) }
                        )
                    }
                }
            }
        } else {
            // Filtered Results List
            if (searchResults.isEmpty()) {
                // Empty search result feedback
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 48.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NotifySurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No results for \"$query\"",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Check the spelling or try searching for another artist, song, or filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filter != SearchFilter.ALL) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(NotifySurfaceElevated)
                                        .clickable { onFilterChange(SearchFilter.ALL) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Search in All Tracks",
                                        color = SpotifyGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(NotifySurfaceVariant)
                                    .clickable { onQueryChange("") }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Clear Query",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${searchResults.size} matches found",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )

                            val localCount = searchResults.count { it.isLocal }
                            if (localCount > 0) {
                                Text(
                                    text = "$localCount from Local Audio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpotifyGreenBright,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    items(searchResults) { track ->
                        TrackListItem(
                            track = track,
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying,
                            isFavorite = isFavorite(track.id),
                            onClick = { onTrackClick(track, searchResults) },
                            onFavoriteToggle = { onFavoriteToggle(track) },
                            onAddToPlaylistClick = { onAddToPlaylistClick(track) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * LocalAudioRepositoryCard
 *
 * Highlights the [AudioRepository] integration on the SearchScreen when idle,
 * giving users clear visibility into scanned device storage tracks and
 * offering a direct rescan action.
 */
@Composable
private fun LocalAudioRepositoryCard(
    localTracksCount: Int,
    onScanClick: () -> Unit,
    onViewLocalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(NotifySurfaceElevated, NotifySurface)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Audio Repository",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = if (localTracksCount > 0) {
                                "$localTracksCount tracks indexed from device storage"
                            } else {
                                "No local tracks detected in storage"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onScanClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan storage",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio format badges
                FormatPill(label = "FLAC", color = ElectricCyan)
                FormatPill(label = "WAV", color = VibrantCoral)
                FormatPill(label = "MP3", color = SpotifyGreen)

                Spacer(modifier = Modifier.weight(1f))

                if (localTracksCount > 0) {
                    Text(
                        text = "View all local tracks →",
                        fontSize = 12.sp,
                        color = SpotifyGreenBright,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onViewLocalClick() }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
