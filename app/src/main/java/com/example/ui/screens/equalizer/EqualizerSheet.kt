package com.example.ui.screens.equalizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BuiltInPresets
import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.EqualizerState
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NotifyBackground
import com.example.ui.theme.NotifySurface
import com.example.ui.theme.NotifySurfaceElevated
import com.example.ui.theme.NotifySurfaceVariant
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VibrantCoral
import com.example.ui.viewmodel.MusicViewModel

/**
 * EqualizerSheet
 *
 * Professional studio-grade audio equalizer modal bottom sheet.
 * Features:
 * - Master ON/OFF power toggle
 * - Interactive Frequency Response Curve canvas with spline interpolation
 * - Preset selector carousel (Flat, Bass Boost, Rock, Pop, EDM, Vocal, etc.)
 * - 5-Band Vertical Parametric Faders with real-time dB readouts
 * - Hardware DSP studio effects: Bass Boost, 3D Virtualizer, Loudness Gain, Stereo Balance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val eqState by viewModel.equalizerState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NotifyBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        },
        modifier = Modifier.testTag("equalizer_sheet")
    ) {
        EqualizerContent(
            eqState = eqState,
            presets = BuiltInPresets,
            onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
            onBandChange = { index, level -> viewModel.setBandLevel(index, level) },
            onPresetSelect = { viewModel.applyPreset(it) },
            onBassBoostChange = { viewModel.setBassBoost(it) },
            onVirtualizerChange = { viewModel.setVirtualizer(it) },
            onLoudnessGainChange = { viewModel.setLoudnessGain(it) },
            onStereoBalanceChange = { viewModel.setStereoBalance(it) },
            onResetToFlat = { viewModel.resetEqualizer() },
            onClose = onDismiss
        )
    }
}

@Composable
fun EqualizerContent(
    eqState: EqualizerState,
    presets: List<EqPreset>,
    onToggleEnabled: (Boolean) -> Unit,
    onBandChange: (index: Int, level: Float) -> Unit,
    onPresetSelect: (EqPreset) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessGainChange: (Int) -> Unit,
    onStereoBalanceChange: (Float) -> Unit,
    onResetToFlat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Studio Header with Master Switch
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (eqState.isEnabled) SpotifyGreen.copy(alpha = 0.18f)
                                else NotifySurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (eqState.isEnabled) SpotifyGreen else TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Audio Equalizer",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = if (eqState.isEnabled) "DSP Processing Active • ${eqState.currentPreset}" else "Bypassed (DSP Inactive)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = if (eqState.isEnabled) SpotifyGreenBright else TextTertiary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset Button
                    IconButton(
                        onClick = onResetToFlat,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to Flat",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Master Power Switch
                    Switch(
                        checked = eqState.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SpotifyGreen,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = NotifySurfaceVariant
                        ),
                        modifier = Modifier.testTag("eq_master_switch")
                    )

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 2. Frequency Response Curve Visualizer Canvas
        item {
            FrequencyResponseCurve(
                bands = eqState.bands,
                isEnabled = eqState.isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NotifySurfaceElevated)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            )
        }

        // 3. Preset Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sound Profiles",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = TextSecondary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        val selected = eqState.currentPreset.equals(preset.name, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onPresetSelect(preset) },
                            enabled = eqState.isEnabled,
                            label = {
                                Text(
                                    text = preset.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpotifyGreen,
                                selectedLabelColor = NotifyBackground,
                                containerColor = NotifySurfaceVariant,
                                labelColor = TextSecondary,
                                disabledContainerColor = NotifySurfaceVariant.copy(alpha = 0.5f),
                                disabledLabelColor = TextTertiary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = eqState.isEnabled,
                                selected = selected,
                                borderColor = if (selected) SpotifyGreen else Color.White.copy(alpha = 0.08f),
                                selectedBorderColor = SpotifyGreen
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("preset_chip_${preset.id}")
                        )
                    }
                }
            }
        }

        // 4. 5-Band Parametric Equalizer Faders
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NotifySurface)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Parametric Bands",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = TextPrimary
                        )

                        Text(
                            text = "Range: ±12 dB",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextTertiary
                        )
                    }

                    // 5 Equalizer Slider Channels
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        eqState.bands.forEach { band ->
                            BandFaderColumn(
                                band = band,
                                isEnabled = eqState.isEnabled,
                                onLevelChange = { newLevel ->
                                    onBandChange(band.index, newLevel)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 5. Studio DSP Effects (Bass Boost, Virtualizer, Loudness Gain, Stereo Balance)
        item {
            Text(
                text = "DSP Hardware Enhancements",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = TextPrimary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Bass Boost Card
                DspEffectCard(
                    title = "Bass Boost",
                    subtitle = "Deep sub-harmonic low-end resonance",
                    icon = Icons.Default.VolumeUp,
                    accentColor = SpotifyGreen,
                    valueText = "${eqState.bassBoostStrength / 10}%",
                    sliderValue = eqState.bassBoostStrength / 1000f,
                    isEnabled = eqState.isEnabled,
                    onValueChange = { onBassBoostChange((it * 1000).toInt()) },
                    tag = "slider_bass_boost"
                )

                // 3D Virtualizer / Spatial Audio Card
                DspEffectCard(
                    title = "3D Virtualizer",
                    subtitle = "Expands stereo acoustic soundstage",
                    icon = Icons.Default.SpatialAudio,
                    accentColor = ElectricCyan,
                    valueText = "${eqState.virtualizerStrength / 10}%",
                    sliderValue = eqState.virtualizerStrength / 1000f,
                    isEnabled = eqState.isEnabled,
                    onValueChange = { onVirtualizerChange((it * 1000).toInt()) },
                    tag = "slider_virtualizer"
                )

                // Loudness Enhancer Card
                DspEffectCard(
                    title = "Loudness Preamp",
                    subtitle = "Acoustic dynamic range boost",
                    icon = Icons.Default.Tune,
                    accentColor = VibrantCoral,
                    valueText = "+${(eqState.loudnessGainMilliBels / 100f).let { "%.1f".format(it) }} dB",
                    sliderValue = eqState.loudnessGainMilliBels / 1000f,
                    isEnabled = eqState.isEnabled,
                    onValueChange = { onLoudnessGainChange((it * 1000).toInt()) },
                    tag = "slider_loudness"
                )

                // Stereo Balance Card
                StereoBalanceCard(
                    balance = eqState.stereoBalance,
                    isEnabled = eqState.isEnabled,
                    onBalanceChange = onStereoBalanceChange
                )
            }
        }
    }
}

/**
 * FrequencyResponseCurve
 *
 * Smooth cubic spline representation of frequency gain across the audible spectrum.
 */
