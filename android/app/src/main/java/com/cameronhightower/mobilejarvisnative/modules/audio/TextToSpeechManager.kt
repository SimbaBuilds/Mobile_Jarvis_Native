package com.cameronhightower.mobilejarvisnative.modules.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

/**
 * Manager for Text-to-Speech functionality
 */
object TextToSpeechManager {
    private const val TAG = "TextToSpeechManager"
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false
    private var pendingCallbacks = mutableMapOf<String, () -> Unit>()
    
    /**
     * Initialize the text-to-speech engine
     * 
     * @param context Application context
     * @return True if initialization was successful
     */
    fun initialize(context: Context): Boolean {
        if (isInitialized) {
            Log.d(TAG, "Text-to-speech already initialized")
            return true
        }
        
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.US)
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language not supported")
                        isInitialized = false
                    } else {
                        Log.i(TAG, "Text-to-speech initialized successfully")
                        
                        // Configure TTS parameters
                        tts?.setPitch(1.0f)
                        tts?.setSpeechRate(1.0f)
                        
                        // Set up progress listener
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String) {
                                Log.d(TAG, "Speech started: $utteranceId")
                                isSpeaking = true
                            }
                            
                            override fun onDone(utteranceId: String) {
                                Log.d(TAG, "Speech completed: $utteranceId")
                                isSpeaking = false
                                
                                // Execute callback if registered
                                val callback = pendingCallbacks[utteranceId]
                                pendingCallbacks.remove(utteranceId)
                                callback?.invoke()
                            }
                            
                            override fun onError(utteranceId: String) {
                                Log.e(TAG, "Speech error: $utteranceId")
                                isSpeaking = false
                                
                                // Execute callback even on error
                                val callback = pendingCallbacks[utteranceId]
                                pendingCallbacks.remove(utteranceId)
                                callback?.invoke()
                            }
                        })
                        
                        isInitialized = true
                    }
                } else {
                    Log.e(TAG, "Failed to initialize text-to-speech: $status")
                    isInitialized = false
                }
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing text-to-speech", e)
            isInitialized = false
            return false
        }
    }
    
    /**
     * Check if the text-to-speech engine is currently speaking
     * 
     * @return True if speaking, false otherwise
     */
    fun isSpeaking(): Boolean {
        return isSpeaking
    }
    
    /**
     * Speak the given text
     * 
     * @param text Text to speak
     * @param queueMode Queue mode for speech
     * @param utteranceId Unique ID for this utterance, or null to generate one
     * @param onComplete Callback to be invoked when speech completes
     */
    fun speak(
        text: String, 
        queueMode: Int = TextToSpeech.QUEUE_FLUSH, 
        utteranceId: String? = null,
        onComplete: () -> Unit = {}
    ) {
        if (!isInitialized) {
            Log.e(TAG, "Cannot speak, text-to-speech not initialized")
            onComplete()
            return
        }
        
        try {
            val id = utteranceId ?: UUID.randomUUID().toString()
            
            // Register completion callback
            pendingCallbacks[id] = onComplete
            
            // Prepare speech params
            val params = HashMap<String, String>().apply {
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id)
            }
            
            // Queue the speech
            tts?.speak(text, queueMode, params)
            
            Log.d(TAG, "Speech queued with ID: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            onComplete()
        }
    }
    
    /**
     * Stop any ongoing speech
     */
    fun stop() {
        if (isInitialized && isSpeaking) {
            try {
                tts?.stop()
                isSpeaking = false
                
                // Call all pending callbacks since we're stopping
                val callbacks = pendingCallbacks.values.toList()
                pendingCallbacks.clear()
                
                for (callback in callbacks) {
                    callback()
                }
                
                Log.d(TAG, "Speech stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech", e)
            }
        }
    }
    
    /**
     * Interrupt the current speech and return to idle
     * 
     * @return True if speech was interrupted, false otherwise
     */
    fun interrupt(): Boolean {
        if (!isInitialized || !isSpeaking) {
            return false
        }
        
        try {
            tts?.stop()
            isSpeaking = false
            
            // Call all pending callbacks since we're interrupting
            val callbacks = pendingCallbacks.values.toList()
            pendingCallbacks.clear()
            
            for (callback in callbacks) {
                callback()
            }
            
            Log.d(TAG, "Speech interrupted")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting speech", e)
            return false
        }
    }
    
    /**
     * Clean up resources used by the text-to-speech engine
     */
    fun shutdown() {
        if (isInitialized) {
            try {
                tts?.stop()
                tts?.shutdown()
                tts = null
                isInitialized = false
                isSpeaking = false
                pendingCallbacks.clear()
                
                Log.d(TAG, "Text-to-speech shut down")
            } catch (e: Exception) {
                Log.e(TAG, "Error shutting down text-to-speech", e)
            }
        }
    }
}
