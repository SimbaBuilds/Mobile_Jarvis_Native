package com.anonymous.MobileJarvisNative.voice

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.anonymous.MobileJarvisNative.utils.Constants

/**
 * Bridge module for exposing Voice functionality to React Native
 */
class VoiceModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val TAG = "VoiceModule"
    private val voiceManager = VoiceManager.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        voiceManager.initialize(reactContext)
    }

    override fun getName(): String {
        return "VoiceModule"
    }

    /**
     * Start listening for voice input
     */
    @ReactMethod
    fun startListening(promise: Promise) {
        Log.d(TAG, "startListening called from JS")
        try {
            // Ensure speech recognition is initialized
            ensureSpeechRecognitionInitialized()
            
            voiceManager.startListening()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting listening", e)
            promise.reject("ERR_VOICE_START", e.message, e)
        }
    }

    /**
     * Ensure speech recognition is initialized
     */
    private fun ensureSpeechRecognitionInitialized() {
        Log.d(TAG, "Ensuring speech recognition is initialized")
        // Re-initialize voice manager
        voiceManager.initialize()
    }

    /**
     * Stop listening for voice input
     */
    @ReactMethod
    fun stopListening(promise: Promise) {
        Log.d(TAG, "stopListening called from JS")
        try {
            voiceManager.stopListening()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listening", e)
            promise.reject("ERR_VOICE_STOP", e.message, e)
        }
    }

    /**
     * Interrupt any ongoing speech
     */
    @ReactMethod
    fun interruptSpeech(promise: Promise) {
        Log.d(TAG, "interruptSpeech called from JS")
        try {
            voiceManager.interruptSpeech()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting speech", e)
            promise.reject("ERR_VOICE_INTERRUPT", e.message, e)
        }
    }

    /**
     * Get the current voice state
     */
    @ReactMethod
    fun getVoiceState(promise: Promise) {
        try {
            val state = voiceManager.voiceState.value
            promise.resolve(state.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting voice state", e)
            promise.reject("ERR_VOICE_STATE", e.message, e)
        }
    }

    /**
     * Add a listener for voice state changes
     */
    @ReactMethod
    fun addListener(eventName: String) {
        // Required for RN built in Event Emitter
        if (eventName == Constants.Actions.VOICE_STATE_CHANGE) {
            // Set up the state flow collector if not already set up
            setupStateFlowListener()
        }
    }

    /**
     * Remove listeners
     */
    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for RN built in Event Emitter
        // Real removal happens in VoiceService class
    }

    /**
     * Initialize state flow listener
     */
    private fun setupStateFlowListener() {
        voiceManager.voiceState
            .onEach { state ->
                // Send the state update to JS
                sendEvent(Constants.Actions.VOICE_STATE_CHANGE, mapOf("state" to state.toString()))
            }
            .launchIn(coroutineScope)
    }

    /**
     * Send an event to JavaScript
     */
    private fun sendEvent(eventName: String, params: Map<String, Any>) {
        val writableMap = params.toWritableMap()
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, writableMap)
    }

    // Helper extension function to convert Map<String, Any> to WritableMap
    private fun Map<String, Any>.toWritableMap(): WritableMap {
        val writableMap = Arguments.createMap()
        for ((key, value) in this) {
            when (value) {
                is String -> writableMap.putString(key, value)
                is Int -> writableMap.putInt(key, value)
                is Double -> writableMap.putDouble(key, value)
                is Boolean -> writableMap.putBoolean(key, value)
                is Float -> writableMap.putDouble(key, value.toDouble())
                is Long -> writableMap.putDouble(key, value.toDouble())
                else -> writableMap.putString(key, value.toString())
            }
        }
        return writableMap
    }

    /**
     * Speak a response using TTS
     */
    @ReactMethod
    fun speakResponse(text: String, promise: Promise) {
        Log.d(TAG, "speakResponse called from JS with text: $text")
        try {
            coroutineScope.launch {
                try {
                    // Update voice state to speaking
                    Log.i(TAG, "Setting voice state to SPEAKING")
                    voiceManager.updateState(VoiceManager.VoiceState.SPEAKING)
                    
                    // Use Deepgram for TTS
                    val deepgramClient = DeepgramClient(reactContext)
                    deepgramClient.initialize()
                    Log.i(TAG, "Speaking response via Deepgram")
                    deepgramClient.speak(text)
                    
                    // After speaking is complete, reset state
                    Log.i(TAG, "Speaking complete, setting state to LISTENING directly instead of IDLE")
                    
                    // Add a small delay before starting to listen again
                    Log.i(TAG, "Waiting 800ms before starting listening")
                    kotlinx.coroutines.delay(800)
                    
                    // Ensure we don't go to IDLE which would trigger wake word detection
                    // Instead go directly to LISTENING state
                    voiceManager.updateState(VoiceManager.VoiceState.LISTENING)
                    
                    Log.i(TAG, "AUTO-RESTART: Response spoken, automatically setting state to LISTENING")
                    try {
                        // Explicitly start listening after the state change to avoid race conditions
                        kotlinx.coroutines.delay(200)
                        voiceManager.startListening()
                        Log.i(TAG, "AUTO-RESTART: Successfully started listening")
                    } catch (startError: Exception) {
                        Log.e(TAG, "AUTO-RESTART: Failed to start listening", startError)
                        // Try one more time after a short delay
                        kotlinx.coroutines.delay(500)
                        try {
                            Log.i(TAG, "AUTO-RESTART: Retrying start listening")
                            voiceManager.startListening()
                            Log.i(TAG, "AUTO-RESTART: Successfully started listening on retry")
                        } catch (retryError: Exception) {
                            Log.e(TAG, "AUTO-RESTART: Failed to start listening on retry", retryError)
                            // If we still can't start listening, at least set the state back to IDLE
                            // so the system isn't stuck in an invalid state
                            Log.i(TAG, "Setting voice state to IDLE after failed restarts")
                            voiceManager.updateState(VoiceManager.VoiceState.IDLE)
                        }
                    }
                    
                    promise.resolve(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in speakResponse coroutine", e)
                    // Try to get back to a good state
                    try {
                        Log.i(TAG, "Setting voice state to IDLE after error")
                        voiceManager.updateState(VoiceManager.VoiceState.IDLE)
                    } catch (stateError: Exception) {
                        Log.e(TAG, "Failed to update state after error", stateError)
                    }
                    promise.reject("ERR_TTS", e.message, e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking response", e)
            promise.reject("ERR_TTS", e.message, e)
        }
    }
} 