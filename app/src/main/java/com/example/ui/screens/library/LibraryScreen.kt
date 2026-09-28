package com.example.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PlaylistEntity
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.TrackListItem
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NotifyBackground
import com.example.ui.theme.NotifySurface
import com.example.ui.theme.NotifySurfaceElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    if (activePlaylist != null) {
        val playlist = activePlaylist!!
        val isCurated = playlist.isCurated
        val playlistTracks = if (isCurated) {
            playlist.tracks
        } else {
            val dbTracks by viewModel.repository.getPlaylistTracks(playlist.id).collectAsState(initial = emptyList())
            dbTracks
        }

        BackHandler {
            viewModel.closePlaylistDetail()
        }

        PlaylistDetailView(
            playlist = playlist,
            tracks = playlistTracks,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            onBack = { viewModel.closePlaylistDetail() },
            onPlayAll = {
                if (playlistTracks.isNotEmpty()) {
                    viewModel.playTrack(playlistTracks.first(), playlistTracks)
                }
            },
            onShufflePlay = {
                if (playlistTracks.isNotEmpty()) {
                    val shuffled = playlistTracks.shuffled()
                    viewModel.playTrack(shuffled.first(), shuffled)
                }
            },
            onTrackClick = { track ->
                viewModel.playTrack(track, playlistTracks)
            },
            onFavoriteToggle = { track ->
                viewModel.toggleFavorite(track)
            },
            onRemoveTrack = { track ->
                if (!isCurated) {
                    viewModel.removeTrackFromPlaylist(playlist.id, track.id)
                }
            },
            isFavorite = { trackId -> viewModel.isTrackFavorited(trackId) },
            modifier = modifier
        )
    } else {
        // Main Library View
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NotifyBackground)
                .testTag("library_screen")
        ) {
            LazyColumn(
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Your Library",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = TextPrimary
                        )

                        IconButton(
                            onClick = { viewModel.showCreatePlaylistDialog() },
                            modifier = Modifier.testTag("create_playlist_icon_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Playlist",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Liked Songs Row (Primary Card)
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openPlaylistDetail(
                                    Playlist(
                                        id = "liked_songs_virtual",
                                        title = "Liked Songs",
                                        description = "Your favorite bookmarked tracks",
                                        coverUrl = null,
                                        trackCount = favoriteTracks.size,
                                        isCurated = true,
                                        tracks = favoriteTracks
                                    )
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("liked_songs_row")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(NeonPurple, SpotifyGreen)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Liked Songs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Playlist • ${favoriteTracks.size} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Audio Equalizer Action Tile
                item {
                    val eqState by viewModel.equalizerState.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showEqualizer() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("library_equalizer_item")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = if (eqState.isEnabled) {
                                            listOf(SpotifyGreen, com.example.ui.theme.ElectricCyan)
                                        } else {
                                            listOf(Color(0xFF333333), Color(0xFF222222))
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Audio Equalizer",
                                tint = if (eqState.isEnabled) NotifyBackground else TextPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audio Equalizer",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = if (eqState.isEnabled) "Hardware DSP Active • ${eqState.currentPreset}" else "Equalizer Bypassed",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (eqState.isEnabled) SpotifyGreenBright else TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (eqState.isEnabled) SpotifyGreen else TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Curated Playlists section
                item {
                    Text(
                        text = "Curated Mixes",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                items(viewModel.curatedPlaylists) { playlist ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openPlaylistDetail(playlist) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NotifySurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!playlist.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = playlist.coverUrl,
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Notify Official • ${playlist.trackCount} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Custom User Playlists Section
                item {
                    Text(
                        text = "Custom Playlists (${userPlaylists.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                if (userPlaylists.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "No custom playlists yet. Tap '+' to build your own mix!",
                                color = TextTertiary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(userPlaylists) { entity ->
                        var showMenu by remember { mutableStateOf(false) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openPlaylistDetail(
                                        Playlist(
                                            id = entity.id,
                                            title = entity.title,
                                            description = entity.description,
                                            isCurated = false
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NotifySurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entity.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = entity.description.ifBlank { "Custom Playlist" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = TextSecondary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(NotifySurfaceElevated)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Playlist", color = TextPrimary) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.deletePlaylist(entity.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onRemoveTrack: (Track) -> Unit,
    isFavorite: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp),
        modifier = modifier
            .fillMaxSize()
            .background(NotifyBackground)
            .testTag("playlist_detail_view")
    ) {
        // Back Button & Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(NotifySurfaceElevated, NotifyBackground)
                        )
                    )
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .testTag("playlist_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NotifySurface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!playlist.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = playlist.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            Icon(
                                imageVector = if (playlist.id.startsWith("liked")) Icons.Default.Favorite else Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    if (playlist.description.isNotBlank()) {
                        Text(
                            text = playlist.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${tracks.size} tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Action Buttons Row (Play All & Shuffle)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onPlayAll,
                    enabled = tracks.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = NotifyBackground
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onShufflePlay,
                    enabled = tracks.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotifySurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, null, tint = SpotifyGreenBright)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs in this playlist yet.",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(tracks) { track ->
                TrackListItem(
                    track = track,
                    isCurrentTrack = currentTrack?.id == track.id,
                    isPlaying = isPlaying,
                    isFavorite = isFavorite(track.id),
                    onClick = { onTrackClick(track) },
                    onFavoriteToggle = { onFavoriteToggle(track) },
                    onAddToPlaylistClick = { onRemoveTrack(track) }
                )
            }
        }
    }
}
