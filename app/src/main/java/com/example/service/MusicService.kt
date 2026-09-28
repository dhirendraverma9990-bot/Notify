package com.example.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import com.example.MainActivity
import com.example.NotifyApplication
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * MusicService — Background Audio Service foundation for Notify.
 *
 * Implements the native Android background audio architecture that powers
 * uninterrupted audio playback when minimized and lockscreen media controls.
 *
 * Coordinates:
 * - [MediaSessionCompat] with [NotifyMediaCallback] for system-wide and lockscreen media commands
 * - [NotifyNotificationManager] with [androidx.media.app.NotificationCompat.MediaStyle]
 *   persistent notification with track metadata, album art, and transport actions
 * - Foreground Service lifecycle management with [ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK]
 * - [MediaButtonReceiver] dispatch for headset and lockscreen hardware/widget buttons
 */
class MusicService : Service() {

    private var mediaSession: MediaSessionCompat? = null
    private lateinit var notificationManager: NotifyNotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateObservationJob: Job? = null

    private var cachedArtworkTrackId: String? = null
    private var cachedArtwork: Bitmap? = null

    companion object {
        const val ACTION_PLAY = "com.example.notify.PLAY"
        const val ACTION_PAUSE = "com.example.notify.PAUSE"
        const val ACTION_NEXT = "com.example.notify.NEXT"
        const val ACTION_PREV = "com.example.notify.PREV"
        const val ACTION_STOP = "com.example.notify.STOP"
        const val ACTION_UPDATE_NOTIFICATION = "com.example.notify.UPDATE_NOTIFICATION"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotifyNotificationManager(this)
        initMediaSession()
        observePlayerState()
    }

    private fun initMediaSession() {
        val app = application as? NotifyApplication
        val playerManager = app?.playerManager ?: return

        // Session Activity pending intent: Clicking lockscreen player or notification opens MainActivity
        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // MediaButtonReceiver intent for hardware & bluetooth controls
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null, this, MediaButtonReceiver::class.java)
        val mediaButtonPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            mediaButtonIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSessionCompat(this, "NotifyMediaSession").apply {
            setSessionActivity(sessionActivityPendingIntent)
            setMediaButtonReceiver(mediaButtonPendingIntent)
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(
                NotifyMediaCallback(
                    playerManager = playerManager,
                    onStopRequested = {
                        stopPlaybackService()
                    }
                )
            )

            isActive = true
        }
    }

    /**
     * Actively observes [playerManager] state flows.
     * Ensures MediaSession and lockscreen notifications are always in sync,
     * even when the user navigates between screens or minimizes the app.
     */
    private fun observePlayerState() {
        val app = application as? NotifyApplication
        val playerManager = app?.playerManager ?: return

        stateObservationJob?.cancel()
        stateObservationJob = serviceScope.launch {
            combine(
                playerManager.currentTrack,
                playerManager.isPlaying,
                playerManager.repeatMode,
                playerManager.isShuffleEnabled
            ) { track, isPlaying, repeatMode, isShuffle ->
                PlaybackSnapshot(track, isPlaying, repeatMode, isShuffle)
            }.collect { snapshot ->
                syncSessionAndNotification(snapshot)
            }
        }
    }

    private data class PlaybackSnapshot(
        val track: Track?,
        val isPlaying: Boolean,
        val repeatMode: RepeatMode,
        val isShuffle: Boolean
    )

    private fun syncSessionAndNotification(snapshot: PlaybackSnapshot) {
        val track = snapshot.track
        val isPlaying = snapshot.isPlaying
        val session = mediaSession ?: return
        val app = application as? NotifyApplication
        val playerManager = app?.playerManager ?: return
        val currentPos = playerManager.currentPositionMs.value

        if (track == null) {
            notificationManager.cancelNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            return
        }

        // Update MediaSessionCompat metadata and playback state
        updateMediaSession(track, isPlaying, currentPos, snapshot.repeatMode, snapshot.isShuffle)

        val artwork = getOrLoadArtwork(track)
        val notification = notificationManager.buildNotification(
            track = track,
            isPlaying = isPlaying,
            mediaSession = session,
            customArtwork = artwork
        )

        if (isPlaying) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NotifyNotificationManager.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NotifyNotificationManager.NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Keep the notification visible on the lockscreen and drawer when paused
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            notificationManager.showNotification(track, isPlaying = false, mediaSession = session, customArtwork = artwork)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? NotifyApplication
        val playerManager = app?.playerManager

        // Delegate media button intents to MediaButtonReceiver and MediaSessionCallback
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        when (intent?.action) {
            ACTION_PLAY -> playerManager?.play()
            ACTION_PAUSE -> playerManager?.pause()
            ACTION_NEXT -> playerManager?.skipToNext()
            ACTION_PREV -> playerManager?.skipToPrevious()
            ACTION_STOP -> {
                playerManager?.pause()
                stopPlaybackService()
                return START_NOT_STICKY
            }
        }

        // Trigger synchronization check
        val track = playerManager?.currentTrack?.value
        val isPlaying = playerManager?.isPlaying?.value ?: false
        val repeat = playerManager?.repeatMode?.value ?: RepeatMode.OFF
        val shuffle = playerManager?.isShuffleEnabled?.value ?: false
        syncSessionAndNotification(PlaybackSnapshot(track, isPlaying, repeat, shuffle))

        return START_STICKY
    }

    private fun updateMediaSession(
        track: Track,
        isPlaying: Boolean,
        currentPos: Long,
        repeatMode: RepeatMode,
        isShuffle: Boolean
    ) {
        val session = mediaSession ?: return

        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        val playbackSpeed = if (isPlaying) 1.0f else 0.0f

        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SET_REPEAT_MODE or
                PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, currentPos, playbackSpeed, SystemClock.elapsedRealtime())
            .addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    NotifyMediaCallback.ACTION_TOGGLE_SHUFFLE,
                    "Toggle Shuffle",
                    android.R.drawable.ic_menu_rotate
                ).build()
            )
            .addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    NotifyMediaCallback.ACTION_TOGGLE_REPEAT,
                    "Toggle Repeat",
                    android.R.drawable.ic_menu_revert
                ).build()
            )

        session.setPlaybackState(stateBuilder.build())

        // Sync repeat and shuffle modes to MediaSession
        val sessionRepeatMode = when (repeatMode) {
            RepeatMode.OFF -> PlaybackStateCompat.REPEAT_MODE_NONE
            RepeatMode.ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
            RepeatMode.ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
        }
        session.setRepeatMode(sessionRepeatMode)

        val sessionShuffleMode = if (isShuffle) {
            PlaybackStateCompat.SHUFFLE_MODE_ALL
        } else {
            PlaybackStateCompat.SHUFFLE_MODE_NONE
        }
        session.setShuffleMode(sessionShuffleMode)

        // Set rich MediaMetadataCompat for lockscreen & Bluetooth displays
        val artwork = getOrLoadArtwork(track)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, track.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artwork)
            .build()

        session.setMetadata(metadata)
    }

    private fun getOrLoadArtwork(track: Track): Bitmap {
        if (cachedArtworkTrackId == track.id && cachedArtwork != null) {
            return cachedArtwork!!
        }
        val loaded = notificationManager.loadOrGenerateArtwork(track)
        cachedArtwork = loaded
        cachedArtworkTrackId = track.id
        return loaded
    }

    private fun stopPlaybackService() {
        notificationManager.cancelNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateObservationJob?.cancel()
        serviceScope.cancel()
        notificationManager.cancelNotification()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        cachedArtwork = null
        cachedArtworkTrackId = null
    }
}
