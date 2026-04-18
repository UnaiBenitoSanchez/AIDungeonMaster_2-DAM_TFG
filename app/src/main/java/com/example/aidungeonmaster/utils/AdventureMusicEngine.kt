package com.example.aidungeonmaster.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

object AdventureMusicEngine {

    private const val SR = 44100
    private const val TAG = "ADV_MUSIC"

    private const val STEPS_PER_BAR = 16
    private const val DELAY_BUFFER_SECONDS = 0.75
    private const val CROSSFADE_STEPS = 8
    private const val DEFAULT_STOP_DELAY_MS = 900L

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val DELAY_BUF = (SR * DELAY_BUFFER_SECONDS).toInt().coerceAtLeast(1)

    enum class MusicScreen(val volume: Float) {
        GAMEPLAY(1.00f),
        INVENTORY(0.42f),
        JOURNAL(0.34f),
        BESTIARY(0.34f),
        MAP(0.55f),
        GALLERY(0.46f),
        BACKGROUND(0.18f),
        MUTED(0.00f)
    }

    private enum class LeadWave {
        SQUARE, TRIANGLE, SAW, SINE
    }

    private data class AdventureThemeProfile(
        val name: String,
        val bpm: Double,
        val bass: List<String>,
        val melody: List<String>,
        val pad: List<String>,
        val kick: IntArray,
        val snare: IntArray,
        val hihat: IntArray,
        val leadWave: LeadWave,
        val bassGain: Float,
        val melodyGain: Float,
        val padGain: Float,
        val kickGain: Float,
        val snareGain: Float,
        val hihatGain: Float,
        val bassLpf: Float,
        val melodyLpf: Float,
        val delaySeconds: Double,
        val delayFeedback: Float,
        val stereoSpread: Float
    ) {
        companion object {
            fun fromTheme(theme: String): AdventureThemeProfile {
                return when (theme.trim().lowercase()) {
                    "terror gótico", "terror gotico" -> gothic()
                    "cyberpunk" -> cyberpunk()
                    "misterio" -> mystery()
                    else -> epicFantasy()
                }
            }

            private fun epicFantasy() = AdventureThemeProfile(
                name = "Fantasía Épica",
                bpm = 140.0,
                bass = listOf(
                    "d2","d2","d2","d2",
                    "f2","f2","f2","f2",
                    "c2","c2","c2","c2",
                    "d2","d2","d2","d2"
                ),
                melody = listOf(
                    "d4","~","f4","g4",
                    "a4","g4","f4","~",
                    "d4","~","c4","d4",
                    "e4","d4","c4","~"
                ),
                pad = listOf(
                    "a2","a2","a2","a2",
                    "c3","c3","c3","c3",
                    "g2","g2","g2","g2",
                    "a2","a2","a2","a2"
                ),
                kick = intArrayOf(1,0,0,0, 0,0,0,0, 1,0,0,0, 0,0,0,0),
                snare = intArrayOf(0,0,0,0, 1,0,0,0, 0,0,0,0, 1,0,0,0),
                hihat = intArrayOf(1,0,1,0, 1,0,1,0, 1,0,1,0, 1,0,1,0),
                leadWave = LeadWave.SQUARE,
                bassGain = 0.70f,
                melodyGain = 0.46f,
                padGain = 0.25f,
                kickGain = 0.95f,
                snareGain = 0.80f,
                hihatGain = 0.28f,
                bassLpf = 420f,
                melodyLpf = 1300f,
                delaySeconds = 0.10,
                delayFeedback = 0.35f,
                stereoSpread = 0.12f
            )

            private fun gothic() = AdventureThemeProfile(
                name = "Terror Gótico",
                bpm = 88.0,
                bass = listOf(
                    "d2","~","d2","~",
                    "bb1","~","bb1","~",
                    "f2","~","f2","~",
                    "c2","~","c2","~"
                ),
                melody = listOf(
                    "a3","~","c4","~",
                    "d4","~","f4","~",
                    "e4","~","d4","~",
                    "c4","~","a3","~"
                ),
                pad = listOf(
                    "d3","d3","d3","d3",
                    "bb2","bb2","bb2","bb2",
                    "f3","f3","f3","f3",
                    "c3","c3","c3","c3"
                ),
                kick = intArrayOf(1,0,0,0, 0,0,0,0, 1,0,0,0, 0,0,0,0),
                snare = intArrayOf(0,0,0,0, 0,0,1,0, 0,0,0,0, 0,0,1,0),
                hihat = intArrayOf(1,0,0,0, 1,0,0,0, 1,0,0,0, 1,0,0,0),
                leadWave = LeadWave.TRIANGLE,
                bassGain = 0.62f,
                melodyGain = 0.38f,
                padGain = 0.32f,
                kickGain = 0.70f,
                snareGain = 0.50f,
                hihatGain = 0.12f,
                bassLpf = 250f,
                melodyLpf = 700f,
                delaySeconds = 0.22,
                delayFeedback = 0.48f,
                stereoSpread = 0.20f
            )

            private fun cyberpunk() = AdventureThemeProfile(
                name = "Cyberpunk",
                bpm = 128.0,
                bass = listOf(
                    "d2","d2","f2","e2",
                    "d2","d2","a1","a1",
                    "c2","c2","e2","f2",
                    "d2","d2","a1","c2"
                ),
                melody = listOf(
                    "d4","a4","c5","a4",
                    "f4","a4","e4","a4",
                    "g4","c5","a4","f4",
                    "e4","g4","d4","a4"
                ),
                pad = listOf(
                    "d3","d3","d3","d3",
                    "f3","f3","f3","f3",
                    "c3","c3","c3","c3",
                    "a2","a2","a2","a2"
                ),
                kick = intArrayOf(1,0,0,0, 1,0,0,0, 1,0,0,0, 1,0,0,0),
                snare = intArrayOf(0,0,0,0, 1,0,0,0, 0,0,0,0, 1,0,0,0),
                hihat = intArrayOf(1,1,0,1, 1,1,0,1, 1,1,0,1, 1,1,0,1),
                leadWave = LeadWave.SAW,
                bassGain = 0.78f,
                melodyGain = 0.42f,
                padGain = 0.18f,
                kickGain = 1.00f,
                snareGain = 0.72f,
                hihatGain = 0.22f,
                bassLpf = 520f,
                melodyLpf = 2200f,
                delaySeconds = 0.07,
                delayFeedback = 0.26f,
                stereoSpread = 0.08f
            )

            private fun mystery() = AdventureThemeProfile(
                name = "Misterio",
                bpm = 96.0,
                bass = listOf(
                    "e2","~","e2","~",
                    "g2","~","g2","~",
                    "d2","~","d2","~",
                    "c2","~","c2","~"
                ),
                melody = listOf(
                    "b3","~","d4","~",
                    "e4","~","g4","~",
                    "f4","~","e4","~",
                    "d4","~","b3","~"
                ),
                pad = listOf(
                    "e3","e3","e3","e3",
                    "g3","g3","g3","g3",
                    "d3","d3","d3","d3",
                    "c3","c3","c3","c3"
                ),
                kick = intArrayOf(1,0,0,0, 0,0,0,0, 1,0,0,0, 0,0,0,0),
                snare = intArrayOf(0,0,0,0, 0,0,1,0, 0,0,0,0, 0,0,1,0),
                hihat = intArrayOf(1,0,0,0, 0,0,1,0, 1,0,0,0, 0,0,1,0),
                leadWave = LeadWave.SINE,
                bassGain = 0.52f,
                melodyGain = 0.28f,
                padGain = 0.34f,
                kickGain = 0.55f,
                snareGain = 0.42f,
                hihatGain = 0.10f,
                bassLpf = 300f,
                melodyLpf = 900f,
                delaySeconds = 0.18,
                delayFeedback = 0.42f,
                stereoSpread = 0.18f
            )
        }
    }

