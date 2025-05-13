package com.anonymous.MobileJarvisNative.voice

import android.content.Context
import android.util.Log
import com.anonymous.MobileJarvisNative.utils.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Interface for voice processing strategy
 * Used to create strategies for different voice processing backends
 */
interface VoiceProcessor {
    /**
     * Initialize the voice processor
     */
    fun initialize()
    
    /**
     * Start voice processing
     * 
     * @return True if successfully started, false otherwise
     */
    fun start(): Boolean
    
    /**
     * Process recognized speech
     * 
     * @param text Recognized text to process
     * @param onResult Callback for when result is ready
     */
    fun processText(text: String, onResult: (String) -> Unit)
    
    /**
     * Process audio data directly (for fallback STT)
     * 
     * @param audioData Raw audio data to process
     * @param onResult Callback for when result is ready
     */
    fun processAudio(audioData: ByteArray, onResult: (String) -> Unit)
    
    /**
     * Speak a response using text-to-speech
     * 
     * @param text Text to speak
     * @param onComplete Callback for when speech is complete
     */
    fun speak(text: String, onComplete: () -> Unit = {})
    
    /**
     * Stop any ongoing processing or speech
     */
    fun stop()
    
    /**
     * Interrupt any ongoing speech
     * 
     * @return True if speech was interrupted, false otherwise
     */
    fun interrupt(): Boolean
    
    /**
     * Check if currently speaking
     * 
     * @return True if currently speaking, false otherwise
     */
    fun isSpeaking(): Boolean
    
    /**
     * Shutdown and clean up resources
     */
    fun shutdown()

    /**
     * Called when no speech is detected after the wake word
     */
    fun onNoSpeechDetected()
}

/**
 * Vapi implementation of VoiceProcessor
 * Uses Vapi for speech processing and handles audio entirely through Vapi
 */
class VapiVoiceProcessor(private val context: Context) : VoiceProcessor {
    private val TAG = "VapiVoiceProcessor"
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var isSessionActive = false
    
    override fun initialize() {
        Log.i(TAG, "Initializing Vapi voice processor")
        TextToSpeechManager.initialize(context)
    }
    
    override fun start(): Boolean {
        Log.i(TAG, "Starting Vapi voice processor")
        isSessionActive = true
        return true
    }
    
    override fun processText(text: String, onResult: (String) -> Unit) {
        Log.i(TAG, "Processing text with Vapi: $text")
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // This would be replaced with actual Vapi API call
                    // Simplified example implementation
                    val response = "I processed your request: \"$text\" using Vapi service"
                    onResult(response)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in Vapi API call", e)
                    onResult("Error processing your request: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing text with Vapi", e)
            onResult("Error processing your request: ${e.message}")
        }
    }
    
    override fun processAudio(audioData: ByteArray, onResult: (String) -> Unit) {
        Log.w(TAG, "Vapi does not support direct audio processing in this implementation")
        onResult("Audio processing not supported in Vapi mode")
    }
    
    override fun speak(text: String, onComplete: () -> Unit) {
        Log.i(TAG, "Speaking with Vapi/system TTS: $text")
        // Use system TTS
        TextToSpeechManager.speak(text, onComplete = onComplete)
    }
    
    override fun stop() {
        Log.i(TAG, "Stopping Vapi voice processor")
        try {
            isSessionActive = false
            TextToSpeechManager.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Vapi session", e)
        }
    }
    
    override fun interrupt(): Boolean {
        Log.i(TAG, "Interrupting Vapi voice processor")
        try {
            isSessionActive = false
            TextToSpeechManager.interrupt()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting Vapi session", e)
            return false
        }
    }
    
    override fun isSpeaking(): Boolean {
        return TextToSpeechManager.isSpeaking()
    }
    
    override fun shutdown() {
        Log.i(TAG, "Shutting down Vapi voice processor")
        try {
            isSessionActive = false
            TextToSpeechManager.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down Vapi processor", e)
        }
    }
    
    override fun onNoSpeechDetected() {
        Log.i(TAG, "No speech detected in Vapi processor")
        // Get VoiceManager instance and delegate handling to it
        VoiceManager.getInstance()?.handleNoSpeechDetected()
    }
}

