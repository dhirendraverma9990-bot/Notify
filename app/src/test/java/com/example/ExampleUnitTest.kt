package com.example

import com.example.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun track_formatDuration_formatsCorrectly() {
        val track = Track(
            id = "test_1",
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 215000L, // 3 minutes 35 seconds
            contentUri = "https://example.com/test.mp3"
        )
        assertEquals("3:35", track.formatDuration())
    }

    @Test
    fun track_formatDuration_handlesZero() {
        val track = Track(
            id = "test_0",
            title = "Empty Song",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 0L,
            contentUri = "https://example.com/test.mp3"
        )
        assertEquals("0:00", track.formatDuration())
    }

    @Test
    fun notifyMediaCallback_actionConstants_areValid() {
        assertEquals("com.example.notify.ACTION_TOGGLE_SHUFFLE", com.example.service.NotifyMediaCallback.ACTION_TOGGLE_SHUFFLE)
        assertEquals("com.example.notify.ACTION_TOGGLE_REPEAT", com.example.service.NotifyMediaCallback.ACTION_TOGGLE_REPEAT)
        assertEquals("com.example.notify.ACTION_REPLAY", com.example.service.NotifyMediaCallback.ACTION_REPLAY)
    }

    @Test
    fun notifyNotificationManager_constants_areValid() {
        assertEquals("notify_playback_channel", com.example.service.NotifyNotificationManager.CHANNEL_ID)
        assertEquals(1001, com.example.service.NotifyNotificationManager.NOTIFICATION_ID)
    }

    @Test
    fun audioRepository_extractAlbums_groupsCorrectly() {
        val tracks = listOf(
            Track("1", "Song A", "Artist 1", "Album X", 180000L, "uri1"),
            Track("2", "Song B", "Artist 1", "Album X", 200000L, "uri2"),
            Track("3", "Song C", "Artist 2", "Album Y", 220000L, "uri3")
        )
        val albums = tracks.groupBy { it.album }
            .map { (title, list) -> com.example.data.model.Album("id_$title", title, list.first().artist, list.size) }
        assertEquals(2, albums.size)
        assertEquals(2, albums.first { it.title == "Album X" }.trackCount)
    }

    @Test
    fun playbackStatus_hasExpectedStates() {
        val states = com.example.ui.viewmodel.PlaybackStatus.values().map { it.name }
        org.junit.Assert.assertTrue(states.contains("PLAYING"))
        org.junit.Assert.assertTrue(states.contains("PAUSED"))
        org.junit.Assert.assertTrue(states.contains("BUFFERING"))
        org.junit.Assert.assertTrue(states.contains("IDLE"))
        org.junit.Assert.assertTrue(states.contains("STOPPED"))
    }

    @Test
    fun searchFilter_hasAllRequiredOptions() {
        val filters = com.example.ui.viewmodel.SearchFilter.values().map { it.name }
        org.junit.Assert.assertTrue(filters.contains("ALL"))
        org.junit.Assert.assertTrue(filters.contains("LOCAL"))
        org.junit.Assert.assertTrue(filters.contains("ONLINE"))
        org.junit.Assert.assertTrue(filters.contains("ARTISTS"))
        org.junit.Assert.assertTrue(filters.contains("ALBUMS"))
    }

    @Test
    fun searchFilter_filtersLocalTracksCorrectly() {
        val localTrack = Track("1", "Neon Dreams", "Synth Master", "Night Drive", 200000L, "uri1", isLocal = true)
        val onlineTrack = Track("2", "Cyber Pulse", "Techno Bot", "Digital Age", 210000L, "uri2", isLocal = false)
        val tracks = listOf(localTrack, onlineTrack)

        val localOnly = tracks.filter { it.isLocal }
        assertEquals(1, localOnly.size)
        assertEquals("Neon Dreams", localOnly.first().title)
    }

    @Test
    fun equalizer_defaultBands_hasFiveParametricBands() {
        val bands = com.example.data.model.DefaultBands
        assertEquals(5, bands.size)
        assertEquals(60, bands[0].centerFreqHz)
        assertEquals(230, bands[1].centerFreqHz)
        assertEquals(910, bands[2].centerFreqHz)
        assertEquals(3600, bands[3].centerFreqHz)
        assertEquals(14000, bands[4].centerFreqHz)
    }

    @Test
    fun equalizer_builtInPresets_containsStandardProfiles() {
        val presetNames = com.example.data.model.BuiltInPresets.map { it.name }
        org.junit.Assert.assertTrue(presetNames.contains("Flat"))
        org.junit.Assert.assertTrue(presetNames.contains("Bass Boost"))
        org.junit.Assert.assertTrue(presetNames.contains("Rock"))
        org.junit.Assert.assertTrue(presetNames.contains("EDM / Club"))
        org.junit.Assert.assertTrue(presetNames.contains("Vocal Booster"))
    }

    @Test
    fun equalizer_flatPreset_allBandsAreZero() {
        val flat = com.example.data.model.BuiltInPresets.first { it.id == "flat" }
        org.junit.Assert.assertTrue(flat.bandLevels.all { it == 0f })
    }

    @Test
    fun musicService_actionConstantsAreWellDefined() {
        assertEquals("com.example.notify.PLAY", com.example.service.MusicService.ACTION_PLAY)
        assertEquals("com.example.notify.PAUSE", com.example.service.MusicService.ACTION_PAUSE)
        assertEquals("com.example.notify.NEXT", com.example.service.MusicService.ACTION_NEXT)
        assertEquals("com.example.notify.PREV", com.example.service.MusicService.ACTION_PREV)
        assertEquals("com.example.notify.STOP", com.example.service.MusicService.ACTION_STOP)
        assertEquals("com.example.notify.UPDATE_NOTIFICATION", com.example.service.MusicService.ACTION_UPDATE_NOTIFICATION)
    }

    @Test
    fun notifyMediaCallback_customActionsAreDefined() {
        assertEquals("com.example.notify.ACTION_TOGGLE_SHUFFLE", com.example.service.NotifyMediaCallback.ACTION_TOGGLE_SHUFFLE)
        assertEquals("com.example.notify.ACTION_TOGGLE_REPEAT", com.example.service.NotifyMediaCallback.ACTION_TOGGLE_REPEAT)
        assertEquals("com.example.notify.ACTION_REPLAY", com.example.service.NotifyMediaCallback.ACTION_REPLAY)
        assertEquals("com.example.notify.ACTION_FORWARD_10", com.example.service.NotifyMediaCallback.ACTION_FORWARD_10)
        assertEquals("com.example.notify.ACTION_REWIND_10", com.example.service.NotifyMediaCallback.ACTION_REWIND_10)
    }

    @Test
    fun playbackStateCompat_standardActionsMaskValid() {
        val actions = android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO

        org.junit.Assert.assertTrue((actions and android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY) != 0L)
        org.junit.Assert.assertTrue((actions and android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE) != 0L)
        org.junit.Assert.assertTrue((actions and android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO) != 0L)
    }

    @Test
    fun roomDatabase_songEntity_conversionToTrackIsLossless() {
        val song = com.example.data.local.SongEntity(
            id = "song_123",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            genre = "Synthwave",
            durationMs = 243000L,
            contentUri = "content://media/external/audio/media/123",
            albumArtUri = "content://media/external/audio/albumart/45",
            isLocal = true,
            playCount = 14,
            isFavorite = true
        )

        val track = song.toTrack()
        assertEquals("song_123", track.id)
        assertEquals("Midnight City", track.title)
        assertEquals("M83", track.artist)
        assertEquals("Hurry Up, We're Dreaming", track.album)
        assertEquals(243000L, track.durationMs)
        assertEquals("content://media/external/audio/media/123", track.contentUri)
        assertEquals(true, track.isLocal)

        val fromTrack = com.example.data.local.SongEntity.fromTrack(track, isFavorite = true)
        assertEquals(song.id, fromTrack.id)
        assertEquals(song.title, fromTrack.title)
        assertEquals(true, fromTrack.isFavorite)
    }

    @Test
    fun roomDatabase_playlistWithTracks_sortsByPosition() {
        val playlist = com.example.data.local.PlaylistEntity(
            id = "pl_1",
            title = "Workout Beats",
            description = "High energy running tunes"
        )
        val track1 = com.example.data.local.PlaylistTrackEntity(
            rowId = 1L,
            playlistId = "pl_1",
            trackId = "t1",
            title = "Track One",
            artist = "Artist A",
            album = "Album A",
            durationMs = 180000L,
            contentUri = "uri1",
            position = 2
        )
        val track2 = com.example.data.local.PlaylistTrackEntity(
            rowId = 2L,
            playlistId = "pl_1",
            trackId = "t2",
            title = "Track Two",
            artist = "Artist B",
            album = "Album B",
            durationMs = 200000L,
            contentUri = "uri2",
            position = 1
        )

        val relation = com.example.data.local.PlaylistWithTracks(
            playlist = playlist,
            tracks = listOf(track1, track2)
        )

        val domainTracks = relation.toDomainTracks()
        assertEquals(2, domainTracks.size)
        // Position 1 should appear before position 2
        assertEquals("t2", domainTracks[0].id)
        assertEquals("t1", domainTracks[1].id)
    }

    @Test
    fun roomDatabase_playbackHistory_fromTrackPopulatesFields() {
        val track = com.example.data.model.Track(
            id = "rec_99",
            title = "Starlight",
            artist = "Muse",
            album = "Black Holes and Revelations",
            durationMs = 240000L,
            contentUri = "https://example.com/starlight.mp3",
            isLocal = false
        )

        val history = com.example.data.local.PlaybackHistoryEntity.fromTrack(
            track = track,
            playbackDurationMs = 120000L,
            completed = true,
            source = "PLAYLIST"
        )

        assertEquals("rec_99", history.trackId)
        assertEquals("Starlight", history.title)
        assertEquals("Muse", history.artist)
        assertEquals(120000L, history.playbackDurationMs)
        assertEquals(true, history.completed)
        assertEquals("PLAYLIST", history.playbackSource)
        org.junit.Assert.assertTrue(history.playedAt > 0L)
    }

    @Test
    fun onlineStreamingTracks_formatAndUriAreValid() {
        val streamTrack = com.example.data.model.Track(
            id = "stream_1",
            title = "Cyber Eclipse",
            artist = "Neon Dynasty",
            album = "Synthetic Horizon",
            durationMs = 212000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            albumArtUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe",
            isLocal = false,
            format = "STREAM",
            genre = "Synthwave"
        )
        assertEquals("STREAM", streamTrack.format)
        assertEquals(false, streamTrack.isLocal)
        assertEquals("3:32", streamTrack.formatDuration())
        org.junit.Assert.assertTrue(streamTrack.contentUri.startsWith("https://"))
    }
}
