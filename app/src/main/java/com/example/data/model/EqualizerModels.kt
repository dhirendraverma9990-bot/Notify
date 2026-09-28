package com.example.data.model

/**
 * EqBand
 * Represents an individual frequency band in the Equalizer.
 *
 * @param index zero-based index of the band (0 to 4 for 5-band EQ)
 * @param name readable band label (e.g. "60 Hz", "14 kHz")
 * @param centerFreqHz center frequency in Hertz
 * @param levelDb gain level in decibels (-12 dB to +12 dB)
 * @param minDb minimum gain limit
 * @param maxDb maximum gain limit
 */
data class EqBand(
    val index: Int,
    val name: String,
    val centerFreqHz: Int,
    val levelDb: Float,
    val minDb: Float = -12f,
    val maxDb: Float = 12f
)

/**
 * EqPreset
 * Predefined audio frequency response curve.
 */
data class EqPreset(
    val id: String,
    val name: String,
    val bandLevels: List<Float>
)

/**
 * EqualizerState
 * Comprehensive state of the professional audio equalizer and DSP rack.
 */
data class EqualizerState(
    val isEnabled: Boolean = true,
    val bands: List<EqBand> = DefaultBands,
    val currentPreset: String = "Flat",
    val bassBoostStrength: Int = 350,       // 0 to 1000 (0% - 100%)
    val virtualizerStrength: Int = 250,     // 0 to 1000 (0% - 100%)
    val loudnessGainMilliBels: Int = 150,   // 0 to 1000 mB
    val stereoBalance: Float = 0.0f         // -1.0f (Left) to +1.0f (Right)
)

val DefaultBands: List<EqBand> = listOf(
    EqBand(index = 0, name = "60 Hz", centerFreqHz = 60, levelDb = 0f),
    EqBand(index = 1, name = "230 Hz", centerFreqHz = 230, levelDb = 0f),
    EqBand(index = 2, name = "910 Hz", centerFreqHz = 910, levelDb = 0f),
    EqBand(index = 3, name = "3.6 kHz", centerFreqHz = 3600, levelDb = 0f),
    EqBand(index = 4, name = "14 kHz", centerFreqHz = 14000, levelDb = 0f)
)

val BuiltInPresets: List<EqPreset> = listOf(
    EqPreset("flat", "Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    EqPreset("bass_boost", "Bass Boost", listOf(7.5f, 4.5f, 0.5f, -0.5f, -1.5f)),
    EqPreset("bass_reducer", "Bass Reducer", listOf(-6.0f, -3.5f, 0f, 2.0f, 3.5f)),
    EqPreset("rock", "Rock", listOf(5.0f, 3.0f, -1.0f, 3.5f, 6.0f)),
    EqPreset("pop", "Pop", listOf(-1.5f, 2.0f, 4.5f, 3.0f, -1.0f)),
    EqPreset("edm", "EDM / Club", listOf(6.5f, 4.0f, 1.0f, 3.5f, 5.5f)),
    EqPreset("hip_hop", "Hip-Hop", listOf(7.0f, 5.0f, 0.0f, 2.0f, 3.0f)),
    EqPreset("vocal", "Vocal Booster", listOf(-2.5f, 1.0f, 6.0f, 4.0f, 0.5f)),
    EqPreset("jazz", "Jazz", listOf(3.5f, 2.0f, -1.5f, 2.5f, 4.5f)),
    EqPreset("acoustic", "Acoustic", listOf(4.0f, 3.0f, 2.0f, 3.5f, 4.0f)),
    EqPreset("treble_boost", "Treble Boost", listOf(-3.0f, -1.0f, 1.0f, 5.5f, 8.0f))
)
