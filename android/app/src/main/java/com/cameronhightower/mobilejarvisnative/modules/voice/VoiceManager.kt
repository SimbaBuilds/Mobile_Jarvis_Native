package com.cameronhightower.mobilejarvisnative.modules.voice

import android.content.Context
import android.util.Log
import com.cameronhightower.mobilejarvisnative.modules.voice.processors.DefaultVoiceProcessor
import com.cameronhightower.mobilejarvisnative.modules.voice.processors.SpeechRecognitionManager
import com.cameronhightower.mobilejarvisnative.modules.voice.processors.VoiceProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central manager for voice functionality that coordinates between wake word detection and voice processing
 */
class VoiceManager private constructor(private val context: Context) {
    private val TAG = "VoiceManager"
    
    // Voice state management
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    // Service status tracking
    private var isSpeechRecognitionInitialized = false
    
    // Voice processor
    private var voiceProcessor: VoiceProcessor = DefaultVoiceProcessor(context)
    
    // State tracking
    private var lastWakeWordTimestamp: Long = 0
    private const val MIN_WAKE_WORD_INTERVAL = 2000L
    private var noSpeechRetryCount = 0
    private const val MAX_NO_SPEECH_RETRIES = 2
    
    // Callback registry
    private val stateChangeCallbacks = mutableListOf<(VoiceState) -> Unit>()
    
    // Coroutine scope for background operations
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Initialize required services
     */
    fun initialize() {
        Log.i(TAG, "Initializing VoiceManager services")
        
        // Initialize SpeechRecognitionManager
        isSpeechRecognitionInitialized = SpeechRecognitionManager.initialize(context)
        Log.d(TAG, "Speech recognition initialized: $isSpeechRecognitionInitialized")
        
        // Initialize voice processor
        voiceProcessor.initialize()
    }
    
    /**
     * Handle wake word detection event
     * @param timestamp The system time when the wake word was detected
     * @return true if the wake word was processed, false if it was a duplicate
     */
    fun onWakeWordDetected(timestamp: Long): Boolean {
        // Check if this is a duplicate detection (can happen if wake word service
        // is temporarily processing the same audio segment)
        if (timestamp - lastWakeWordTimestamp < MIN_WAKE_WORD_INTERVAL) {
            Log.d(TAG, "Ignoring duplicate wake word detection")
            return false
        }
        
        // Current state already processing voice
        if (_voiceState.value != VoiceState.IDLE && _voiceState.value != VoiceState.ERROR) {
            Log.d(TAG, "Ignoring wake word detection while in state: ${_voiceState.value}")
            return false
        }
        
        lastWakeWordTimestamp = timestamp
        Log.i(TAG, "Wake word detected at $timestamp")
        
        // Update state
        updateState(VoiceState.WAKE_WORD_DETECTED)
        
        // Start voice recognition
        startVoiceRecognition()
        
        return true
    }
    
