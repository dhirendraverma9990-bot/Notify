package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Track
import com.example.service.RepeatMode
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NotifyBackground
import com.example.ui.theme.NotifySurfaceElevated
import com.example.ui.theme.NotifySurfaceVariant
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun NowPlayingSheet(
    track: Track?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (track == null) return

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val currentSliderVal = if (isDraggingSlider) {
        sliderPosition
    } else {
        if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
    }

    Surface(
        color = NotifyBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("now_playing_sheet")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NotifySurfaceVariant.copy(alpha = 0.8f),
                            NotifyBackground,
                            NotifyBackground
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                // Top Action Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("collapse_player_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (track.isLocal) "PLAYING FROM LOCAL STORAGE" else "PLAYING FROM NOTIFY STREAM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontSize = 10.sp
                            ),
                            color = TextTertiary
                        )
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenEqualizer,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("equalizer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Equalizer",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenQueue,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("queue_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Up Next Queue",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High-Res Artwork
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .aspectRatio(1f)
                        .shadow(24.dp, RoundedCornerShape(20.dp), spotColor = SpotifyGreen)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NotifySurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.albumArtUri.isNullOrBlank()) {
                        AsyncImage(
                            model = track.albumArtUri,
                            contentDescription = track.album,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Track Title and Artist & Favorite Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_player_fav_btn")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) SpotifyGreenBright else TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Audio Waveform Visualizer
                AudioWaveformVisualizer(
                    isPlaying = isPlaying,
                    height = 36.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Seek Bar Slider
                Slider(
                    value = currentSliderVal,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        onSeek((sliderPosition * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = TextPrimary,
                        activeTrackColor = SpotifyGreen,
                        inactiveTrackColor = NotifySurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_seek_slider")
                )

                // Elapsed and Total Time Labels
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayedPos = if (isDraggingSlider) {
                        (sliderPosition * durationMs).toLong()
                    } else {
                        currentPositionMs
                    }
                    Text(
                        text = formatMillis(displayedPos),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = formatMillis(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Playback Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Shuffle
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("shuffle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) SpotifyGreenBright else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = onSkipPrev,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Play/Pause Big Button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen)
                            .testTag("play_pause_main_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    color = NotifyBackground,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = NotifyBackground,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    // Next
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Repeat
                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("repeat_btn")
                    ) {
                        val (icon, tint) = when (repeatMode) {
                            RepeatMode.OFF -> Pair(Icons.Default.Repeat, TextSecondary)
                            RepeatMode.ALL -> Pair(Icons.Default.Repeat, SpotifyGreenBright)
                            RepeatMode.ONE -> Pair(Icons.Default.RepeatOne, SpotifyGreenBright)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Metadata Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = if (track.isLocal) "HQ Audio • ${track.format}" else "Lossless Streaming • 320kbps",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = ElectricCyan,
                        modifier = Modifier
                            .background(
                                ElectricCyan.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
