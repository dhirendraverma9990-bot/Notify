package com.example.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF, ALL, ONE
}

class MusicPlayerManager(private val context: Context) :
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener,
    AudioManager.OnAudioFocusChangeListener {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val wifiLock: WifiManager.WifiLock by lazy {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Notify:PlaybackWifiLock")
    }

    private fun acquireWifiLockIfNeeded() {
        try {
            if (!wifiLock.isHeld) {
                wifiLock.acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWifiLockIfNeeded() {
        try {
            if (wifiLock.isHeld) {
                wifiLock.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // State flows observed by UI
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _bufferPercentage = MutableStateFlow(0)
    val bufferPercentage: StateFlow<Int> = _bufferPercentage.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    fun clearError() {
        _playbackError.value = null
    }

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Callback for track changed (e.g. record into history)
    var onTrackStarted: ((Track) -> Unit)? = null

    val equalizerManager: EqualizerManager by lazy { EqualizerManager(context) }

    init {
        equalizerManager.onBalanceChanged = { left, right ->
            try {
                mediaPlayer?.setVolume(left, right)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener(this@MusicPlayerManager)
            setOnCompletionListener(this@MusicPlayerManager)
            setOnErrorListener(this@MusicPlayerManager)
            setOnBufferingUpdateListener { _, percent ->
                _bufferPercentage.value = percent
            }
            setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        _isBuffering.value = true
                        true
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                        _isBuffering.value = false
                        true
                    }
                    else -> false
                }
            }
        }
        val sessionId = mediaPlayer?.audioSessionId ?: 0
        if (sessionId > 0) {
            equalizerManager.attachSession(sessionId)
        }
    }

    fun playTrackList(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        _queue.value = tracks
        val safeIndex = startIndex.coerceIn(0, tracks.size - 1)
        _currentIndex.value = safeIndex
        playTrack(tracks[safeIndex])
    }

    fun playTrack(track: Track) {
        scope.launch {
            try {
                _playbackError.value = null
                _isBuffering.value = true
                _bufferPercentage.value = 0
                _currentTrack.value = track
                _currentPositionMs.value = 0L

                // Update queue if track not in queue
                val curQueue = _queue.value
                val existingIndex = curQueue.indexOfFirst { it.id == track.id }
                if (existingIndex >= 0) {
                    _currentIndex.value = existingIndex
                } else {
                    _queue.value = curQueue + track
                    _currentIndex.value = _queue.value.size - 1
                }

                if (!requestAudioFocus()) {
                    _isBuffering.value = false
                    return@launch
                }

                initMediaPlayer()
                mediaPlayer?.let { player ->
                    player.reset()
                    val uri = Uri.parse(track.contentUri)
                    player.setDataSource(context, uri)
                    player.prepareAsync()
                }

                onTrackStarted?.invoke(track)
                notifyService()
            } catch (e: Exception) {
                e.printStackTrace()
                _isBuffering.value = false
                _isPlaying.value = false
                _playbackError.value = "Streaming error: ${e.localizedMessage ?: "Unable to connect"}"
            }
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        _isBuffering.value = false
        val sessionId = mp?.audioSessionId ?: 0
        if (sessionId > 0) {
            equalizerManager.attachSession(sessionId)
        }
        acquireWifiLockIfNeeded()
        mp?.start()
        _isPlaying.value = true
        _durationMs.value = mp?.duration?.toLong() ?: _currentTrack.value?.durationMs ?: 0L
        startProgressTracking()
        notifyService()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                _isPlaying.value = true
            }
            RepeatMode.ALL -> {
                skipToNext()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value < _queue.value.size - 1) {
                    skipToNext()
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    stopProgressTracking()
                    releaseWifiLockIfNeeded()
                    notifyService()
                }
            }
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        _isBuffering.value = false
        _isPlaying.value = false
        releaseWifiLockIfNeeded()

        val errorDescription = when (extra) {
            MediaPlayer.MEDIA_ERROR_IO -> "Network connection lost. Unable to stream track."
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "Streaming request timed out."
            MediaPlayer.MEDIA_ERROR_MALFORMED -> "Unsupported audio stream format."
            else -> "Streaming error ($what, $extra)"
        }
        _playbackError.value = errorDescription

        initMediaPlayer()
        return true
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (_currentTrack.value == null) {
            val q = _queue.value
            if (q.isNotEmpty()) {
                playTrack(q[0])
            }
            return
        }
        if (requestAudioFocus()) {
            acquireWifiLockIfNeeded()
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracking()
            notifyService()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        stopProgressTracking()
        releaseWifiLockIfNeeded()
        notifyService()
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        _currentPositionMs.value = target
        try {
            mediaPlayer?.seekTo(target.toInt())
            notifyService()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun skipToNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIndex = if (_isShuffleEnabled.value) {
            (q.indices - _currentIndex.value).randomOrNull() ?: 0
        } else {
            if (_currentIndex.value + 1 < q.size) _currentIndex.value + 1 else 0
        }

        _currentIndex.value = nextIndex
        playTrack(q[nextIndex])
    }

    fun skipToPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        // If played more than 3 seconds, replay current track
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = if (_currentIndex.value - 1 >= 0) {
            _currentIndex.value - 1
        } else {
            q.size - 1
        }

        _currentIndex.value = prevIndex
        playTrack(q[prevIndex])
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        notifyService()
    }

    fun setShuffleEnabled(enabled: Boolean) {
        _isShuffleEnabled.value = enabled
        notifyService()
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        notifyService()
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        notifyService()
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = -1
        stop()
    }

    fun stop() {
        mediaPlayer?.stop()
        _isPlaying.value = false
        _currentTrack.value = null
        _currentPositionMs.value = 0L
        stopProgressTracking()
        releaseWifiLockIfNeeded()
        abandonAudioFocus()
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_STOP
        }
        context.startService(intent)
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (_isPlaying.value && mediaPlayer != null) {
                    try {
                        val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                        _currentPositionMs.value = pos
                    } catch (e: Exception) {
                        // ignored
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setOnAudioFocusChangeListener(this)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                play()
            }
        }
    }

    private fun notifyService() {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_UPDATE_NOTIFICATION
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        stopProgressTracking()
        mediaPlayer?.release()
        mediaPlayer = null
        equalizerManager.release()
        releaseWifiLockIfNeeded()
        abandonAudioFocus()
    }
}