    /**
     * Start voice recognition after wake word detection
     */
    private fun startVoiceRecognition() {
        Log.i(TAG, "Starting voice recognition")
        
        // Update state
        updateState(VoiceState.LISTENING)
        
        // Reset the no speech retry counter
        noSpeechRetryCount = 0
        
        try {
            // Start the voice processor
            if (!voiceProcessor.start()) {
                Log.e(TAG, "Failed to start voice processor")
                setError("Failed to start voice recognition")
                return
            }
            
            // Set up the recognition listener
            SpeechRecognitionManager.setupRecognitionListener { recognizedText ->
                onSpeechRecognized(recognizedText)
            }
            
            // Start listening for speech
            SpeechRecognitionManager.startListening(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition", e)
            setError("Error starting voice recognition: ${e.message}")
        }
    }
    
    /**
     * Process speech recognition result
     */
    private fun onSpeechRecognized(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Empty speech recognition result received")
            handleNoSpeechDetected()
            return
        }
        
        Log.i(TAG, "Speech recognized: \"$text\"")
        
        // Update state to PROCESSING
        updateState(VoiceState.PROCESSING)
        
        try {
            // Process the recognized text with our voice processor
            voiceProcessor.processText(text) { response ->
                if (response.isNotEmpty()) {
                    // Update state to SPEAKING
                    updateState(VoiceState.SPEAKING)
                    
                    // Speak the response
                    voiceProcessor.speak(response) {
                        // Return to idle when done speaking
                        resetToIdle()
                    }
                } else {
                    Log.w(TAG, "Empty response from voice processor")
                    resetToIdle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing speech", e)
            setError("Error processing speech: ${e.message}")
        }
    }
    
    /**
     * Handle the case when no speech was detected
     */
    fun handleNoSpeechDetected() {
        Log.d(TAG, "No speech detected")
        
        // Increment retry counter
        noSpeechRetryCount++
        
        if (noSpeechRetryCount < MAX_NO_SPEECH_RETRIES) {
            // Try again after a short delay
            coroutineScope.launch {
                delay(500) // Short delay before retrying
                
                try {
                    Log.d(TAG, "Retrying speech recognition (attempt $noSpeechRetryCount)")
                    startVoiceRecognition()
                } catch (e: Exception) {
                    Log.e(TAG, "Error retrying speech recognition: ${e.message}", e)
                    resetToIdle()
                }
            }
        } else {
            // Max retries reached, reset to idle
            Log.d(TAG, "Maximum retry attempts reached ($MAX_NO_SPEECH_RETRIES), returning to idle")
            resetToIdle()
            noSpeechRetryCount = 0 // Reset counter
        }
    }
    
    /**
     * Manually activate voice assistant (without wake word)
     */
    fun activateManually() {
        if (_voiceState.value == VoiceState.IDLE || _voiceState.value == VoiceState.ERROR) {
            Log.i(TAG, "Voice assistant manually activated")
            startVoiceRecognition()
        } else {
            Log.d(TAG, "Cannot manually activate while in state: ${_voiceState.value}")
        }
    }
    
    /**
     * Interrupt the current speech
     */
    fun interruptSpeech(): Boolean {
        Log.i(TAG, "Interrupting speech")
        
        if (_voiceState.value != VoiceState.SPEAKING) {
            return false
        }
        
        val result = voiceProcessor.interrupt()
        
        if (result) {
            resetToIdle()
        }
        
        return result
    }
    
    /**
     * Register a state change callback
     */
    fun registerStateChangeCallback(callback: (VoiceState) -> Unit) {
        stateChangeCallbacks.add(callback)
        
        // Call immediately with current state
        callback(_voiceState.value)
    }
    
    /**
     * Unregister a state change callback
     */
    fun unregisterStateChangeCallback(callback: (VoiceState) -> Unit) {
        stateChangeCallbacks.remove(callback)
    }
    
    /**
     * Update the voice state and notify callbacks
     */
    private fun updateState(newState: VoiceState) {
        Log.d(TAG, "Updating voice state: ${_voiceState.value} -> $newState")
        _voiceState.value = newState
        
        // Notify callbacks
        stateChangeCallbacks.forEach { callback ->
            try {
                callback(newState)
            } catch (e: Exception) {
                Log.e(TAG, "Error in state change callback", e)
            }
        }
    }
    
    /**
     * Reset state to idle (used when a session completes or errors)
     */
    fun resetToIdle() {
        Log.i(TAG, "Resetting voice state to IDLE")
        updateState(VoiceState.IDLE)
    }
    
    /**
     * Set error state
     */
    fun setError(message: String) {
        Log.e(TAG, "Voice error: $message")
        updateState(VoiceState.ERROR)
    }
    
    /**
     * Clean up resources
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down VoiceManager")
        
        // Stop the voice processor
        try {
            voiceProcessor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down voice processor", e)
        }
        
        // Clean up speech recognition
        try {
            SpeechRecognitionManager.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognition", e)
        }
        
        // Clear callbacks
        stateChangeCallbacks.clear()
        
        // Reset state
        _voiceState.value = VoiceState.IDLE
    }
    
    /**
     * Voice state enum representing the current state of voice processing
     */
    sealed class VoiceState {
        object IDLE : VoiceState()
        object WAKE_WORD_DETECTED : VoiceState()
        object LISTENING : VoiceState()
        object PROCESSING : VoiceState()
        object SPEAKING : VoiceState()
        object ERROR : VoiceState()
        
        override fun toString(): String {
            return when (this) {
                is IDLE -> "IDLE"
                is WAKE_WORD_DETECTED -> "WAKE_WORD_DETECTED"
                is LISTENING -> "LISTENING"
                is PROCESSING -> "PROCESSING"
                is SPEAKING -> "SPEAKING"
                is ERROR -> "ERROR"
            }
        }
    }
    
    companion object {
        private var instance: VoiceManager? = null
        
        @JvmStatic
        fun initialize(context: Context) {
            if (instance == null) {
                instance = VoiceManager(context.applicationContext)
                instance?.initialize()
            }
        }
        
        @JvmStatic
        fun getInstance(): VoiceManager {
            return instance ?: throw IllegalStateException("VoiceManager is not initialized")
        }
    }
} 