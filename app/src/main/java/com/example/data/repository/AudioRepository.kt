package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Folder
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AudioRepository
 *
 * Abstracts local filesystem and [MediaStore] audio scanning logic.
 * Provides a clean, modern, and coroutine-safe interface for ViewModels
 * to fetch local audio tracks, albums, artists, and folder hierarchies.
 */
class AudioRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val _cachedTracks = MutableStateFlow<List<Track>>(emptyList())
    val cachedTracksFlow: StateFlow<List<Track>> = _cachedTracks.asStateFlow()

    /**
     * Scans device storage using [MediaStore] content resolver for audio files.
     * Caches and emits the results to [_cachedTracks].
     */
    suspend fun getLocalAudioTracks(forceRefresh: Boolean = false): List<Track> = withContext(ioDispatcher) {
        if (!forceRefresh && _cachedTracks.value.isNotEmpty()) {
            return@withContext _cachedTracks.value
        }

        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE
        )

        // Select only music files with positive duration
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val mimeCol = it.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol)?.trim().takeUnless { t -> t.isNullOrBlank() } ?: "Unknown Title"
                    val artistRaw = it.getString(artistCol)?.trim()
                    val artist = if (artistRaw.isNullOrBlank() || artistRaw == "<unknown>") "Unknown Artist" else artistRaw
                    val albumRaw = it.getString(albumCol)?.trim()
                    val album = if (albumRaw.isNullOrBlank() || albumRaw == "<unknown>") "Unknown Album" else albumRaw
                    val duration = it.getLong(durationCol).coerceAtLeast(0L)
                    val path = it.getString(dataCol) ?: ""
                    val albumId = it.getLong(albumIdCol)
                    val mimeType = if (mimeCol >= 0) it.getString(mimeCol) ?: "" else ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val file = File(path)
                    val folderName = file.parentFile?.name ?: "Music"
                    val ext = file.extension.uppercase().ifBlank {
                        when {
                            mimeType.contains("flac") -> "FLAC"
                            mimeType.contains("wav") -> "WAV"
                            mimeType.contains("ogg") -> "OGG"
                            mimeType.contains("m4a") -> "M4A"
                            mimeType.contains("aac") -> "AAC"
                            else -> "MP3"
                        }
                    }

                    tracks.add(
                        Track(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            isLocal = true,
                            filePath = path,
                            folderName = folderName,
                            format = ext,
                            genre = "Local Audio"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _cachedTracks.value = tracks
        tracks
    }

    /**
     * Extracts and groups tracks into unique [Album] instances.
     */
    suspend fun getAlbums(): List<Album> = withContext(ioDispatcher) {
        extractAlbums(getLocalAudioTracks())
    }

    /**
     * Extracts and groups tracks into unique [Artist] instances.
     */
    suspend fun getArtists(): List<Artist> = withContext(ioDispatcher) {
        extractArtists(getLocalAudioTracks())
    }

    /**
     * Extracts and groups tracks by parent filesystem [Folder].
     */
    suspend fun getFolders(): List<Folder> = withContext(ioDispatcher) {
        extractFolders(getLocalAudioTracks())
    }

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
            .sortedBy { it.title.lowercase() }
    }

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
            .sortedBy { it.name.lowercase() }
    }

    fun extractFolders(tracks: List<Track>): List<Folder> {
        return tracks.groupBy { it.folderName ?: "Music" }
            .map { (folderName, trackList) ->
                Folder(
                    path = trackList.firstOrNull()?.filePath ?: folderName,
                    name = folderName,
                    trackCount = trackList.size
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Returns all tracks belonging to a given album.
     */
    suspend fun getTracksByAlbum(albumTitle: String): List<Track> = withContext(ioDispatcher) {
        getLocalAudioTracks().filter { it.album.equals(albumTitle, ignoreCase = true) }
    }

    /**
     * Returns all tracks belonging to a given artist.
     */
    suspend fun getTracksByArtist(artistName: String): List<Track> = withContext(ioDispatcher) {
        getLocalAudioTracks().filter { it.artist.equals(artistName, ignoreCase = true) }
    }

    /**
     * Returns all tracks in a given folder.
     */
    suspend fun getTracksByFolder(folderName: String): List<Track> = withContext(ioDispatcher) {
        getLocalAudioTracks().filter { (it.folderName ?: "Music").equals(folderName, ignoreCase = true) }
    }

    /**
     * Generates sample PCM 16-bit WAV demo audio files directly to the device's
     * Music directory in case local emulator storage contains no audio files.
     */
    suspend fun generateDemoAudioFiles(): Result<Int> = withContext(ioDispatcher) {
        try {
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: File(context.filesDir, "Music")
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            var createdCount = 0
            val demoTracks = listOf(
                Pair("Notify Chill Pulse.wav", 440.0),
                Pair("Electro Bass Beat.wav", 220.0),
                Pair("Neon Skyline Rhythm.wav", 554.37)
            )

            for ((filename, freq) in demoTracks) {
                val file = File(musicDir, filename)
                if (!file.exists()) {
                    writeSampleWavFile(file, freq, durationSeconds = 12)
                    createdCount++
                    // Notify Android MediaScanner so file is indexed into MediaStore
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file.absolutePath),
                        arrayOf("audio/wav"),
                        null
                    )
                }
            }

            // Force refresh cached tracks
            getLocalAudioTracks(forceRefresh = true)
            Result.success(createdCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun writeSampleWavFile(file: File, frequency: Double, durationSeconds: Int) {
        val sampleRate = 44100
        val numSamples = sampleRate * durationSeconds
        val numChannels = 2
        val bytesPerSample = 2
        val subChunk2Size = numSamples * numChannels * bytesPerSample
        val chunkSize = 36 + subChunk2Size

        FileOutputStream(file).use { out ->
            val buffer = ByteBuffer.allocate(44 + subChunk2Size).order(ByteOrder.LITTLE_ENDIAN)

            // RIFF chunk
            buffer.put("RIFF".toByteArray())
            buffer.putInt(chunkSize)
            buffer.put("WAVE".toByteArray())

            // fmt chunk
            buffer.put("fmt ".toByteArray())
            buffer.putInt(16) // Subchunk1Size for PCM
            buffer.putShort(1) // AudioFormat 1 = PCM
            buffer.putShort(numChannels.toShort())
            buffer.putInt(sampleRate)
            buffer.putInt(sampleRate * numChannels * bytesPerSample) // ByteRate
            buffer.putShort((numChannels * bytesPerSample).toShort()) // BlockAlign
            buffer.putShort((bytesPerSample * 8).toShort()) // BitsPerSample

            // data chunk
            buffer.put("data".toByteArray())
            buffer.putInt(subChunk2Size)

            // Harmonic synth tone with envelope
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val sine1 = Math.sin(2.0 * Math.PI * frequency * t)
                val sine2 = 0.5 * Math.sin(2.0 * Math.PI * (frequency * 1.5) * t)
                val sine3 = 0.25 * Math.sin(2.0 * Math.PI * (frequency * 0.5) * t)
                val envelope = Math.min(1.0, Math.sin(Math.PI * (i.toDouble() / numSamples)))
                val sampleValue = ((sine1 + sine2 + sine3) * envelope * 0.4 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                val sampleShort = sampleValue.toShort()
                buffer.putShort(sampleShort)
                buffer.putShort(sampleShort)
            }

            out.write(buffer.array())
            out.flush()
        }
    }
}
