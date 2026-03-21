package com.example.aidungeonmaster.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlin.math.*

/**
 * ══════════════════════════════════════════════════════════════════
 *  ADVENTURE MUSIC ENGINE — "Épica de la Aventura"
 *  Traducción del Strudel de la pantalla de aventura.
 *
 *  setcpm(140/4) → 140 BPM, semicorcheas
 *
 *  Capa 1 — Bajo sawtooth,  <d2 d2 f2 c2>*4,  lpf=400 fijo
 *  Capa 2 — Melodía square, 4 barras de 4 notas, lpf=1200, delay=0.1
 *  Capa 3 — Pad sine,       <a2 a2 c3 g2>*2 .slow(2)
 *  Capa 4 — Percusión TR808: bd*2, ~sd~sd, hh*8
 * ══════════════════════════════════════════════════════════════════
 */
object AdventureMusicEngine {

    private const val SR  = 44100
    private const val TAG = "ADV_MUSIC"

    private const val BPM            = 140.0
    private const val STEPS_PER_BAR  = 16
    private val STEP_SEC    = 60.0 / BPM / 4.0
    private val STEP_SAMPLES get()   = (SR * STEP_SEC).toInt()

    // ── FRECUENCIAS ───────────────────────────────────────────────
    private fun hz(note: String): Float = when (note) {
        "d2"  -> 73.42f;  "f2"  -> 87.31f;  "c2"  -> 65.41f
        "a2"  -> 110.00f; "c3"  -> 130.81f; "g2"  -> 98.00f
        "d4"  -> 293.66f; "f4"  -> 349.23f; "g4"  -> 392.00f
        "a4"  -> 440.00f; "e4"  -> 329.63f; "c4"  -> 261.63f
        "~"   -> 0f;      else  -> 0f
    }

    // ── PATRONES ──────────────────────────────────────────────────

    // Bajo: <d2 d2 f2 c2>*4 → 4 notas × 4 reps = 16 pasos, 1 paso/nota
    private val BASS = listOf(
        "d2","d2","d2","d2",
        "f2","f2","f2","f2",
        "c2","c2","c2","c2",
        "d2","d2","d2","d2"
    )

    // Melodía: <d4 ~ f4 g4> <a4 g4 f4 ~> <d4 ~ c4 d4> <e4 d4 c4 ~>
    // 4 barras × 4 notas = 16 pasos, 1 paso/nota
    private val MELODY = listOf(
        "d4","~","f4","g4",
        "a4","g4","f4","~",
        "d4","~","c4","d4",
        "e4","d4","c4","~"
    )

    // Pad: <a2 a2 c3 g2>*2 .slow(2)
    // slow(2) → cada nota dura 2 pasos; *2 → 4 notas × 2 reps × 2 pasos = 16
    private val PAD = listOf(
        "a2","a2","a2","a2",
        "c3","c3","c3","c3",
        "g2","g2","g2","g2",
        "a2","a2","a2","a2"
    )

    // Percusión TR808
    // bd*2    → pasos 0 y 8
    // ~sd~sd  → pasos 4 y 12
    // hh*8    → pasos 0,2,4,6,8,10,12,14 (cada 2)
    private val KICK  = intArrayOf(1,0,0,0, 0,0,0,0, 1,0,0,0, 0,0,0,0)
    private val SNARE = intArrayOf(0,0,0,0, 1,0,0,0, 0,0,0,0, 1,0,0,0)
    private val HIHAT = intArrayOf(1,0,1,0, 1,0,1,0, 1,0,1,0, 1,0,1,0)

    // ── ESTADO ────────────────────────────────────────────────────
    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    @Volatile private var isPlaying = false

    private val DELAY_BUF = (SR * 0.4).toInt()
    private val delayBufL = FloatArray(DELAY_BUF)
    private val delayBufR = FloatArray(DELAY_BUF)
    private var delayIdx  = 0

    // ── API PÚBLICA ───────────────────────────────────────────────

    fun start(scope: CoroutineScope) {
        if (isPlaying) return
        isPlaying = true
        delayBufL.fill(0f); delayBufR.fill(0f); delayIdx = 0
        Log.d(TAG, "▶ Música de aventura iniciada")
        job = scope.launch(Dispatchers.Default) { runLoop() }
    }

    fun stop() {
        isPlaying = false
        job?.cancel(); job = null
        runCatching {
            audioTrack?.pause(); audioTrack?.flush()
            audioTrack?.stop();  audioTrack?.release()
        }
        audioTrack = null
        Log.d(TAG, "■ Música de aventura detenida")
    }