@Composable
fun FrequencyResponseCurve(
    bands: List<EqBand>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Background Zero dB line
        val zeroY = height / 2f
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, zeroY),
            end = Offset(width, zeroY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // +6 dB and -6 dB guideline
        val quarterH = height / 4f
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, quarterH),
            end = Offset(width, quarterH),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, height - quarterH),
            end = Offset(width, height - quarterH),
            strokeWidth = 1f
        )

        if (bands.isEmpty()) return@Canvas

        // Calculate (x, y) coordinates for each band
        val stepX = width / (bands.size + 1)
        val points = mutableListOf<Offset>()

        // Left boundary anchor point
        points.add(Offset(0f, zeroY - (if (isEnabled) (bands.first().levelDb / 12f) * (height * 0.4f) else 0f)))

        bands.forEachIndexed { i, band ->
            val x = stepX * (i + 1)
            val levelRatio = if (isEnabled) (band.levelDb / 12f).coerceIn(-1f, 1f) else 0f
            val y = zeroY - (levelRatio * (height * 0.4f))
            points.add(Offset(x, y))
        }

        // Right boundary anchor point
        points.add(Offset(width, zeroY - (if (isEnabled) (bands.last().levelDb / 12f) * (height * 0.4f) else 0f)))

        // Build smooth Bézier spline
        val curvePath = Path()
        val fillPath = Path()

        curvePath.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, zeroY)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val midX = (p0.x + p1.x) / 2f
            curvePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
        }

        fillPath.lineTo(width, zeroY)
        fillPath.close()

        val glowColor = if (isEnabled) SpotifyGreen else TextTertiary

        // Draw gradient under-fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    glowColor.copy(alpha = if (isEnabled) 0.35f else 0.08f),
                    ElectricCyan.copy(alpha = if (isEnabled) 0.10f else 0.02f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )

        // Draw curve outline
        drawPath(
            path = curvePath,
            color = if (isEnabled) SpotifyGreenBright else TextTertiary,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw node circles at band frequencies
        for (i in 1..bands.size) {
            val pt = points[i]
            drawCircle(
                color = if (isEnabled) Color.White else TextTertiary,
                radius = 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = if (isEnabled) SpotifyGreen else Color.DarkGray,
                radius = 2.dp.toPx(),
                center = pt
            )
        }
    }
}

