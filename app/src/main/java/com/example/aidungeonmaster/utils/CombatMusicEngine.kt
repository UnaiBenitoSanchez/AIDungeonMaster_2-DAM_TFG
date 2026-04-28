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
 *  COMBAT MUSIC ENGINE — "Furia de los Condenados"
 *  Traducción directa del patrón Strudel a síntesis PCM Android.
 *
 *  setcpm(140/4) → 35 ciclos/min → 1 barra = 1.714s → BPM = 140
 *  Resolución: semicorcheas (16 pasos por barra)
 *
 *  Capa 1 — Bajo sawtooth  distorsionado, LPF sweep 300→800
 *  Capa 2 — Melodía triangle, staccato (sustain 0.15), delay+fb
 *  Capa 3 — Pad sine, detune +10 cents, slow×4
 *  Capa 4 — Percusión TR909: kick síncopa, snare, hihat×16
 * ══════════════════════════════════════════════════════════════════
 */
object CombatMusicEngine {

    private const val SR  = 44100          // Sample rate
    private const val TAG = "COMBAT_MUSIC"
    private const val VOICE_CONTROL_DUCKING_MULTIPLIER = 0.16f

    // ── BPM / TEMPO ───────────────────────────────────────────────
    // setcpm(140/4) = 35 ciclos/min → 1 ciclo = 1.714 s
    // 16 pasos por ciclo → 1 paso = 0.1071 s
    private const val BPM          = 140.0
    private const val STEPS_PER_BAR = 16
    private val STEP_SEC = 60.0 / BPM / 4.0          // semicorchea
    private val STEP_SAMPLES get() = (SR * STEP_SEC).toInt()