/**
 * Modular implementation of VoiceProcessor
 * Uses composable cloud services (Whisper, Mistral, ElevenLabs) for each step of processing
 */
class ModularVoiceProcessor(private val context: Context) : VoiceProcessor {
    private val TAG = "ModularVoiceProcessor"
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var isActive = false
    private var isSpeaking = false
    private val cacheDir = File(context.cacheDir, "tts_cache")
    
    override fun initialize() {
        Log.i(TAG, "Initializing modular voice processor")
        try {
            TextToSpeechManager.initialize(context)
            cacheDir.mkdirs()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing modular processor", e)
        }
    }
    
    override fun start(): Boolean {
        Log.i(TAG, "Starting modular voice processor")
        return try {
            isActive = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error activating modular mode", e)
            false
        }
    }
    
    override fun processText(text: String, onResult: (String) -> Unit) {
        Log.i(TAG, "Processing text with modular processor: $text")
        try {
            // Using the LLM service for processing - launching in a coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // This would be replaced with actual LLM API call
                    // Simplified example implementation
                    val response = "I processed your request: \"$text\" using the modular processor"
                    onResult(response)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in LLM processing coroutine", e)
                    onResult("Error processing your request: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing text with modular processor", e)
            onResult("Error processing your request: ${e.message}")
        }
    }
    
    override fun processAudio(audioData: ByteArray, onResult: (String) -> Unit) {
        Log.i(TAG, "Processing audio with modular processor (${audioData.size} bytes)")
        try {
            // Use Whisper for speech recognition - launching in a coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // This would be replaced with actual Whisper API call
                    // Simplified example implementation
                    val recognizedText = "Example transcribed text from audio data"
                    
                    if (recognizedText.isNotEmpty()) {
                        Log.i(TAG, "Whisper STT result: \"$recognizedText\"")
                        
                        // Now process the recognized text with the LLM
                        processText(recognizedText, onResult)
                    } else {
                        Log.w(TAG, "Empty result from Whisper STT")
                        onResult("I couldn't understand what you said. Please try again.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in STT processing coroutine", e)
                    onResult("Error processing your speech: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio with modular processor", e)
            onResult("Error processing your speech: ${e.message}")
        }
    }
    
    override fun speak(text: String, onComplete: () -> Unit) {
        Log.i(TAG, "Speaking with TTS: $text")
        try {
            // Use system TTS
            isSpeaking = true
            TextToSpeechManager.speak(text) {
                isSpeaking = false
                onComplete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking with TTS", e)
            isSpeaking = false
            onComplete()
        }
    }
    
    override fun stop() {
        Log.i(TAG, "Stopping modular voice processor")
        try {
            // Stop TTS
            TextToSpeechManager.stop()
            isSpeaking = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping modular processor", e)
        }
    }
    
    override fun interrupt(): Boolean {
        Log.i(TAG, "Interrupting modular voice processor")
        try {
            isActive = false
            isSpeaking = false
            TextToSpeechManager.interrupt()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting modular processor", e)
            return false
        }
    }
    
    override fun isSpeaking(): Boolean {
        return isSpeaking || TextToSpeechManager.isSpeaking()
    }
    
    override fun shutdown() {
        Log.i(TAG, "Shutting down modular voice processor")
        try {
            isActive = false
            isSpeaking = false
            
            // Shutdown system TTS
            TextToSpeechManager.shutdown()
            
            // Clean up cache
            cleanupCache()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down modular processor", e)
        }
    }
    
    /**
     * Cleans old cache files
     */
    private fun cleanupCache() {
        try {
            val files = cacheDir.listFiles() ?: return
            // Keep only the 20 most recent files
            if (files.size > 20) {
                files.sortBy { it.lastModified() }
                for (i in 0 until files.size - 20) {
                    files[i].delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning cache", e)
        }
    }
    
    override fun onNoSpeechDetected() {
        Log.i(TAG, "No speech detected in Modular processor")
        // Get VoiceManager instance and delegate handling to it
        VoiceManager.getInstance()?.handleNoSpeechDetected()
    }
} 