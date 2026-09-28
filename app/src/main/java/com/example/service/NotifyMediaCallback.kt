package com.example.service

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent

/**
 * NotifyMediaCallback
 *
 * A specialized [MediaSessionCompat.Callback] implementation that receives and translates
 * media commands originating from the Android system notification, lock screen controls,
 * hardware Bluetooth/headset buttons, Android Auto, and wear devices into actions
 * performed on the [MusicPlayerManager].
 */
class NotifyMediaCallback(
    private val playerManager: MusicPlayerManager,
    private val onStopRequested: (() -> Unit)? = null
) : MediaSessionCompat.Callback() {

    override fun onPlay() {
        super.onPlay()
        playerManager.play()
    }

    override fun onPause() {
        super.onPause()
        playerManager.pause()
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        playerManager.skipToNext()
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        playerManager.skipToPrevious()
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        playerManager.seekTo(pos)
    }

    override fun onStop() {
        super.onStop()
        playerManager.stop()
        onStopRequested?.invoke()
    }

    override fun onFastForward() {
        super.onFastForward()
        val current = playerManager.currentPositionMs.value
        val duration = playerManager.durationMs.value
        playerManager.seekTo((current + 10000L).coerceAtMost(duration))
    }

    override fun onRewind() {
        super.onRewind()
        val current = playerManager.currentPositionMs.value
        playerManager.seekTo((current - 10000L).coerceAtLeast(0L))
    }

    override fun onSetRepeatMode(repeatMode: Int) {
        super.onSetRepeatMode(repeatMode)
        when (repeatMode) {
            PlaybackStateCompat.REPEAT_MODE_ONE -> playerManager.setRepeatMode(RepeatMode.ONE)
            PlaybackStateCompat.REPEAT_MODE_ALL, PlaybackStateCompat.REPEAT_MODE_GROUP -> playerManager.setRepeatMode(RepeatMode.ALL)
            PlaybackStateCompat.REPEAT_MODE_NONE -> playerManager.setRepeatMode(RepeatMode.OFF)
            else -> playerManager.toggleRepeat()
        }
    }

    override fun onSetShuffleMode(shuffleMode: Int) {
        super.onSetShuffleMode(shuffleMode)
        when (shuffleMode) {
            PlaybackStateCompat.SHUFFLE_MODE_ALL, PlaybackStateCompat.SHUFFLE_MODE_GROUP -> playerManager.setShuffleEnabled(true)
            PlaybackStateCompat.SHUFFLE_MODE_NONE -> playerManager.setShuffleEnabled(false)
            else -> playerManager.toggleShuffle()
        }
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
        super.onCustomAction(action, extras)
        when (action) {
            ACTION_TOGGLE_SHUFFLE -> playerManager.toggleShuffle()
            ACTION_TOGGLE_REPEAT -> playerManager.toggleRepeat()
            ACTION_REPLAY -> playerManager.seekTo(0)
            ACTION_FORWARD_10 -> onFastForward()
            ACTION_REWIND_10 -> onRewind()
        }
    }

    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        val keyEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }

        if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    playerManager.play()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    playerManager.pause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                    playerManager.togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    playerManager.skipToNext()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    playerManager.skipToPrevious()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    playerManager.stop()
                    onStopRequested?.invoke()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    onFastForward()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    onRewind()
                    return true
                }
            }
        }

        return super.onMediaButtonEvent(mediaButtonEvent)
    }

    companion object {
        const val ACTION_TOGGLE_SHUFFLE = "com.example.notify.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.example.notify.ACTION_TOGGLE_REPEAT"
        const val ACTION_REPLAY = "com.example.notify.ACTION_REPLAY"
        const val ACTION_FORWARD_10 = "com.example.notify.ACTION_FORWARD_10"
        const val ACTION_REWIND_10 = "com.example.notify.ACTION_REWIND_10"
    }
}