    // ── FRECUENCIAS (Hz) ──────────────────────────────────────────
    private fun noteHz(note: String): Float {
        val map = mapOf(
            "d1" to 36.71f,  "f1" to 43.65f,
            "d2" to 73.42f,  "eb2" to 77.78f, "f2" to 87.31f,
            "d3" to 146.83f, "eb3" to 155.56f,"f3" to 174.61f, "g3" to 196.00f,
            "c#4" to 277.18f,"d4" to 293.66f, "eb4" to 311.13f,"f4" to 349.23f,
            "g4"  to 392.00f,"g#4" to 415.30f,"a4"  to 440.00f,
            "~"   to 0f
        )
        return map[note] ?: 0f
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║  PATRONES — traducción directa del Strudel                  ║
    // ╚══════════════════════════════════════════════════════════════╝

    // 1. Bajo: <d1 d1 d1 [d1 f1]>*2
    //    4 items × 2 = 8 notas/ciclo → cada una dura 2 pasos
    //    [d1 f1] = 2 notas en el espacio de 1 → cada una 1 paso
    //    Resultado en 16 pasos:
    private val BASS = listOf(
        "d1","d1", "d1","d1", "d1","d1",  // d1*3 × 2 pasos
        "d1","f1",                          // [d1 f1] × 1 paso c/u
        "d1","d1", "d1","d1", "d1","d1",   // repetición segunda mitad
        "d1","f1"
    )

    // 2. Melodía: <d4 d4 f4 g4> <g#4!3 a4> <d4 f4 d4 f4> <c#4!3 d4>
    //    4 frases de 4 notas, cada nota dura 1 paso (sustain 0.15 = staccato)
    private val MELODY = listOf(
        "d4","d4","f4","g4",
        "g#4","g#4","g#4","a4",
        "d4","f4","d4","f4",
        "c#4","c#4","c#4","d4"
    )

    // 3. Pad: <d3 eb3 d3 f3>*2 .slow(4)
    //    slow(4) → cada nota dura 4 pasos × 4 notas = 16 pasos/ciclo
    //    *2 dentro de slow(4) → en realidad cada nota dura 2 pasos
    private val PAD = listOf(
        "d3","d3","d3","d3",  // d3 × 4 pasos
        "eb3","eb3","eb3","eb3",
        "d3","d3","d3","d3",
        "f3","f3","f3","f3"
    )

    // 4. Percusión: bd [~ bd] bd ~, ~ sd [~ sd] ~, hh*16
    //    Kick  (1=sí, 0=no) por los 16 pasos:
    private val KICK  = intArrayOf(1,0,0,1,1,0,0,0, 1,0,0,1,1,0,0,0)
    //    Snare (pasos 4 y 10 en el ciclo):
    private val SNARE = intArrayOf(0,0,0,0,1,0,0,0, 0,0,1,0,0,0,0,0)
    //    Hihat en todos los pasos:
    private val HIHAT = IntArray(16) { 1 }

    // ── LPF SWEEP para el bajo: sine.range(300, 1200).slow(4)
    // Un ciclo sinusoidal completo cada 4 barras = 64 pasos
    // En cada paso calculamos la fase global acumulada
    private fun lpfForStep(globalStep: Int): Float {
        val cycleSteps = STEPS_PER_BAR * 4.0   // slow(4) → 64 pasos/ciclo
        val phase = (globalStep / cycleSteps) * 2 * PI
        // sine.range(300, 1200): mapea [-1,1] → [300, 1200]
        val mid   = (300.0 + 1200.0) / 2.0     // 750
        val amp   = (1200.0 - 300.0) / 2.0     // 450
        return (mid + amp * sin(phase)).toFloat()
    }

    // ── ESTADO ────────────────────────────────────────────────────
    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    @Volatile private var isPlaying = false
    @Volatile private var voiceControlDuckingEnabled = false

    // Buffer de delay para la melodía
    private val DELAY_BUF_SIZE = (SR * 0.5).toInt()   // 500ms máximo
    private val delayBufL = FloatArray(DELAY_BUF_SIZE)
    private val delayBufR = FloatArray(DELAY_BUF_SIZE)
    private var delayIdx  = 0

    // ── API PÚBLICA ───────────────────────────────────────────────

    fun start(scope: CoroutineScope) {
        if (isPlaying) return
        isPlaying = true
        delayBufL.fill(0f); delayBufR.fill(0f); delayIdx = 0
        Log.d(TAG, "▶ Iniciando música de combate")
        job = scope.launch(Dispatchers.Default) { runMusicLoop() }
    }

    fun stop() {
        isPlaying = false
        job?.cancel(); job = null
        runCatching {
            audioTrack?.pause(); audioTrack?.flush()
            audioTrack?.stop();  audioTrack?.release()
        }
        audioTrack = null
        Log.d(TAG, "■ Música detenida")
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

    fun setVoiceControlDucking(enabled: Boolean) {
        voiceControlDuckingEnabled = enabled
    }

    // ── BUCLE PRINCIPAL ───────────────────────────────────────────

    private suspend fun runMusicLoop() {
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

            val bassFreq   = noteHz(BASS  [s])
            val melodyFreq = noteHz(MELODY[s])
            val padFreq    = noteHz(PAD   [s])
            val lpfHz      = lpfForStep(step)   // paso global → seno continuo
            val hasKick    = KICK [s] == 1
            val hasSnare   = SNARE[s] == 1
            val hasHihat   = HIHAT[s] == 1

            val buf = synthesizeStep(
                bassFreq   = bassFreq,
                melodyFreq = melodyFreq,
                padFreq    = padFreq,
                lpfHz      = lpfHz,
                hasKick    = hasKick,
                hasSnare   = hasSnare,
                hasHihat   = hasHihat,
                isStaccato = true   // sustain 0.15 → siempre staccato en melodía
            )

            audioTrack?.write(buf, 0, buf.size)
            step++
        }
    }

    // ── SÍNTESIS DE UN PASO (STEP) ────────────────────────────────

    private fun synthesizeStep(
        bassFreq: Float,
        melodyFreq: Float,
        padFreq: Float,
        lpfHz: Float,
        hasKick: Boolean,
        hasSnare: Boolean,
        hasHihat: Boolean,
        isStaccato: Boolean
    ): ShortArray {
        val n   = STEP_SAMPLES
        val dur = n.toDouble() / SR
        val buf = ShortArray(n * 2)   // estéreo
        val masterGain = if (voiceControlDuckingEnabled) {
            VOICE_CONTROL_DUCKING_MULTIPLIER
        } else {
            1f
        }

        // Detune para el pad: +10 cents → freq × 2^(10/1200)
        val padDetuned = padFreq * 2f.pow(10f / 1200f)

        // Coeficiente LPF simple (RC one-pole): α = dt/(RC+dt), RC = 1/(2π·fc)
        val dt    = 1.0 / SR
        val rcBass= 1.0 / (2 * PI * lpfHz)
        val alpha = (dt / (rcBass + dt)).toFloat()
        var lpfState = 0f

        // Buffer de delay melodía: delaytime=0.2s, delayfb=0.4
        val delayTimeSamples = (0.2 * SR).toInt()
        val delayFb          = 0.4f

        for (i in 0 until n) {
            val t = i.toDouble() / SR

            // ── 1. BAJO sawtooth + distorsión ────────────────────
            var bassAmp = 0.0
            if (bassFreq > 0f) {
                val p   = (t * bassFreq) % 1.0
                val saw = 2.0 * p - 1.0       // sawtooth puro
                // Distorsión waveshaper (tanh suave): distort(0.4)
                val dist= tanh(saw * (1.0 + 0.4 * 3.0)) / tanh(1.0 + 0.4 * 3.0)
                // LPF one-pole
                lpfState = alpha * dist.toFloat() + (1f - alpha) * lpfState
                val env = adsr(t, dur, 0.005, 0.05, 0.8, 0.1)
                bassAmp = lpfState * env * 0.8
            }

            // ── 2. MELODÍA triangle + staccato + delay ───────────
            var melAmp = 0.0
            if (melodyFreq > 0f) {
                val p  = (t * melodyFreq) % 1.0
                // Triangle wave: |2p - 1| * 2 - 1
                val tri = abs(2.0 * p - 1.0) * 2.0 - 1.0
                // Sustain 0.15 = staccato: nota se corta al 15% del paso
                val staccatoDur = if (isStaccato) dur * 0.15 else dur
                val env = adsr(t, staccatoDur, 0.005, 0.02, 0.7, 0.03)
                melAmp = tri * env * 0.5
            }

            // Delay con feedback (delaytime=0.2, delayfb=0.4)
            val dReadIdx = ((delayIdx - delayTimeSamples) + DELAY_BUF_SIZE) % DELAY_BUF_SIZE
            val delayedL = delayBufL[dReadIdx]
            val delayedR = delayBufR[dReadIdx]
            val melL     = (melAmp + delayedL * delayFb).toFloat()
            val melR     = (melAmp + delayedR * delayFb).toFloat()
            delayBufL[delayIdx % DELAY_BUF_SIZE] = melL
            delayBufR[delayIdx % DELAY_BUF_SIZE] = melR
            if (i == 0) delayIdx = (delayIdx + 1) % DELAY_BUF_SIZE

            // ── 3. PAD sine + detune ──────────────────────────────
            val padAmp = if (padFreq > 0f) {
                val s1 = sin(2 * PI * padFreq   * t)
                val s2 = sin(2 * PI * padDetuned * t)
                // Mezcla de dos senos: uno afinado y uno +10 cents (corusing)
                (s1 + s2) * 0.5 * 0.2
            } else 0.0

            // ── 4. PERCUSIÓN TR909 ────────────────────────────────
            val drumAmp = when {
                hasKick  -> kick909(t)
                hasSnare -> snare909(t)
                else     -> 0.0
            }
            val hihatAmp = if (hasHihat) hihat909(t) else 0.0

            // ── MEZCLA + SOFT CLIP ────────────────────────────────
            val mixL = (bassAmp + melL * 0.45 + padAmp + drumAmp + hihatAmp)
            val mixR = (bassAmp + melR * 0.45 + padAmp + drumAmp + hihatAmp)

            // Soft clip final para evitar clipping duro
            val outL = tanh(mixL).toFloat().coerceIn(-1f, 1f)
            val outR = tanh(mixR).toFloat().coerceIn(-1f, 1f)

            buf[i * 2]     = (outL * masterGain * Short.MAX_VALUE).toInt().toShort()
            buf[i * 2 + 1] = (outR * masterGain * Short.MAX_VALUE).toInt().toShort()
        }
        return buf
    }

    // ── ADSR ──────────────────────────────────────────────────────

    private fun adsr(
        t: Double, dur: Double,
        a: Double, d: Double, s: Double, r: Double
    ): Double {
        val rStart = (dur - r).coerceAtLeast(a + d)
        return when {
            t < a          -> t / a
            t < a + d      -> 1.0 - (1.0 - s) * ((t - a) / d)
            t < rStart     -> s
            t < dur        -> s * (1.0 - (t - rStart) / r)
            else           -> 0.0
        }.coerceIn(0.0, 1.0)
    }

    // ── PERCUSIÓN TR909 ───────────────────────────────────────────

    /** Bombo 909: sweep exponencial + click + distorsión leve */
    private fun kick909(t: Double): Double {
        if (t > 0.30) return 0.0
        val freq  = 55.0 * exp(-18.0 * t) + 28.0   // sweep 83→28 Hz
        val body  = sin(2 * PI * freq * t)
        val click = exp(-60.0 * t) * 0.5
        val raw   = (body + click) * exp(-8.0 * t)
        return tanh(raw * 1.4) * 0.9   // .distort(0.2)
    }

    /** Caja 909: ruido + tono 200Hz + transiente agresivo */
    private fun snare909(t: Double): Double {
        if (t > 0.18) return 0.0
        val noise     = (Math.random() * 2 - 1) * exp(-28.0 * t)
        val tone      = sin(2 * PI * 185.0 * t) * exp(-25.0 * t)
        val transient = exp(-80.0 * t) * 0.4
        return (noise * 0.55 + tone * 0.3 + transient) * 0.85
    }

    /** Hi-hat 909: ruido blanco de muy corta duración, filtrado alto */
    private fun hihat909(t: Double): Double {
        if (t > 0.035) return 0.0
        // Ruido + componente de alta frecuencia (simula filtro paso-alto)
        val noise  = (Math.random() * 2 - 1)
        val hpEnv  = exp(-90.0 * t)
        return noise * hpEnv * 0.28
    }
}