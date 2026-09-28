package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NotifyApplication
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Folder
import com.example.data.model.Track
import com.example.data.repository.AudioRepository
import com.example.service.MusicPlayerManager
import com.example.service.MusicService
import com.example.service.NotifyMediaCallback
import com.example.service.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * PlaybackStatus
 *
 * Represents the discrete playback states of the background media engine.
 */
enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    STOPPED
}

/**
 * MusicPlayerViewModel
 *
 * Connects the [AudioRepository] to the UI layer and interacts with the background [MediaSessionCompat].
 *
 * Key Responsibilities:
 * 1. Exposes real-time track metadata (title, artist, album, album art URI, format, duration).
 * 2. Exposes granular playback status (playing, paused, buffering, idle) and high-resolution progress.
 * 3. Dispatches transport controls (play, pause, next, prev, seek, shuffle, repeat) directly to the
 *    background MediaSession and [MusicService].
 * 4. Coordinates with [AudioRepository] to load, scan, and filter local audio files.
 */
class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotifyApplication
    val audioRepository: AudioRepository = app.audioRepository
    private val playerManager: MusicPlayerManager = app.playerManager

    // ==========================================
    // 1. Current Track Metadata & Queue States
    // ==========================================

    val currentTrack: StateFlow<Track?> = playerManager.currentTrack

    val trackTitle: StateFlow<String> = currentTrack
        .map { it?.title ?: "No Track Playing" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "No Track Playing")

    val trackArtist: StateFlow<String> = currentTrack
        .map { it?.artist ?: "Unknown Artist" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Unknown Artist")

    val trackAlbum: StateFlow<String> = currentTrack
        .map { it?.album ?: "Unknown Album" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Unknown Album")

    val trackAlbumArtUri: StateFlow<String?> = currentTrack
        .map { it?.albumArtUri }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLocalTrack: StateFlow<Boolean> = currentTrack
        .map { it?.isLocal ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val queue: StateFlow<List<Track>> = playerManager.queue
    val currentIndex: StateFlow<Int> = playerManager.currentIndex

    // ==========================================
    // 2. Playback Status States
    // ==========================================

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val isBuffering: StateFlow<Boolean> = playerManager.isBuffering

    val playbackStatus: StateFlow<PlaybackStatus> = combine(
        isPlaying,
        isBuffering,
        currentTrack
    ) { playing, buffering, track ->
        when {
            track == null -> PlaybackStatus.IDLE
            buffering -> PlaybackStatus.BUFFERING
            playing -> PlaybackStatus.PLAYING
            else -> PlaybackStatus.PAUSED
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackStatus.IDLE)

    // ==========================================
    // 3. Playback Progress & Duration States
    // ==========================================

    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs

    val progressFraction: StateFlow<Float> = combine(
        currentPositionMs,
        durationMs
    ) { pos, dur ->
        if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0.0f, 1.0f) else 0.0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0f)

    val formattedPosition: StateFlow<String> = currentPositionMs
        .map { formatTime(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0:00")

    val formattedDuration: StateFlow<String> = durationMs
        .map { formatTime(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0:00")

    val remainingMs: StateFlow<Long> = combine(
        currentPositionMs,
        durationMs
    ) { pos, dur ->
        (dur - pos).coerceAtLeast(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val formattedRemaining: StateFlow<String> = remainingMs
        .map { "-${formatTime(it)}" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "-0:00")

    // ==========================================
    // 4. Playback Modes (Shuffle & Repeat)
    // ==========================================

    val isShuffleEnabled: StateFlow<Boolean> = playerManager.isShuffleEnabled
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode

    // ==========================================
    // 5. AudioRepository Local Audio Scanning & Data
    // ==========================================

    val localTracks: StateFlow<List<Track>> = audioRepository.cachedTracksFlow

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    /**
     * Scans local device audio using [AudioRepository].
     */
    fun scanLocalAudio(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val tracks = audioRepository.getLocalAudioTracks(forceRefresh = forceRefresh)
                _statusMessage.value = "Found ${tracks.size} local tracks"
            } catch (e: Exception) {
                _statusMessage.value = "Scanning error: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * Generates sample audio tracks into storage if local files are missing.
     */
    fun generateDemoAudioFiles() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = audioRepository.generateDemoAudioFiles()
            result.onSuccess { count ->
                _statusMessage.value = "Generated $count sample tracks!"
                scanLocalAudio(forceRefresh = true)
            }.onFailure {
                _statusMessage.value = "Demo generation failed: ${it.localizedMessage}"
                _isScanning.value = false
            }
        }
    }

    suspend fun getAlbums(): List<Album> = audioRepository.getAlbums()

    suspend fun getArtists(): List<Artist> = audioRepository.getArtists()

    suspend fun getFolders(): List<Folder> = audioRepository.getFolders()

    suspend fun getTracksByAlbum(album: String): List<Track> = audioRepository.getTracksByAlbum(album)

    suspend fun getTracksByArtist(artist: String): List<Track> = audioRepository.getTracksByArtist(artist)

    suspend fun getTracksByFolder(folder: String): List<Track> = audioRepository.getTracksByFolder(folder)

    // ==========================================
    // 6. MediaSession & Background Transport Controls
    // ==========================================

    /**
     * Starts or resumes playback and updates background MediaSession.
     */
    fun play() {
        playerManager.play()
    }

    /**
     * Pauses playback while retaining current media position.
     */
    fun pause() {
        playerManager.pause()
    }

    /**
     * Toggles between play and pause states.
     */
    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    /**
     * Skips to the next track in the queue.
     */
    fun skipToNext() {
        playerManager.skipToNext()
    }

    /**
     * Skips to the previous track or replays current song.
     */
    fun skipToPrevious() {
        playerManager.skipToPrevious()
    }

    /**
     * Seeks to a specific millisecond position in the active track.
     */
    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    /**
     * Seeks based on a normalized fraction (0.0 to 1.0) of duration.
     */
    fun seekToFraction(fraction: Float) {
        val duration = durationMs.value
        if (duration > 0) {
            val targetMs = (fraction.coerceIn(0.0f, 1.0f) * duration).toLong()
            playerManager.seekTo(targetMs)
        }
    }

    /**
     * Plays a specific track and optionally updates the playback queue.
     */
    fun playTrack(track: Track, newQueue: List<Track> = emptyList()) {
        if (newQueue.isNotEmpty()) {
            val index = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            playerManager.playTrackList(newQueue, index)
        } else {
            playerManager.playTrack(track)
        }
    }

    /**
     * Plays a full playlist/queue starting at [startIndex].
     */
    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        playerManager.playTrackList(tracks, startIndex)
    }

    /**
     * Toggles shuffle mode on/off.
     */
    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    /**
     * Cycles through repeat modes (OFF -> ALL -> ONE -> OFF).
     */
    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    /**
     * Stops playback and stops the background foreground service.
     */
    fun stop() {
        playerManager.stop()
    }

    /**
     * Clears the current playback queue.
     */
    fun clearQueue() {
        playerManager.clearQueue()
    }

    /**
     * Sends custom media action intent to [MusicService] background receiver.
     */
    fun sendMediaSessionAction(action: String, extras: Bundle? = null) {
        val intent = Intent(app, MusicService::class.java).apply {
            this.action = action
            if (extras != null) putExtras(extras)
        }
        try {
            app.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // ==========================================
    // Professional Audio Equalizer Controls
    // ==========================================

    val equalizerState: StateFlow<com.example.data.model.EqualizerState> = playerManager.equalizerManager.equalizerState
    val builtInPresets: List<com.example.data.model.EqPreset> = com.example.data.model.BuiltInPresets

    fun setEqualizerEnabled(enabled: Boolean) = playerManager.equalizerManager.setEnabled(enabled)
    fun setBandLevel(bandIndex: Int, levelDb: Float) = playerManager.equalizerManager.setBandLevel(bandIndex, levelDb)
    fun applyPreset(preset: com.example.data.model.EqPreset) = playerManager.equalizerManager.applyPreset(preset)
    fun setBassBoost(strength: Int) = playerManager.equalizerManager.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = playerManager.equalizerManager.setVirtualizer(strength)
    fun setLoudnessGain(gainMb: Int) = playerManager.equalizerManager.setLoudnessGain(gainMb)
    fun setStereoBalance(balance: Float) = playerManager.equalizerManager.setStereoBalance(balance)
    fun resetEqualizer() = playerManager.equalizerManager.resetToFlat()

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
