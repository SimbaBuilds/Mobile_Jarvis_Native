package com.anonymous.MobileJarvisNative.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.anonymous.MobileJarvisNative.utils.SpeechRecognitionManager
import com.anonymous.MobileJarvisNative.utils.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * VoiceManager - Unified manager for all voice-related functionality
 * 
 * This class serves as the central coordinator for:
 * - Wake word detection
 * - Speech recognition
 * - Voice processing (Modular or Vapi services)
 * - Text-to-speech output
 */
class VoiceManager private constructor(private val context: Context) {
    private val TAG = "VoiceManager"
    
    // Voice state management
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    // Service status tracking
    private var isSpeechRecognitionInitialized = false
    
    // Voice processor strategy
    private var voiceProcessor: VoiceProcessor
    private var useVapiMode = false
    
    // State tracking
    private var lastWakeWordTimestamp = 0L
    private val WAKE_WORD_DEBOUNCE_MS = 5000L
    private var lastProcessedText: String = ""
    private var noSpeechRetryCount = 0
    private const val MAX_NO_SPEECH_RETRIES = 2
    
    // Callback registry
    private val stateChangeCallbacks = mutableListOf<(VoiceState) -> Unit>()
    
    // Tool handlers
    private val toolHandlers = mutableMapOf<String, (JSONObject) -> String>()
    
    init {
        // Create default processor (Modular)
        voiceProcessor = ModularVoiceProcessor(context)
    }
    
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
        
