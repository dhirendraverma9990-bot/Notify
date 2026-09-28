package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.example.data.model.BuiltInPresets
import com.example.data.model.DefaultBands
import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.EqualizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * EqualizerManager
 *
 * Hardware-level audio DSP engine that controls Android's [Equalizer],
 * [BassBoost], [Virtualizer], and [LoudnessEnhancer] audio effects.
 *
 * Persists user customizations to [SharedPreferences] and exposes a reactive
 * [StateFlow] for real-time Jetpack Compose UI synchronization.
 */
class EqualizerManager(private val context: Context) {

    companion object {
        private const val TAG = "EqualizerManager"
        private const val PREFS_NAME = "notify_equalizer_prefs"
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_PRESET_NAME = "eq_preset_name"
        private const val KEY_BASS_BOOST = "eq_bass_boost"
        private const val KEY_VIRTUALIZER = "eq_virtualizer"
        private const val KEY_LOUDNESS = "eq_loudness"
        private const val KEY_BALANCE = "eq_balance"
        private const val KEY_BAND_PREFIX = "eq_band_"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var currentAudioSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var onBalanceChanged: ((leftVolume: Float, rightVolume: Float) -> Unit)? = null

    private val _equalizerState = MutableStateFlow(loadInitialState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    init {
        notifyBalance()
    }

    /**
     * Attaches audio effects to a new MediaPlayer [audioSessionId].
     */
    @Synchronized
    fun attachSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (currentAudioSessionId == audioSessionId && equalizer != null) {
            applyAllEffects()
            return
        }

        releaseEffects()
        currentAudioSessionId = audioSessionId

        try {
            // Priority 0, session audioSessionId
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer initialization error on session $audioSessionId: ${e.message}")
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_equalizerState.value.bassBoostStrength.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost initialization error: ${e.message}")
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_equalizerState.value.virtualizerStrength.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer initialization error: ${e.message}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = _equalizerState.value.isEnabled
                    setTargetGain(_equalizerState.value.loudnessGainMilliBels)
                }
            } catch (e: Exception) {
                Log.w(TAG, "LoudnessEnhancer initialization error: ${e.message}")
            }
        }

