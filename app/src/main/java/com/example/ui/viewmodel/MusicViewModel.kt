package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NotifyApplication
import com.example.data.local.PlaylistEntity
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Folder
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import com.example.service.MusicPlayerManager
import com.example.service.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LocalCategory {
    SONGS, ARTISTS, ALBUMS, FOLDERS
}

enum class SearchFilter {
    ALL, LOCAL, ONLINE, ARTISTS, ALBUMS
}

enum class LocalSortOrder {
    TITLE, ARTIST, DURATION
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotifyApplication
    val repository: MusicRepository = app.repository
    val audioRepository: com.example.data.repository.AudioRepository = app.audioRepository
    val playerManager: MusicPlayerManager = app.playerManager

    // UI Tab Navigation
    private val _selectedTab = MutableStateFlow(0) // 0: Home, 1: Search, 2: Local, 3: Library
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Now Playing full-screen modal visibility
    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    // Up Next Queue bottom sheet
    private val _isQueueSheetVisible = MutableStateFlow(false)
    val isQueueSheetVisible: StateFlow<Boolean> = _isQueueSheetVisible.asStateFlow()

    // Add to playlist modal track
    private val _trackForPlaylistDialog = MutableStateFlow<Track?>(null)
    val trackForPlaylistDialog: StateFlow<Track?> = _trackForPlaylistDialog.asStateFlow()

    // Create playlist dialog visibility
    private val _isCreatePlaylistDialogVisible = MutableStateFlow(false)
    val isCreatePlaylistDialogVisible: StateFlow<Boolean> = _isCreatePlaylistDialogVisible.asStateFlow()

    // Active playlist being viewed in detail
    private val _activePlaylist = MutableStateFlow<Playlist?>(null)
    val activePlaylist: StateFlow<Playlist?> = _activePlaylist.asStateFlow()

    // Equalizer sheet modal visibility
    private val _isEqualizerVisible = MutableStateFlow(false)
    val isEqualizerVisible: StateFlow<Boolean> = _isEqualizerVisible.asStateFlow()

    // Audio Equalizer State forwarded from EqualizerManager
    val equalizerState: StateFlow<com.example.data.model.EqualizerState> = playerManager.equalizerManager.equalizerState
    val builtInPresets: List<com.example.data.model.EqPreset> = com.example.data.model.BuiltInPresets

    // Audio Player State forwarded from MusicPlayerManager
    val currentTrack: StateFlow<Track?> = playerManager.currentTrack
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val isBuffering: StateFlow<Boolean> = playerManager.isBuffering
    val bufferPercentage: StateFlow<Int> = playerManager.bufferPercentage
    val playbackError: StateFlow<String?> = playerManager.playbackError
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs
    val isShuffleEnabled: StateFlow<Boolean> = playerManager.isShuffleEnabled
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val queue: StateFlow<List<Track>> = playerManager.queue

    fun clearPlaybackError() = playerManager.clearError()

    // Room Database Flows
    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPlaylists: StateFlow<List<PlaylistEntity>> = repository.userPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Track>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Home Screen Data
    val curatedPlaylists: List<Playlist> = repository.getCuratedPlaylists()
    val curatedTracks: List<Track> = repository.getCuratedStreamingTracks()

    // Local Music Data & State
    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _localCategory = MutableStateFlow(LocalCategory.SONGS)
    val localCategory: StateFlow<LocalCategory> = _localCategory.asStateFlow()

    private val _localSortOrder = MutableStateFlow(LocalSortOrder.TITLE)
    val localSortOrder: StateFlow<LocalSortOrder> = _localSortOrder.asStateFlow()