        // Also initialize Vapi processor to ensure it's ready if needed
        if (voiceProcessor !is VapiVoiceProcessor) {
            try {
                val vapiProcessor = VapiVoiceProcessor(context)
                vapiProcessor.initialize()
            } catch (e: Exception) {
                Log.e(TAG, "Error pre-initializing Vapi processor", e)
            }
        }
    }
    
    /**
     * Set whether to use Vapi mode
     */
    fun setVapiMode(useVapi: Boolean) {
        Log.i(TAG, "Setting Vapi mode to: $useVapi")
        
        if (useVapi == useVapiMode) {
            Log.d(TAG, "Already in requested Vapi state: $useVapi")
            return
        }
        
        useVapiMode = useVapi
        
        // Switch voice processors
        voiceProcessor = if (useVapi) {
            VapiVoiceProcessor(context)
        } else {
            ModularVoiceProcessor(context)
        }
        
        // Initialize the new processor
        voiceProcessor.initialize()
        
        // Start the processor if we're currently in a non-idle state
        if (_voiceState.value != VoiceState.IDLE) {
            voiceProcessor.start()
        }
    }
    
    /**
     * Called when the wake word is detected.
     * Returns true if the wake word detection was processed (not a duplicate).
     */
    fun onWakeWordDetected(timestamp: Long): Boolean {
        // Debounce wake word detection
        val lastWakeWordTime = lastWakeWordTimestamp
        if (lastWakeWordTime != null && timestamp - lastWakeWordTime < WAKE_WORD_DEBOUNCE_MS) {
            Log.d(TAG, "Ignoring duplicate wake word detection")
            return false
        }
        
        lastWakeWordTimestamp = timestamp
        
        Log.i(TAG, "Wake word detected, starting voice processor...")
        
        try {
            // Update state
            _voiceState.value = VoiceState.WAKE_WORD_DETECTED
            
            // Start the appropriate voice processor
            val processor = getVoiceProcessor()
            processor.startListening()
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice processor after wake word: ${e.message}", e)
            showError("Unable to start voice recognition: ${e.message}")
            resetToIdle()
            return false
        }
    }
    
    /**
     * Start listening for speech input
     */
    fun startListening() {
        Log.i(TAG, "Starting speech recognition")
        
        // Update state to LISTENING
        updateState(VoiceState.LISTENING)
        
        // Use SpeechRecognitionManager to start listening
        try {
            SpeechRecognitionManager.setupRecognitionListener { recognizedText ->
                onSpeechRecognized(recognizedText)
            }
            
            SpeechRecognitionManager.startListening(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _voiceState.value = VoiceState.ERROR("Failed to start speech recognition: ${e.message}")
        }
    }
    
    /**
     * Handle recognized speech
     */
    private fun onSpeechRecognized(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Received empty speech recognition result")
            updateState(VoiceState.IDLE)
            return
        }
        
        lastProcessedText = text
        Log.i(TAG, "Speech recognized: \"$text\"")
        
        // Update state to PROCESSING
        updateState(VoiceState.PROCESSING)
        
        // Process with voice processor
        try {
            voiceProcessor.processText(text) { response ->
                if (response.isNotEmpty()) {
                    // Update state to RESPONDING
                    _voiceState.value = VoiceState.RESPONDING(response)
                    
                    // Speak the response
                    voiceProcessor.speak(response) {
                        // Return to idle when done speaking
                        updateState(VoiceState.IDLE)
                    }
                } else {
                    Log.w(TAG, "Received empty response from voice processor")
                    _voiceState.value = VoiceState.ERROR("No response received")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing recognized text", e)
            _voiceState.value = VoiceState.ERROR("Processing error: ${e.message}")
            
            // If using Vapi and it failed, try switching to modular
            if (voiceProcessor is VapiVoiceProcessor) {
                Log.i(TAG, "Switching to modular mode after Vapi processing error")
                setVapiMode(false)
                
                // Try processing with modular
                try {
                    voiceProcessor.processText(text) { response ->
                        if (response.isNotEmpty()) {
                            _voiceState.value = VoiceState.RESPONDING(response)
                            
                            voiceProcessor.speak(response) {
                                updateState(VoiceState.IDLE)
                            }
                        } else {
                            Log.w(TAG, "Received empty response from modular processor")
                            _voiceState.value = VoiceState.ERROR("No response received from modular processor")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing with modular processor too", e)
                    _voiceState.value = VoiceState.ERROR("Modular processing error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Interrupt the current speech playback
     * 
     * @return Boolean indicating if speech was successfully interrupted
     */
    fun interruptSpeech(): Boolean {
        Log.i(TAG, "Interrupting current speech")
        
        // First, check if we're in a responding state
        if (_voiceState.value !is VoiceState.RESPONDING) {
            Log.d(TAG, "No active speech to interrupt (current state: ${_voiceState.value.javaClass.simpleName})")
            return false
        }
        
        var interrupted = false
        
        // Try interrupting with voice processor first
        try {
            interrupted = voiceProcessor.interrupt()
            Log.d(TAG, "Voice processor interrupt result: $interrupted")
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting voice processor speech", e)
        }
        
        // If using fallback mode, also try to interrupt the fallback TTS
        if (!interrupted && (voiceProcessor is ModularVoiceProcessor)) {
            try {
                interrupted = com.anonymous.MobileJarvisNative.agent.modular_voice.FallbackManager.getInstance().interruptSpeech()
                Log.d(TAG, "Fallback TTS interrupt result: $interrupted")
            } catch (e: Exception) {
                Log.e(TAG, "Error interrupting fallback TTS", e)
            }
        }
        
        // Also use TextToSpeechManager as a fallback option
        if (!interrupted) {
            try {
                TextToSpeechManager.stop()
                Log.d(TAG, "Stopped TextToSpeechManager")
                interrupted = true
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping TextToSpeechManager", e)
            }
        }
        
        // If we successfully interrupted, update the state
        if (interrupted) {
            Log.i(TAG, "Successfully interrupted speech")
            updateState(VoiceState.IDLE)
        }
        
        return interrupted
    }
    
    /**
     * Register a callback for state changes
     */
    fun registerStateChangeCallback(callback: (VoiceState) -> Unit) {
        stateChangeCallbacks.add(callback)
        Log.d(TAG, "State change callback registered, total callbacks: ${stateChangeCallbacks.size}")
        
        // Immediately notify with current state
        callback(voiceState.value)
    }
    
    /**
     * Unregister a previously registered state change callback
     */
    fun unregisterStateChangeCallback(callback: (VoiceState) -> Unit) {
        stateChangeCallbacks.remove(callback)
        Log.d(TAG, "State change callback unregistered, remaining callbacks: ${stateChangeCallbacks.size}")
    }
    
    /**
     * Register a tool handler
     */
    fun registerToolHandler(toolName: String, handler: (JSONObject) -> String) {
        toolHandlers[toolName] = handler
        Log.d(TAG, "Registered tool handler for $toolName")
    }
    
    /**
     * Get a tool handler
     */
    fun getToolHandler(toolName: String): ((JSONObject) -> String)? {
        return toolHandlers[toolName]
    }
    
    /**
     * Update the voice state and notify registered callbacks
     */
    private fun updateState(newState: VoiceState) {
        Log.d(TAG, "Updating voice state from ${_voiceState.value.javaClass.simpleName} to ${newState.javaClass.simpleName}")
        _voiceState.value = newState
        
        // Notify all registered callbacks
        for (callback in stateChangeCallbacks) {
            try {
                callback(newState)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying state change callback", e)
            }
        }
    }
    
    /**
     * Clean up resources
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down VoiceManager")
        
        // Reset state
        updateState(VoiceState.IDLE)
        
        // Shutdown voice processor
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
        
        // Clear tool handlers
        toolHandlers.clear()
    }
    
    /**
     * Handle the case when no speech was detected
     */
    fun handleNoSpeechDetected() {
        Log.d(TAG, "No speech detected")
        
        // Increment retry counter
        noSpeechRetryCount++
        
        if (noSpeechRetryCount < MAX_NO_SPEECH_RETRIES) {
            // Show message to the user
            showMessage("I didn't hear anything. Listening again...")
            
            // Try again after a short delay
            coroutineScope.launch {
                delay(500) // Short delay before retrying
                
                try {
                    Log.d(TAG, "Retrying speech recognition (attempt $noSpeechRetryCount)")
                    val processor = getVoiceProcessor()
                    processor.startListening()
                } catch (e: Exception) {
                    Log.e(TAG, "Error retrying speech recognition: ${e.message}", e)
                    showError("Unable to restart voice recognition")
                    resetToIdle()
                }
            }
        } else {
            // Max retries reached, reset to idle
            Log.d(TAG, "Maximum retry attempts reached ($MAX_NO_SPEECH_RETRIES), returning to idle")
            showMessage("I didn't hear anything. Please try again later.")
            resetToIdle()
            noSpeechRetryCount = 0 // Reset counter
        }
    }
    
    /**
     * Voice states for the state machine
     */
    sealed class VoiceState {
        object IDLE : VoiceState()
        object WAKE_WORD_DETECTED : VoiceState()
        object LISTENING : VoiceState()
        object PROCESSING : VoiceState()
        data class RESPONDING(val message: String) : VoiceState()
        data class ERROR(val message: String) : VoiceState()
    }
    
    companion object {
        private var instance: VoiceManager? = null
        
        fun init(context: Context) {
            if (instance == null) {
                Log.i("VoiceManager", "Creating new VoiceManager instance")
                instance = VoiceManager(context.applicationContext)
                instance?.initialize()
            }
        }
        
        fun getInstance(): VoiceManager {
            return instance ?: throw IllegalStateException(
                "VoiceManager not initialized. Call init() first."
            )
        }
    }
} 