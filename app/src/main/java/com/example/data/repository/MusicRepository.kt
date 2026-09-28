package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteTrackEntity
import com.example.data.local.PlaybackHistoryDao
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.local.PlaylistWithTracks
import com.example.data.local.RecentlyPlayedDao
import com.example.data.local.RecentlyPlayedEntity
import com.example.data.local.SongDao
import com.example.data.local.SongEntity
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Folder
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    val songDao: SongDao? = null,
    val playbackHistoryDao: PlaybackHistoryDao? = null,
    val audioRepository: AudioRepository = AudioRepository(context)
) {
    // Flow of favorite tracks from Room
    val favoriteTracks: Flow<List<Track>> = favoriteDao.getAllFavorites().map { list ->
        list.map { it.toTrack() }
    }

    // Flow of custom user playlists from Room
    val userPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    // Flow of recently played tracks from Room
    val recentlyPlayed: Flow<List<Track>> = recentlyPlayedDao.getRecentlyPlayed().map { list ->
        list.map { it.toTrack() }
    }

    // Flow of all songs persisted in Room
    val allPersistedSongs: Flow<List<Track>> = songDao?.getAllSongs()?.map { list ->
        list.map { it.toTrack() }
    } ?: flowOf(emptyList())

    // Flow of granular user playback history sessions from Room
    val playbackHistory: Flow<List<PlaybackHistoryEntity>> =
        playbackHistoryDao?.getRecentHistory(50) ?: flowOf(emptyList())

    fun isFavorite(trackId: String): Flow<Boolean> = favoriteDao.isFavorite(trackId)

    suspend fun toggleFavorite(track: Track): Boolean = withContext(Dispatchers.IO) {
        val exists = favoriteDao.isFavoriteDirect(track.id)
        if (exists) {
            favoriteDao.deleteFavorite(track.id)
            songDao?.setFavorite(track.id, false)
            false
        } else {
            favoriteDao.insertFavorite(FavoriteTrackEntity.fromTrack(track))
            songDao?.setFavorite(track.id, true)
            true
        }
    }

    suspend fun addRecentlyPlayed(track: Track) = withContext(Dispatchers.IO) {
        recentlyPlayedDao.insertRecent(RecentlyPlayedEntity.fromTrack(track))
        playbackHistoryDao?.insertHistory(PlaybackHistoryEntity.fromTrack(track))
        songDao?.incrementPlayCount(track.id)
    }

    suspend fun recordPlaybackSession(
        track: Track,
        durationListenedMs: Long,
        completed: Boolean,
        source: String = "APP"
    ) = withContext(Dispatchers.IO) {
        val historyEntry = PlaybackHistoryEntity.fromTrack(
            track = track,
            playbackDurationMs = durationListenedMs,
            completed = completed,
            source = source
        )
        playbackHistoryDao?.insertHistory(historyEntry)
        songDao?.incrementPlayCount(track.id)
    }

    suspend fun saveSongMetadata(track: Track) = withContext(Dispatchers.IO) {
        songDao?.insertSong(SongEntity.fromTrack(track))
    }

    suspend fun saveMultipleSongsMetadata(tracks: List<Track>) = withContext(Dispatchers.IO) {
        val entities = tracks.map { SongEntity.fromTrack(it) }
        songDao?.insertSongs(entities)
    }

    fun getPlaylistWithTracks(playlistId: String): Flow<PlaylistWithTracks?> =
        playlistDao.getPlaylistWithTracks(playlistId)

    suspend fun clearAllPlaybackHistory() = withContext(Dispatchers.IO) {
        playbackHistoryDao?.clearHistory()
        recentlyPlayedDao.clearHistory()
    }

    suspend fun createPlaylist(name: String, description: String = ""): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val playlist = PlaylistEntity(
            id = id,
            title = name.ifBlank { "My Playlist" },
            description = description,
            coverUrl = null
        )
        playlistDao.insertPlaylist(playlist)
        id
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
    }

    fun getPlaylistTracks(playlistId: String): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { list ->
            list.map { it.toTrack() }
        }
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: Track) = withContext(Dispatchers.IO) {
        val entity = PlaylistTrackEntity(
            playlistId = playlistId,
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            contentUri = track.contentUri,
            albumArtUri = track.albumArtUri,
            isLocal = track.isLocal
        )
        playlistDao.insertTrackToPlaylist(entity)
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    // MediaStore Scan for real device audio delegated to AudioRepository
    suspend fun scanLocalAudio(): List<Track> = audioRepository.getLocalAudioTracks(forceRefresh = true)

    // Generator for Sample Demo Audio in case emulator storage is completely empty
    suspend fun generateDemoLocalAudioFiles(): Result<Int> = audioRepository.generateDemoAudioFiles()

    // Helper to group tracks into Albums
    fun extractAlbums(tracks: List<Track>): List<Album> {
        return tracks.groupBy { it.album }
            .map { (albumTitle, trackList) ->
                val first = trackList.first()
                Album(
                    id = "album_${albumTitle.hashCode()}",
                    title = albumTitle,
                    artist = first.artist,
                    trackCount = trackList.size,
                    albumArtUri = first.albumArtUri
                )
            }
            .sortedBy { it.title }
    }

    // Helper to group tracks into Artists
    fun extractArtists(tracks: List<Track>): List<Artist> {
        return tracks.groupBy { it.artist }
            .map { (artistName, trackList) ->
                Artist(
                    id = "artist_${artistName.hashCode()}",
                    name = artistName,
                    trackCount = trackList.size,
                    imageUri = trackList.firstOrNull()?.albumArtUri
                )
            }
            .sortedBy { it.name }
    }

    // Helper to group tracks into Folders
    fun extractFolders(tracks: List<Track>): List<Folder> {
        return tracks.groupBy { it.folderName ?: "Music" }
            .map { (folderName, trackList) ->
                Folder(
                    path = trackList.firstOrNull()?.filePath ?: folderName,
                    name = folderName,
                    trackCount = trackList.size
                )
            }
            .sortedBy { it.name }
    }

    // Online / Simulated Streaming Library with rich curated tracks
    fun getCuratedStreamingTracks(): List<Track> {
        return listOf(
            Track(
                id = "stream_1",
                title = "Cyber Eclipse",
                artist = "Neon Dynasty",
                album = "Synthetic Horizon",
                durationMs = 212000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Synthwave"
            ),
            Track(
                id = "stream_2",
                title = "Midnight Odyssey",
                artist = "Aura Borealis",
                album = "Echoes in Void",
                durationMs = 195000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Electronic"
            ),
            Track(
                id = "stream_3",
                title = "Velvet Rain",
                artist = "Kira Thorne",
                album = "Afterglow Chronicles",
                durationMs = 245000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Lo-Fi Chill"
            ),
            Track(
                id = "stream_4",
                title = "Hyperdrive Groove",
                artist = "Pulse Vector",
                album = "Tokyo Velocity",
                durationMs = 280000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Future Bass"
            ),
            Track(
                id = "stream_5",
                title = "Solitary Echoes",
                artist = "Lunar Collective",
                album = "Starlight Lullaby",
                durationMs = 220000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Ambient"
            ),
            Track(
                id = "stream_6",
                title = "Electric Nirvana",
                artist = "Subatomic",
                album = "Resonance Protocol",
                durationMs = 188000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "EDM"
            ),
            Track(
                id = "stream_7",
                title = "Golden Hour Beats",
                artist = "Mira Waves",
                album = "Sunset Blvd",
                durationMs = 205000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Chillhop"
            ),
            Track(
                id = "stream_8",
                title = "Neon Horizon",
                artist = "RetroWave Knights",
                album = "Outrun 1984",
                durationMs = 230000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Synthwave"
            ),
            Track(
                id = "stream_9",
                title = "Astral Mind",
                artist = "Stellar Drift",
                album = "Cosmic Echoes",
                durationMs = 215000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Ambient"
            ),
            Track(
                id = "stream_10",
                title = "Sunset Memories",
                artist = "Chill Factory",
                album = "Tokyo Dusk",
                durationMs = 198000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Chillhop"
            ),
            Track(
                id = "stream_11",
                title = "Club Inferno",
                artist = "DJ Vortex",
                album = "Rave Protocol",
                durationMs = 225000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "EDM"
            ),
            Track(
                id = "stream_12",
                title = "Lo-Fi Coffee",
                artist = "Study Beats",
                album = "Rainy Sunday",
                durationMs = 185000,
                contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500&q=80",
                isLocal = false,
                format = "STREAM",
                genre = "Lo-Fi Chill"
            )
        )
    }

    // Carousels for Home Screen
    fun getCuratedPlaylists(): List<Playlist> {
        val tracks = getCuratedStreamingTracks()
        return listOf(
            Playlist(
                id = "curated_made_for_you",
                title = "Made For You",
                description = "Personalized mix based on your taste & audio frequency",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
                trackCount = tracks.size,
                isCurated = true,
                tracks = tracks
            ),
            Playlist(
                id = "curated_top_charts",
                title = "Top Charts Global",
                description = "The hottest 50 tracks buzzing around the world",
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
                trackCount = tracks.size,
                isCurated = true,
                tracks = tracks.reversed()
            ),
            Playlist(
                id = "curated_new_releases",
                title = "New Releases Radar",
                description = "Brand new drops from premier electronic & indie artists",
                coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&q=80",
                trackCount = tracks.size,
                isCurated = true,
                tracks = tracks.shuffled()
            ),
            Playlist(
                id = "curated_deep_focus",
                title = "Deep Flow State",
                description = "High focus binaural & ambient beats for deep coding",
                coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
                trackCount = 4,
                isCurated = true,
                tracks = tracks.take(4)
            )
        )
    }
}
