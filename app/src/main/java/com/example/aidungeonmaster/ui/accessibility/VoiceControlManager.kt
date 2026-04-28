package com.example.aidungeonmaster.ui.accessibility

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Orquestador nativo de reconocimiento de voz + narración.
 *
 * Mantiene el reconocimiento en escucha continua mientras está activo, envía
 * cada orden a la capa de navegación y espera a que TextToSpeech termine de
 * narrar antes de volver a abrir el micrófono. Así se evita que la app se
 * escuche a sí misma mientras confirma una acción.
 */
class VoiceControlManager(
    context: Context,
    private val onUiStateChanged: (VoiceControlUiState) -> Unit,
    private val onCommandRecognized: (String) -> String
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val spanishLocale = Locale("es", "ES")

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = TextToSpeech(appContext, this)
    private var ttsReady = false
    private var active = false
    private var listening = false
    private var speaking = false
    private var lastCommand: String? = null

    init {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    speaking = false
                    publishState("Escuchando órdenes…")
                    scheduleListeningRestart()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    speaking = false
                    publishState("Escuchando órdenes…")
                    scheduleListeningRestart()
                }
            }
        })
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            textToSpeech?.language = spanishLocale
        }
        publishState(if (active) "Control por voz preparado" else "Control por voz desactivado")
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            active = false
            publishState("El reconocimiento de voz no está disponible en este dispositivo.")
            return
        }

        ensureRecognizer()
        active = true
        speak("Control por voz activado. Di ayuda para escuchar algunos comandos disponibles.")
    }

    fun stop(announce: Boolean = true) {
        active = false
        listening = false
        speaking = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { speechRecognizer?.stopListening() }
        runCatching { speechRecognizer?.cancel() }

        if (announce) {
            speak("Control por voz desactivado.")
        } else {
            publishState("Control por voz desactivado")
        }
    }

    fun destroy() {
        active = false
        listening = false
        speaking = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        runCatching { textToSpeech?.stop() }
        runCatching { textToSpeech?.shutdown() }
        textToSpeech = null
    }

    private fun ensureRecognizer() {
        if (speechRecognizer != null) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    publishState("Escuchando órdenes…")
                }

                override fun onBeginningOfSpeech() {
                    listening = true
                    publishState("Te escucho…")
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    listening = false
                    publishState("Procesando orden…")
                }

                override fun onError(error: Int) {
                    listening = false
                    if (!active) {
                        publishState("Control por voz desactivado")
                        return
                    }

                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No he entendido la orden."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No he escuchado ninguna orden."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso de micrófono."
                        else -> "No he podido procesar la voz."
                    }

                    publishState(message)
                    scheduleListeningRestart(delayMillis = 900L)
                }

                override fun onResults(results: Bundle?) {
                    listening = false

                    val recognizedText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()

                    if (recognizedText.isBlank()) {
                        publishState("No he entendido la orden.")
                        scheduleListeningRestart(delayMillis = 700L)
                        return
                    }

                    lastCommand = recognizedText
                    publishState("Orden: $recognizedText")

                    val spokenFeedback = runCatching {
                        onCommandRecognized(recognizedText)
                    }.getOrElse { error ->
                        "No he podido ejecutar la orden: ${error.message ?: "error desconocido"}."
                    }

                    speak(spokenFeedback)
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun startListening() {
        if (!active || speaking || listening) return
        ensureRecognizer()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, spanishLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, spanishLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di una orden para AI Dungeon Master")
        }

        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(intent)
            listening = true
            publishState("Escuchando órdenes…")
        }.onFailure {
            listening = false
            publishState("No se pudo iniciar el micrófono.")
            scheduleListeningRestart(delayMillis = 1200L)
        }
    }

    private fun scheduleListeningRestart(delayMillis: Long = 450L) {
        if (!active) {
            publishState("Control por voz desactivado")
            return
        }
        mainHandler.postDelayed({ startListening() }, delayMillis)
    }

    private fun speak(message: String) {
        if (message.isBlank()) {
            scheduleListeningRestart()
            return
        }

        speaking = true
        listening = false
        runCatching { speechRecognizer?.cancel() }
        publishState("Narrando respuesta…")

        if (!ttsReady) {
            speaking = false
            publishState(message)
            scheduleListeningRestart(delayMillis = 800L)
            return
        }

        val utteranceId = "aidm_voice_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun publishState(status: String) {
        val state = VoiceControlUiState(
            active = active,
            listening = listening,
            speaking = speaking,
            status = status,
            lastCommand = lastCommand
        )

        if (Looper.myLooper() == Looper.getMainLooper()) {
            onUiStateChanged(state)
        } else {
            mainHandler.post { onUiStateChanged(state) }
        }
    }
}

data class VoiceControlUiState(
    val active: Boolean = false,
    val listening: Boolean = false,
    val speaking: Boolean = false,
    val status: String = "Control por voz desactivado",
    val lastCommand: String? = null
)
