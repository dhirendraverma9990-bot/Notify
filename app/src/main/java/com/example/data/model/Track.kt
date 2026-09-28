package com.example.data.model

import java.util.Locale

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val isLocal: Boolean = false,
    val filePath: String? = null,
    val folderName: String? = null,
    val format: String = "MP3",
    val genre: String? = "Pop"
) {
    fun formatDuration(): String {
        if (durationMs <= 0) return "0:00"
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val albumArtUri: String? = null,
    val year: String? = null
)

data class Artist(
    val id: String,
    val name: String,
    val trackCount: Int,
    val imageUri: String? = null
)

data class Folder(
    val path: String,
    val name: String,
    val trackCount: Int
)

data class Playlist(
    val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val isCurated: Boolean = false,
    val tracks: List<Track> = emptyList()
)