        applyAllEffects()
        notifyBalance()
    }

    /**
     * Toggles equalizer power switch on/off.
     */
    fun setEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            loudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle enabled state: ${e.message}")
        }

        if (enabled) {
            applyAllEffects()
        }
    }

    /**
     * Adjusts gain of an individual frequency band in decibels (-12 dB to +12 dB).
     */
    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        val current = _equalizerState.value
        val updatedBands = current.bands.map { band ->
            if (band.index == bandIndex) {
                band.copy(levelDb = levelDb.coerceIn(band.minDb, band.maxDb))
            } else {
                band
            }
        }

        _equalizerState.value = current.copy(
            bands = updatedBands,
            currentPreset = "Custom"
        )

        applyBandToHardware(bandIndex, levelDb)
        saveBandsToPrefs(updatedBands, "Custom")
    }

    /**
     * Applies a built-in or custom [EqPreset] frequency curve.
     */
    fun applyPreset(preset: EqPreset) {
        val current = _equalizerState.value
        val updatedBands = current.bands.mapIndexed { idx, band ->
            val level = preset.bandLevels.getOrNull(idx) ?: 0f
            band.copy(levelDb = level.coerceIn(band.minDb, band.maxDb))
        }

        _equalizerState.value = current.copy(
            bands = updatedBands,
            currentPreset = preset.name
        )

        updatedBands.forEach { applyBandToHardware(it.index, it.levelDb) }
        saveBandsToPrefs(updatedBands, preset.name)
    }

    /**
     * Resets all bands back to flat (0 dB).
     */
    fun resetToFlat() {
        val flatPreset = BuiltInPresets.firstOrNull { it.id == "flat" }
            ?: EqPreset("flat", "Flat", listOf(0f, 0f, 0f, 0f, 0f))
        applyPreset(flatPreset)
        setBassBoost(300)
        setVirtualizer(200)
        setLoudnessGain(0)
        setStereoBalance(0f)
    }

    /**
     * Adjusts Bass Boost strength (0 to 1000).
     */
    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.value = _equalizerState.value.copy(bassBoostStrength = clamped)
        prefs.edit().putInt(KEY_BASS_BOOST, clamped).apply()

        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting bass boost: ${e.message}")
        }
    }

    /**
     * Adjusts 3D Virtualizer / Spatial sound strength (0 to 1000).
     */
    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.value = _equalizerState.value.copy(virtualizerStrength = clamped)
        prefs.edit().putInt(KEY_VIRTUALIZER, clamped).apply()

        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting virtualizer: ${e.message}")
        }
    }

    /**
     * Adjusts studio Loudness Enhancer gain in millibels (0 to 1000 mB).
     */
    fun setLoudnessGain(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1000)
        _equalizerState.value = _equalizerState.value.copy(loudnessGainMilliBels = clamped)
        prefs.edit().putInt(KEY_LOUDNESS, clamped).apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                loudnessEnhancer?.setTargetGain(clamped)
            } catch (e: Exception) {
                Log.w(TAG, "Error setting loudness gain: ${e.message}")
            }
        }
    }

    /**
     * Adjusts Stereo Balance from -1.0f (Full Left) to +1.0f (Full Right).
     */
    fun setStereoBalance(balance: Float) {
        val clamped = balance.coerceIn(-1.0f, 1.0f)
        _equalizerState.value = _equalizerState.value.copy(stereoBalance = clamped)
        prefs.edit().putFloat(KEY_BALANCE, clamped).apply()
        notifyBalance()
    }

    /**
     * Computes left and right volume channels based on stereo balance and applies it.
     */
    private fun notifyBalance() {
        val balance = _equalizerState.value.stereoBalance
        val left = if (balance <= 0f) 1.0f else (1.0f - balance).coerceIn(0.0f, 1.0f)
        val right = if (balance >= 0f) 1.0f else (1.0f + balance).coerceIn(0.0f, 1.0f)
        onBalanceChanged?.invoke(left, right)
    }

    private fun applyBandToHardware(bandIndex: Int, levelDb: Float) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            if (bandIndex < numBands) {
                // Android Equalizer uses millibels (1 dB = 100 mB)
                val mB = (levelDb * 100f).roundToInt().toShort()
                val range = eq.bandLevelRange
                val safeMb = mB.coerceIn(range[0], range[1])
                eq.setBandLevel(bandIndex.toShort(), safeMb)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying band level to hardware: ${e.message}")
        }
    }

    private fun applyAllEffects() {
        val state = _equalizerState.value
        state.bands.forEach { applyBandToHardware(it.index, it.levelDb) }
        setBassBoost(state.bassBoostStrength)
        setVirtualizer(state.virtualizerStrength)
        setLoudnessGain(state.loudnessGainMilliBels)
    }

    private fun saveBandsToPrefs(bands: List<EqBand>, presetName: String) {
        val editor = prefs.edit()
        editor.putString(KEY_PRESET_NAME, presetName)
        bands.forEach { band ->
            editor.putFloat("$KEY_BAND_PREFIX${band.index}", band.levelDb)
        }
        editor.apply()
    }

    private fun loadInitialState(): EqualizerState {
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        val presetName = prefs.getString(KEY_PRESET_NAME, "Flat") ?: "Flat"
        val bass = prefs.getInt(KEY_BASS_BOOST, 350)
        val virtualizer = prefs.getInt(KEY_VIRTUALIZER, 250)
        val loudness = prefs.getInt(KEY_LOUDNESS, 150)
        val balance = prefs.getFloat(KEY_BALANCE, 0.0f)

        val bands = DefaultBands.map { defaultBand ->
            val savedLevel = prefs.getFloat("$KEY_BAND_PREFIX${defaultBand.index}", defaultBand.levelDb)
            defaultBand.copy(levelDb = savedLevel)
        }

        return EqualizerState(
            isEnabled = enabled,
            bands = bands,
            currentPreset = presetName,
            bassBoostStrength = bass,
            virtualizerStrength = virtualizer,
            loudnessGainMilliBels = loudness,
            stereoBalance = balance
        )
    }

    /**
     * Releases hardware audio effect handles.
     */
    @Synchronized
    fun release() {
        releaseEffects()
        currentAudioSessionId = 0
    }

    private fun releaseEffects() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing equalizer: ${e.message}")
        }
        equalizer = null

        try {
            bassBoost?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing bassBoost: ${e.message}")
        }
        bassBoost = null

        try {
            virtualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing virtualizer: ${e.message}")
        }
        virtualizer = null

        try {
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing loudnessEnhancer: ${e.message}")
        }
        loudnessEnhancer = null
    }
}
