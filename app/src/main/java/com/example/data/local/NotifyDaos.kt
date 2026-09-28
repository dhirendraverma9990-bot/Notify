package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * SongDao
 *
 * Data Access Object for local song metadata catalog.
 * Supports reactive queries (Flow) for reactive Compose UI, full-text searching,
 * filtering by artist/album, and engagement metrics tracking.
 */
@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    fun getSongById(id: String): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongByIdDirect(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY title ASC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY trackNumber ASC, title ASC")
    fun getSongsByAlbum(album: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int = 20): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAddedSongs(limit: Int = 30): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: String)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM songs")
    suspend fun clearAllSongs()
}

/**
 * PlaylistDao
 *
 * Data Access Object for custom user playlists and relational playlist tracks.
 */
@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistById(playlistId: String): Flow<PlaylistEntity?>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithTracks(playlistId: String): Flow<PlaylistWithTracks?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC, addedAt ASC")
    fun getTracksForPlaylist(playlistId: String): Flow<List<PlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackToPlaylist(track: PlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracksToPlaylist(tracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: String)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getTrackCount(playlistId: String): Int

    @Query("UPDATE playlist_tracks SET position = :newPosition WHERE rowId = :rowId")
    suspend fun updateTrackPosition(rowId: Long, newPosition: Int)

    @Query("UPDATE playlists SET trackCount = :count, updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun updatePlaylistMetadata(playlistId: String, count: Int, updatedAt: Long = System.currentTimeMillis())
}

/**
 * PlaybackHistoryDao
 *
 * Data Access Object for recording playback sessions, history timeline,
 * and aggregate listening metrics.
 */
@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history GROUP BY trackId ORDER BY MAX(playedAt) DESC LIMIT :limit")
    fun getDistinctRecentTracks(limit: Int = 30): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT trackId, title, artist, album, albumArtUri, COUNT(*) as playCount, MAX(playedAt) as lastPlayedAt FROM playback_history GROUP BY trackId ORDER BY playCount DESC LIMIT :limit")
    fun getPlaybackStats(limit: Int = 20): Flow<List<TrackPlaybackStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Long)

    @Query("DELETE FROM playback_history WHERE trackId = :trackId")
    suspend fun deleteHistoryForTrack(trackId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM playback_history")
    fun getHistoryCount(): Flow<Int>
}

/**
 * FavoriteDao
 *
 * Preserved for backwards compatibility.
 */
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :trackId)")
    fun isFavorite(trackId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :trackId)")
    suspend fun isFavoriteDirect(trackId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE id = :trackId")
    suspend fun deleteFavorite(trackId: String)
}

/**
 * RecentlyPlayedDao
 *
 * Preserved for backwards compatibility.
 */
@Dao
interface RecentlyPlayedDao {
    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT 30")
    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(entity: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played")
    suspend fun clearHistory()
}
