package com.cameronhightower.mobilejarvisnative.modules.voice

import android.content.Context
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class VoiceProcessor {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isProcessing = false
    private var isSpeaking = false
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context
        setupSpeechRecognizer()
        setupTextToSpeech()
    }

    private fun setupSpeechRecognizer() {
        context?.let { ctx ->
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isProcessing = true
                    }

                    override fun onResults(results: Bundle?) {
                        isProcessing = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            // Process the recognized text
                            speakResponse("I heard: ${matches[0]}")
                        }
                    }

                    override fun onError(error: Int) {
                        isProcessing = false
                    }

                    // Required empty implementations
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun setupTextToSpeech() {
        context?.let { ctx ->
            textToSpeech = TextToSpeech(ctx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.US
                }
            }
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isProcessing = false
    }

    fun interruptSpeech() {
        textToSpeech?.stop()
        isSpeaking = false
    }

    private fun speakResponse(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "response_${System.currentTimeMillis()}")
        isSpeaking = true
    }

    fun isProcessing(): Boolean = isProcessing

    fun isSpeaking(): Boolean = isSpeaking

    fun release() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
} 