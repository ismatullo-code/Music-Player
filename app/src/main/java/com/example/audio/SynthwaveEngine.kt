package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin
import kotlin.math.PI
import java.util.Random

class SynthwaveEngine {
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sampleRate = 22050 // Keep sample rate moderate for efficient synthesis
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    @Volatile
    private var isPlaying = false
    
    @Volatile
    private var volume = 0.5f

    // Current synthesis parameters
    @Volatile
    private var selectedPreset = 0 // 0: CYBERNETIC PULSE, 1: NEON NIGHTRIDER, 2: TOXIC WASTEWATER, 3: VISION SLATE
    
    // Real-time visualization spectrum updated by synth
    private val visualData = FloatArray(16) { 0f }
    private val random = Random()

    fun start() {
        if (isPlaying) return
        isPlaying = true
        
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        ).apply {
            setVolume(volume)
            play()
        }

        synthJob = scope.launch {
            synthesizeLoop()
        }
    }

    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
        visualData.fill(0f)
    }

    fun setVolume(vol: Float) {
        this.volume = vol.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(this.volume)
        } catch (_: Exception) {}
    }

    fun setPreset(presetIndex: Int) {
        this.selectedPreset = presetIndex.coerceIn(0, 3)
    }

    fun getVisualizerData(): FloatArray {
        if (!isPlaying) {
            // Decaying ambient visualization effect when static
            for (i in visualData.indices) {
                visualData[i] = (visualData[i] * 0.85f).coerceAtLeast(0f)
            }
            return visualData.clone()
        }
        return visualData.clone()
    }

    private suspend fun synthesizeLoop() {
        val buffer = ShortArray(bufferSize)
        var phaseAccumulator0 = 0.0
        var phaseAccumulator1 = 0.0
        var phaseAccumulatorSub = 0.0
        var sampleCount = 0L

        // Synthwave music configuration (120 BPM)
        val bpm = 120
        val samplesPerBeat = (sampleRate * 60) / bpm // 11025 samples per beat
        val samplesPerEighth = samplesPerBeat / 2

        // Synthesizer melodies based on current preset
        // Presets: Cybernetic Pulse (D-minor, fast), Neon Nightrider (A-minor, drive), Toxic Wastewater (F-minor, electro), Vision Slate (E-minor, ambient)
        val scalePresets = arrayOf(
            // Cybernetic Pulse (D minor)
            intArrayOf(146, 174, 196, 220, 261, 293, 349, 392), // D3, F3, G3, A3, C4, D4, F4, G4
            // Neon Nightrider (A minor)
            intArrayOf(110, 130, 146, 164, 196, 220, 261, 293), // A2, C3, D3, E3, G3, A3, C4, D4
            // Toxic Wastewater (F minor)
            intArrayOf(87, 103, 116, 130, 155, 174, 207, 233),   // F2, Ab2, Bb2, C3, Eb3, F3, Ab3, Bb3
            // Vision Slate (E minor)
            intArrayOf(164, 196, 220, 246, 293, 329, 392, 440)  // E3, G3, A3, B3, D4, E4, G4, A4
        )

        // Arpeggiator note patterns
        val arps = arrayOf(
            intArrayOf(0, 3, 5, 3, 7, 5, 3, 0), // Lead arp
            intArrayOf(0, 2, 4, 3, 5, 4, 2, 0), // Smooth drive
            intArrayOf(0, 0, 7, 7, 3, 3, 5, 5), // Industrial pulse
            intArrayOf(0, 4, 7, 11, 7, 4, 0, 0) // Atmospheric chord
        )

        while (isPlaying) {
            val scale = scalePresets[selectedPreset]
            val arp = arps[selectedPreset]

            for (i in buffer.indices) {
                if (!isPlaying) break

                val beatStep = ((sampleCount / samplesPerEighth) % 16).toInt() // 16-step sequencer loop
                val subStep = (sampleCount % samplesPerEighth).toFloat() / samplesPerEighth // Fraction of current step

                val noteIndex = arp[beatStep % arp.size]
                val baseFreq = scale[noteIndex % scale.size].toDouble()
                
                // --- OSCILLATOR 1: LEAD SYNTH (Square/Saw Hybrid Wave with Volume Envelope) ---
                val leadVolumeEnv = if (selectedPreset == 3) {
                    // Vision Slate (Ambient, slow attack & slow release)
                    if (subStep < 0.4f) subStep / 0.4f else (1.0f - subStep) / 0.6f
                } else {
                    // Percussive Synth lead Pluck (fast attack, exponential decay)
                    kotlin.math.max(0.0, 1.0 - subStep * 1.8).toFloat()
                }

                // Lead frequency slide for extra future sci-fi glide
                val prevNoteIdx = arp[(if (beatStep == 0) arp.size - 1 else beatStep - 1) % arp.size]
                val prevFreq = scale[prevNoteIdx % scale.size].toDouble()
                val activeFreq = if (subStep < 0.15f) {
                    prevFreq + (baseFreq - prevFreq) * (subStep / 0.15f)
                } else {
                    baseFreq
                }

                // Lead Oscillator
                phaseAccumulator0 += (2.0 * PI * activeFreq) / sampleRate
                if (phaseAccumulator0 > 2.0 * PI) phaseAccumulator0 -= 2.0 * PI
                
                // Cyber hybrid wave: sum of principal sine and square-wave third harmonic
                val leadSine = sin(phaseAccumulator0)
                val leadSquare = if (sin(phaseAccumulator0 * 3.0) > 0.0) 0.25 else -0.25
                val leadWave = leadSine * 0.7 + leadSquare * 0.3
                val leadSignal = leadWave * leadVolumeEnv * 0.22

                // --- OSCILLATOR 2: ARPEGGIATED BASS (Deep Saw Wave) ---
                // Bass Note selection (octave lower than principal scale)
                val bassNoteIndex = beatStep % 4
                val bassFreq = scale[bassNoteIndex] / 2.0
                
                // Bass sub-step envelope
                val bassEnv = if (beatStep % 2 == 1) 0.85f else 0.4f // Syncopated driving bass kick

                phaseAccumulator1 += (2.0 * PI * bassFreq) / sampleRate
                if (phaseAccumulator1 > 2.0 * PI) phaseAccumulator1 -= 2.0 * PI
                // Sawtooth synthesis approximation
                val bassSaw = 1.0 - (phaseAccumulator1 / PI)
                val bassSignal = bassSaw * bassEnv * 0.32

                // --- NOISE OSCILLATOR: DRUM BEATS (Cyber Drum Engine) ---
                var drumSignal = 0.0
                // Kick drum on beats 0, 4, 8, 12
                val isKick = (beatStep % 4 == 0)
                if (isKick) {
                    val kickTime = subStep.toDouble() * 0.25
                    if (kickTime < 1.0) {
                        // Rapid sweep sine oscillator for high-impact kick thud
                        phaseAccumulatorSub += (2.0 * PI * (120.0 * (1.0 - kickTime * 3.0).coerceAtLeast(35.0))) / sampleRate
                        if (phaseAccumulatorSub > 2.0 * PI) phaseAccumulatorSub -= 2.0 * PI
                        drumSignal += sin(phaseAccumulatorSub) * (1.0 - kickTime) * 0.35
                    }
                }

                // Snare/Hat noise bursts on beats 4, 12 or 2, 6, 10, 14
                val isSnare = (beatStep % 8 == 4)
                val isHihat = (beatStep % 2 == 1)
                if (isSnare && subStep < 0.3f) {
                    // White noise burst
                    val noiseAlpha = (1.0 - subStep.toDouble() / 0.3)
                    drumSignal += (random.nextGaussian() * noiseAlpha * 0.12)
                } else if (isHihat && subStep < 0.08f) {
                    // High-passed white noise tick
                    val tickAlpha = (1.0 - subStep.toDouble() / 0.08)
                    drumSignal += (random.nextGaussian() * tickAlpha * 0.07)
                }

                // --- COMBINE MIX AND CONVERT TO PCM 16-BIT ---
                val mixedSignal = (leadSignal + bassSignal + drumSignal) * volume
                val clamped = (mixedSignal * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                buffer[i] = clamped
                sampleCount++

                // Update FFT-like visualizer data on subdivisions
                if (i % (bufferSize / 16) == 0) {
                    val bucketIndex = (i / (bufferSize / 16)) % 16
                    // Extract signal intensity of lead, bass, and drums for visual columns
                    val valFactor = when {
                        bucketIndex in 0..3 -> (kotlin.math.abs(bassSignal) * 2.2).toFloat() // Bass driving lower columns
                        bucketIndex in 4..11 -> (kotlin.math.abs(leadSignal) * 3.5).toFloat() // Lead driving mid columns
                        else -> (kotlin.math.abs(drumSignal) * 2.5).toFloat() // Drum driving upper columns
                    }
                    val currentTarget = (valFactor + 0.05f + random.nextFloat() * 0.15f).coerceIn(0f, 1f)
                    // Interpolate smoothly towards target to reduce visual jittering
                    visualData[bucketIndex] = (visualData[bucketIndex] * 0.4f + currentTarget * 0.6f)
                }
            }

            // Write chunk to audio stream
            audioTrack?.write(buffer, 0, buffer.size)
        }
    }
}