    val localAlbums: StateFlow<List<Album>> = _localTracks
        .combine(_localSortOrder) { tracks, _ -> repository.extractAlbums(tracks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localArtists: StateFlow<List<Artist>> = _localTracks
        .combine(_localSortOrder) { tracks, _ -> repository.extractArtists(tracks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localFolders: StateFlow<List<Folder>> = _localTracks
        .combine(_localSortOrder) { tracks, _ -> repository.extractFolders(tracks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Screen State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    // Search Results combined from local and curated streaming tracks
    val searchResults: StateFlow<List<Track>> = combine(
        _searchQuery,
        _searchFilter,
        _localTracks
    ) { query, filter, localList ->
        if (query.isBlank()) {
            return@combine emptyList()
        }
        val pool = when (filter) {
            SearchFilter.ALL -> localList + curatedTracks
            SearchFilter.LOCAL -> localList
            SearchFilter.ONLINE -> curatedTracks
            SearchFilter.ARTISTS -> (localList + curatedTracks).filter {
                it.artist.contains(query, ignoreCase = true)
            }
            SearchFilter.ALBUMS -> (localList + curatedTracks).filter {
                it.album.contains(query, ignoreCase = true)
            }
        }
        pool.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true) ||
                    (it.genre?.contains(query, ignoreCase = true) == true)
        }.distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback message (e.g. "Track added to playlist", "Demo audio created")
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        scanLocalMusic()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        _activePlaylist.value = null // reset detail view on tab switch
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun setQueueSheetVisible(visible: Boolean) {
        _isQueueSheetVisible.value = visible
    }

    fun setLocalCategory(category: LocalCategory) {
        _localCategory.value = category
    }

    fun setLocalSortOrder(order: LocalSortOrder) {
        _localSortOrder.value = order
        val current = _localTracks.value
        _localTracks.value = when (order) {
            LocalSortOrder.TITLE -> current.sortedBy { it.title.lowercase() }
            LocalSortOrder.ARTIST -> current.sortedBy { it.artist.lowercase() }
            LocalSortOrder.DURATION -> current.sortedByDescending { it.durationMs }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun openPlaylistDetail(playlist: Playlist) {
        _activePlaylist.value = playlist
    }

    fun closePlaylistDetail() {
        _activePlaylist.value = null
    }

    fun scanLocalMusic() {
        viewModelScope.launch {
            _isScanning.value = true
            val tracks = audioRepository.getLocalAudioTracks(forceRefresh = true)
            _localTracks.value = when (_localSortOrder.value) {
                LocalSortOrder.TITLE -> tracks.sortedBy { it.title.lowercase() }
                LocalSortOrder.ARTIST -> tracks.sortedBy { it.artist.lowercase() }
                LocalSortOrder.DURATION -> tracks.sortedByDescending { it.durationMs }
            }
            _isScanning.value = false
        }
    }

    fun generateDemoAudio() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = audioRepository.generateDemoAudioFiles()
            result.onSuccess { count ->
                _userMessage.value = "Generated $count high-fidelity sample tracks!"
                scanLocalMusic()
            }.onFailure {
                _userMessage.value = "Failed to generate demo tracks: ${it.localizedMessage}"
                _isScanning.value = false
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // Playback Controls
    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            val idx = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            playerManager.playTrackList(newQueue, idx)
        } else {
            playerManager.playTrack(track)
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)

    fun skipToNext() = playerManager.skipToNext()

    fun skipToPrevious() = playerManager.skipToPrevious()

    fun toggleShuffle() = playerManager.toggleShuffle()

    fun toggleRepeat() = playerManager.toggleRepeat()

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavorite(track)
            _userMessage.value = if (isNowFav) "Added to Liked Songs" else "Removed from Liked Songs"
        }
    }

    fun isTrackFavorited(trackId: String): Boolean {
        return favoriteTracks.value.any { it.id == trackId }
    }

    // Playlist Dialog and Actions
    fun showAddToPlaylistDialog(track: Track) {
        _trackForPlaylistDialog.value = track
    }

    fun dismissAddToPlaylistDialog() {
        _trackForPlaylistDialog.value = null
    }

    fun showCreatePlaylistDialog() {
        _isCreatePlaylistDialogVisible.value = true
    }

    fun dismissCreatePlaylistDialog() {
        _isCreatePlaylistDialogVisible.value = false
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
            _userMessage.value = "Playlist \"$name\" created!"
            _isCreatePlaylistDialogVisible.value = false
        }
    }

    fun addTrackToPlaylist(playlistId: String, playlistName: String, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
            _userMessage.value = "Added to \"$playlistName\""
            _trackForPlaylistDialog.value = null
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            _userMessage.value = "Removed from playlist"
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _activePlaylist.value = null
            _userMessage.value = "Playlist deleted"
        }
    }

    // ==========================================
    // Professional Audio Equalizer Controls
    // ==========================================

    fun showEqualizer() {
        _isEqualizerVisible.value = true
    }

    fun hideEqualizer() {
        _isEqualizerVisible.value = false
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        playerManager.equalizerManager.setEnabled(enabled)
    }

    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        playerManager.equalizerManager.setBandLevel(bandIndex, levelDb)
    }

    fun applyPreset(preset: com.example.data.model.EqPreset) {
        playerManager.equalizerManager.applyPreset(preset)
    }

    fun setBassBoost(strength: Int) {
        playerManager.equalizerManager.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Int) {
        playerManager.equalizerManager.setVirtualizer(strength)
    }

    fun setLoudnessGain(gainMb: Int) {
        playerManager.equalizerManager.setLoudnessGain(gainMb)
    }

    fun setStereoBalance(balance: Float) {
        playerManager.equalizerManager.setStereoBalance(balance)
    }

    fun resetEqualizer() {
        playerManager.equalizerManager.resetToFlat()
    }
}