/**
 * BandFaderColumn
 *
 * Parametric band vertical channel with frequency name, role descriptor,
 * real-time dB readout, and touch fader.
 */
@Composable
fun BandFaderColumn(
    band: EqBand,
    isEnabled: Boolean,
    onLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val roleName = when (band.index) {
        0 -> "SUB"
        1 -> "BASS"
        2 -> "MID"
        3 -> "PRES"
        4 -> "AIR"
        else -> "FREQ"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        // dB Readout Badge
        val formattedDb = if (band.levelDb > 0) "+%.1f".format(band.levelDb) else "%.1f".format(band.levelDb)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isEnabled && band.levelDb != 0f) SpotifyGreen.copy(alpha = 0.15f)
                    else NotifySurfaceVariant
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${formattedDb}dB",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled && band.levelDb != 0f) SpotifyGreenBright else TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Notch Line & Slider Channel
        Box(
            modifier = Modifier
                .height(130.dp)
                .width(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Zero center detent notch
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.25f))
            )

            // Rotated slider for vertical behavior or step buttons
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxHeight()
            ) {
                // + button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NotifySurfaceElevated)
                        .clickable(enabled = isEnabled && band.levelDb < band.maxDb) {
                            onLevelChange((band.levelDb + 1.0f).coerceAtMost(band.maxDb))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = if (isEnabled) TextPrimary else TextTertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Visual level indicator bar
                val ratio = ((band.levelDb - band.minDb) / (band.maxDb - band.minDb)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(NotifySurfaceVariant),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(ratio)
                            .background(
                                if (isEnabled) Brush.verticalGradient(listOf(ElectricCyan, SpotifyGreen))
                                else Brush.verticalGradient(listOf(Color.Gray, Color.DarkGray))
                            )
                    )
                }

                // - button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NotifySurfaceElevated)
                        .clickable(enabled = isEnabled && band.levelDb > band.minDb) {
                            onLevelChange((band.levelDb - 1.0f).coerceAtLeast(band.minDb))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", color = if (isEnabled) TextPrimary else TextTertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Frequency
        Text(
            text = band.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = TextPrimary
        )

        // Band Role
        Text(
            text = roleName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            ),
            color = TextTertiary
        )
    }
}

/**
 * DspEffectCard
 *
 * Modern hardware DSP slider card with gradient glow and fine control.
 */
@Composable
fun DspEffectCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    valueText: String,
    sliderValue: Float,
    isEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NotifySurface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) accentColor.copy(alpha = 0.15f) else NotifySurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isEnabled) accentColor else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextTertiary
                        )
                    }
                }

                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = if (isEnabled) accentColor else TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = sliderValue,
                onValueChange = onValueChange,
                enabled = isEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = if (isEnabled) accentColor else TextTertiary,
                    activeTrackColor = if (isEnabled) accentColor else TextTertiary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(tag)
            )
        }
    }
}

/**
 * StereoBalanceCard
 *
 * Left / Right audio balance slider with center zero detent.
 */
@Composable
fun StereoBalanceCard(
    balance: Float,
    isEnabled: Boolean,
    onBalanceChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val balanceLabel = when {
        balance < -0.05f -> "Left ${(balance * -100).toInt()}%"
        balance > 0.05f -> "Right ${(balance * 100).toInt()}%"
        else -> "Center (Balanced)"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NotifySurface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Stereo Balance",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = TextPrimary
                )

                Text(
                    text = balanceLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = if (isEnabled) SpotifyGreenBright else TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Balance slider: map -1.0..1.0 to 0.0..1.0
            Slider(
                value = (balance + 1f) / 2f,
                onValueChange = { norm ->
                    val newBalance = (norm * 2f) - 1f
                    // Center detent snapping
                    if (kotlin.math.abs(newBalance) < 0.06f) {
                        onBalanceChange(0f)
                    } else {
                        onBalanceChange(newBalance)
                    }
                },
                enabled = isEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_stereo_balance")
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("L", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("CENTER", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text("R", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
