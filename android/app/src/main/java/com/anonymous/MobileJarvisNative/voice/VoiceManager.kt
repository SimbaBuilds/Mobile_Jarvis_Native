package com.anonymous.MobileJarvisNative.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.content.Intent
import android.os.Bundle
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
    
    // Speech Recognition properties
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastRecognitionStartTime = 0L
    private var speechRecognitionRetryCount = 0
    
    // Callback registry
    private val stateChangeCallbacks = mutableListOf<(VoiceState) -> Unit>()
    
    // Tool handlers
    private val toolHandlers = mutableMapOf<String, (JSONObject) -> String>()
    
    // Coroutine scope
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    init {
        // Create default processor (Modular)
        voiceProcessor = ModularVoiceProcessor(context)
    }
    
    /**
     * Initialize required services
     */
    fun initialize() {
        Log.i(TAG, "Initializing VoiceManager services")
        
        // Initialize speech recognition
        initializeSpeechRecognition()
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
     * Initialize speech recognition
     */
    private fun initializeSpeechRecognition() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                isSpeechRecognitionInitialized = true
                Log.d(TAG, "Speech recognizer initialized")
            } else {
                Log.e(TAG, "Speech recognition not available on this device")
                isSpeechRecognitionInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speech recognizer", e)
            isSpeechRecognitionInitialized = false
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
        if (timestamp - lastWakeWordTimestamp < WAKE_WORD_DEBOUNCE_MS) {
            Log.d(TAG, "Ignoring duplicate wake word detection")
            return false
        }
        
        lastWakeWordTimestamp = timestamp
        
        Log.i(TAG, "Wake word detected, starting voice processor...")
        
        try {
            // Update state
            _voiceState.value = VoiceState.WAKE_WORD_DETECTED
            
            // Start the appropriate voice processor
            val processor = voiceProcessor
            
            // Start listening for speech
            startListening()
            
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
        
        // Check if already listening or if it's too soon
        if (isListening) {
            Log.d(TAG, "Already listening, ignoring start request")
            return
        }
        
        val now = System.currentTimeMillis()
        if (now - lastRecognitionStartTime < RECOGNITION_DEBOUNCE_MS) {
            Log.d(TAG, "Ignoring start request due to debounce period")
            return
        }
        lastRecognitionStartTime = now
        
        // Update state to LISTENING
        updateState(VoiceState.LISTENING)
        
        // Create recognition intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
        }
        
        try {
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _voiceState.value = VoiceState.ERROR("Failed to start speech recognition: ${e.message}")
            isListening = false
        }
    }
    
    /**
     * Create a RecognitionListener instance
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                isListening = true
                speechRecognitionRetryCount = 0 // Reset retry count on successful start
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Not used
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Not used
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                isListening = false
            }
            
            override fun onError(error: Int) {
                val errorMessage = getSpeechRecognitionErrorMessage(error)
                Log.e(TAG, "Speech recognition error: $errorMessage")
                
                // Reset state flags
                isListening = false
                
                // Handle permission error by retrying
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS && 
                   speechRecognitionRetryCount < MAX_SPEECH_RECOGNITION_RETRY_COUNT) {
                    speechRecognitionRetryCount++
                    Log.w(TAG, "Permission error, will retry speech recognition (attempt $speechRecognitionRetryCount)")
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        startListening()
                    }, 1000)
                    return
                }
                
                // Special handling for "No speech detected" errors
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    handleNoSpeechDetected()
                    return
                }
                
                // Otherwise update state to error
                _voiceState.value = VoiceState.ERROR("Speech recognition error: $errorMessage")
            }
            
            override fun onResults(results: Bundle?) {
                isListening = false
                
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0) ?: ""
                
                if (text.isNotBlank()) {
                    onSpeechRecognized(text)
                } else {
                    handleNoSpeechDetected()
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Not used in this implementation
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used in this implementation
            }
        }
    }
    
    /**
     * Get a string description of speech recognition error code
     */
    private fun getSpeechRecognitionErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error $errorCode"
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
     * Display an error message
     */
    private fun showError(message: String) {
        Log.e(TAG, "Error: $message")
        _voiceState.value = VoiceState.ERROR(message)
    }
    
    /**
     * Display a message to the user
     */
    private fun showMessage(message: String) {
        Log.i(TAG, "Message: $message")
        _voiceState.value = VoiceState.RESPONDING(message)
    }
    
    /**
     * Reset to idle state
     */
    private fun resetToIdle() {
        updateState(VoiceState.IDLE)
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
            speechRecognizer?.destroy()
            speechRecognizer = null
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
                    startListening()
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
        
        // Constants moved to companion object
        const val MAX_NO_SPEECH_RETRIES = 2
        const val RECOGNITION_DEBOUNCE_MS = 1000L
        const val MAX_SPEECH_RECOGNITION_RETRY_COUNT = 3
        
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