package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.data.model.Track

/**
 * SongEntity
 *
 * Local Room table for persisting complete song metadata, audio technical details,
 * and playback engagement stats (play count, favorite status, last played).
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["dateAdded"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "Unknown",
    val durationMs: Long = 0L,
    val contentUri: String,
    val albumArtUri: String? = null,
    val isLocal: Boolean = false,
    val year: Int = 0,
    val trackNumber: Int = 0,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val fileSize: Long = 0L,
    val mimeType: String = "audio/mpeg",
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        isLocal = isLocal
    )

    companion object {
        fun fromTrack(track: Track, isFavorite: Boolean = false): SongEntity = SongEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            contentUri = track.contentUri,
            albumArtUri = track.albumArtUri,
            isLocal = track.isLocal,
            isFavorite = isFavorite
        )
    }
}

/**
 * PlaylistEntity
 *
 * Local Room table storing playlist header information, cover artwork,
 * aggregate track counts, and timestamps.
 */
@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["title"]),
        Index(value = ["createdAt"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * PlaylistTrackEntity
 *
 * Join table mapping songs to playlists with ordering position.
 * Configured with ForeignKey cascade deletion so removing a playlist cleanly
 * clears its track associations.
 */
@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["trackId"]),
        Index(value = ["playlistId", "position"])
    ]
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val playlistId: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val isLocal: Boolean = false,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        id = trackId,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        isLocal = isLocal
    )

    companion object {
        fun fromTrack(playlistId: String, track: Track, position: Int = 0): PlaylistTrackEntity =
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                contentUri = track.contentUri,
                albumArtUri = track.albumArtUri,
                isLocal = track.isLocal,
                position = position
            )
    }
}

/**
 * PlaylistWithTracks
 *
 * Relation class providing 1-to-many lookup of a playlist together with all its tracks.
 */
data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val tracks: List<PlaylistTrackEntity>
) {
    fun toDomainTracks(): List<Track> = tracks.sortedBy { it.position }.map { it.toTrack() }
}

/**
 * PlaybackHistoryEntity
 *
 * Local Room table recording discrete user playback history sessions and listening stats.
 */
@Entity(
    tableName = "playback_history",
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["playedAt"])
    ]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val isLocal: Boolean = false,
    val playedAt: Long = System.currentTimeMillis(),
    val playbackDurationMs: Long = 0L,
    val completed: Boolean = false,
    val playbackSource: String = "APP"
) {
    fun toTrack(): Track = Track(
        id = trackId,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        isLocal = isLocal
    )

    companion object {
        fun fromTrack(
            track: Track,
            playbackDurationMs: Long = 0L,
            completed: Boolean = false,
            source: String = "APP"
        ): PlaybackHistoryEntity = PlaybackHistoryEntity(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            contentUri = track.contentUri,
            albumArtUri = track.albumArtUri,
            isLocal = track.isLocal,
            playedAt = System.currentTimeMillis(),
            playbackDurationMs = playbackDurationMs,
            completed = completed,
            playbackSource = source
        )
    }
}

/**
 * TrackPlaybackStats
 *
 * Projection model for aggregate playback statistics.
 */
data class TrackPlaybackStats(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUri: String?,
    val playCount: Int,
    val lastPlayedAt: Long
)

/**
 * FavoriteTrackEntity
 *
 * Preserved for backwards compatibility with existing favorites queries.
 */
@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String?,
    val isLocal: Boolean,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        isLocal = isLocal
    )

    companion object {
        fun fromTrack(track: Track): FavoriteTrackEntity = FavoriteTrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            contentUri = track.contentUri,
            albumArtUri = track.albumArtUri,
            isLocal = track.isLocal
        )
    }
}

/**
 * RecentlyPlayedEntity
 *
 * Preserved for backwards compatibility with existing recently played queries.
 */
@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String?,
    val isLocal: Boolean,
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        isLocal = isLocal
    )

    companion object {
        fun fromTrack(track: Track): RecentlyPlayedEntity = RecentlyPlayedEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            contentUri = track.contentUri,
            albumArtUri = track.albumArtUri,
            isLocal = track.isLocal,
            playedAt = System.currentTimeMillis()
        )
    }
}
