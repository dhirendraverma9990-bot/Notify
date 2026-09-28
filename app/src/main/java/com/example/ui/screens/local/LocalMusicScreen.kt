package com.example.ui.screens.local

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Folder
import com.example.data.model.Track
import com.example.ui.components.TrackListItem
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NotifyBackground
import com.example.ui.theme.NotifySurface
import com.example.ui.theme.NotifySurfaceElevated
import com.example.ui.theme.NotifySurfaceVariant
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.LocalCategory
import com.example.ui.viewmodel.LocalSortOrder

@Composable
fun LocalMusicScreen(
    localTracks: List<Track>,
    albums: List<Album>,
    artists: List<Artist>,
    folders: List<Folder>,
    category: LocalCategory,
    sortOrder: LocalSortOrder,
    isScanning: Boolean,
    currentTrack: Track?,
    isPlaying: Boolean,
    onCategoryChange: (LocalCategory) -> Unit,
    onSortChange: (LocalSortOrder) -> Unit,
    onRescanClick: () -> Unit,
    onGenerateDemoAudio: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    isFavorite: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var hasPermission by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            onRescanClick()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NotifyBackground)
            .padding(top = 16.dp)
            .testTag("local_music_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                Text(
                    text = "Local Music",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "${localTracks.size} audio files on device",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row {
                // Sort Menu Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("local_sort_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort tracks",
                            tint = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(NotifySurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Title", color = TextPrimary) },
                            onClick = {
                                showSortMenu = false
                                onSortChange(LocalSortOrder.TITLE)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Artist", color = TextPrimary) },
                            onClick = {
                                showSortMenu = false
                                onSortChange(LocalSortOrder.ARTIST)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Duration", color = TextPrimary) },
                            onClick = {
                                showSortMenu = false
                                onSortChange(LocalSortOrder.DURATION)
                            }
                        )
                    }
                }

                // Rescan Button
                IconButton(
                    onClick = onRescanClick,
                    enabled = !isScanning,
                    modifier = Modifier.testTag("rescan_local_btn")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = SpotifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan storage",
                            tint = SpotifyGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Categories Tabs: Songs, Artists, Albums, Folders
        TabRow(
            selectedTabIndex = category.ordinal,
            containerColor = NotifyBackground,
            contentColor = SpotifyGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[category.ordinal]),
                    color = SpotifyGreen
                )
            },
            divider = {}
        ) {
            LocalCategory.values().forEach { cat ->
                Tab(
                    selected = category == cat,
                    onClick = { onCategoryChange(cat) },
                    text = {
                        Text(
                            text = when (cat) {
                                LocalCategory.SONGS -> "Songs"
                                LocalCategory.ARTISTS -> "Artists"
                                LocalCategory.ALBUMS -> "Albums"
                                LocalCategory.FOLDERS -> "Folders"
                            },
                            fontWeight = if (category == cat) FontWeight.Bold else FontWeight.Normal,
                            color = if (category == cat) SpotifyGreenBright else TextSecondary
                        )
                    }
                )
            }
        }

        if (localTracks.isEmpty() && !isScanning) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Local Audio Found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your device storage currently has no scanned .mp3, .wav, or .flac files, or permission was not granted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { permissionLauncher.launch(permissionToRequest) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyGreen,
                                contentColor = NotifyBackground
                            )
                        ) {
                            Text("Grant Permission")
                        }

                        OutlinedButton(
                            onClick = onGenerateDemoAudio,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ElectricCyan
                            )
                        ) {
                            Text("Generate Sample Audio")
                        }
                    }
                }
            }
        } else {
            // Category content
            when (category) {
                LocalCategory.SONGS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(localTracks) { track ->
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
                LocalCategory.ALBUMS -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(albums) { album ->
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NotifySurface)
                                    .clickable {
                                        val albumSongs = localTracks.filter { it.album == album.title }
                                        if (albumSongs.isNotEmpty()) {
                                            onTrackClick(albumSongs.first(), albumSongs)
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NotifySurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!album.albumArtUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = album.albumArtUri,
                                            contentDescription = album.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Album,
                                            contentDescription = null,
                                            tint = SpotifyGreen,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${album.artist} • ${album.trackCount} tracks",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                LocalCategory.ARTISTS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(artists) { artist ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val artistSongs = localTracks.filter { it.artist == artist.name }
                                        if (artistSongs.isNotEmpty()) {
                                            onTrackClick(artistSongs.first(), artistSongs)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(NotifySurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${artist.trackCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                LocalCategory.FOLDERS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(folders) { folder ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val folderSongs = localTracks.filter { it.folderName == folder.name }
                                        if (folderSongs.isNotEmpty()) {
                                            onTrackClick(folderSongs.first(), folderSongs)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NotifySurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${folder.trackCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
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
