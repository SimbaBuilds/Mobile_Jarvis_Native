package com.cameronhightower.mobilejarvisnative.modules.voice

import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * React Native module to expose voice processing capabilities to JavaScript
 */
class VoiceModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val TAG = "VoiceModule"
    private var voiceManager: VoiceManager? = null
    private var hasListeners = false
    private val stateCallback: (VoiceState) -> Unit = { state -> 
        sendStateChangeEvent(state)
    }
    
    init {
        try {
            // Initialize voice manager
            VoiceManager.initialize(reactContext.applicationContext)
            voiceManager = VoiceManager.getInstance()
            
            // Register for state changes
            voiceManager?.registerStateChangeCallback(stateCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing VoiceModule", e)
        }
    }
    
    override fun getName(): String {
        return "VoiceModule"
    }
    
    /**
     * Add event listener tracking
     */
    @ReactMethod
    fun addListener(eventName: String) {
        hasListeners = true
    }
    
    /**
     * Remove event listener tracking
     */
    @ReactMethod
    fun removeListeners(count: Int) {
        hasListeners = count > 0
    }
    
    /**
     * Start voice recognition manually
     */
    @ReactMethod
    fun startListening(promise: Promise) {
        try {
            val manager = voiceManager ?: throw IllegalStateException("VoiceManager not initialized")
            manager.activateManually()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting listening", e)
            promise.reject("ERROR", "Failed to start listening: ${e.message}")
        }
    }
    
    /**
     * Stop voice recognition
     */
    @ReactMethod
    fun stopListening(promise: Promise) {
        try {
            val manager = voiceManager ?: throw IllegalStateException("VoiceManager not initialized")
            manager.resetToIdle()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listening", e)
            promise.reject("ERROR", "Failed to stop listening: ${e.message}")
        }
    }
    
    /**
     * Interrupt speech
     */
    @ReactMethod
    fun interruptSpeech(promise: Promise) {
        try {
            val manager = voiceManager ?: throw IllegalStateException("VoiceManager not initialized")
            val result = manager.interruptSpeech()
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting speech", e)
            promise.reject("ERROR", "Failed to interrupt speech: ${e.message}")
        }
    }
    
    /**
     * Get current voice state
     */
    @ReactMethod
    fun getVoiceState(promise: Promise) {
        try {
            val manager = voiceManager ?: throw IllegalStateException("VoiceManager not initialized")
            val state = manager.voiceState.value
            promise.resolve(stateToString(state))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting voice state", e)
            promise.reject("ERROR", "Failed to get voice state: ${e.message}")
        }
    }
    
    /**
     * Convert voice state to string representation for React Native
     */
    private fun stateToString(state: VoiceState): String {
        return when (state) {
            is VoiceState.IDLE -> "IDLE"
            is VoiceState.WAKE_WORD_DETECTED -> "WAKE_WORD_DETECTED"
            is VoiceState.LISTENING -> "LISTENING"
            is VoiceState.PROCESSING -> "PROCESSING"
            is VoiceState.SPEAKING -> "SPEAKING"
            is VoiceState.ERROR -> "ERROR"
        }
    }
    
    /**
     * Send state change events to JavaScript
     */
    private fun sendStateChangeEvent(state: VoiceState) {
        if (!hasListeners) return
        
        try {
            val eventData = Arguments.createMap().apply {
                putString("state", stateToString(state))
            }
            
            reactApplicationContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("onVoiceStateChange", eventData)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending state change event", e)
        }
    }
    
    override fun onCatalystInstanceDestroy() {
        try {
            // Unregister state callback
            voiceManager?.unregisterStateChangeCallback(stateCallback)
            
            // Clean up resources
            voiceManager?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error during module cleanup", e)
        }
        
        super.onCatalystInstanceDestroy()
    }
}