    fun fadeOutAndStop(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            for (v in 10 downTo 0) {
                audioTrack?.setVolume(v / 10f)
                delay(80)
            }
            stop()
        }
    }

    // ── BUCLE PRINCIPAL ───────────────────────────────────────────

    private suspend fun runLoop() {
        val bufSize = AudioTrack.getMinBufferSize(
            SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(STEP_SAMPLES * 4)

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
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        var step = 0
        while (isPlaying && coroutineContext.isActive) {
            val s = step % STEPS_PER_BAR
            val buf = synthesizeStep(
                bassFreq   = hz(BASS  [s]),
                melodyFreq = hz(MELODY[s]),
                padFreq    = hz(PAD   [s]),
                hasKick    = KICK [s] == 1,
                hasSnare   = SNARE[s] == 1,
                hasHihat   = HIHAT[s] == 1
            )
            audioTrack?.write(buf, 0, buf.size)
            step++
        }
    }

    // ── SÍNTESIS ──────────────────────────────────────────────────

    private fun synthesizeStep(
        bassFreq: Float, melodyFreq: Float, padFreq: Float,
        hasKick: Boolean, hasSnare: Boolean, hasHihat: Boolean
    ): ShortArray {
        val n   = STEP_SAMPLES
        val dur = n.toDouble() / SR
        val buf = ShortArray(n * 2)

        // LPF fijo para el bajo (lpf=400)
        val dt    = 1.0 / SR
        val rc    = 1.0 / (2 * PI * 400.0)
        val alpha = (dt / (rc + dt)).toFloat()
        var lpfL  = 0f

        // LPF para melodía (lpf=1200)
        val rcM   = 1.0 / (2 * PI * 1200.0)
        val alphaM = (dt / (rcM + dt)).toFloat()
        var lpfM  = 0f

        val delayTimeSamples = (0.1 * SR).toInt()  // delay=0.1s
        val delayFb          = 0.35f               // delayfb implícito suave

        for (i in 0 until n) {
            val t = i.toDouble() / SR

            // ── Bajo sawtooth lpf=400 ─────────────────────────────
            var bassAmp = 0.0
            if (bassFreq > 0f) {
                val p   = (t * bassFreq) % 1.0
                val saw = (2.0 * p - 1.0).toFloat()
                lpfL    = alpha * saw + (1f - alpha) * lpfL
                val env = adsr(t, dur, 0.005, 0.05, 0.8, 0.1)
                bassAmp = lpfL * env * 0.7
            }

            // ── Melodía square lpf=1200 ───────────────────────────
            var melAmp = 0.0
            if (melodyFreq > 0f) {
                val p   = (t * melodyFreq) % 1.0
                val sq  = (if (p < 0.5) 1.0 else -1.0).toFloat()
                lpfM    = alphaM * sq + (1f - alphaM) * lpfM
                val env = adsr(t, dur, 0.008, 0.04, 0.7, 0.12)
                melAmp  = lpfM * env * 0.45
            }

            // Delay melodía (delay=0.1, room=0.3 → reverb suave)
            val dIdx    = ((delayIdx - delayTimeSamples) + DELAY_BUF) % DELAY_BUF
            val dL      = delayBufL[dIdx]
            val dR      = delayBufR[dIdx]
            val melWetL = (melAmp + dL * delayFb).toFloat()
            val melWetR = (melAmp + dR * delayFb).toFloat()
            delayBufL[delayIdx % DELAY_BUF] = melWetL
            delayBufR[delayIdx % DELAY_BUF] = melWetR
            if (i == 0) delayIdx = (delayIdx + 1) % DELAY_BUF

            // ── Pad sine (slow=2 → nota larga, room=0.6) ─────────
            val padAmp = if (padFreq > 0f) {
                // room=0.6 → reverb simulado con dos reflexiones
                val direct  = sin(2 * PI * padFreq * t)
                val reflect = sin(2 * PI * padFreq * (t - 0.03)) * 0.4
                (direct + reflect) * 0.25 *
                        adsr(t, dur, 0.02, 0.1, 0.8, 0.2)
            } else 0.0

            // ── Percusión TR808 ───────────────────────────────────
            val drumAmp  = when {
                hasKick  -> kick808(t)
                hasSnare -> snare808(t)
                else     -> 0.0
            }
            val hihatAmp = if (hasHihat) hihat808(t) else 0.0

            // ── Mezcla + soft clip ────────────────────────────────
            val mixL = bassAmp + melWetL * 0.5 + padAmp + drumAmp + hihatAmp
            val mixR = bassAmp + melWetR * 0.5 + padAmp + drumAmp + hihatAmp

            buf[i * 2]     = (tanh(mixL).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            buf[i * 2 + 1] = (tanh(mixR).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buf
    }

    private fun adsr(t: Double, dur: Double, a: Double, d: Double, s: Double, r: Double): Double {
        val rStart = (dur - r).coerceAtLeast(a + d)
        return when {
            t < a      -> t / a
            t < a + d  -> 1.0 - (1.0 - s) * ((t - a) / d)
            t < rStart -> s
            t < dur    -> s * (1.0 - (t - rStart) / r)
            else       -> 0.0
        }.coerceIn(0.0, 1.0)
    }

    // ── PERCUSIÓN TR808 ───────────────────────────────────────────

    /** Bombo 808: sweep suave, más redondo que el 909 */
    private fun kick808(t: Double): Double {
        if (t > 0.35) return 0.0
        val freq = 65.0 * exp(-12.0 * t) + 40.0   // sweep más suave
        return sin(2 * PI * freq * t) * exp(-6.0 * t) * 0.9
    }

    /** Caja 808: más airada, ruido + tono más bajo */
    private fun snare808(t: Double): Double {
        if (t > 0.22) return 0.0
        val noise = (Math.random() * 2 - 1) * exp(-22.0 * t)
        val tone  = sin(2 * PI * 160.0 * t) * exp(-20.0 * t) * 0.35
        return (noise * 0.6 + tone) * 0.8
    }

    /** Hi-hat 808: más metálico */
    private fun hihat808(t: Double): Double {
        if (t > 0.05) return 0.0
        return (Math.random() * 2 - 1) * exp(-60.0 * t) * 0.3
    }
}