    private data class RenderState(
        var profile: AdventureThemeProfile,
        val delayBufL: FloatArray = FloatArray(DELAY_BUF),
        val delayBufR: FloatArray = FloatArray(DELAY_BUF),
        var delayIdx: Int = 0
    )

    private var audioTrack: AudioTrack? = null
    private var renderJob: Job? = null
    private var stopJob: Job? = null

    @Volatile
    private var isPlaying = false

    @Volatile
    private var requestedTheme: AdventureThemeProfile =
        AdventureThemeProfile.fromTheme("Fantasía Épica")

    @Volatile
    private var requestedScreen: MusicScreen = MusicScreen.GAMEPLAY

    private var activeState = RenderState(AdventureThemeProfile.fromTheme("Fantasía Épica"))
    private var pendingState: RenderState? = null
    private var crossfadeProgress = 1f
    private var currentGain = 0f
    private var stepCounter = 0

    fun enterGameplay(theme: String) {
        requestedTheme = AdventureThemeProfile.fromTheme(theme)
        requestedScreen = MusicScreen.GAMEPLAY
        stopJob?.cancel()
        ensurePlaying()
    }

    fun setScreen(screen: MusicScreen) {
        requestedScreen = screen
        stopJob?.cancel()
    }

    fun releaseScreen(delayMs: Long = DEFAULT_STOP_DELAY_MS) {
        stopJob?.cancel()
        stopJob = engineScope.launch {
            requestedScreen = MusicScreen.MUTED
            delay(delayMs)
            if (requestedScreen == MusicScreen.MUTED) {
                stopNow()
            }
        }
    }

