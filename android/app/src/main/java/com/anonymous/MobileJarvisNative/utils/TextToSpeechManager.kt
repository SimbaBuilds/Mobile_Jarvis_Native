package com.anonymous.MobileJarvisNative.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * Manager for text-to-speech functionality.
 * This is a temporary fallback solution until the full Vapi audio pipeline is implemented.
 */
object TextToSpeechManager {
    private const val TAG = "TextToSpeechManager"
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    /**
     * Initialize the TextToSpeech engine
     *
     * @param context Application context
     * @param onInitListener Optional callback for when initialization completes
     */
    fun initialize(context: Context, onInitListener: ((Boolean) -> Unit)? = null) {
        if (textToSpeech != null) {
            Log.d(TAG, "TextToSpeech already initialized")
            onInitListener?.invoke(isInitialized)
            return
        }

        try {
            Log.d(TAG, "Initializing TextToSpeech")
            textToSpeech = TextToSpeech(context) { status ->
                isInitialized = status == TextToSpeech.SUCCESS
                if (isInitialized) {
                    val result = textToSpeech?.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language not supported")
                        isInitialized = false
                    } else {
                        Log.d(TAG, "TextToSpeech initialized successfully")
                    }
                } else {
                    Log.e(TAG, "Failed to initialize TextToSpeech")
                }
                onInitListener?.invoke(isInitialized)
            }

            // Set up progress listener
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "Speech started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "Speech completed: $utteranceId")
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "Speech error: $utteranceId")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TextToSpeech", e)
            onInitListener?.invoke(false)
        }
    }

    /**
     * Speak the given text
     *
     * @param text Text to speak
     * @param queueMode How to queue this text
     * @param onComplete Optional callback for when speech completes
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_ADD, onComplete: (() -> Unit)? = null) {
        if (!isInitialized || textToSpeech == null) {
            Log.e(TAG, "TextToSpeech not initialized")
            onComplete?.invoke()
            return
        }

        try {
            val utteranceId = UUID.randomUUID().toString()
            
            // Set up listener for this specific utterance if needed
            if (onComplete != null) {
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}

                    override fun onDone(id: String?) {
                        if (id == utteranceId) {
                            onComplete.invoke()
                        }
                    }

                    override fun onError(id: String?) {
                        if (id == utteranceId) {
                            onComplete.invoke()
                        }
                    }
                })
            }

            // Speak the text
            Log.d(TAG, "Speaking: '$text'")
            textToSpeech?.speak(text, queueMode, null, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            onComplete?.invoke()
        }
    }

    /**
     * Check if TextToSpeech is currently speaking
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    /**
     * Stop any ongoing speech
     */
    fun stop() {
        textToSpeech?.stop()
    }

    /**
     * Interrupt any ongoing speech
     * 
     * @return True if speech was interrupted, false otherwise
     */
    fun interrupt(): Boolean {
        try {
            if (isSpeaking()) {
                textToSpeech?.stop()
                Log.d(TAG, "Speech interrupted successfully")
                return true
            } else {
                Log.d(TAG, "No speech to interrupt")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting speech", e)
            return false
        }
    }

    /**
     * Release TTS resources
     */
    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
            Log.d(TAG, "TextToSpeech shut down")
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TextToSpeech", e)
        }
    }
} 