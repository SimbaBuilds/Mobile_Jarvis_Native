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
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.anonymous.MobileJarvisNative.utils.Constants
import com.anonymous.MobileJarvisNative.ConfigManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * VoiceManager - Unified manager for all voice-related functionality
 * 
 * This class serves as the central coordinator for:
 * - Wake word detection
 * - Speech recognition
 * - Voice processing (Modular Services)
 * - Text-to-speech output
 */
class VoiceManager private constructor() {
    private val TAG = "VoiceManager"
    
    // Voice state management
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    // Service status tracking
    private var isSpeechRecognitionInitialized = false
    
    // Voice processor strategy
    private lateinit var voiceProcessor: VoiceProcessor
    private var useVapiMode = false
    
    // State tracking
    private var lastWakeWordTimestamp = 0L
    private var lastProcessedText: String = ""
    private var noSpeechRetryCount = 0
    
    // Speech Recognition properties
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastRecognitionStartTime = 0L
    private var speechRecognitionRetryCount = 0
    
    // Config manager
    private lateinit var configManager: ConfigManager
    
    // Callback registry
    private val stateChangeCallbacks = mutableListOf<(VoiceState) -> Unit>()
    
    // Tool handlers
    private val toolHandlers = mutableMapOf<String, (JSONObject) -> String>()
    
    // Coroutine scope
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Context reference
    private lateinit var context: Context
    
    // Additional properties for Whisper client and Deepgram
    private lateinit var whisperClient: WhisperClient
    private lateinit var deepgramClient: DeepgramClient

    companion object {
        @Volatile
        private var instance: VoiceManager? = null
        
        // Constants
        private const val RECOGNITION_DEBOUNCE_MS = 3000L
        private const val MAX_SPEECH_RECOGNITION_RETRY_COUNT = 3
        private var MAX_NO_SPEECH_RETRIES = 2 // Will be overridden from config
        
        fun getInstance(): VoiceManager {
            return instance ?: synchronized(this) {
                instance ?: VoiceManager().also { instance = it }
            }
        }
    }
    
