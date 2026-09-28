package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.MainActivity
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NotifyNotificationManager
 *
 * Dedicated NotificationManager implementation that uses [NotificationCompat.MediaStyle]
 * to display persistent, rich media playback controls with track metadata and high-fidelity
 * album art in the Android notification drawer and lock screen.
 *
 * Conforms to modern Android media requirements:
 * - Proper [NotificationChannel] with [NotificationManager.IMPORTANCE_LOW]
 * - [MediaStyle] binding with [MediaSessionCompat.Token]
 * - Compact view actions (Previous, Play/Pause, Next) for lock screens & heads-up
 * - Seamless integration with Android system media player widget
 */
class NotifyNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Spotify-style persistent media playback controls for Notify"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds a persistent [Notification] using [MediaStyle] with track metadata,
     * action intents, and album artwork.
     */
    fun buildNotification(
        track: Track,
        isPlaying: Boolean,
        mediaSession: MediaSessionCompat,
        customArtwork: Bitmap? = null
    ): Notification {
        val contentPendingIntent = createContentPendingIntent()
        val prevPendingIntent = createActionPendingIntent(MusicService.ACTION_PREV, REQUEST_CODE_PREV)
        val playPauseAction = if (isPlaying) MusicService.ACTION_PAUSE else MusicService.ACTION_PLAY
        val playPausePendingIntent = createActionPendingIntent(playPauseAction, REQUEST_CODE_PLAY_PAUSE)
        val nextPendingIntent = createActionPendingIntent(MusicService.ACTION_NEXT, REQUEST_CODE_NEXT)
        val stopPendingIntent = createActionPendingIntent(MusicService.ACTION_STOP, REQUEST_CODE_STOP)

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        // Resolve or generate high-fidelity Spotify-style artwork
        val artworkBitmap = customArtwork ?: loadOrGenerateArtwork(track)

        // Configure MediaStyle
        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2) // Previous, Play/Pause, Next
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(artworkBitmap)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(track.album)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            // Action 0: Previous
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            // Action 1: Play/Pause
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            // Action 2: Next
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            // Action 3: Stop / Close
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPendingIntent)
            .setStyle(mediaStyle)
            .setColor(COLOR_SPOTIFY_GREEN)
            .setColorized(true)

        return builder.build()
    }

    /**
     * Posts or updates the media notification in the notification drawer.
     */
    fun showNotification(
        track: Track,
        isPlaying: Boolean,
        mediaSession: MediaSessionCompat,
        customArtwork: Bitmap? = null
    ) {
        val notification = buildNotification(track, isPlaying, mediaSession, customArtwork)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancels the active notification when playback stops.
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createContentPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_CONTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Loads artwork from MediaStore or generates a branded Spotify-style vinyl artwork.
     */
    fun loadOrGenerateArtwork(track: Track): Bitmap {
        if (!track.albumArtUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(track.albumArtUri)
                if (uri.scheme == "content") {
                    val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.setTargetSize(256, 256)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    if (bitmap != null) return bitmap
                }
            } catch (e: Exception) {
                // Fallback to vector/canvas rendering on any decode failure
            }
        }

        return createBrandedArtwork(track.title, track.artist)
    }

    /**
     * Generates a sleek, high-contrast Spotify album artwork bitmap with
     * neon acoustic pulse waves and typography.
     */
    fun createBrandedArtwork(title: String, artist: String): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark Spotify surface background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#0E131E")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Subtle gradient vignette badge
        val center = size / 2f
        val discPaint = Paint().apply {
            color = Color.parseColor("#171F30")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(center, center, 96f, discPaint)

        // Inner glowing ring
        val ringPaint = Paint().apply {
            color = Color.parseColor("#1DB954")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            alpha = 100
            isAntiAlias = true
        }
        canvas.drawCircle(center, center, 80f, ringPaint)

        // Spotify Waveform Arcs
        val wavePaint = Paint().apply {
            color = Color.parseColor("#1DB954")
            strokeWidth = 11f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        val arc1 = Path().apply {
            moveTo(center - 46f, center - 26f)
            quadTo(center, center - 46f, center + 46f, center - 26f)
        }
        val arc2 = Path().apply {
            moveTo(center - 36f, center)
            quadTo(center, center - 18f, center + 36f, center)
        }
        val arc3 = Path().apply {
            moveTo(center - 26f, center + 26f)
            quadTo(center, center + 10f, center + 26f, center + 26f)
        }

        canvas.drawPath(arc1, wavePaint)
        canvas.drawPath(arc2, wavePaint)
        canvas.drawPath(arc3, wavePaint)

        return bitmap
    }

    companion object {
        const val CHANNEL_ID = "notify_playback_channel"
        const val CHANNEL_NAME = "Notify Audio Playback"
        const val NOTIFICATION_ID = 1001

        const val REQUEST_CODE_CONTENT = 100
        const val REQUEST_CODE_PREV = 101
        const val REQUEST_CODE_PLAY_PAUSE = 102
        const val REQUEST_CODE_NEXT = 103
        const val REQUEST_CODE_STOP = 104

        val COLOR_SPOTIFY_GREEN: Int = Color.parseColor("#1DB954")
    }
}
