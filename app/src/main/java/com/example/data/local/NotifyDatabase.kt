package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * NotifyDatabase
 *
 * Primary Room Database instance for Notify.
 * Manages tables for song metadata, playlist definitions, playlist tracks,
 * user playback history, and favorites.
 */
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        PlaybackHistoryEntity::class,
        FavoriteTrackEntity::class,
        RecentlyPlayedEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NotifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao

    companion object {
        @Volatile
        private var INSTANCE: NotifyDatabase? = null

        fun getDatabase(context: Context): NotifyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotifyDatabase::class.java,
                    "notify_music_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