    /**
     * Initialize with context
     */
    fun initialize(context: Context) {
        this.context = context
        
        // Initialize ConfigManager
        configManager = ConfigManager.getInstance()
        
        // Update constants from config
        MAX_NO_SPEECH_RETRIES = configManager.getMaxNoSpeechRetries()
        
        // Create default processor (Modular)
        voiceProcessor = ModularVoiceProcessor(context)
        
        // Initialize services
        initialize()
        
        // Initialize Whisper client
        whisperClient = WhisperClient(context)
        
        // Initialize Deepgram client for TTS
        deepgramClient = DeepgramClient(context)
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
                // Set the recognition listener
                speechRecognizer?.setRecognitionListener(createRecognitionListener())
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
     * Returns true if the wake word detection was processed.
     */
    fun onWakeWordDetected(timestamp: Long): Boolean {
        // Only process wake word if we're in IDLE state
        if (_voiceState.value !is VoiceState.IDLE) {
            Log.d(TAG, "Ignoring wake word detection - conversation already in progress")
            return false
        }
        
        lastWakeWordTimestamp = timestamp
        Log.i(TAG, "Wake word detected, stopping wake word detection but keeping listening on...")
        
        try {
            // Update state - this will automatically pause wake word detection
            updateState(VoiceState.WAKE_WORD_DETECTED)
            
            // Explicitly tell the WakeWordService to pause but keep mic active
            val intent = Intent("com.anonymous.MobileJarvisNative.PAUSE_WAKE_WORD_KEEP_LISTENING")
            context.sendBroadcast(intent)
            Log.d(TAG, "Sent broadcast to pause wake word detection but keep mic active")
            
            // Initialize Whisper client for speech recognition
            initializeWhisperClient()
            
            // Play "Sir?" response using Deepgram TTS
            playWakeWordResponse()
            
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
     * Initialize Whisper client for speech recognition
     */
    private fun initializeWhisperClient() {
        Log.i(TAG, "Initializing OpenAI Whisper client")
        try {
            whisperClient.initialize()
            Log.d(TAG, "Whisper client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Whisper client: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Play wake word response sound
     */
    private fun playWakeWordResponse() {
        Log.d(TAG, "Attempting to play wake word response...")
        
        // Use local TTS by default to avoid delays
        try {
            TextToSpeechManager.speak("Sir?")
            Log.i(TAG, "Played wake word response using local TTS")
            
            // Try Deepgram in the background for next use
            prepareDeepgramForFutureUse()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing wake word response: ${e.message}", e)
        }
    }
    
    /**
     * Prepare Deepgram for future use without blocking current flow
     */
    private fun prepareDeepgramForFutureUse() {
        coroutineScope.launch {
            try {
                // Initialize Deepgram client if needed
                if (!::deepgramClient.isInitialized) {
                    deepgramClient = DeepgramClient(context)
                }
                deepgramClient.initialize()
                Log.d(TAG, "Deepgram client initialized for future use")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Deepgram for future use: ${e.message}", e)
            }
        }
    }
    
    /**
     * Start listening for speech input
     */
    fun startListening() {
        Log.d(TAG, "startListening() called. Attempting to start speech recognition...")
        
        // Always ensure wake word detection is paused when actively listening
        try {
            val intent = Intent("com.anonymous.MobileJarvisNative.PAUSE_WAKE_WORD_KEEP_LISTENING")
            context.sendBroadcast(intent)
            Log.d(TAG, "Sent broadcast to pause wake word detection during listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending pause wake word broadcast: ${e.message}", e)
        }
        
        // Check if we're already listening to avoid duplicate requests
        if (isListening) {
            Log.d(TAG, "Already listening, ignoring startListening() call")
            return
        }
        
        // Check if speechRecognizer is still valid and reinitialize if needed
        if (speechRecognizer == null || !isSpeechRecognitionInitialized) {
            Log.w(TAG, "Speech recognizer was null or not initialized, reinitializing...")
            initializeSpeechRecognition()
        }
        
        try {
            // Update state before starting recognition
            isListening = true
            updateState(VoiceState.LISTENING)
            
            // Start the actual speech recognizer
            speechRecognizer?.startListening(createRecognizerIntent())
            Log.i(TAG, "SpeechRecognizer started listening.")
            
            // Set timestamp to detect potential hangs
            lastRecognitionStartTime = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}", e)
            isListening = false
            _voiceState.value = VoiceState.ERROR("Failed to start speech recognition: ${e.message}")
        }
    }
    
    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        if (isListening) {
            Log.d(TAG, "stopListening() called. Stopping speech recognition...")
            isListening = false
            _voiceState.value = VoiceState.IDLE
            speechRecognizer?.stopListening()
            Log.i(TAG, "SpeechRecognizer stopped listening.")
        }
    }
    
    /**
     * Create speech recognizer intent
     */
    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            
            // Get timing parameters from ConfigManager
            val minLengthMs = configManager.getSpeechRecognitionMinimumLengthMs()
            val completeSilenceMs = configManager.getSpeechRecognitionCompleteSilenceMs()
            val possibleSilenceMs = configManager.getSpeechRecognitionPossibleSilenceMs()
            
            // Log the values regardless of whether they'll be used
            Log.d(TAG, "Speech recognition parameters: minLength=$minLengthMs, " +
                      "completeSilence=$completeSilenceMs, possibleSilence=$possibleSilenceMs")
            
            // Only apply custom parameters if explicitly enabled in config
            if (configManager.useCustomRecognizerParams()) {
                Log.i(TAG, "Applying custom timing parameters to SpeechRecognizer")
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, minLengthMs)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, completeSilenceMs)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, possibleSilenceMs)
            } else {
                Log.i(TAG, "Using Android default timing parameters for SpeechRecognizer")
                // Use Android defaults for RecognizerIntent parameters, but still use our
                // custom timing values for the rest of the voice processing pipeline
            }
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
        
        // Log the start of API processing
        Log.d(TAG, "Starting API processing for recognized text")
        
        // Process with voice processor
        try {
            Log.d(TAG, "Sending text to voice processor for processing")
            voiceProcessor.processText(text) { response ->
                Log.d(TAG, "Received response from voice processor, length: ${response.length} chars")
                
                if (response.isNotEmpty()) {
                    // Update state to RESPONDING
                    _voiceState.value = VoiceState.RESPONDING(response)
                    Log.i(TAG, "Processing complete, responding to user")
                    
                    // Speak the response
                    voiceProcessor.speak(response) {
                        // Don't return to idle when done speaking, set to LISTENING instead
                        Log.i(TAG, "TTS complete, setting state to LISTENING to continue conversation")
                        
                        // Add a short delay for better transition
                        Handler(Looper.getMainLooper()).postDelayed({
                            updateState(VoiceState.LISTENING)
                        }, 300)
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
                                // Don't return to idle when done speaking, set to LISTENING instead
                                Log.i(TAG, "TTS complete (from modular fallback), setting state to LISTENING to continue conversation")
                                
                                // Add a short delay for better transition
                                Handler(Looper.getMainLooper()).postDelayed({
                                    updateState(VoiceState.LISTENING)
                                }, 300)
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
        
        // If we successfully interrupted, update the state to LISTENING
        if (interrupted) {
            Log.i(TAG, "Successfully interrupted speech, changing to LISTENING state")
            
            // Add a short delay before switching to listening state for better UX
            Handler(Looper.getMainLooper()).postDelayed({
                updateState(VoiceState.LISTENING)
                
                // Start listening for speech input
                try {
                    startListening()
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting listening after interruption: ${e.message}", e)
                }
            }, 300)
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
    internal fun updateState(newState: VoiceState) {
        val oldState = _voiceState.value
        Log.d(TAG, "Voice state transition: ${oldState.javaClass.simpleName} -> ${newState.javaClass.simpleName}")
        
        // If transitioning to PROCESSING state, log details about speech recognition
        if (newState is VoiceState.PROCESSING && oldState is VoiceState.LISTENING) {
            Log.i(TAG, "Speech recognition completed successfully, processing command")
        }
        
        // If transitioning from PROCESSING to RESPONDING, log success
        if (oldState is VoiceState.PROCESSING && newState is VoiceState.RESPONDING) {
            Log.i(TAG, "Command processed successfully, generating response")
        }
        
        // If transitioning to ERROR state, log detailed error information
        if (newState is VoiceState.ERROR) {
            Log.e(TAG, "Error in voice processing: ${newState.message}")
            if (oldState is VoiceState.PROCESSING) {
                Log.e(TAG, "Error occurred during command processing")
            } else if (oldState is VoiceState.LISTENING) {
                Log.e(TAG, "Error occurred during speech recognition")
            }
        }
        
        _voiceState.value = newState
        
        // Manage wake word detection based on state
        when (newState) {
            is VoiceState.IDLE -> {
                // Only re-enable wake word detection when returning to IDLE
                Log.d(TAG, "Conversation completed, re-enabling wake word detection")
                try {
                    voiceProcessor.start()
                } catch (e: Exception) {
                    Log.e(TAG, "Error re-enabling wake word detection", e)
                }
            }
            is VoiceState.WAKE_WORD_DETECTED,
            is VoiceState.LISTENING,
            is VoiceState.PROCESSING,
            is VoiceState.RESPONDING,
            is VoiceState.SPEAKING,
            is VoiceState.ERROR -> {
                // Ensure wake word detection is paused during active conversation
                Log.d(TAG, "Active conversation state (${newState.javaClass.simpleName}), pausing wake word detection")
                try {
                    voiceProcessor.stop()
                    
                    // For LISTENING state, make sure speech recognizer is active
                    if (newState is VoiceState.LISTENING && !isListening) {
                        Log.d(TAG, "LISTENING state detected but isListening=false, reactivating speech recognizer")
                        isListening = true
                        speechRecognizer?.startListening(createRecognizerIntent())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error pausing wake word detection", e)
                }
            }
        }
        
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
            
            // Try again after a customizable delay
            coroutineScope.launch {
                // Get configurable delay from ConfigManager
                val retryDelayMs = configManager.getSpeechRetryDelayMs().toLong()
                Log.d(TAG, "Will retry speech recognition after $retryDelayMs ms")
                delay(retryDelayMs)
                
                try {
                    Log.d(TAG, "Retrying speech recognition (attempt $noSpeechRetryCount)")
                    startListening()
                    
                    // Log listening state for debugging
                    Log.d(TAG, "Speech recognition restarted, listening state: $isListening, voice state: ${_voiceState.value.javaClass.simpleName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error retrying speech recognition: ${e.message}", e)
                    showError("Unable to restart voice recognition")
                    resetToIdle()
                }
            }
        } else {
            // Max retries reached, reset to idle
            Log.d(TAG, "Maximum retry attempts reached ($MAX_NO_SPEECH_RETRIES), resetting to idle")
            showMessage("I didn't hear anything. Please try saying 'Jarvis' again when you're ready.")
            
            // Reset counter
            noSpeechRetryCount = 0
            
            // Allow message to be spoken before resetting
            coroutineScope.launch {
                // Get configurable delay from ConfigManager
                val finalMessageDelayMs = configManager.getSpeechFinalMessageDelayMs().toLong()
                Log.d(TAG, "Will reset to idle after $finalMessageDelayMs ms")
                delay(finalMessageDelayMs)
                resetToIdle()
            }
        }
    }
    
    /**
     * Process speech recognition result
     */
    private fun processSpeechResult(text: String) {
        if (text.isEmpty()) {
            Log.d(TAG, "Empty speech result, ignoring")
            return
        }
        
        Log.i(TAG, "Processing speech result: $text")
        lastProcessedText = text
        
        try {
            // Update state to processing
            updateState(VoiceState.PROCESSING)
            
            // Send event to RN with the recognized text
            sendSpeechResultToReactNative(text)
            
            // Don't reset back to idle after sending to RN
            // Let React Native drive the state changes
            Log.d(TAG, "Speech result sent to RN, maintaining PROCESSING state")
            // Note: React Native will handle further processing and state management
        } catch (e: Exception) {
            Log.e(TAG, "Error processing speech: ${e.message}", e)
            showError("Error processing speech: ${e.message}")
        }
    }
    
    /**
     * Send speech result to React Native
     */
    private fun sendSpeechResultToReactNative(text: String) {
        try {
            val params = JSONObject()
            params.put("text", text)
            
            Handler(Looper.getMainLooper()).post {
                try {
                    // Use ApplicationContext instead of ReactApplicationContext directly
                    val currentContext = context.applicationContext
                    if (currentContext is com.facebook.react.bridge.ReactContext) {
                        currentContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                            ?.emit(Constants.Actions.SPEECH_RESULT, params.toString())
                        Log.d(TAG, "Speech result sent to React Native")
                    } else {
                        Log.e(TAG, "Context is not a ReactContext, cannot send event to React Native")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending event to React Native: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending speech result to React Native: ${e.message}", e)
        }
    }
    
    /**
     * Process incoming audio data for speech recognition
     */
    fun processAudioData(audioData: ByteArray, size: Int) {
        if (_voiceState.value != VoiceState.LISTENING) {
            // Only process audio data when we're actively listening
            return
        }
        
        try {
            Log.d(TAG, "Processing audio data of size: $size bytes")
            
            // Save audio data to temporary file for Whisper processing
            val tempFile = File.createTempFile("whisper_audio_", ".wav", context.cacheDir)
            FileOutputStream(tempFile).use { fos ->
                // Write WAV header
                writeWavHeader(fos, size)
                
                // Write audio data
                fos.write(audioData, 0, size)
                fos.flush()
            }
            
            Log.d(TAG, "Audio saved to temporary file: ${tempFile.absolutePath}")
            
            // Process audio file with Whisper
            coroutineScope.launch {
                try {
                    // Update state to processing
                    updateState(VoiceState.PROCESSING)
                    
                    // Transcribe the audio using Whisper
                    val transcript = whisperClient.transcribeAudio(tempFile)
                    
                    if (transcript.isNotEmpty()) {
                        Log.i(TAG, "Whisper transcription successful: '$transcript'")
                        processSpeechResult(transcript)
                    } else {
                        Log.w(TAG, "Empty transcription result from Whisper")
                        // Reset to listening state to continue capturing audio
                        updateState(VoiceState.LISTENING)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error transcribing audio with Whisper: ${e.message}", e)
                    showError("Error transcribing speech: ${e.message}")
                } finally {
                    // Delete the temporary file
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio data: ${e.message}", e)
        }
    }
    
    /**
     * Write WAV header for the audio file
     */
    private fun writeWavHeader(outputStream: OutputStream, audioDataSize: Int) {
        // Standard WAV header for 16-bit PCM, 16kHz, mono
        val sampleRate = 16000
        val channels = 1
        val bitsPerSample = 16
        
        try {
            // RIFF header
            outputStream.write("RIFF".toByteArray()) // ChunkID
            writeInt(outputStream, 36 + audioDataSize) // ChunkSize
            outputStream.write("WAVE".toByteArray()) // Format
            
            // fmt subchunk
            outputStream.write("fmt ".toByteArray()) // Subchunk1ID
            writeInt(outputStream, 16) // Subchunk1Size (16 for PCM)
            writeShort(outputStream, 1) // AudioFormat (1 for PCM)
            writeShort(outputStream, channels) // NumChannels
            writeInt(outputStream, sampleRate) // SampleRate
            writeInt(outputStream, sampleRate * channels * bitsPerSample / 8) // ByteRate
            writeShort(outputStream, channels * bitsPerSample / 8) // BlockAlign
            writeShort(outputStream, bitsPerSample) // BitsPerSample
            
            // data subchunk
            outputStream.write("data".toByteArray()) // Subchunk2ID
            writeInt(outputStream, audioDataSize) // Subchunk2Size
        } catch (e: Exception) {
            Log.e(TAG, "Error writing WAV header: ${e.message}", e)
        }
    }
    
    /**
     * Helper method to write an integer to output stream in little-endian format
     */
    private fun writeInt(outputStream: OutputStream, value: Int) {
        outputStream.write(value and 0xFF)
        outputStream.write(value shr 8 and 0xFF)
        outputStream.write(value shr 16 and 0xFF)
        outputStream.write(value shr 24 and 0xFF)
    }
    
    /**
     * Helper method to write a short to output stream in little-endian format
     */
    private fun writeShort(outputStream: OutputStream, value: Int) {
        outputStream.write(value and 0xFF)
        outputStream.write(value shr 8 and 0xFF)
    }
    
    /**
     * Process a test phrase directly (for debugging)
     * This bypasses the speech recognition system and directly processes a text input
     * 
     * @param testPhrase The phrase to process
     * @return Boolean indicating success
     */
    fun processTestPhrase(testPhrase: String): Boolean {
        if (testPhrase.isBlank()) {
            Log.w(TAG, "Cannot process empty test phrase")
            return false
        }
        
        Log.i(TAG, "Processing test phrase: \"$testPhrase\"")
        
        try {
            // Update state to simulate wake word and listening
            updateState(VoiceState.WAKE_WORD_DETECTED)
            
            // Brief delay to simulate state transition
            Handler(Looper.getMainLooper()).postDelayed({
                // Call onSpeechRecognized directly with the test phrase
                onSpeechRecognized(testPhrase)
            }, 300)
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing test phrase: ${e.message}", e)
            return false
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
        object SPEAKING : VoiceState()
    }
} 