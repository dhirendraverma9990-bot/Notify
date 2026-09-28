package com.example

import android.app.Application
import com.example.data.local.NotifyDatabase
import com.example.data.repository.MusicRepository
import com.example.service.MusicPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotifyApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val database: NotifyDatabase by lazy {
        NotifyDatabase.getDatabase(this)
    }

    val audioRepository: com.example.data.repository.AudioRepository by lazy {
        com.example.data.repository.AudioRepository(this)
    }

    val repository: MusicRepository by lazy {
        MusicRepository(
            context = this,
            favoriteDao = database.favoriteDao(),
            playlistDao = database.playlistDao(),
            recentlyPlayedDao = database.recentlyPlayedDao(),
            songDao = database.songDao(),
            playbackHistoryDao = database.playbackHistoryDao(),
            audioRepository = audioRepository
        )
    }

    val playerManager: MusicPlayerManager by lazy {
        MusicPlayerManager(this).apply {
            onTrackStarted = { track ->
                applicationScope.launch {
                    repository.addRecentlyPlayed(track)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onTerminate() {
        super.onTerminate()
        playerManager.release()
    }
}