    fun stopNow() {
        isPlaying = false

        val localRenderJob = renderJob
        renderJob = null
        if (localRenderJob != null) {
            engineScope.launch {
                runCatching { localRenderJob.cancelAndJoin() }
            }
        }

        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        }

        audioTrack = null
        pendingState = null
        crossfadeProgress = 1f
        currentGain = 0f
        Log.d(TAG, "■ Música de aventura detenida")
    }

    private fun ensurePlaying() {
        if (isPlaying) return

        isPlaying = true
        activeState = RenderState(requestedTheme)
        pendingState = null
        crossfadeProgress = 1f
        currentGain = 0f
        stepCounter = 0

        Log.d(TAG, "▶ Música de aventura iniciada: ${requestedTheme.name}")
        renderJob = engineScope.launch(Dispatchers.Default) { runLoop() }
    }

    private suspend fun runLoop() {
        val initialBuffer = AudioTrack.getMinBufferSize(
            SR,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(8192)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SR)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(initialBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        while (isPlaying && engineScope.isActive) {
            val targetTheme = requestedTheme
            if (targetTheme.name != activeState.profile.name &&
                pendingState?.profile?.name != targetTheme.name
            ) {
                pendingState = RenderState(targetTheme)
                crossfadeProgress = 0f
            }

            val stepSamples = stepSamplesFor(activeState.profile)
            val activeBuffer = synthesizeStep(
                renderState = activeState,
                stepIndex = stepCounter,
                stepSamples = stepSamples
            )

            val mixedBuffer = if (pendingState != null) {
                val incoming = pendingState!!
                val nextBuffer = synthesizeStep(
                    renderState = incoming,
                    stepIndex = stepCounter,
                    stepSamples = stepSamples
                )

                val amount = crossfadeProgress.coerceIn(0f, 1f)
                val out = FloatArray(activeBuffer.size)
                for (i in out.indices) {
                    out[i] = activeBuffer[i] * (1f - amount) + nextBuffer[i] * amount
                }

                crossfadeProgress += (1f / CROSSFADE_STEPS)
                if (crossfadeProgress >= 1f) {
                    activeState = incoming
                    pendingState = null
                    crossfadeProgress = 1f
                }

                out
            } else {
                activeBuffer
            }

            val targetGain = requestedScreen.volume
            val outShorts = applyMasterGainAndConvert(
                input = mixedBuffer,
                startGain = currentGain,
                endGain = targetGain
            )
            currentGain = targetGain

            audioTrack?.write(outShorts, 0, outShorts.size)
            stepCounter++
        }
    }

    private fun stepSamplesFor(profile: AdventureThemeProfile): Int {
        val stepSec = 60.0 / profile.bpm / 4.0
        return (SR * stepSec).toInt().coerceAtLeast(1)
    }

    private fun synthesizeStep(
        renderState: RenderState,
        stepIndex: Int,
        stepSamples: Int
    ): FloatArray {
        val profile = renderState.profile
        val s = stepIndex % STEPS_PER_BAR

        val bassFreq = hz(profile.bass[s])
        val melodyFreq = hz(profile.melody[s])
        val padFreq = hz(profile.pad[s])
        val hasKick = profile.kick[s] == 1
        val hasSnare = profile.snare[s] == 1
        val hasHihat = profile.hihat[s] == 1

        val dur = stepSamples.toDouble() / SR
        val buf = FloatArray(stepSamples * 2)

        val dt = 1.0 / SR

        val bassCut = profile.bassLpf.toDouble().coerceAtLeast(40.0)
        val rcBass = 1.0 / (2 * PI * bassCut)
        val alphaBass = (dt / (rcBass + dt)).toFloat()
        var bassLpfState = 0f

        val melodyCut = profile.melodyLpf.toDouble().coerceAtLeast(60.0)
        val rcMel = 1.0 / (2 * PI * melodyCut)
        val alphaMel = (dt / (rcMel + dt)).toFloat()
        var melodyLpfState = 0f

        val delayTimeSamples = (profile.delaySeconds * SR)
            .toInt()
            .coerceIn(1, DELAY_BUF - 1)

        for (i in 0 until stepSamples) {
            val t = i.toDouble() / SR

            var bassAmp = 0f
            if (bassFreq > 0f) {
                val p = (t * bassFreq) % 1.0
                val saw = (2.0 * p - 1.0).toFloat()
                bassLpfState = alphaBass * saw + (1f - alphaBass) * bassLpfState
                val env = adsr(t, dur, 0.004, 0.05, 0.82, 0.10).toFloat()
                bassAmp = bassLpfState * env * profile.bassGain
            }

            var melodyAmp = 0f
            if (melodyFreq > 0f) {
                val osc = leadOsc(profile.leadWave, melodyFreq.toDouble(), t)
                melodyLpfState = alphaMel * osc + (1f - alphaMel) * melodyLpfState
                val env = adsr(t, dur, 0.006, 0.05, 0.72, 0.12).toFloat()
                melodyAmp = melodyLpfState * env * profile.melodyGain
            }

            val delayedIndex = ((renderState.delayIdx - delayTimeSamples) + DELAY_BUF) % DELAY_BUF
            val delayedL = renderState.delayBufL[delayedIndex]
            val delayedR = renderState.delayBufR[delayedIndex]

            val wetL = (melodyAmp + delayedL * profile.delayFeedback)
            val wetR = (melodyAmp + delayedR * profile.delayFeedback)

            renderState.delayBufL[renderState.delayIdx] =
                (wetL * 0.62f).coerceIn(-1f, 1f)
            renderState.delayBufR[renderState.delayIdx] =
                (wetR * 0.62f).coerceIn(-1f, 1f)
            renderState.delayIdx = (renderState.delayIdx + 1) % DELAY_BUF

            val padAmp = if (padFreq > 0f) {
                val direct = sin(2 * PI * padFreq * t).toFloat()
                val reflect1 = (sin(2 * PI * padFreq * (t - 0.032)) * 0.35).toFloat()
                val reflect2 = (sin(2 * PI * padFreq * (t - 0.061)) * 0.18).toFloat()
                val env = adsr(t, dur, 0.02, 0.10, 0.82, 0.22).toFloat()
                (direct + reflect1 + reflect2) * profile.padGain * env
            } else {
                0f
            }

            val drumAmp =
                (if (hasKick) kick808(t) * profile.kickGain else 0f) +
                        (if (hasSnare) snare808(t) * profile.snareGain else 0f)

            val hihatAmp = if (hasHihat) hihat808(t) * profile.hihatGain else 0f

            val spread = profile.stereoSpread
            val melodyLeft = wetL * (1f - spread)
            val melodyRight = wetR * (1f + spread)

            val mixL = bassAmp + melodyLeft + padAmp + drumAmp + hihatAmp
            val mixR = bassAmp + melodyRight + padAmp + drumAmp + hihatAmp

            buf[i * 2] = softClip(mixL)
            buf[i * 2 + 1] = softClip(mixR)
        }

        return buf
    }

    private fun applyMasterGainAndConvert(
        input: FloatArray,
        startGain: Float,
        endGain: Float
    ): ShortArray {
        val out = ShortArray(input.size)
        val frames = (input.size / 2).coerceAtLeast(1)

        for (frame in 0 until frames) {
            val t = if (frames == 1) 1f else frame.toFloat() / (frames - 1).toFloat()
            val gain = lerp(startGain, endGain, t)

            val left = (input[frame * 2] * gain).coerceIn(-1f, 1f)
            val right = (input[frame * 2 + 1] * gain).coerceIn(-1f, 1f)

            out[frame * 2] = (left * Short.MAX_VALUE).toInt().toShort()
            out[frame * 2 + 1] = (right * Short.MAX_VALUE).toInt().toShort()
        }

        return out
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t.coerceIn(0f, 1f)
    }

    private fun softClip(x: Float): Float {
        return tanh(x.toDouble()).toFloat().coerceIn(-1f, 1f)
    }

    private fun leadOsc(wave: LeadWave, freq: Double, t: Double): Float {
        val p = (t * freq) % 1.0
        val value = when (wave) {
            LeadWave.SQUARE -> if (p < 0.5) 1.0 else -1.0
            LeadWave.TRIANGLE -> 1.0 - 4.0 * abs(p - 0.5)
            LeadWave.SAW -> 2.0 * p - 1.0
            LeadWave.SINE -> sin(2.0 * PI * p)
        }
        return value.toFloat()
    }

    private fun hz(note: String): Float {
        val n = note.trim().lowercase()
        if (n.isBlank() || n == "~") return 0f

        val match = Regex("^([a-g])(#{1}|b{1})?(-?\\d+)$").matchEntire(n) ?: return 0f
        val letter = match.groupValues[1]
        val accidental = match.groupValues[2]
        val octave = match.groupValues[3].toInt()

        val semitone = when (letter) {
            "c" -> 0
            "d" -> 2
            "e" -> 4
            "f" -> 5
            "g" -> 7
            "a" -> 9
            "b" -> 11
            else -> 0
        } + when (accidental) {
            "#" -> 1
            "b" -> -1
            else -> 0
        }

        val midi = (octave + 1) * 12 + semitone
        return (440.0 * 2.0.pow((midi - 69) / 12.0)).toFloat()
    }

    private fun adsr(
        t: Double,
        dur: Double,
        a: Double,
        d: Double,
        s: Double,
        r: Double
    ): Double {
        val rStart = (dur - r).coerceAtLeast(a + d)
        return when {
            t < a -> t / a
            t < a + d -> 1.0 - (1.0 - s) * ((t - a) / d)
            t < rStart -> s
            t < dur -> s * (1.0 - (t - rStart) / r)
            else -> 0.0
        }.coerceIn(0.0, 1.0)
    }

    private fun kick808(t: Double): Float {
        if (t > 0.35) return 0f
        val freq = 65.0 * exp(-12.0 * t) + 40.0
        return (sin(2 * PI * freq * t) * exp(-6.0 * t) * 0.9).toFloat()
    }

    private fun snare808(t: Double): Float {
        if (t > 0.22) return 0f
        val noise = ((Math.random() * 2.0 - 1.0) * exp(-22.0 * t)).toFloat()
        val tone = (sin(2 * PI * 160.0 * t) * exp(-20.0 * t) * 0.35).toFloat()
        return ((noise * 0.6f) + tone) * 0.8f
    }

    private fun hihat808(t: Double): Float {
        if (t > 0.05) return 0f
        return ((Math.random() * 2.0 - 1.0) * exp(-60.0 * t) * 0.3).toFloat()
    }
}