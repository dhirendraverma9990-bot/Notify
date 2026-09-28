package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.QueueSheet
import com.example.ui.screens.equalizer.EqualizerSheet
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.local.LocalMusicScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.theme.NotifyBackground
import com.example.ui.theme.NotifySurface
import com.example.ui.theme.NotifySurfaceVariant
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.MusicViewModel

data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun MainScreen(viewModel: MusicViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val isQueueSheetVisible by viewModel.isQueueSheetVisible.collectAsState()
    val isEqualizerVisible by viewModel.isEqualizerVisible.collectAsState()
    val trackForPlaylistDialog by viewModel.trackForPlaylistDialog.collectAsState()
    val isCreatePlaylistDialogVisible by viewModel.isCreatePlaylistDialogVisible.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isShuffle by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()

    val isCurrentTrackFav = currentTrack?.let { viewModel.isTrackFavorited(it.id) } ?: false

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(playbackError) {
        playbackError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPlaybackError()
        }
    }

    val tabs = remember {
        listOf(
            NavTabItem("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            NavTabItem("Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
            NavTabItem("Local", Icons.Filled.Folder, Icons.Outlined.Folder, "nav_local"),
            NavTabItem("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library")
        )
    }

    // Handle back button when full-screen player is open or when not on Home tab
    if (isNowPlayingExpanded) {
        BackHandler {
            viewModel.setNowPlayingExpanded(false)
        }
    } else if (selectedTab != 0) {
        BackHandler {
            viewModel.selectTab(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = NotifyBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NotifyBackground)
                ) {
                    // Persistent Mini Player Bar
                    MiniPlayerBar(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        isFavorite = isCurrentTrackFav,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onSkipNextClick = { viewModel.skipToNext() },
                        onFavoriteToggle = { currentTrack?.let { viewModel.toggleFavorite(it) } },
                        onBarClick = { viewModel.setNowPlayingExpanded(true) }
                    )

                    // Spotify Bottom Navigation Bar
                    NavigationBar(
                        containerColor = NotifySurface,
                        contentColor = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = selectedTab == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(index) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SpotifyGreenBright,
                                    selectedTextColor = SpotifyGreenBright,
                                    unselectedIconColor = TextTertiary,
                                    unselectedTextColor = TextTertiary,
                                    indicatorColor = NotifySurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> {
                        val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
                        HomeScreen(
                            curatedPlaylists = viewModel.curatedPlaylists,
                            curatedTracks = viewModel.curatedTracks,
                            recentlyPlayed = recentlyPlayed,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                            onPlaylistClick = { playlist -> viewModel.openPlaylistDetail(playlist) },
                            onFavoriteToggle = { track -> viewModel.toggleFavorite(track) },
                            onAddToPlaylistClick = { track -> viewModel.showAddToPlaylistDialog(track) },
                            isFavorite = { trackId -> viewModel.isTrackFavorited(trackId) }
                        )
                    }
                    1 -> {
                        val searchQuery by viewModel.searchQuery.collectAsState()
                        val searchFilter by viewModel.searchFilter.collectAsState()
                        val searchResults by viewModel.searchResults.collectAsState()
                        val localTracks by viewModel.localTracks.collectAsState()

                        SearchScreen(
                            query = searchQuery,
                            filter = searchFilter,
                            searchResults = searchResults,
                            localTracks = localTracks,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onFilterChange = { viewModel.setSearchFilter(it) },
                            onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                            onFavoriteToggle = { track -> viewModel.toggleFavorite(track) },
                            onAddToPlaylistClick = { track -> viewModel.showAddToPlaylistDialog(track) },
                            onScanLocalClick = { viewModel.scanLocalMusic() },
                            isFavorite = { trackId -> viewModel.isTrackFavorited(trackId) }
                        )
                    }
                    2 -> {
                        val localTracks by viewModel.localTracks.collectAsState()
                        val localAlbums by viewModel.localAlbums.collectAsState()
                        val localArtists by viewModel.localArtists.collectAsState()
                        val localFolders by viewModel.localFolders.collectAsState()
                        val category by viewModel.localCategory.collectAsState()
                        val sortOrder by viewModel.localSortOrder.collectAsState()
                        val isScanning by viewModel.isScanning.collectAsState()

                        LocalMusicScreen(
                            localTracks = localTracks,
                            albums = localAlbums,
                            artists = localArtists,
                            folders = localFolders,
                            category = category,
                            sortOrder = sortOrder,
                            isScanning = isScanning,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onCategoryChange = { viewModel.setLocalCategory(it) },
                            onSortChange = { viewModel.setLocalSortOrder(it) },
                            onRescanClick = { viewModel.scanLocalMusic() },
                            onGenerateDemoAudio = { viewModel.generateDemoAudio() },
                            onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                            onFavoriteToggle = { track -> viewModel.toggleFavorite(track) },
                            onAddToPlaylistClick = { track -> viewModel.showAddToPlaylistDialog(track) },
                            isFavorite = { trackId -> viewModel.isTrackFavorited(trackId) }
                        )
                    }
                    3 -> {
                        LibraryScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Full Screen Now Playing Sheet Modal
        AnimatedVisibility(
            visible = isNowPlayingExpanded && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            NowPlayingSheet(
                track = currentTrack,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                isFavorite = isCurrentTrackFav,
                onCollapse = { viewModel.setNowPlayingExpanded(false) },
                onPlayPause = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) },
                onSkipNext = { viewModel.skipToNext() },
                onSkipPrev = { viewModel.skipToPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onToggleFavorite = { currentTrack?.let { viewModel.toggleFavorite(it) } },
                onOpenQueue = { viewModel.setQueueSheetVisible(true) },
                onOpenEqualizer = { viewModel.showEqualizer() }
            )
        }

        // Equalizer Bottom Sheet
        if (isEqualizerVisible) {
            EqualizerSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.hideEqualizer() }
            )
        }

        // Up Next Queue Sheet
        if (isQueueSheetVisible) {
            val currentIndex by viewModel.playerManager.currentIndex.collectAsState()
            QueueSheet(
                queue = queue,
                currentIndex = currentIndex,
                onTrackSelect = { index ->
                    viewModel.playTrack(queue[index], queue)
                },
                onClearQueue = { viewModel.playerManager.clearQueue() },
                onDismiss = { viewModel.setQueueSheetVisible(false) }
            )
        }

        // Add To Playlist Dialog
        trackForPlaylistDialog?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = userPlaylists,
                onSelectPlaylist = { playlistId, playlistName ->
                    viewModel.addTrackToPlaylist(playlistId, playlistName, track)
                },
                onCreateNewPlaylistClick = {
                    viewModel.showCreatePlaylistDialog()
                },
                onDismiss = { viewModel.dismissAddToPlaylistDialog() }
            )
        }

        // Create Playlist Dialog
        if (isCreatePlaylistDialogVisible) {
            CreatePlaylistDialog(
                onConfirm = { name, desc ->
                    viewModel.createPlaylist(name, desc)
                },
                onDismiss = { viewModel.dismissCreatePlaylistDialog() }
            )
        }
    }
}
